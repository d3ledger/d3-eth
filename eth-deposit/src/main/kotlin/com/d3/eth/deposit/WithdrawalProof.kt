/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit

import java.math.BigInteger

/**
 * Represents Ethereum withdrawal proof from one node
 */
data class WithdrawalProof(
    // Client Iroha account id
    val accountId: String,

    // ethereum address of token contract
    val tokenContractAddress: String,

    // amount of assets to withdraw
    val amount: String,

    // beneficiary ethereum address
    val beneficiary: String,

    // initial iroha withdrawal transaction
    val irohaHash: String,

    // caller ethereum address
    val relay: String,

    // ethereum notary signature
    val signature: VRSSignature
)

/**
 * Data class which stores signature splitted into components
 * @param v v component of signature
 * @param r r component of signature in hex
 * @param s s component of signature in hex
 */
data class VRSSignature(val v: BigInteger, val r: String, val s: String)
