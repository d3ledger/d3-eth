/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging

/** EthAddressProvider implementation.
 * Firstly try to load from system environments, in case of failure loads from Iroha
 * @param environmentVariableName - environment variable with contract address
 * @param ethContractAddressStorageAccountId - storage Iroha account id
 * @param ethContractAddressWriterAccountId - writer Iroha account id
 * @param detailsKey - Iroha key for detail
 * @param irohaQueryHelper - Iroha query helper implementaion
 */
class EthAddressesProviderSystemEnvOrIrohaDetailsImpl(
    private val environmentVariableName: String,
    private val ethContractAddressStorageAccountId: String,
    private val ethContractAddressWriterAccountId: String,
    private val detailsKey: String,
    private val irohaQueryHelper: IrohaQueryHelper
) : EthAddressesProvider {

    override fun getEtereumAddress(): Result<String, Exception> {
        val env = System.getenv(environmentVariableName)
        return if (env == null) {
            irohaQueryHelper.getAccountDetails(
                ethContractAddressStorageAccountId,
                ethContractAddressWriterAccountId
            ).map {
                if (!it.containsKey(detailsKey))
                    throw IllegalArgumentException(
                        "Ethereum address not found neither in $environmentVariableName " +
                                "nor in Iroha account details of $ethContractAddressStorageAccountId, " +
                                "writer $ethContractAddressWriterAccountId, key $detailsKey"
                    )
                val res = it.get(detailsKey)!!
                logger.debug { "Load Ethereum contract address from Iroha: $res" }
                res
            }
        } else {
            Result.of {
                logger.info { "Get Ethereum address from system environment: $env" }
                env
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
