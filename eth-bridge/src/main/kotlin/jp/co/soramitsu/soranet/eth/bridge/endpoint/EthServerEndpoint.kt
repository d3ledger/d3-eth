/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge.endpoint

import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.squareup.moshi.Moshi
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import mu.KLogging
import java.io.Closeable
import java.math.BigInteger
import java.util.concurrent.TimeUnit

data class Response(val code: HttpStatusCode, val message: String)

/**
 * Class is waiting for custodian's intention for rollback
 */
class EthServerEndpoint(
    private val serverBundle: ServerInitializationBundle,
    private val addPeerStrategy: EthAddPeerStrategy
): Closeable {
    private val moshi = Moshi
        .Builder()
        .add(EthNotaryResponseMoshiAdapter())
        .add(BigInteger::class.java, BigIntegerMoshiAdapter())
        .build()!!
    private val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
    private val server: NettyApplicationEngine

    init {
        logger.info { "Start refund server on port ${serverBundle.port}" }

        server = embeddedServer(Netty, port = serverBundle.port) {
            install(CORS)
            {
                anyHost()
                allowCredentials = true
            }
            install(ContentNegotiation) {
                gson()
            }
            routing {
                get("ethereum/proof/add_peer/{tx_hash}") {
                    logger.info { "Add peer endpoint called with parameters: ${call.parameters}" }
                    val response = onCallAddPeer(call.parameters["tx_hash"])
                    call.respondText(response.message, status = response.code)
                }
                get("/actuator/health") {
                    call.respond(
                        mapOf(
                            "status" to "UP"
                        )
                    )
                }
            }
        }
        server.start(wait = false)
    }

    /**
     * Add new peer proof for Ethereum
     */
    fun onCallAddPeer(rawRequest: String?): Response {
        return rawRequest?.let { request ->
            val response = addPeerStrategy.performAddPeer(request)
            notaryResponseToHTTPResponse(response)
        } ?: onErrorPipelineCall()
    }

    /**
     * Transforms EthNotaryResponse to Response
     */
    private fun notaryResponseToHTTPResponse(notaryResponse: EthNotaryResponse): Response {
        return when (notaryResponse) {
            is EthNotaryResponse.Successful -> Response(
                HttpStatusCode.OK,
                ethNotaryAdapter.toJson(notaryResponse)
            )
            is EthNotaryResponse.Error -> {
                logger.error(notaryResponse.reason)
                Response(HttpStatusCode.BadRequest, ethNotaryAdapter.toJson(notaryResponse))
            }
        }
    }

    /**
     * Method return response on stateless invalid request
     */
    private fun onErrorPipelineCall(): Response {
        logger.error { "Request has been failed" }
        return Response(HttpStatusCode.BadRequest, "Request has been failed. Error in URL")
    }

    override fun close() {
        server.stop(1, 1, TimeUnit.SECONDS)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
