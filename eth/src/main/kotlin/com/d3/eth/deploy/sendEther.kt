/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("EthSendEther")

package com.d3.eth.deploy

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.loadEthPasswords
import com.d3.commons.config.loadLocalConfigs
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import mu.KLogging
import java.math.BigInteger

private val logger = KLogging().logger

/**
 * Send ethereum.
 * [args] should contain the address and amount of ether to send from genesis account
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        logger.error { "No arguments provided." }
        System.exit(1)
    }
    val addr = args[0]
    val amount = BigInteger(args[1])
    logger.info { "Send ether $amount from genesis to $addr" }


    loadLocalConfigs("predeploy.ethereum", EthereumConfig::class.java, "predeploy.properties")
        .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
        .map { (ethereumConfig, passwordConfig) ->
            DeployHelper(
                ethereumConfig,
                passwordConfig
            )
        }
        .map { deployHelper ->
            deployHelper.sendEthereum(
                amount.multiply(BigInteger.valueOf(1000000000000000000)),
                addr
            )
            logger.info { "Ether was sent" }

        }
        .failure { ex ->
            logger.error("Cannot send eth", ex)
            System.exit(1)
        }
}
