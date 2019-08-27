/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("TestingEndpointsMain")

package com.d3.eth.endpoints

import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.eth.endpoints.config.TestingEndpointConfig
import com.d3.eth.endpoints.endpoint.TestingEndpoint
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import integration.eth.config.EthereumConfig
import integration.eth.config.loadEthPasswords
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging
import kotlin.system.exitProcess

private val logger = KLogging().logger

private val endpointConfig = loadRawLocalConfigs(
    "endpoints",
    TestingEndpointConfig::class.java,
    "testing_endpoints.properties"
)

/**
 * Main entry point of Testing endpoints deployment module
 */
fun main() {
    loadLocalConfigs("predeploy.ethereum", EthereumConfig::class.java, "predeploy.properties")
        .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
        .map { (ethereumConfig, passwordConfig) ->
            TestingEndpoint(
                endpointConfig.port,
                DeployHelper(
                    ethereumConfig,
                    passwordConfig
                ),
                IrohaAPI(endpointConfig.iroha.hostname, endpointConfig.iroha.port),
                endpointConfig.notaryIrohaAccount
            )
        }
        .failure { ex ->
            logger.error("Cannot run testing endpoints service", ex)
            exitProcess(1)
        }
}
