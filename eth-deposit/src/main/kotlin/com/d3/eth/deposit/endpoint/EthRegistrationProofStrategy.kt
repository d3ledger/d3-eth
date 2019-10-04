/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit.endpoint

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.hashToRegistration
import com.github.kittinunf.result.map
import integration.eth.config.EthereumConfig
import integration.eth.config.EthereumPasswords
import mu.KLogging

private const val ETH_ADDRESS_KEY = "eth_address"

interface EthRegistrationProofStrategy {
    fun performRegistrationProof(irohaTxHash: IrohaTransactionHashType): EthNotaryResponse
}

/**
 * Class that is responsible for getting registration proofs 
 */
class EthRegistrationProofStrategyImpl(
    private val queryHelper: IrohaQueryHelper,
    ethereumConfig: EthereumConfig,
    ethereumPasswords: EthereumPasswords
) : EthRegistrationProofStrategy {

    private val deployHelper = DeployHelper(ethereumConfig, ethereumPasswords)

    /**
     * Performs proof of registration
     * @param irohaTxHash - hash of transaction to prove
     * @return proof
     */
    override fun performRegistrationProof(irohaTxHash: IrohaTransactionHashType): EthNotaryResponse {
        return queryHelper.getSingleTransaction(irohaTxHash)
            .map { tx ->
                val registrationCommand =
                    tx.payload.reducedPayload.commandsList.first { command -> command.hasSetAccountDetail() }
                        .setAccountDetail
                if (tx.payload.reducedPayload.creatorAccountId != registrationCommand.accountId) {
                    throw IllegalArgumentException("Cannot register account in the Ethereum network. Bad tx creator.")
                } else if (registrationCommand.key != ETH_ADDRESS_KEY) {
                    throw IllegalArgumentException("Cannot register account in the Ethereum network. No address was specified.")
                }
                val address = registrationCommand.value
                val hash = hashToRegistration(
                    address = address,
                    accountId = registrationCommand.accountId,
                    irohaHash = irohaTxHash
                )
                deployHelper.signUserData(hash)
            }.fold(
                { signature ->
                    EthNotaryResponse.Successful(signature)
                },
                { ex ->
                    logger.error("Cannot register", ex)
                    EthNotaryResponse.Error(ex.message ?: "Cannot register")
                })
    }

    companion object : KLogging()
}
