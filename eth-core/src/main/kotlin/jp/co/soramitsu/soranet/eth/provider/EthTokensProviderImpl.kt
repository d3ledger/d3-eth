/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
import mu.KLogging
import java.util.concurrent.ConcurrentHashMap

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

    private val irohaAnchoredTokens: MutableMap<String, String>
    private val ethAnchoredTokens: MutableMap<String, String>
    private val tokenPrecisions: MutableMap<String, Int>

    init {
        logger.info {
            """
                Init token provider
                Ethereum anchored token storage: '$ethAnchoredTokenStorageAccount', setter: '$ethAnchoredTokenSetterAccount'
                Iroha anchored token storage: '$irohaAnchoredTokenStorageAccount', setter: '$irohaAnchoredTokenSetterAccount'
            """.trimIndent()
        }
        irohaAnchoredTokens = ConcurrentHashMap(
            irohaQueryHelper.getAccountDetails(
                irohaAnchoredTokenStorageAccount,
                irohaAnchoredTokenSetterAccount
            ).get()
        )
        ethAnchoredTokens = ConcurrentHashMap(
            irohaQueryHelper.getAccountDetails(
                ethAnchoredTokenStorageAccount,
                ethAnchoredTokenSetterAccount
            ).get()
        )
        tokenPrecisions = mutableMapOf("$ETH_NAME#$ETH_DOMAIN" to ETH_PRECISION)
    }

    /**
     * Get all Ethereum tokens.
     * @returns map (EthreumAddress -> TokenName)
     */
    override fun getEthTokens(): Result<Map<String, String>, Exception> {
        return getEthAnchoredTokens().fanout { getIrohaAnchoredTokens() }
            .map { (ethAnchored, irohaAnchored) ->
                ethAnchored.plus(irohaAnchored).plus(ETH_ADDRESS to "$ETH_NAME#$ETH_DOMAIN")
            }
    }

    /**
     * Get tokens anchored in Ethereum.
     * @returns map (EthreumAddress -> TokenName)
     */
    override fun getEthAnchoredTokens(): Result<Map<String, String>, Exception> {
        return Result.of { ethAnchoredTokens }
    }

    /**
     * Get tokens anchored in Iroha.
     * @returns map (EthreumAddress -> TokenName)
     */
    override fun getIrohaAnchoredTokens(): Result<Map<String, String>, Exception> {
        return Result.of { irohaAnchoredTokens }
    }

    /**
     * Get precision of [assetId] asset in Iroha.
     */
    override fun getTokenPrecision(assetId: String): Result<Int, Exception> {
        return if (tokenPrecisions.contains(assetId))
            Result.of { tokenPrecisions[assetId]!! }
        else irohaQueryHelper.getAssetPrecision(assetId).map { precision ->
            tokenPrecisions[assetId] = precision
            logger.info("Got new token precision from Iroha: $assetId : $precision")
            precision
        }
    }

    /**
     * Get token address of [assetId] asset. For ether returns 0x0000000000000000000000000000000000000000
     */
    override fun getTokenAddress(assetId: String): Result<String, Exception> {
        return Result.of {
            if (assetId == "$ETH_NAME#$ETH_DOMAIN") {
                return@of ETH_ADDRESS
            }
            val ethAnchoredEntries = ethAnchoredTokens.filter { (_, value) -> value == assetId }.entries
            if (ethAnchoredEntries.isNotEmpty()) {
                return@of ethAnchoredEntries.iterator().next().key
            }

            val irohaAnchoredEntries = irohaAnchoredTokens.filter { (_, value) -> value == assetId }.entries
            if (irohaAnchoredEntries.isNotEmpty()) {
                return@of irohaAnchoredEntries.iterator().next().key
            }

            throw IllegalArgumentException("Token $assetId not found")
        }
    }

    /** Return true if asset is Iroha anchored. */
    override fun isIrohaAnchored(assetId: String): Result<Boolean, Exception> {
        return Result.of {
            if (assetId == "$ETH_NAME#$ETH_DOMAIN")
                return@of false
            return@of if (irohaAnchoredTokens.filter { (_, value) -> value == assetId }.isNotEmpty()) {
                true
            } else {
                if (ethAnchoredTokens.filter { (_, value) -> value == assetId }.isEmpty()) {
                    throw IllegalArgumentException("Token $assetId not found")
                }
                false
            }
        }
    }

    override fun filterAndExpand(block: BlockOuterClass.Block) {
        val map = block.blockV1.payload.transactionsList.filter {
            val creatorAccountId = it.payload.reducedPayload.creatorAccountId
            creatorAccountId == ethAnchoredTokenSetterAccount || creatorAccountId == irohaAnchoredTokenSetterAccount
        }.associate {
            val reducedPayload = it.payload.reducedPayload
            reducedPayload.creatorAccountId to reducedPayload.commandsList.filter { command ->
                command.hasSetAccountDetail()
            }.map { command ->
                command.setAccountDetail
            }.filter { details ->
                details.accountId == ethAnchoredTokenStorageAccount ||
                        details.accountId == irohaAnchoredTokenStorageAccount
            }
        }
        map[ethAnchoredTokenSetterAccount]?.forEach {
            if(it.accountId == ethAnchoredTokenStorageAccount) {
                ethAnchoredTokens[it.key] = it.value
                logger.info("Got new token from Iroha: ${it.key} : ${it.value}")
            }
        }
        map[irohaAnchoredTokenSetterAccount]?.forEach {
            if(it.accountId == irohaAnchoredTokenStorageAccount) {
                irohaAnchoredTokens[it.key] = it.value
                logger.info("Got new token from Iroha: ${it.key} : ${it.value}")
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
