/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit.endpoint

import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.squareup.moshi.Moshi
import io.ktor.http.HttpStatusCode
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

    /** Stub for Ethereum refund request, should represent tx hash from Iroha */
    private val ethRequest = EthRefundRequest("tx_hash_from_iroha")

    /** Successful response */
    private val successResponse = EthNotaryResponse.Successful(
        "signature"
    )

    /** Strategy mock that always returns success */
    private val ethRefundStrategyMock = mock<EthRefundStrategy> {
        val request = any<EthRefundRequest>()
        on {
            performRefund(request)
        } doReturn successResponse
    }

    private val ethAddPeerStrategyMock = mock<EthAddPeerStrategy> {
        val request = any<IrohaTransactionHashType>()
        on {
            performAddPeer(request)
        } doReturn successResponse
    }

    private val server =
        RefundServerEndpoint(serverBundle, ethRefundStrategyMock, ethAddPeerStrategyMock)

    /**
     * @given initialized server class
     * @when  call onCallEthRefund()
     * @then  check that answer returns success
     */
    @Test
    fun onEthRefundCallTest() {
        val request = moshi.adapter(EthRefundRequest::class.java).toJson(ethRequest)
        val result = server.onCallEthRefund(request)

        assertEquals(HttpStatusCode.OK, result.code)
        assertEquals(
            successResponse,
            moshi.adapter(EthNotaryResponse::class.java).fromJson(result.message)
        )
    }

    /**
     * @given initialized server class
     * @when  call onCallEthRefund() with null parameter
     * @then  check that answer returns key not found
     */
    @Test
    fun emptyRefundCall() {
        val failureResponse = EthNotaryResponse.Error("Request has been failed. Error in URL")

        val refundAnswer = server.onCallEthRefund(null)

        assertEquals(HttpStatusCode.BadRequest, refundAnswer.code)
        assertEquals(failureResponse.reason, refundAnswer.message)
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
