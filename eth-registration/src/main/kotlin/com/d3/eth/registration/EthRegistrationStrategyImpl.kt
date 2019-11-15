/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration

import com.d3.commons.model.D3ErrorException
import com.d3.commons.registration.RegistrationStrategy
import com.d3.commons.registration.SideChainRegistrator
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.eth.provider.ETH_RELAY
import com.d3.eth.provider.EthAddressProvider
import com.d3.eth.provider.EthFreeRelayProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import mu.KLogging

/**
 * Effective implementation of [RegistrationStrategy]
 */
class EthRegistrationStrategyImpl(
    private val ethFreeRelayProvider: EthFreeRelayProvider,
    private val ethAddressProvider: EthAddressProvider,
    private val irohaConsumer: IrohaConsumer,
    relayStorageAccount: String
) : RegistrationStrategy {

    init {
        logger.info { "Init EthRegistrationStrategyImpl with irohaCreator=${irohaConsumer.creator}, relayStorageAccount=$relayStorageAccount" }
    }

    private val ethereumAccountRegistrator = SideChainRegistrator(
        irohaConsumer,
        relayStorageAccount,
        ETH_RELAY
    )

    /**
     * Register new notary client
     * @param accountName - client name
     * @param domainId - client domain
     * @param publicKey - client public key
     * @return ethereum wallet has been registered
     */
    @Synchronized
    override fun register(
        accountName: String,
        domainId: String,
        publicKey: String
    ): Result<String, Exception> {
        // check that client hasn't been registered yet
        return ethAddressProvider.getAddressByAccountId("$accountName@$domainId")
            .flatMap { assignedRelays ->
                if (assignedRelays.isPresent)
                    throw D3ErrorException.warning(
                        failedOperation = REGISTRATION_OPERATION,
                        description = "Client $accountName@$domainId has already been registered with relay: ${assignedRelays.get()}"
                    )
                ethFreeRelayProvider.getRelay()
            }.flatMap { freeEthWallet ->
                // register with relay in Iroha
                ethereumAccountRegistrator.register(
                    freeEthWallet,
                    "$accountName@$domainId",
                    System.currentTimeMillis()
                ) { "$accountName@$domainId" }
            }
    }

    /**
     * Return number of free relays.
     */
    override fun getFreeAddressNumber(): Result<Int, Exception> {
        return ethFreeRelayProvider.getRelaysCount()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
