/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration.relay

import com.d3.commons.model.D3ErrorException
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import integration.eth.config.EthereumPasswords
import com.d3.eth.provider.EthFreeRelayProvider
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KLogging
import java.io.File

/**
 * Class is responsible for relay addresses registration.
 * Deploys relay smart contracts in Ethereum network and records it in Iroha.
 */
class RelayRegistration(
    private val freeRelayProvider: EthFreeRelayProvider,
    private val relayRegistrationConfig: RelayRegistrationConfig,
    private val ethMasterAddress: String,
    private val ethRelayImplementationAddress: String,
    relayCredential: IrohaCredential,
    irohaAPI: IrohaAPI,
    relayRegistrationEthereumPasswords: EthereumPasswords
) {
    init {
        logger.info {
            "Start relay registration (ethMasterAddress = $ethMasterAddress, " +
                    "ethRelayImplementationAddress = $ethRelayImplementationAddress)"
        }
    }

    /** Ethereum endpoint */
    private val deployHelper =
        DeployHelper(relayRegistrationConfig.ethereum, relayRegistrationEthereumPasswords)

    /** Iroha endpoint */
    private val irohaConsumer = IrohaConsumerImpl(relayCredential, irohaAPI)

    private val relayStorageAccount = relayRegistrationConfig.relayStorageAccount

    /**
     * Registers relay in Iroha.
     * @param relayAddress - relay address to record into Iroha
     * @return Result with string representation of hash or possible failure
     */
    fun registerRelayIroha(relayAddress: String): Result<String, Exception> {
        return ModelUtil.setAccountDetail(irohaConsumer, relayStorageAccount, relayAddress, "free")
    }

    /**
     * Check that Relay Implementation and Master contracts are actually deployed
     * @param ethRelayImplementationAddress - address of Relay contract
     * @param ethMasterWallet - address of Master contract
     */
    fun checkContracts(
        ethRelayImplementationAddress: String,
        ethMasterWallet: String
    ): Result<Unit, Exception> {
        return Result.of {
            val relayIsValid = deployHelper.loadRelayContract(ethRelayImplementationAddress).isValid
            val masterIsValid = deployHelper.loadOwnedUpgradabilityProxy(ethMasterWallet).isValid
            if (!relayIsValid || !masterIsValid) {
                var message = ""
                if (!relayIsValid)
                    message += "Contract address is incorrect for relay implementation at $ethRelayImplementationAddress\n"
                if (!masterIsValid)
                    message += "Contract address is incorrect for master at $ethMasterWallet\n"
                throw IllegalArgumentException(message)
            }
        }
    }

    fun deploy(
        relaysToDeploy: Int,
        ethRelayImplementationAddress: String,
        ethMasterAddress: String
    ): Result<Unit, Exception> {
        return checkContracts(ethRelayImplementationAddress, ethMasterAddress)
            .map {
                if (relaysToDeploy > 0)
                    logger.info { "Deploy $relaysToDeploy ethereum relays" }

                (1..relaysToDeploy).forEach { _ ->
                    val relayWallet =
                        deployHelper.deployUpgradableRelaySmartContract(
                            ethRelayImplementationAddress,
                            ethMasterAddress
                        )
                            .contractAddress
                    registerRelayIroha(relayWallet).fold(
                        { logger.info("Relay $relayWallet was deployed") },
                        { ex -> logger.error("Cannot deploy relay $relayWallet", ex) })
                }
            }
    }

    /**
     * Run a job that every replenishmentPeriod checks that number from config free relays are present. In case of
     * lack of free relays deploys lacking amount.
     */
    fun runRelayReplenishment(): Result<Unit, Exception> {
        logger.info { "Run relay replenishment" }

        return Result.of {
            while (true) {
                logger.info { "Relay replenishment triggered" }

                freeRelayProvider.getRelaysCount().flatMap { relaysCount ->
                    logger.info { "Free relays: $relaysCount" }
                    val toDeploy = relayRegistrationConfig.number - relaysCount
                    deploy(
                        toDeploy,
                        ethRelayImplementationAddress,
                        ethMasterAddress
                    )
                }.failure {
                    throw D3ErrorException.warning(
                        failedOperation = RELAY_REGISTRATION_OPERATION,
                        description = "Cannot get relays",
                        errorCause = it
                    )
                }

                runBlocking { delay(relayRegistrationConfig.replenishmentPeriod * 1000) }
            }
        }
    }

    fun import(filename: String): Result<Unit, Exception> {
        return Result.of {
            getRelaysFromFile(filename).forEach { relay ->
                registerRelayIroha(relay).fold(
                    { logger.info("Relay $relay was imported") },
                    { ex -> logger.error("Cannot import relay $relay", ex) })
            }
        }
    }

    private fun getRelaysFromFile(filename: String): List<String> {
        return File(filename).readLines()
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
