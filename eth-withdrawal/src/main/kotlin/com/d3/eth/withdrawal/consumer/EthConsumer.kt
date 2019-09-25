/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.consumer

import com.d3.commons.model.D3ErrorException
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.vacuum.RelayVacuumConfig
import com.d3.eth.vacuum.executeVacuum
import com.d3.eth.withdrawal.withdrawalservice.WITHDRAWAL_OPERATION
import com.d3.eth.withdrawal.withdrawalservice.WithdrawalServiceOutputEvent
import contract.Relay
import integration.eth.config.EthereumConfig
import integration.eth.config.EthereumPasswords
import mu.KLogging
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.utils.Numeric
import java.math.BigInteger

class EthConsumer(
    ethereumConfig: EthereumConfig,
    ethereumPasswords: EthereumPasswords,
    private val relayVacuumConfig: RelayVacuumConfig
) {
    private val deployHelper = DeployHelper(ethereumConfig, ethereumPasswords)

    fun consume(event: WithdrawalServiceOutputEvent): TransactionReceipt? {
        logger.info { "Consumed eth event $event" }
        if (event !is WithdrawalServiceOutputEvent.EthRefund) {
            throw IllegalArgumentException("Unsupported output event type")
        }
        logger.info {
            "Got proof:\n" +
                    "account ${event.proof.account}\n" +
                    "amount ${event.proof.amount}\n" +
                    "token ${event.proof.tokenContractAddress}\n" +
                    "iroha hash ${event.proof.irohaHash}\n" +
                    "relay ${event.proof.relay}\n"
        }

        val relay = deployHelper.loadRelayContract(event.proof.relay)

        return if (event.isIrohaAnchored) {
            withdrawIrohaAnchored(relay, event)
        } else {
            withdrawEthereumAnchored(relay, event)
        }
    }

    /**
     * Mint in Ethereum tokens that should be withdrawen.
     */
    fun withdrawIrohaAnchored(
        relay: Relay,
        event: WithdrawalServiceOutputEvent.EthRefund
    ): TransactionReceipt? {
        try {
            logger.info { "Withdraw Iroha anchored" }
            return relay.mintTokensByPeers(
                event.proof.tokenContractAddress,
                BigInteger(event.proof.amount),
                event.proof.account,
                Numeric.hexStringToByteArray(event.proof.irohaHash),
                event.proof.v,
                event.proof.r,
                event.proof.s,
                relay.contractAddress
            ).send()
        } catch (ex: Exception) {
            logger.error("Web3j exception encountered", ex)
            return null
        }
    }

    /**
     * Call Relay method withdraw and preform vacuum if needed.
     */
    fun withdrawEthereumAnchored(
        relay: Relay,
        event: WithdrawalServiceOutputEvent.EthRefund
    ): TransactionReceipt? {
        logger.info { "Withdraw Ethereum anchored" }
        // The first withdraw call
        val call = withdraw(relay, event)
        // Here works next logic:
        // If the first call returns logs with size 2 then check if a destination address is equal to the address
        // from the second log
        // If its true then we start vacuum process
        val logs = call?.logs
        if (logs != null) {
            for (log in logs) {
                if (log.topics.contains("0x33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293")
                    && event.proof.account.toLowerCase() == "0x" + log.data.toLowerCase().subSequence(
                        90,
                        130
                    )
                ) {
                    executeVacuum(relayVacuumConfig).fold(
                        {
                            return withdraw(relay, event)
                        },
                        { ex ->
                            throw D3ErrorException.fatal(
                                failedOperation = WITHDRAWAL_OPERATION,
                                description = "Cannot execute vacuum",
                                errorCause = ex
                            )
                        }
                    )
                }
            }
        }
        return call
    }

    /**
     * Call relay method to withdraw
     */
    fun withdraw(relay: Relay, event: WithdrawalServiceOutputEvent.EthRefund): TransactionReceipt? {
        try {
            return relay.withdraw(
                event.proof.tokenContractAddress,
                BigInteger(event.proof.amount),
                event.proof.account,
                Numeric.hexStringToByteArray(event.proof.irohaHash),
                event.proof.v,
                event.proof.r,
                event.proof.s,
                relay.contractAddress
            ).send()
        } catch (ex: Exception) {
            logger.error("Web3j exception encountered", ex)
            return null
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
