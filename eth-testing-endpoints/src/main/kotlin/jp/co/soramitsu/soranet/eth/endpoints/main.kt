/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("TestingEndpointsMain")

package jp.co.soramitsu.soranet.eth.endpoints

import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.config.loadRawLocalConfigs
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.soranet.eth.config.EthereumConfig
import jp.co.soramitsu.soranet.eth.config.loadEthPasswords
import jp.co.soramitsu.soranet.eth.endpoints.config.TestingEndpointConfig
import jp.co.soramitsu.soranet.eth.endpoints.endpoint.TestingEndpoint
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
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
