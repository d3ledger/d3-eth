/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.endpoints

import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.Result
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
import iroha.protocol.Endpoint
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import java.math.BigInteger

const val DEPOSIT_PATH = "deposit"
const val WITHDRAWAL_PATH = "withdraw"
const val TRANSFER_PATH = "transfer"
const val ETH_ASSET = "ether#ethereum"

/**
 * Class represent useful functionality for testing via REST API
 */
class TestingEndpoint(
    private val serverBundle: ServerInitializationBundle,
    private val deployHelper: DeployHelper,
    private val irohaAPI: IrohaAPI,
    private val notaryAccountId: String
) {

    init {
        logger.info { "Start test deposit on port ${serverBundle.port}" }

        val server = embeddedServer(Netty, port = serverBundle.port) {
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
                post(serverBundle.ethRefund + "/$WITHDRAWAL_PATH") {
                    val testWithdrawal = call.receive(TestWithdrawal::class)
                    logger.info { "Testing withdrawal invoked with parameters:${testWithdrawal.address}, ${testWithdrawal.amount}" }
                    withdrawEth(testWithdrawal).fold({
                        logger.info { "Ether was withdrawn successfully" }
                        call.respondText("", status = HttpStatusCode.NoContent)
                    },
                        { ex -> call.respondText(ex.message!!, status = HttpStatusCode.BadRequest) }
                    )
                }
                post(serverBundle.ethRefund + "/$TRANSFER_PATH") {
                    val testTransfer = call.receive(TestTransfer::class)
                    logger.info { "Testing transfer invoked with parameters:${testTransfer.destAccountId}, ${testTransfer.amount}" }
                    transferEth(testTransfer).fold({
                        logger.info { "Ether was transferred successfully" }
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

    private fun withdrawEth(testWithdrawal: TestWithdrawal): Result<Unit, Exception> {
        return Result.of {
            transferEthRaw(
                testWithdrawal.accountId,
                notaryAccountId,
                testWithdrawal.address,
                testWithdrawal.amount,
                testWithdrawal.publicKey,
                testWithdrawal.privateKey
            )
        }
    }

    private fun transferEth(testTransfer: TestTransfer): Result<Unit, Exception> {
        return Result.of {
            transferEthRaw(
                testTransfer.accountId,
                testTransfer.destAccountId,
                "test",
                testTransfer.amount,
                testTransfer.publicKey,
                testTransfer.privateKey
            )
        }
    }

    private fun transferEthRaw(
        accountId: String,
        destAccountId: String,
        description: String,
        amount: String,
        publicKey: String,
        privateKey: String
    ) {
        val response = irohaAPI.transaction(
            Transaction.builder(accountId)
                .transferAsset(
                    accountId,
                    destAccountId,
                    ETH_ASSET,
                    description,
                    amount
                )
                .setQuorum(2)
                .sign(
                    Utils.parseHexKeypair(
                        publicKey,
                        privateKey
                    )
                )
                .build()
        ).lastElement().blockingGet()
        if (response.txStatus != Endpoint.TxStatus.COMMITTED) {
            throw Exception("Not committed in Iroha. Got response:\n$response")
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

data class TestDeposit(val address: String, val amount: String)

data class TestWithdrawal(
    val accountId: String,
    val address: String,
    val amount: String,
    val publicKey: String,
    val privateKey: String
)

data class TestTransfer(
    val accountId: String,
    val destAccountId: String,
    val amount: String,
    val publicKey: String,
    val privateKey: String
)
