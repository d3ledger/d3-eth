/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.registration.wallet

import com.d3.commons.registration.SideChainRegistrator
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.irohaUnEscape
import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.soranet.eth.provider.ETH_WALLET
import jp.co.soramitsu.soranet.eth.provider.EthAddressProvider
import mu.KLogging

const val ETH_FAILED_REGISTRATION_KEY = "failed_registration"

/**
 * Handles ethereum wallet registration events from Iroha blocks
 */
class EthereumWalletRegistrationHandler(
    private val irohaConsumer: IrohaConsumer,
    private val registrationTriggerAccountId: String,
    walletStorageIrohaAccountId: String,
    private val ethWalletProvider: EthAddressProvider
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
                val txHash = Utils.toHexHash(tx)
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

                            // ensure wallet is signed with correct private key
                            if (checkRegistrationProof(registrationProof)) {
                                val registrationTxHash = registrator.register(
                                    ethAddress,
                                    clientId,
                                    time
                                ) { clientId }.get()
                                logger.info { "Registration request with Ethereum wallet $ethAddress triggered for $clientId submitted with tx $registrationTxHash" }

                            } else {
                                throw IllegalArgumentException("Registration triggered with wrong proof for ${setAccountDetail.accountId} with wallet $ethAddress")
                            }
                        } catch (ex: Exception) {
                            logger.error(
                                "Ethereum registration for client $clientId failed",
                                ex
                            )
                            saveFailedRegistration(clientId, txHash, ex.message!!, time)
                            logger.info("Saved the fail reason for client $clientId")
                        }
                    }
            }
    }

    private fun saveFailedRegistration(accountId: String, hash: String, reason: String, time: Long) {
        val tx = Transaction.builder(irohaConsumer.creator, time)
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
