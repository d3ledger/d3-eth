/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EthTokensProviderImplTest {

    private val ethAnchored = mapOf(
        "0x0001" to "token_1#ethereum",
        "0x0002" to "token_2#ethereum",
        "0x0003" to "token_3#ethereum"
    )

    private val irohaAnchored = mapOf(
        "0xAAA1" to "token_1#sora",
        "0xAAA2" to "token_2#sora",
        "0xAAA3" to "token_3#sora"
    )

    private val wrongAssetId = "wrong#asset"
    private val ethAssetId = "$ETH_NAME#$ETH_DOMAIN"

    private val ethAnchoredTokenStorageAccount = "eth_anchored_token_storage@notary"
    private val ethAnchoredTokenSetterAccount = "eth_anchored_token_setter@notary"
    private val irohaAnchoredTokenStorageAccount = "iroha_anchored_token_storage@notary"
    private val irohaAnchoredTokenSetterAccount = "iroha_anchored_token_setter@notary"

    private val irohaQueryHelper = mock<IrohaQueryHelper> {
        on {
            getAccountDetails(
                eq(ethAnchoredTokenStorageAccount),
                eq(ethAnchoredTokenSetterAccount)
            )
        } doReturn Result.of { ethAnchored }

        on {
            getAccountDetails(
                eq(irohaAnchoredTokenStorageAccount),
                eq(irohaAnchoredTokenSetterAccount)
            )
        } doReturn Result.of { irohaAnchored }
    }

    private val ethTokenProvider = EthTokensProviderImpl(
        irohaQueryHelper,
        ethAnchoredTokenStorageAccount,
        ethAnchoredTokenSetterAccount,
        irohaAnchoredTokenStorageAccount,
        irohaAnchoredTokenSetterAccount
    )

    /**
     * @given initialized ethTokenProvider and lists of iroha and ethereum anchored tokens
     * @when getTokenAddress() called with assesId
     * @then corresponding address is returned
     */
    @Test
    fun getTokenAddressTest() {
        ethAnchored.forEach { (address, assetId) ->
            assertEquals(address, ethTokenProvider.getTokenAddress(assetId).get())
        }

        irohaAnchored.forEach { (address, assetId) ->
            assertEquals(address, ethTokenProvider.getTokenAddress(assetId).get())
        }
    }

    /**
     * @given initialized ethTokenProvider and wrongAssetId is not present
     * @when query wrongAssetId
     * @then IllegalArgumentException exception is thrown
     */
    @Test
    fun getWrongAssetIdTest() {
        assertThrows<IllegalArgumentException> {
            ethTokenProvider.getTokenAddress(wrongAssetId).get()
        }
    }

    /**
     * @given ethTokenProvider is initialized with Iroha anchored tokens
     * @when function isIrohaAnchored called with Iroha anchored tokens
     * @then returns true
     */
    @Test
    fun isIrohaAnchoredTest() {
        irohaAnchored.forEach { (_, assetId) ->
            assertTrue { ethTokenProvider.isIrohaAnchored(assetId).get() }
        }
    }

    /**
     * @given ethTokenProvider is initialized with Ethereum anchored tokens
     * @when function isIrohaAnchored called with Ethereum anchored tokens
     * @then returns false
     */
    @Test
    fun isNotIrohaAnchoredTest() {
        ethAnchored.forEach { (_, assetId) ->
            assertFalse { ethTokenProvider.isIrohaAnchored(assetId).get() }
        }
    }

    /**
     * @given ethTokenProvider is initialized with tokens
     * @when function isIrohaAnchored called with Ether
     * @then returns false
     */
    @Test
    fun isEthIrohaAnchoredTest() {
        assertFalse { ethTokenProvider.isIrohaAnchored(ethAssetId).get() }
    }

    /**
     * @given ethTokenProvider is initialized with tokens
     * @when function isIrohaAnchored called with wrong asset id
     * @then Exception is thrown
     */
    @Test
    fun wrongAssetIsIrohaAnchoredTest() {
        assertThrows<IllegalArgumentException> {
            ethTokenProvider.isIrohaAnchored(wrongAssetId).get()
        }
    }
}
