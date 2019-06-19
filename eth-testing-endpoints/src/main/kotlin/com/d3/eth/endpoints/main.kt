/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("TestingEndpointsMain")

package com.d3.eth.endpoints

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.loadEthPasswords
import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.ENDPOINT_ETHEREUM
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging

private val logger = KLogging().logger

private val endpointConfig =
    loadRawLocalConfigs("endpoints", TestingEndpointConfig::class.java, "testing_endpoints.properties")

/**
 * Main entry point of Testing endpoints deployment module
 */
fun main(args: Array<String>) {
    loadLocalConfigs("predeploy.ethereum", EthereumConfig::class.java, "predeploy.properties")
        .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
        .map { (ethereumConfig, passwordConfig) ->
            DeployHelper(
                ethereumConfig,
                passwordConfig
            )
        }
        .fanout {
            Result.of {
                ServerInitializationBundle(
                    endpointConfig.port,
                    ENDPOINT_ETHEREUM
                )
            }
        }
        .map { (deployHelper, bundle) ->
            TestingEndpoint(
                bundle,
                deployHelper,
                IrohaAPI(endpointConfig.iroha.hostname, endpointConfig.iroha.port),
                endpointConfig.notaryIrohaAccount
            )
        }
        .failure { ex ->
            logger.error("Cannot run testing endpoints service", ex)
            System.exit(1)
        }
}
