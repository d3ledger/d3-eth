/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.github.kittinunf.result.Result

/** Provides Ethereum contract address */
interface EthAddressesProvider {

    /** Get Ethereum address of master contract */
    fun getEtereumAddress(): Result<String, Exception>
}
