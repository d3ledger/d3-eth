/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge.endpoint

import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.squareup.moshi.Moshi
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

/**
 * Fixture for testing notary.endpoint
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefundServerEndpointTest {

    /** Server initialization bundle */
    private val serverBundle = ServerInitializationBundle(8080, "eth")

    /** JSON adapter */
    private val moshi = Moshi.Builder().add(EthNotaryResponseMoshiAdapter()).add(
        BigInteger::class.java,
        BigIntegerMoshiAdapter()
    ).build()

    /** Successful response */
    private val successResponse = EthNotaryResponse.Successful(
        "signature"
    )

    private val ethAddPeerStrategyMock = mock<EthAddPeerStrategy> {
        val request = any<IrohaTransactionHashType>()
        on {
            performAddPeer(request)
        } doReturn successResponse
    }

    private val server =
        EthServerEndpoint(serverBundle, ethAddPeerStrategyMock)

    @AfterAll
    fun tearDown() {
        server.close()
    }

    /**
     * @given initialized server class
     * @when  call onCallAddPeerRefund()
     * @then  check that answer returns success
     */
    @Test
    fun onEthAddPeerCallTest() {
        val irohaTxHash = "tx_hash_from_iroha"
        val request = moshi.adapter(IrohaTransactionHashType::class.java).toJson(irohaTxHash)
        val result = server.onCallAddPeer(request)

        assertEquals(HttpStatusCode.OK, result.code)
        assertEquals(
            successResponse,
            moshi.adapter(EthNotaryResponse::class.java).fromJson(result.message)
        )
    }

    /**
     * @given initialized server class
     * @when  call onCallAddPeerRefund() with null parameter
     * @then  check that answer returns key not found
     */
    @Test
    fun emptyAddPeerCall() {
        val failureResponse = EthNotaryResponse.Error("Request has been failed. Error in URL")

        val answer = server.onCallAddPeer(null)

        assertEquals(HttpStatusCode.BadRequest, answer.code)
        assertEquals(failureResponse.reason, answer.message)
    }
}
