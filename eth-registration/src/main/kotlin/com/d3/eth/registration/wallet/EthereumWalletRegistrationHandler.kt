/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration.wallet

import com.d3.commons.registration.SideChainRegistrator
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.hex
import com.d3.commons.util.irohaUnEscape
import com.d3.eth.provider.ETH_WALLET
import com.d3.eth.provider.EthAddressProvider
import com.d3.eth.registration.wallet.ETH_REGISTRATION_KEY
import com.d3.eth.registration.wallet.EthereumRegistrationProof
import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging

const val ETH_FAILED_REGISTRATION_KEY = "failed_registration"

/**
 * Handles ethereum wallet registration events from Iroha blocks
 */
class EthereumWalletRegistrationHandler(
    val irohaConsumer: IrohaConsumer,
    val registrationTriggerAccountId: String,
    walletStorageIrohaAccountId: String,
    private val ethWalletProvider: EthAddressProvider,
    private val ethRelayProvider: EthAddressProvider
) {
    init {
        logger.info { "Initialization of EthereumWalletRegistrationHandler with registrationTriggerAccountId=$registrationTriggerAccountId" }
    }

    private val gson = GsonInstance.get()

    private val registrator = SideChainRegistrator(
        irohaConsumer,
        walletStorageIrohaAccountId,
        ETH_WALLET
    )

    fun filterAndRegister(block: BlockOuterClass.Block) {
        block.blockV1.payload.transactionsList
            // Get commands
            .flatMap { tx ->
                val txHash = String.hex(Utils.hash(tx))
                val clientId = tx.payload.reducedPayload.creatorAccountId
                val time = tx.payload.reducedPayload.createdTime
                tx.payload.reducedPayload.commandsList
                    .filter { command -> command.hasSetAccountDetail() }
                    .map { command -> command.setAccountDetail }
                    // Get registration trigger
                    .filter { setAccountDetail ->
                        setAccountDetail.accountId == registrationTriggerAccountId &&
                                setAccountDetail.key == ETH_REGISTRATION_KEY
                    }
                    .map { setAccountDetail ->
                        try {
                            logger.info { "Check registration wallet proof for account $clientId" }
                            val registrationProof = gson.fromJson(
                                setAccountDetail.value.irohaUnEscape(),
                                EthereumRegistrationProof::class.java
                            )
                            val ethAddress = "0x${registrationProof.getAddress()}"

                            // check address is not used
                            if (ethWalletProvider.getAddresses().get().containsKey(ethAddress))
                                throw IllegalArgumentException("Address $ethAddress already registered as wallet")
                            if (ethRelayProvider.getAddresses().get().containsKey(ethAddress))
                                throw IllegalArgumentException("Address $ethAddress already registered as relay")

                            // ensure wallet is signed with correct private key
                            if (checkRegistrationProof(registrationProof)) {
                                val registrationTxHash = registrator.register(
                                    ethAddress,
                                    clientId,
                                    time
                                ) { clientId }.get()
                                logger.info { "Registration with Ethereum wallet $ethAddress triggered for $clientId completed with tx $registrationTxHash" }

                            } else {
                                throw IllegalArgumentException("Registration triggered with wrong proof for ${setAccountDetail.accountId} with wallet $ethAddress")
                            }
                        } catch (ex: Exception) {
                            logger.error(
                                "Ethereum registration for client $clientId failed",
                                ex
                            )
                            saveFailedRegistration(clientId, txHash, ex.message!!)
                        }
                    }
            }
    }

    private fun saveFailedRegistration(accountId: String, hash: String, reason: String) {
        val tx = Transaction.builder(irohaConsumer.creator)
            .setAccountDetail(
                accountId,
                ETH_FAILED_REGISTRATION_KEY,
                "registration $hash, reason $reason"
            )
            .setQuorum(irohaConsumer.getConsumerQuorum().get())
            .build()
        irohaConsumer.send(tx).get()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
