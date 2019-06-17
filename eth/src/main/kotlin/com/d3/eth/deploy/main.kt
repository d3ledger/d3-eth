/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("EthPreDeployMain")

package com.d3.eth.deploy

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.loadEthPasswords
import com.d3.commons.config.loadLocalConfigs
import com.d3.eth.sidechain.util.DeployHelperBuilder
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import mu.KLogging
import java.io.File

private val logger = KLogging().logger

/**
 * Entry point to deploy smart contracts.
 * Contracts are deployed via UpgradableProxy.
 * [args] should contain the list of notary ethereum addresses
 */
fun main(args: Array<String>) {
    logger.info { "Run predeploy with notary addresses: ${args.toList()}" }
    if (args.isEmpty()) {
        logger.error { "No notary ethereum addresses are provided." }
        System.exit(1)
    }

    loadLocalConfigs("predeploy.ethereum", EthereumConfig::class.java, "predeploy.properties")
        .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
        .map { (ethereumConfig, passwordConfig) ->
            DeployHelperBuilder(
                ethereumConfig,
                passwordConfig
            )
                .setFastTransactionManager()
                .build()
        }
        .map { deployHelper ->
            val relayRegistry = deployHelper.deployUpgradableRelayRegistrySmartContract()
            File("relay_registry_eth_address").printWriter().use {
                it.print(relayRegistry.contractAddress)
            }

            val master = deployHelper.deployUpgradableMasterSmartContract(
                relayRegistry.contractAddress,
                args.toList()
            )
            File("master_eth_address").printWriter().use {
                it.print(master.contractAddress)
            }
            File("sora_token_eth_address").printWriter().use {
                it.print(master.xorTokenInstance().send())
            }

            val relayImplementation = deployHelper.deployRelaySmartContract(master.contractAddress)
            File("relay_implementation_address").printWriter().use {
                it.print(relayImplementation.contractAddress)
            }
        }
        .failure { ex ->
            logger.error("Cannot deploy smart contract", ex)
            System.exit(1)
        }
}
