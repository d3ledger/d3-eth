/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit.endpoint

import com.d3.commons.model.D3ErrorException
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.FEE_DESCRIPTION
import com.d3.commons.sidechain.iroha.util.getWithdrawalCommands
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.sidechain.iroha.util.isWithdrawalTransaction
import com.d3.eth.deposit.EthDepositConfig
import com.d3.eth.deposit.REFUND_OPERATION
import com.d3.eth.provider.ETH_RELAY
import com.d3.eth.provider.EthAddressProviderIrohaImpl
import com.d3.eth.provider.EthTokensProvider
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.hashToWithdraw
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import integration.eth.config.EthereumConfig
import integration.eth.config.EthereumPasswords
import iroha.protocol.TransactionOuterClass.Transaction
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging
import java.math.BigDecimal

/**
 * Class performs effective implementation of refund strategy for Ethereum
 */
class EthRefundStrategyImpl(
    depositConfig: EthDepositConfig,
    irohaAPI: IrohaAPI,
    credential: IrohaCredential,
    ethereumConfig: EthereumConfig,
    ethereumPasswords: EthereumPasswords,
    private val tokensProvider: EthTokensProvider
) : EthRefundStrategy {
    private val queryHelper =
        IrohaQueryHelperImpl(irohaAPI, credential.accountId, credential.keyPair)
    private val relayProvider = EthAddressProviderIrohaImpl(
        queryHelper,
        depositConfig.ethereumRelayStorageAccount,
        depositConfig.ethereumRelaySetterAccount,
        ETH_RELAY
    )

    private val withdrawalAccountId = depositConfig.notaryCredential.accountId

    private val deployHelper = DeployHelper(ethereumConfig, ethereumPasswords)

    override fun performRefund(request: EthRefundRequest): EthNotaryResponse {
        logger.info("Check tx ${request.irohaTx} for refund")

        return queryHelper.getSingleTransaction(request.irohaTx)
            .flatMap { checkTransaction(it, request) }
            .flatMap { makeRefund(it) }
            .fold({ it },
                { ex ->
                    logger.error("Cannot perform refund", ex)
                    EthNotaryResponse.Error(ex.toString())
                })
    }

    /**
     * The method checks transaction and create refund if it is correct
     * @param appearedTx - target transaction from Iroha
     * @param request - user's request with transaction hash
     * @return Refund or error
     */
    private fun checkTransaction(
        appearedTx: Transaction,
        request: EthRefundRequest
    ): Result<EthRefund, Exception> {
        return Result.of {
            when {
                // withdrawal case
                isWithdrawalTransaction(
                    appearedTx,
                    withdrawalAccountId
                ) -> {
                    // pick withdrawal transfers
                    val withdrawalCommand = getWithdrawalCommands(appearedTx, withdrawalAccountId)
                        .map { cmd -> cmd.transferAsset }
                        .first { cmd -> cmd.description != FEE_DESCRIPTION }
                    val amount = withdrawalCommand.amount
                    val assetId = withdrawalCommand.assetId
                    val destEthAddress = withdrawalCommand.description

                    val tokenInfo = tokensProvider.getTokenAddress(assetId)
                        .fanout { tokensProvider.getTokenPrecision(assetId) }

                    relayProvider.getAddressByAccountId(withdrawalCommand.srcAccountId)
                        .fanout {
                            tokenInfo
                        }.fold(
                            { (relayAddress, tokenInfo) ->
                                val decimalAmount =
                                    BigDecimal(amount).scaleByPowerOfTen(tokenInfo.second)
                                        .toPlainString()
                                EthRefund(
                                    destEthAddress,
                                    tokenInfo.first,
                                    decimalAmount,
                                    request.irohaTx,
                                    relayAddress.orElseThrow {
                                        D3ErrorException.fatal(
                                            failedOperation = REFUND_OPERATION,
                                            description = "Relay address not found for user ${withdrawalCommand.srcAccountId}"
                                        )
                                    }
                                )
                            },
                            {
                                throw D3ErrorException.fatal(
                                    failedOperation = REFUND_OPERATION,
                                    description = "Cannot get relay by account id ${withdrawalCommand.srcAccountId}",
                                    errorCause = it
                                )
                            }
                        )
                }
                else -> {
                    throw D3ErrorException.warning(
                        failedOperation = REFUND_OPERATION,
                        description = "Transaction doesn't contain expected commands"
                    )
                }
            }
        }
    }

    /**
     * The method signs refund and return valid parameters for Ethereum smart contract call
     * @param ethRefund - refund for signing
     * @return signed refund or error
     */
    private fun makeRefund(ethRefund: EthRefund): Result<EthNotaryResponse, Exception> {
        logger.info { "Make refund. Asset address: ${ethRefund.assetId}, amount: ${ethRefund.amount}, to address: ${ethRefund.address}, hash: ${ethRefund.irohaTxHash}, relay: ${ethRefund.relayAddress}" }
        return Result.of {
            val finalHash =
                hashToWithdraw(
                    ethRefund.assetId,
                    ethRefund.amount,
                    ethRefund.address,
                    ethRefund.irohaTxHash,
                    ethRefund.relayAddress
                )

            val signature = deployHelper.signUserData(finalHash)
            EthNotaryResponse.Successful(signature)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
