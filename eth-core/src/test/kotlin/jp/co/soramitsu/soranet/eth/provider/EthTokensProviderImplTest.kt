/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.provider

import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.GsonInstance
import com.nhaarman.mockitokotlin2.*
import iroha.protocol.Primitive
import iroha.protocol.QryResponses
import jp.co.soramitsu.iroha.java.QueryAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EthTokensProviderImplTest {

    private val gson = GsonInstance.get()

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

    private val mockEthAnchoredResponse = buildDetailsResponse(ethAnchoredTokenSetterAccount, ethAnchored)
    private val mockIrohaAnchoredResponse =
        buildDetailsResponse(irohaAnchoredTokenSetterAccount, irohaAnchored)
    private val queryAPI = mock<QueryAPI> {
        on {
            getAccountDetails(
                eq(ethAnchoredTokenStorageAccount),
                eq(ethAnchoredTokenSetterAccount),
                isNull(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        } doReturn mockEthAnchoredResponse
        on {
            getAccountDetails(
                eq(irohaAnchoredTokenStorageAccount),
                eq(irohaAnchoredTokenSetterAccount),
                isNull(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        } doReturn mockIrohaAnchoredResponse
    }

    private val irohaQueryHelper = IrohaQueryHelperImpl(queryAPI)

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

    private fun buildDetailsResponse(
        writer: String,
        map: Map<String, String>
    ): QryResponses.AccountDetailResponse {
        val builder = QryResponses.AccountDetailResponse.newBuilder()
        builder.clearNextRecordId()
        builder.nextRecordId = Primitive.AccountDetailRecordId.getDefaultInstance()
        builder.detail = "{\"$writer\":${gson.toJson(map)}}"
        return spy(builder.build()) {
            on {
                hasNextRecordId()
            } doReturn false
        }
    }
}
