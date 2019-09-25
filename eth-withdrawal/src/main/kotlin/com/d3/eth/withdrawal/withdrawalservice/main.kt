/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("WithdrawalServiceMain")

package com.d3.eth.withdrawal.withdrawalservice

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.model.IrohaCredential
import integration.eth.config.EthereumPasswords
import integration.eth.config.loadEthPasswords
import com.d3.eth.vacuum.RelayVacuumConfig
import com.github.kittinunf.result.*
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging

private val logger = KLogging().logger

private const val RELAY_VACUUM_PREFIX = "relay-vacuum"
const val WITHDRAWAL_OPERATION = "Ethereum withdrawal"

const val ETH_WITHDRAWAL_SERVICE_NAME = "eth-withdrawal"

/**
 * Main entry point of Withdrawal Service app
 */
fun main() {
    loadLocalConfigs("withdrawal", WithdrawalServiceConfig::class.java, "withdrawal.properties")
        .fanout { loadEthPasswords("withdrawal", "/eth/ethereum_password.properties") }
        .map { (withdrawalConfig, passwordConfig) ->
            loadLocalConfigs(
                RELAY_VACUUM_PREFIX,
                RelayVacuumConfig::class.java,
                "vacuum.properties"
            )
                .map { relayVacuumConfig ->
                    val rmqConfig = loadRawLocalConfigs(
                        "rmq",
                        RMQConfig::class.java,
                        "rmq.properties"
                    )
                    executeWithdrawal(
                        withdrawalConfig,
                        passwordConfig,
                        relayVacuumConfig,
                        rmqConfig
                    )
                }
        }
        .failure { ex ->
            logger.error("Cannot run withdrawal service", ex)
            System.exit(1)
        }
}

fun executeWithdrawal(
    withdrawalConfig: WithdrawalServiceConfig,
    passwordConfig: EthereumPasswords,
    relayVacuumConfig: RelayVacuumConfig,
    rmqConfig: RMQConfig
) {
    logger.info { "Run withdrawal service" }
    val irohaAPI = IrohaAPI(withdrawalConfig.iroha.hostname, withdrawalConfig.iroha.port)

    Result.of {
        val keyPair = Utils.parseHexKeypair(
            withdrawalConfig.withdrawalCredential.pubkey,
            withdrawalConfig.withdrawalCredential.privkey
        )
        IrohaCredential(
            withdrawalConfig.withdrawalCredential.accountId,
            keyPair
        )
    }.flatMap { credential ->
        WithdrawalServiceInitialization(
            withdrawalConfig,
            credential,
            irohaAPI,
            passwordConfig,
            relayVacuumConfig,
            rmqConfig
        ).init()
    }.failure { ex ->
        logger.error("Cannot run withdrawal service", ex)
        irohaAPI.close()
        System.exit(1)
    }
}
