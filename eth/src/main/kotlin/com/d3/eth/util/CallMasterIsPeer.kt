/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("CallMasterIsPeer")

package com.d3.eth.util

import com.d3.commons.config.loadLocalConfigs
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import integration.eth.config.EthereumConfig
import integration.eth.config.loadEthPasswords
import mu.KLogging

private val logger = KLogging().logger

/**
 * Task that checks if Ethereum address is a Master peer.
 */
fun main() {
    val masterContractAddress = "0x4732c999ad17382522807dfa9451c53d002d576e" // < dev soranet
    // "0xc41f90922b508658425f9b485eba5679938cd585" // < test soranet

    // dev soranet notary peer addresses
    val peers = listOf(
        "dbe7676f26680cb5715ff312ff82c50e73c3e755",
        "8831ecd9669de4b2f821edd262bc51356ad9ad02",
        "5c79d2b4743ea338f5246674327afcd7c78f89ed",
        "5cb096d0638f7d3d260379973587e20b7b2cb6fd",
        "a86e8d6b975d97905a656442faf7a35c60787470",
        "aa7a60eec6152ac605b06e1740fa3573b9eb0c1a",
        "35dab79359af95aa835d3a56feb1c898c990bcf9"
    )

    loadLocalConfigs("predeploy.ethereum", EthereumConfig::class.java, "predeploy.properties")
        .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
        .map { (ethereumConfig, passwordConfig) ->
            DeployHelper(
                ethereumConfig,
                passwordConfig
            )
        }
        .map { deployHelper ->
            logger.info { "Load master contract at $masterContractAddress" }
            val master = deployHelper.loadMasterContract(masterContractAddress)
            peers.forEach {
                logger.info { "$it is peer: ${master.isPeer(it).send()}" }
            }
            logger.info { "xor ${master.xorTokenInstance().send()}" }
        }
        .failure { ex ->
            logger.error("Master contract call exception", ex)
            System.exit(1)
        }
}
