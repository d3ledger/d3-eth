/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.vacuum

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.eth.helper.getWalletByAddress
import com.d3.eth.provider.EthRelayProviderIrohaImpl
import com.d3.eth.provider.EthTokensProviderImpl
import com.d3.eth.sidechain.util.ATTEMPTS_DEFAULT
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import integration.eth.config.EthereumPasswords
import mu.KLogging
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import java.math.BigInteger


val WALLETS_PATH = "."

/**
 * Class is responsible for relay contracts vacuum
 * Sends all tokens from relay smart contracts to master contract in Ethereum network
 */
class RelayVacuum(
    val relayVacuumConfig: RelayVacuumConfig,
    val ethereumPasswords: EthereumPasswords,
    queryHelper: IrohaQueryHelper
) {
    private val ethTokenAddress = "0x0000000000000000000000000000000000000000"

    /** Ethereum endpoint */
    private val deployHelper = DeployHelper(relayVacuumConfig.ethereum, ethereumPasswords)

    private val ethTokensProvider = EthTokensProviderImpl(
        queryHelper,
        relayVacuumConfig.ethAnchoredTokenStorageAccount,
        relayVacuumConfig.ethAnchoredTokenSetterAccount,
        relayVacuumConfig.irohaAnchoredTokenStorageAccount,
        relayVacuumConfig.irohaAnchoredTokenSetterAccount
    )

    private val ethRelayProvider = EthRelayProviderIrohaImpl(
        queryHelper,
        relayVacuumConfig.notaryIrohaAccount,
        relayVacuumConfig.registrationServiceIrohaAccount
    )

    /**
     * Returns all non free relays
     */
    private fun getAllRelays(): Result<List<Credentials>, Exception> {
        return ethRelayProvider.getAddresses().map { wallets ->
            wallets.keys.map { ethAddress ->
                getWalletByAddress(WALLETS_PATH, ethAddress)
                    .map { wallet ->
                        WalletUtils.loadCredentials(ethereumPasswords.credentialsPassword, wallet)!!
                    }.get()
            }
        }
    }

    /**
     * Moves all currency(ETH and tokens) from non free relay contracts to master contract
     */
    fun vacuum(): Result<Unit, Exception> {
        return ethTokensProvider.getEthAnchoredTokens().flatMap { providedTokens ->
            logger.info { "Provided tokens $providedTokens" }
            val res = getAllRelays().map { relays ->
                logger.info { "Wallets to vacuum ${relays.map { wallet -> wallet.address }}" }
                relays.forEach { credentials ->

                    val ethBalance = deployHelper.getETHBalance(credentials.address)
                    // ethBalance > 0
                    if (ethBalance.compareTo(BigInteger.ZERO) > 0) {
                        logger.info("Vacuum ${credentials.address} for $ethBalance, send to ${relayVacuumConfig.ethMasterAddress}")

                        val clientEthUtils = DeployHelper(
                            relayVacuumConfig.ethereum,
                            ethereumPasswords.nodeLogin,
                            ethereumPasswords.nodePassword,
                            credentials,
                            ATTEMPTS_DEFAULT
                        )
                        clientEthUtils.sendEthereum(ethBalance, relayVacuumConfig.ethMasterAddress)
                    }

//                    wallet.(ethTokenAddress).send()
                    providedTokens.forEach { providedToken ->
                        logger.info("${credentials.address} send to master ${providedToken.value} ${providedToken.key}")
//                        relay.sendToMaster(providedToken.key).send()
                    }
                }
            }
            logger.info { "Vacuum finished" }
            res
        }
    }

    /**
     * Logger
     */
    private companion object : KLogging()
}
