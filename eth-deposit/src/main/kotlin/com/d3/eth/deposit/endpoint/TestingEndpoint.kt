/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit.endpoint

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.loadEthPasswords
import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogging
import java.math.BigInteger

const val DEPOSIT_PATH = "deposit"

/**
 * Class represent useful functionality for testing via REST API
 */
class TestingEndpoint(
    private val serverBundle: ServerInitializationBundle
) {

    private val deployHelper: DeployHelper

    init {
        deployHelper =
                loadLocalConfigs("predeploy.ethereum", EthereumConfig::class.java, "predeploy.properties")
                    .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
                    .map { (ethereumConfig, passwordConfig) ->
                        DeployHelper(
                            ethereumConfig,
                            passwordConfig
                        )
                    }.get()

        logger.info { "Start test deposit on port ${serverBundle.port + 1}" }

        val server = embeddedServer(Netty, port = serverBundle.port + 1) {
            install(CORS)
            {
                anyHost()
                allowCredentials = true
            }
            install(ContentNegotiation) {
                gson()
            }
            routing {
                post(serverBundle.ethRefund + "/$DEPOSIT_PATH") {
                    val testDeposit = call.receive(TestDeposit::class)
                    logger.info { "Testing deposit invoked with parameters:${testDeposit.address}, ${testDeposit.amount}" }
                    sendEth(testDeposit).fold({
                        logger.info { "Ether was sent successfully" }
                        call.respondText("", status = HttpStatusCode.NoContent)
                    },
                        { ex -> call.respondText(ex.message!!, status = HttpStatusCode.BadRequest) }
                    )
                }
            }
        }
        server.start(wait = false)
    }

    private fun sendEth(testDeposit: TestDeposit): Result<Unit, Exception> {
        return Result.of {
            deployHelper.sendEthereum(
                BigInteger(testDeposit.amount).multiply(BigInteger.valueOf(1000000000000000000)),
                testDeposit.address
            )
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

data class TestDeposit(val address: String, val amount: String)
