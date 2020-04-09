/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("EthPreDeployMain")

package jp.co.soramitsu.soranet.eth.deploy

import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.soranet.eth.config.loadEthPasswords
import jp.co.soramitsu.soranet.eth.constants.ETH_MASTER_ADDRESS_KEY
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelperBuilder
import mu.KLogging
import kotlin.system.exitProcess

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
        exitProcess(1)
    }

    loadLocalConfigs("predeploy", PredeployConfig::class.java, "predeploy.properties")
        .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
        .map { (predeployConfig, passwordConfig) ->
            val irohaApi = IrohaAPI(predeployConfig.iroha.hostname, predeployConfig.iroha.port)
            val irohaConsumer =
                IrohaConsumerImpl(IrohaCredential(predeployConfig.irohaCredential), irohaApi)
            val deployHelper = DeployHelperBuilder(
                predeployConfig.ethereum,
                passwordConfig
            )
                .setFastTransactionManager()
                .build()

            val master = deployHelper.deployUpgradableMasterSmartContract(
                args.toList()
            )
            saveContract(
                master.contractAddress,
                irohaConsumer,
                predeployConfig.ethContractAddressStorageAccountId,
                ETH_MASTER_ADDRESS_KEY
            )
        }
        .failure { ex ->
            logger.error("Cannot deploy smart contract", ex)
            exitProcess(1)
        }
}

/**
 * Save contract address both to file and Iroha account details
 */
private fun saveContract(
    contractAddress: String,
    irohaConsumer: IrohaConsumer,
    storageAccountId: String,
    key: String
) {
    ModelUtil.setAccountDetail(
        irohaConsumer,
        storageAccountId,
        key,
        contractAddress
    ).failure { throw it }
}
