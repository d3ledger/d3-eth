/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging

const val ETH_NAME = "ether"
const val ETH_DOMAIN = "ethereum"
const val ETH_PRECISION: Int = 18
const val ETH_ADDRESS = "0x0000000000000000000000000000000000000000"

/**
 * Implementation of [EthTokensProvider] with Iroha storage.
 *
 * @param irohaQueryHelper - iroha queries network layer
 * @param ethAnchoredTokenStorageAccount - tokenStorageAccount that contains details about Ethereum
 * anchored ERC20 tokens
 * @param ethAnchoredTokenSetterAccount - tokenSetterAccount that set details about ERC20 tokens
 * anchored in Ethereum
 * @param irohaAnchoredTokenStorageAccount - tokenStorageAccount that contains details about Iroha
 * anchored ERC20 tokens
 * @param irohaAnchoredTokenSetterAccount - tokenSetterAccount that set details about ERC20 tokens
 * anchored in Iroha
 */
class EthTokensProviderImpl(
    private val irohaQueryHelper: IrohaQueryHelper,
    private val ethAnchoredTokenStorageAccount: String,
    private val ethAnchoredTokenSetterAccount: String,
    private val irohaAnchoredTokenStorageAccount: String,
    private val irohaAnchoredTokenSetterAccount: String
) : EthTokensProvider {

    init {
        logger.info {
            """
                Init token provider
                Ethereum anchored token storage: '$ethAnchoredTokenStorageAccount', setter: '$ethAnchoredTokenSetterAccount'
                Iroha anchored token storage: '$irohaAnchoredTokenStorageAccount', setter: '$irohaAnchoredTokenSetterAccount'
            """.trimIndent()
        }
    }

    /**
     * Get all Ethereum tokens.
     * @returns map (EthreumAddress -> TokenName)
     */
    override fun getEthTokens(): Result<Map<String, String>, Exception> {
        return irohaQueryHelper.getAccountDetails(
            ethAnchoredTokenStorageAccount,
            ethAnchoredTokenSetterAccount
        ).fanout {
            irohaQueryHelper.getAccountDetails(
                irohaAnchoredTokenStorageAccount,
                irohaAnchoredTokenSetterAccount
            )
        }.map { (ethAnchored, irohaAnchored) ->
            ethAnchored.plus(irohaAnchored).plus(ETH_ADDRESS to "$ETH_NAME#$ETH_DOMAIN")
        }
    }

    /**
     * Get tokens anchored in Ethereum.
     * @returns map (EthreumAddress -> TokenName)
     */
    override fun getEthAnchoredTokens(): Result<Map<String, String>, Exception> {
        return irohaQueryHelper.getAccountDetails(
            ethAnchoredTokenStorageAccount,
            ethAnchoredTokenSetterAccount
        )
    }

    /**
     * Get tokens anchored in Iroha.
     * @returns map (EthreumAddress -> TokenName)
     */
    override fun getIrohaAnchoredTokens(): Result<Map<String, String>, Exception> {
        return irohaQueryHelper.getAccountDetails(
            irohaAnchoredTokenStorageAccount,
            irohaAnchoredTokenSetterAccount
        )
    }

    /**
     * Get precision of [assetId] asset in Iroha.
     */
    override fun getTokenPrecision(assetId: String): Result<Int, Exception> {
        return if (assetId == "$ETH_NAME#$ETH_DOMAIN")
            Result.of { ETH_PRECISION }
        else irohaQueryHelper.getAssetPrecision(assetId)
    }

    /**
     * Get token address of [assetId] asset. For ether returns 0x0000000000000000000000000000000000000000
     */
    override fun getTokenAddress(assetId: String): Result<String, Exception> {
        return if (assetId == "$ETH_NAME#$ETH_DOMAIN")
            Result.of { ETH_ADDRESS }
        else irohaQueryHelper.getAccountDetailsFirst(
            ethAnchoredTokenStorageAccount,
            ethAnchoredTokenSetterAccount
        ) { _, value -> value == assetId }
            .flatMap { tokenAddress ->
                if (tokenAddress.isPresent) {
                    Result.of { tokenAddress }
                } else {
                    irohaQueryHelper.getAccountDetailsFirst(
                        irohaAnchoredTokenStorageAccount,
                        irohaAnchoredTokenSetterAccount
                    ) { _, value -> value == assetId }
                }
            }
            .map { tokenAddress ->
                if (!tokenAddress.isPresent) {
                    throw IllegalArgumentException("Token $assetId not found")
                } else {
                    tokenAddress.get().key
                }
            }
    }

    /**
     * @inheritdoc
     */
    override fun isIrohaAnchored(assetId: String): Result<Boolean, Exception> {
        if (assetId == "$ETH_NAME#$ETH_DOMAIN")
            return Result.of { false }
        return irohaQueryHelper.getAccountDetailsFirst(
            ethAnchoredTokenStorageAccount,
            ethAnchoredTokenSetterAccount
        ) { _, value -> value == assetId }.fanout {
            irohaQueryHelper.getAccountDetailsFirst(
                irohaAnchoredTokenStorageAccount,
                irohaAnchoredTokenSetterAccount
            ) { _, value -> value == assetId }
        }.map { (ethAnchored, irohaAnchored) ->
            if (irohaAnchored.isPresent)
                return@map true
            if (!ethAnchored.isPresent)
                throw IllegalArgumentException("Token $assetId not found")
            false
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
