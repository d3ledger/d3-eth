/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.withdrawalservice

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.eth.provider.EthTokensProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import io.reactivex.Observable
import jp.co.soramitsu.iroha.java.IrohaAPI
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

    private val masterAccount = withdrawalServiceConfig.notaryIrohaAccount

    private val irohaConsumer: IrohaConsumer by lazy { IrohaConsumerImpl(credential, irohaAPI) }

    init {
        logger.info { "Init withdrawal service, irohaCredentials = ${credential.accountId}, notaryAccount = $masterAccount'" }
    }

    /**
     * Handle IrohaEvent
     * @param irohaEvent - iroha event
     * @return withdrawal service output event or exception
     */
    override fun onIrohaEvent(irohaEvent: SideChainEvent.IrohaEvent): Result<List<WithdrawalServiceOutputEvent>, Exception> {
        when (irohaEvent) {
            is SideChainEvent.IrohaEvent.SideChainTransfer -> {
                logger.info { "Iroha transfer event to ${irohaEvent.dstAccount}, expected $masterAccount" }

                if (irohaEvent.dstAccount == masterAccount) {
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

    override fun returnIrohaAssets(event: WithdrawalServiceOutputEvent): Result<Unit, Exception> {
        if (event !is WithdrawalServiceOutputEvent.EthRefund) {
            return Result.error(IllegalArgumentException("Unsupported output event type"))
        }

        logger.info("Withdrawal rollback initiated: ${event.proof.irohaHash}")
        return queryHelper.getSingleTransaction(event.proof.irohaHash)
            .map { tx ->
                tx.payload.reducedPayload.commandsList.first { command ->
                    val transferAsset = command.transferAsset
                    transferAsset?.srcAccountId != "" && transferAsset?.destAccountId == masterAccount
                }
            }
            .map { transferCommand ->
                val destAccountId = transferCommand?.transferAsset?.srcAccountId
                    ?: throw IllegalStateException("Unable to identify primary Iroha transaction data")

                ModelUtil.transferAssetIroha(
                    irohaConsumer,
                    masterAccount,
                    destAccountId,
                    transferCommand.transferAsset.assetId,
                    "Rollback transaction due to failed withdrawal in Ethereum",
                    transferCommand.transferAsset.amount
                )
                    .fold({ txHash ->
                        logger.info("Successfully sent rollback transaction to Iroha, hash: $txHash")
                    }, { ex: Exception ->
                        logger.error("Error during rollback transfer transaction", ex)
                        throw ex
                    })
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
