/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.provider

import com.github.kittinunf.result.Result

/** Interface of an instance that provides with ethereum ERC20 token white list. */
interface EthTokensProvider {

    /**
     * Return all supported Ethereum tokens.
     */
    fun getEthTokens(): Result<Map<String, String>, Exception>

    /**
     * Returns ERC20 Ethereum anchored tokens list in form of
     * (Ethereum wallet -> iroha assetId)
     */
    fun getEthAnchoredTokens(): Result<Map<String, String>, Exception>

    /**
     * Returns ERC20 Iroha anchored tokens list in form of
     * (Ethereum wallet -> iroha assetId)
     */
    fun getIrohaAnchoredTokens(): Result<Map<String, String>, Exception>

    /** Return token precision by asset id */
    fun getTokenPrecision(assetId: String): Result<Int, Exception>

    /** Return token precision by asset id */
    fun getTokenAddress(assetId: String): Result<String, Exception>

    /** Return true if asset is Iroha anchored. */
    fun isIrohaAnchored(assetId: String): Result<Boolean, Exception>
}
