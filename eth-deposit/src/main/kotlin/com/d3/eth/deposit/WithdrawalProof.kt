/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit

/**
 * Represents Ethereum withdrawal proof from one node
 */
data class WithdrawalProof(
    val tokenContractAddress: String,
    val amount: String,
    val beneficiary: String,
    val irohaHash: String,
    val relay: String,
    val signature: String
)
