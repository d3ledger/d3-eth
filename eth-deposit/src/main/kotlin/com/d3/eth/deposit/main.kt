/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("EthDepositMain")

package com.d3.eth.deposit

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import integration.eth.config.EthereumPasswords
import integration.eth.config.loadEthPasswords
import com.d3.eth.provider.EthRelayProviderIrohaImpl
import com.d3.eth.provider.EthTokensProviderImpl
import com.github.kittinunf.result.*
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging

private val logger = KLogging().logger

const val ETH_DEPOSIT_SERVICE_NAME = "eth-deposit"

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    loadLocalConfigs("eth-deposit", EthDepositConfig::class.java, "deposit.properties")
        .fanout { loadEthPasswords("eth-deposit", "/eth/ethereum_password.properties") }
        .map { (depositConfig, ethereumPasswords) ->
            val rmqConfig = loadRawLocalConfigs(
                "rmq",
                RMQConfig::class.java,
                "rmq.properties"
            )
            executeDeposit(ethereumPasswords, depositConfig, rmqConfig)
        }
        .failure { ex ->
            logger.error("Cannot run eth deposit", ex)
            System.exit(1)
        }
}

fun executeDeposit(
    ethereumPasswords: EthereumPasswords,
    depositConfig: EthDepositConfig,
    rmqConfig: RMQConfig
) {
    Result.of {
        val keypair = Utils.parseHexKeypair(
            depositConfig.notaryCredential.pubkey,
            depositConfig.notaryCredential.privkey
        )
        IrohaCredential(depositConfig.notaryCredential.accountId, keypair)
    }.flatMap { irohaCredential ->
        executeDeposit(irohaCredential, ethereumPasswords, depositConfig, rmqConfig)
    }.failure { ex ->
        logger.error("Cannot run eth deposit", ex)
        System.exit(1)
    }
}

/** Run deposit instance with particular [irohaCredential] */
fun executeDeposit(
    irohaCredential: IrohaCredential,
    ethereumPasswords: EthereumPasswords,
    depositConfig: EthDepositConfig,
    rmqConfig: RMQConfig
): Result<Unit, Exception> {
    logger.info { "Run ETH deposit" }

    val irohaAPI = IrohaAPI(
        depositConfig.iroha.hostname,
        depositConfig.iroha.port
    )

    val queryHelper = IrohaQueryHelperImpl(
        irohaAPI,
        irohaCredential.accountId,
        irohaCredential.keyPair
    )

    val ethRelayProvider = EthRelayProviderIrohaImpl(
        queryHelper,
        irohaCredential.accountId,
        depositConfig.registrationServiceIrohaAccount
    )
    val ethTokensProvider = EthTokensProviderImpl(
        queryHelper,
        depositConfig.ethAnchoredTokenStorageAccount,
        depositConfig.ethAnchoredTokenSetterAccount,
        depositConfig.irohaAnchoredTokenStorageAccount,
        depositConfig.irohaAnchoredTokenSetterAccount
    )
    return EthDepositInitialization(
        irohaCredential,
        irohaAPI,
        depositConfig,
        ethereumPasswords,
        rmqConfig,
        ethRelayProvider,
        ethTokensProvider
    ).init()
}
