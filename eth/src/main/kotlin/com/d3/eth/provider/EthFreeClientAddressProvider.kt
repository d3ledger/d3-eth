/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.github.kittinunf.result.Result

/**
 * Provide free client Ethereum addresses
 */
interface EthFreeClientAddressProvider {

    /**
     * Get free client address
     */
    fun getAddress(): Result<String, Exception>

    /**
     * Get free client address count
     */
    fun getAddressCount(): Result<Int, Exception>
}
