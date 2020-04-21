/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("EthDepositMain")

package jp.co.soramitsu.soranet.eth.bridge

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.MultiSigIrohaConsumer
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.github.kittinunf.result.*
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.soranet.eth.config.EthereumPasswords
import jp.co.soramitsu.soranet.eth.config.loadEthPasswords
import jp.co.soramitsu.soranet.eth.provider.ETH_WALLET
import jp.co.soramitsu.soranet.eth.provider.EthAddressProviderIrohaImpl
import jp.co.soramitsu.soranet.eth.provider.EthTokensProviderImpl
import jp.co.soramitsu.soranet.eth.registration.EthRegistrationConfig
import jp.co.soramitsu.soranet.eth.registration.wallet.EthereumWalletRegistrationHandler
import mu.KLogging
import kotlin.system.exitProcess

private val logger = KLogging().logger

const val ETH_DEPOSIT_SERVICE_NAME = "eth-deposit"

/**
 * Application entry point
 */
fun main() {
    loadLocalConfigs("eth-deposit", EthDepositConfig::class.java, "deposit.properties")
        .fanout { loadEthPasswords("eth-deposit", "/eth/ethereum_password.properties") }
        .map { (depositConfig, ethereumPasswords) ->
            val rmqConfig = loadRawLocalConfigs(
                "rmq",
                RMQConfig::class.java,
                "rmq.properties"
            )
            val ethRegistrtaionConfig = loadLocalConfigs(
                "eth-registration",
                EthRegistrationConfig::class.java,
                "registration.properties"
            ).get()

            executeDeposit(
                ethereumPasswords,
                depositConfig,
                rmqConfig,
                ethRegistrtaionConfig
            )
        }
        .failure { ex ->
            logger.error("Cannot run eth deposit", ex)
            exitProcess(1)
        }
}

fun executeDeposit(
    ethereumPasswords: EthereumPasswords,
    depositConfig: EthDepositConfig,
    rmqConfig: RMQConfig,
    registrationConfig: EthRegistrationConfig
) {
    Result.of {
        val keypair = Utils.parseHexKeypair(
            depositConfig.notaryCredential.pubkey,
            depositConfig.notaryCredential.privkey
        )
        IrohaCredential(depositConfig.notaryCredential.accountId, keypair)
    }.flatMap { notaryIrohaCredential ->
        executeDeposit(
            notaryIrohaCredential,
            ethereumPasswords,
            depositConfig,
            rmqConfig,
            registrationConfig
        )
    }.failure { ex ->
        logger.error("Cannot run eth deposit", ex)
        exitProcess(1)
    }
}

/** Run deposit instance with particular [irohaCredential] */
fun executeDeposit(
    irohaCredential: IrohaCredential,
    ethereumPasswords: EthereumPasswords,
    depositConfig: EthDepositConfig,
    rmqConfig: RMQConfig,
    registrationConfig: EthRegistrationConfig
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

    val ethWalletProvider = EthAddressProviderIrohaImpl(
        queryHelper,
        depositConfig.ethereumWalletStorageAccount,
        depositConfig.ethereumWalletSetterAccount,
        ETH_WALLET
    )

    val ethTokensProvider = EthTokensProviderImpl(
        queryHelper,
        depositConfig.ethAnchoredTokenStorageAccount,
        depositConfig.ethAnchoredTokenSetterAccount,
        depositConfig.irohaAnchoredTokenStorageAccount,
        depositConfig.irohaAnchoredTokenSetterAccount
    )

    val registrationHandler =
        EthereumWalletRegistrationHandler(
            MultiSigIrohaConsumer(irohaCredential, irohaAPI),
            registrationConfig.registrationCredential.accountId,
            depositConfig.ethereumWalletStorageAccount,
            ethWalletProvider
        )

    return EthDepositInitialization(
        irohaCredential,
        irohaAPI,
        depositConfig,
        ethereumPasswords,
        rmqConfig,
        ethWalletProvider,
        ethTokensProvider,
        registrationHandler
    ).init()
}
