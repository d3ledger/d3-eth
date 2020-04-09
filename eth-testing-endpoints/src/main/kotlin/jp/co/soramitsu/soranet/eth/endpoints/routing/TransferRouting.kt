/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.endpoints.routing

import com.github.kittinunf.result.Result
import de.nielsfalk.ktor.swagger.created
import de.nielsfalk.ktor.swagger.description
import de.nielsfalk.ktor.swagger.post
import de.nielsfalk.ktor.swagger.responds
import de.nielsfalk.ktor.swagger.version.shared.Group
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Routing
import iroha.protocol.Endpoint
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.soranet.eth.endpoints.dto.EthTransferRequest
import jp.co.soramitsu.soranet.eth.endpoints.dto.EthWithdrawalRequest
import jp.co.soramitsu.soranet.eth.endpoints.dto.PlainResponse
import mu.KLogging

private const val ETH_ASSET = "ether#ethereum"
private val logger = KLogging().logger

@Group("eth")
@Location("/eth/transfer")
class TransferLocation

@Group("eth")
@Location("/eth/withdraw")
class WithdrawLocation

fun Routing.transfer(irohaAPI: IrohaAPI) {
    post<TransferLocation, EthTransferRequest>(
        "execute"
            .description("Transfers ETH")
            .responds(created<PlainResponse>())
    ) { _, ethTransferRequest ->
        logger.info("Transfer invoked with parameters:${ethTransferRequest.destAccountId}, ${ethTransferRequest.amount}")
        transferEth(irohaAPI, ethTransferRequest).fold(
            { call.respond(message = PlainResponse.ok(), status = HttpStatusCode.OK) },
            { ex ->
                call.respond(
                    message = PlainResponse.error(ex),
                    status = HttpStatusCode.InternalServerError
                )
            }
        )
    }
}

fun Routing.withdraw(irohaAPI: IrohaAPI, notaryAccountId: String) {
    post<WithdrawLocation, EthWithdrawalRequest>(
        "execute"
            .description("Withdraws ETH")
            .responds(created<PlainResponse>())
    ) { _, ethWithdawalRequest ->
        logger.info("Withdrawal invoked with parameters:${ethWithdawalRequest.address}, ${ethWithdawalRequest.amount}")
        withdrawEth(irohaAPI, notaryAccountId, ethWithdawalRequest).fold(
            { call.respond(message = PlainResponse.ok(), status = HttpStatusCode.OK) },
            { ex ->
                call.respond(
                    message = PlainResponse.error(ex),
                    status = HttpStatusCode.InternalServerError
                )
            }
        )
    }
}

private fun withdrawEth(
    irohaAPI: IrohaAPI,
    notaryAccountId: String,
    ethWithdrawalRequest: EthWithdrawalRequest
): Result<Unit, Exception> {
    return Result.of {
        transferEthRaw(
            irohaAPI,
            ethWithdrawalRequest.accountId,
            notaryAccountId,
            ethWithdrawalRequest.address,
            ethWithdrawalRequest.amount,
            ethWithdrawalRequest.publicKey,
            ethWithdrawalRequest.privateKey
        )
    }
}

private fun transferEth(irohaAPI: IrohaAPI, ethTransferRequest: EthTransferRequest): Result<Unit, Exception> {
    return Result.of {
        transferEthRaw(
            irohaAPI,
            ethTransferRequest.accountId,
            ethTransferRequest.destAccountId,
            "test",
            ethTransferRequest.amount,
            ethTransferRequest.publicKey,
            ethTransferRequest.privateKey
        )
    }
}

private fun transferEthRaw(
    irohaAPI: IrohaAPI,
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
