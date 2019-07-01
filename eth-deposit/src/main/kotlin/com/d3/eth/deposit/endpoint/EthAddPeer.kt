/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit.endpoint

/**
 * Add peer structure
 */
data class EthAddPeer(
    val ethereumAddess: EthereumAddress,
    val irohaTxHash: IrohaTransactionHashType
)
