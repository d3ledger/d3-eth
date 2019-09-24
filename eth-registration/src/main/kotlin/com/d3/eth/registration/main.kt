/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("EthRegistrationMain")

package com.d3.eth.registration

import com.d3.commons.config.loadLocalConfigs
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging
import kotlin.system.exitProcess

private val logger = KLogging().logger
const val REGISTRATION_OPERATION = "Ethereum user registration"
/**
 * Entry point for Registration Service
 */
fun main() {
    loadLocalConfigs(
        "eth-registration",
        EthRegistrationConfig::class.java,
        "registration.properties"
    ).map { registrationConfig ->
        executeRegistration(registrationConfig)
    }
}

fun executeRegistration(ethRegistrationConfig: EthRegistrationConfig) {
    logger.info("Run ETH registration service")
    val irohaNetwork =
        IrohaAPI(ethRegistrationConfig.iroha.hostname, ethRegistrationConfig.iroha.port)

    EthRegistrationServiceInitialization(ethRegistrationConfig, irohaNetwork).init()
        .failure { ex ->
            logger.error("cannot run eth registration", ex)
            irohaNetwork.close()
            exitProcess(1)
        }
}
