/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("TransferEthereumFromContract")

package jp.co.soramitsu.soranet.eth.deploy

import com.d3.commons.config.loadLocalConfigs
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import jp.co.soramitsu.soranet.eth.config.EthereumConfig
import jp.co.soramitsu.soranet.eth.config.loadEthPasswords
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
import mu.KLogging
import java.math.BigInteger
import kotlin.system.exitProcess

private val logger = KLogging().logger

/**
 * Task that transfer Ethereum from contract as internal transaction for testing purpose.
 * Arguments:
 *  - ethereum address to send (0x4a59b6031e42f77df0554d253bd63cbf9113ea30)
 *  - amount
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        logger.error { "No arguments provided." }
        exitProcess(1)
    }
    val addr = args[0]
    val amount = BigInteger.valueOf((1000000000000000000 * args[1].toDouble()).toLong())

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
            val transferEthereum = deployHelper.deployTransferEthereum()
            deployHelper.sendEthereum(amount, transferEthereum.contractAddress)

            val hash = transferEthereum.transfer(addr, amount).send().transactionHash
            logger.info { "Ether was sent, tx_hash=$hash" }
        }
        .failure { ex ->
            logger.error("Cannot send eth", ex)
            exitProcess(1)
        }
}
