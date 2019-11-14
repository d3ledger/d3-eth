/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("DeployRelayMain")

package com.d3.eth.registration.relay

import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import integration.eth.config.loadEthPasswords
import com.d3.eth.provider.EthFreeRelayProvider
import com.github.kittinunf.result.*
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import kotlin.system.exitProcess

private val logger = KLogging().logger

const val RELAY_REGISTRATION_OPERATION = "Ethereum relay registration"

/**
 * Entry point for deployment of relay smart contracts that will be used in client registration.
 * The main reason to move the logic of contract deployment to separate executable is that it takes too much time and
 * thus it should be done in advance.
 * Every [relayRegistrationConfig.replenishmentPeriod] checks that number of free relays is
 * [relayRegistrationConfig.number]. In case of lack of free relays
 */
fun main(args: Array<String>) {
    logger.info { "Run relay deployment" }
    loadLocalConfigs(
        "relay-registration",
        RelayRegistrationConfig::class.java,
        "relay_registration.properties"
    ).fanout {
        loadEthPasswords(
            "relay-registration",
            "/eth/ethereum_password.properties"
        )
    }.map { (relayRegistrationConfig, passwordConfig) ->
        Result.of {
            val keyPair = Utils.parseHexKeypair(
                relayRegistrationConfig.relayRegistrationCredential.pubkey,
                relayRegistrationConfig.relayRegistrationCredential.privkey
            )
            IrohaCredential(
                relayRegistrationConfig.relayRegistrationCredential.accountId,
                keyPair
            )
        }.flatMap { credential ->
            IrohaAPI(
                relayRegistrationConfig.iroha.hostname,
                relayRegistrationConfig.iroha.port
            ).use { irohaAPI ->
                val queryHelper =
                    IrohaQueryHelperImpl(irohaAPI, credential.accountId, credential.keyPair)

                val freeRelayProvider = EthFreeRelayProvider(
                    queryHelper,
                    relayRegistrationConfig.relayStorageAccount,
                    relayRegistrationConfig.relayRegistrationCredential.accountId
                )

                val relayRegistration = RelayRegistration(
                    freeRelayProvider,
                    relayRegistrationConfig,
                    relayRegistrationConfig.ethMasterAddress,
                    relayRegistrationConfig.ethRelayImplementationAddress,
                    credential,
                    irohaAPI,
                    passwordConfig
                )

                if (args.isEmpty()) {
                    relayRegistration.runRelayReplenishment()
                } else {
                    relayRegistration.import(args[0])
                }
            }
        }.failure { ex ->
            logger.error("Cannot run relay deployer", ex)
            exitProcess(1)
        }
    }
}
