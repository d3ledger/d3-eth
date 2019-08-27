/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.endpoints.dto

data class EthDepositRequest(val address: String, val amount: String)

data class EthWithdrawalRequest(
    val accountId: String,
    val address: String,
    val amount: String,
    val publicKey: String,
    val privateKey: String
)

data class EthTransferRequest(
    val accountId: String,
    val destAccountId: String,
    val amount: String,
    val publicKey: String,
    val privateKey: String
)

/**
 * Class that represents plain response
 */
data class PlainResponse(val message: String, val error: Exception? = null) {
    companion object {
        fun ok() = PlainResponse("Ok")
        fun error(ex: Exception) = PlainResponse("Error", ex)
    }
}
