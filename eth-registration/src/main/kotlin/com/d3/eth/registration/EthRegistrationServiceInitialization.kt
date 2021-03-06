/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration

import com.d3.commons.model.IrohaCredential
import com.d3.commons.registration.RegistrationServiceEndpoint
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.eth.provider.ETH_RELAY
import com.d3.eth.provider.EthFreeRelayProvider
import com.d3.eth.provider.EthAddressProviderIrohaImpl
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging

/**
 * Initialisation of Registration Service
 *
 * @param ethRegistrationConfig - configurations of registration service
 */
class EthRegistrationServiceInitialization(
    private val ethRegistrationConfig: EthRegistrationConfig,
    private val irohaAPI: IrohaAPI
) {

    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {

        logger.info {
            "Start registration service init with iroha creator: ${ethRegistrationConfig.registrationCredential.accountId}"
        }

        return Result.of {
            val keyPair = Utils.parseHexKeypair(
                ethRegistrationConfig.registrationCredential.pubkey,
                ethRegistrationConfig.registrationCredential.privkey
            )
            IrohaCredential(
                ethRegistrationConfig.registrationCredential.accountId,
                keyPair
            )
        }.map { credential ->
            val queryHelper =
                IrohaQueryHelperImpl(irohaAPI, credential.accountId, credential.keyPair)
            Pair(
                Pair(
                    EthFreeRelayProvider(
                        queryHelper,
                        ethRegistrationConfig.relayStorageAccount,
                        ethRegistrationConfig.relayRegistrationIrohaAccount
                    ),
                    EthAddressProviderIrohaImpl(
                        queryHelper,
                        ethRegistrationConfig.relayStorageAccount,
                        ethRegistrationConfig.relayRegistrationIrohaAccount,
                        ETH_RELAY
                    )
                ),
                IrohaConsumerImpl(credential, irohaAPI)
            )
        }.map { (providers, irohaConsumer) ->
            val (ethFreeRelayProvider, ethRelayProvider) = providers
            EthRegistrationStrategyImpl(
                ethFreeRelayProvider,
                ethRelayProvider,
                irohaConsumer,
                ethRegistrationConfig.relayStorageAccount
            )
        }.map { registrationStrategy ->
            RegistrationServiceEndpoint(
                ethRegistrationConfig.port,
                registrationStrategy
            )
        }.map { Unit }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
