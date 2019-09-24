/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.withdrawalservice

import com.d3.commons.model.D3ErrorException
import com.d3.commons.model.IrohaCredential
import com.d3.commons.service.RollbackService
import com.d3.commons.service.WithdrawalFinalizationDetails
import com.d3.commons.service.WithdrawalFinalizer
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.FEE_DESCRIPTION
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.eth.provider.EthTokensProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging
import java.math.BigDecimal

/**
 * Implementation of Withdrawal Service
 */
class WithdrawalServiceImpl(
    withdrawalServiceConfig: WithdrawalServiceConfig,
    private val credential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val queryHelper: IrohaQueryHelper,
    private val irohaHandler: Observable<SideChainEvent.IrohaEvent>,
    private val tokensProvider: EthTokensProvider,
    private val proofCollector: ProofCollector
) : WithdrawalService {

    private val irohaConsumer: IrohaConsumer by lazy { IrohaConsumerImpl(credential, irohaAPI) }

    init {
        logger.info { "Init withdrawal service, irohaCredentials = ${credential.accountId}, notaryAccount = ${credential.accountId}'" }
    }

    private val rollbackService = RollbackService(irohaConsumer)

    private val withdrawalFinalizer =
        WithdrawalFinalizer(irohaConsumer, withdrawalServiceConfig.withdrawalBillingAccount)

    /**
     * Handle IrohaEvent
     * @param irohaEvent - iroha event
     * @return withdrawal service output event or exception
     */
    override fun onIrohaEvent(irohaEvent: SideChainEvent.IrohaEvent): Result<List<WithdrawalServiceOutputEvent>, Exception> {
        when (irohaEvent) {
            is SideChainEvent.IrohaEvent.SideChainTransfer -> {
                logger.info { "Iroha transfer event to ${irohaEvent.dstAccount}, expected ${credential.accountId}" }

                if (irohaEvent.dstAccount == credential.accountId) {
                    logger.info { "Withdrawal event" }
                    return proofCollector.collectProofForWithdrawal(irohaEvent)
                        .fanout { tokensProvider.isIrohaAnchored(irohaEvent.asset) }
                        .map { (proof, isIrohaAnchored) ->
                            listOf(WithdrawalServiceOutputEvent.EthRefund(proof, isIrohaAnchored))
                        }
                }

                return Result.of { emptyList<WithdrawalServiceOutputEvent>() }
            }
        }
        return Result.error(Exception("Wrong event type or wrong destination account"))
    }

    /**
     * Relay events to consumer
     */
    override fun output(): Observable<Result<List<WithdrawalServiceOutputEvent>, Exception>> {
        return irohaHandler
            .map {
                onIrohaEvent(it)
            }
    }

    /**
     * Performs rollback. Return all transferred assets.
     */
    override fun returnIrohaAssets(event: WithdrawalServiceOutputEvent): Result<Unit, Exception> {
        logger.info("Withdrawal rollback initiated for Iroha tx ${event}")
        return getWithdrawalDetails(event)
            .flatMap { withdrawalDetails ->
                rollbackService.rollback(withdrawalDetails, "Ethereum rollback")
            }.map { hash ->
                logger.info("Successfully sent rollback transaction to Iroha, hash: $hash")
            }
    }

    /**
     * Finalize withdrawal:
     * 1) Subtract asset
     * 2) send fee to billing account
     *
     * @param event - withdrawal event
     * @result hash of finalization transaction in Iroha
     */
    override fun finalizeWithdrawal(event: WithdrawalServiceOutputEvent): Result<String, Exception> =
        getWithdrawalDetails(event).flatMap { withdrawalFinalizer.finalize(it) }

    /**
     * Get Iroha transaction by hash from WithdrawalServiceOutputEvent
     */
    private fun getIrohaTxByHash(event: WithdrawalServiceOutputEvent): Result<TransactionOuterClass.Transaction, Exception> {
        if (event !is WithdrawalServiceOutputEvent.EthRefund) {
            return Result.error(IllegalArgumentException("Unsupported output event type"))
        }
        return queryHelper.getSingleTransaction(event.proof.irohaHash)
    }

    /**
     * Get transfer commands from Iroha event
     */
    private fun getWithdrawalDetails(event: WithdrawalServiceOutputEvent): Result<WithdrawalFinalizationDetails, Exception> =
        getIrohaTxByHash(event).map { tx ->
            val transferAndFee = tx.payload.reducedPayload.commandsList.filter { cmd ->
                cmd.hasTransferAsset()
            }.map { cmd ->
                cmd.transferAsset
            }.filter { transferAsset ->
                transferAsset.destAccountId == credential.accountId
            }.groupBy { transfers ->
                transfers.description == FEE_DESCRIPTION
            }
            // check transfers
            val transfers = transferAndFee.getOrDefault(false, emptyList())
            if (transfers.isEmpty())
                throw D3ErrorException.warning(
                    failedOperation = WITHDRAWAL_OPERATION,
                    description = "Withdrawal tx not found"
                )

            // check fees
            var feeAmount = BigDecimal.ZERO
            var feeAssetId = ""
            val fees = transferAndFee.getOrDefault(true, emptyList())
            if (fees.size > 1)
                throw D3ErrorException.warning(
                    failedOperation = WITHDRAWAL_OPERATION,
                    description = "Too many fees in transaction"
                )
            if (fees.size == 1) {
                feeAmount = fees.first().amount.toBigDecimal()
                feeAssetId = fees.first().assetId
            }

            WithdrawalFinalizationDetails(
                transfers.first().amount.toBigDecimal(),
                transfers.first().assetId,
                feeAmount,
                feeAssetId,
                transfers.first().srcAccountId,
                tx.payload.reducedPayload.createdTime,
                transfers.first().description
            )
        }

    /**
     * Logger
     */
    companion object : KLogging()
}
