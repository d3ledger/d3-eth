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
import jp.co.soramitsu.soranet.eth.endpoints.dto.EthDepositRequest
import jp.co.soramitsu.soranet.eth.endpoints.dto.PlainResponse
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
import mu.KLogging
import java.math.BigInteger

private val logger = KLogging().logger

@Group("eth")
@Location("/eth/deposit")
class DepositLocation

fun Routing.deposit(deployHelper: DeployHelper) {
    post<DepositLocation, EthDepositRequest>(
        "execute"
            .description("Sends ETH to a specified address")
            .responds(created<PlainResponse>())
    ) { _, ethDepositRequest ->
        logger.info("Eth deposit invoked with parameters:${ethDepositRequest.address}, ${ethDepositRequest.amount}")
        sendEth(deployHelper, ethDepositRequest).fold(
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

private fun sendEth(
    deployHelper: DeployHelper,
    ethDepositRequest: EthDepositRequest
): Result<String, Exception> {
    return Result.of {
        deployHelper.sendEthereum(
            BigInteger(ethDepositRequest.amount).multiply(BigInteger.valueOf(1000000000000000000)),
            ethDepositRequest.address
        )
    }
}
