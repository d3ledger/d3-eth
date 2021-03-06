/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.endpoints.endpoint

import com.d3.eth.endpoints.routing.deposit
import com.d3.eth.endpoints.routing.transfer
import com.d3.eth.endpoints.routing.withdraw
import com.d3.eth.sidechain.util.DeployHelper
import de.nielsfalk.ktor.swagger.SwaggerSupport
import de.nielsfalk.ktor.swagger.version.v2.Swagger
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.locations.Locations
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging
import java.io.Closeable
import java.util.concurrent.TimeUnit


/**
 * Class represent useful functionality for testing via REST API
 */
class TestingEndpoint(
    port: Int,
    private val deployHelper: DeployHelper,
    private val irohaAPI: IrohaAPI,
    private val notaryAccountId: String
) : Closeable {

    private val server: ApplicationEngine

    init {
        server = embeddedServer(Netty, port = port) {
            install(CORS)
            {
                anyHost()
                allowCredentials = true
            }
            install(ContentNegotiation) {
                gson()
            }
            install(Locations)
            install(SwaggerSupport) {
                swagger = Swagger()
            }
            routing {
                deposit(deployHelper)
                transfer(irohaAPI)
                withdraw(irohaAPI, notaryAccountId)
            }
        }
        server.start(wait = false)
    }

    override fun close() = server.stop(5, 5, TimeUnit.SECONDS)

    /**
     * Logger
     */
    companion object : KLogging()
}

