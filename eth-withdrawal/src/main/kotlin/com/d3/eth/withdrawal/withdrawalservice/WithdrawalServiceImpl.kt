/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.withdrawalservice

import com.d3.commons.model.IrohaCredential
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
import iroha.protocol.Commands
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import mu.KLogging

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

    /** Transfer data */
    data class TransferData(
        val dstAccountId: String,
        val assetId: String,
        val amount: String,
        val description: String
    )

    private val billingAccountId = withdrawalServiceConfig.withdrawalBillingAccount

    private val irohaConsumer: IrohaConsumer by lazy { IrohaConsumerImpl(credential, irohaAPI) }

    init {
        logger.info { "Init withdrawal service, irohaCredentials = ${credential.accountId}, notaryAccount = ${credential.accountId}'" }
    }

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
        return getWithdrawalTransfers(event)
            .map { transfers ->
                transfers.filter { transferAsset ->
                    transferAsset.destAccountId == credential.accountId
                }.map { transfer ->
                    val rollbackDescription = "rollback eth " + transfer.description
                    TransferData(
                        transfer.srcAccountId,
                        transfer.assetId,
                        transfer.amount,
                        rollbackDescription.substring(0, 64)
                    )
                }
            }.map { transferData ->
                var transactionBuilder = Transaction
                    .builder(irohaConsumer.creator, System.currentTimeMillis())
                transferData.forEach { transfer ->
                    transactionBuilder = transactionBuilder.transferAsset(
                        credential.accountId,
                        transfer.dstAccountId,
                        transfer.assetId,
                        transfer.description,
                        transfer.amount
                    )
                }
                transactionBuilder.build()
            }.map { transaction ->
                irohaConsumer.send(transaction)
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
        getWithdrawalTransfers(event)
            .map { transfersTxs ->
                val transfers = transfersTxs.filter { it.description != FEE_DESCRIPTION }
                val fees = transfersTxs.filter { it.description == FEE_DESCRIPTION }
                var transactionBuilder = Transaction
                    .builder(irohaConsumer.creator, System.currentTimeMillis())

                transfers.forEach {
                    transactionBuilder = transactionBuilder.subtractAssetQuantity(
                        it.assetId,
                        it.amount
                    )
                }
                fees.forEach {
                    transactionBuilder = transactionBuilder.transferAsset(
                        credential.accountId,
                        billingAccountId,
                        it.assetId,
                        it.description,
                        it.amount
                    )
                }
                transactionBuilder.build()
            }.flatMap { tx ->
                irohaConsumer.send(tx)
            }

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
    private fun getWithdrawalTransfers(event: WithdrawalServiceOutputEvent): Result<List<Commands.TransferAsset>, Exception> =
        getIrohaTxByHash(event).map { tx ->
            tx.payload.reducedPayload.commandsList.filter { cmd ->
                cmd.hasTransferAsset()
            }.map { cmd ->
                cmd.transferAsset
            }.filter { transferAsset ->
                transferAsset.destAccountId == credential.accountId
            }
        }

    /**
     * Logger
     */
    companion object : KLogging()
}
