/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge

import jp.co.soramitsu.soranet.eth.sidechain.util.VRSSignature

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
