/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.vacuum

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.eth.provider.ETH_RELAY
import com.d3.eth.provider.EthAddressProviderIrohaImpl
import com.d3.eth.provider.EthTokensProviderImpl
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import contract.Relay
import integration.eth.config.EthereumPasswords
import mu.KLogging

/**
 * Class is responsible for relay contracts vacuum
 * Sends all tokens from relay smart contracts to master contract in Ethereum network
 */
class RelayVacuum(
    relayVacuumConfig: RelayVacuumConfig,
    relayVacuumEthereumPasswords: EthereumPasswords,
    queryHelper: IrohaQueryHelper
) {
    private val ethTokenAddress = "0x0000000000000000000000000000000000000000"

    /** Ethereum endpoint */
    private val deployHelper =
        DeployHelper(relayVacuumConfig.ethereum, relayVacuumEthereumPasswords)
    private val ethTokensProvider = EthTokensProviderImpl(
        queryHelper,
        relayVacuumConfig.ethAnchoredTokenStorageAccount,
        relayVacuumConfig.ethAnchoredTokenSetterAccount,
        relayVacuumConfig.irohaAnchoredTokenStorageAccount,
        relayVacuumConfig.irohaAnchoredTokenSetterAccount
    )

    private val ethRelayProvider = EthAddressProviderIrohaImpl(
        queryHelper,
        relayVacuumConfig.relayStorageAccount,
        relayVacuumConfig.registrationServiceIrohaAccount,
        ETH_RELAY
    )

    /**
     * Returns all non free relays
     */
    private fun getAllRelays(): Result<List<Relay>, Exception> {
        return ethRelayProvider.getAddresses().map { wallets ->
            wallets.keys.map { ethPublicKey ->
                deployHelper.loadRelayContract(ethPublicKey)
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
                logger.info { "Relays to vacuum ${relays.map { relay -> relay.contractAddress }}" }
                relays.forEach { relay ->
                    relay.sendToMaster(ethTokenAddress).send()
                    logger.info("${relay.contractAddress} send to master eth $ethTokenAddress")
                    providedTokens.forEach { providedToken ->
                        logger.info("${relay.contractAddress} send to master ${providedToken.value} ${providedToken.key}")
                        relay.sendToMaster(providedToken.key).send()
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
