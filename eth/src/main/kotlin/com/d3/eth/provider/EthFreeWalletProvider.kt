/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.github.kittinunf.result.Result
import mu.KLogging

/**
 * Generates new wallets with given password
 * @parameter wallet - address will be returned
 */
class OneWalletProvider(private val wallet: String) :
    EthFreeClientAddressProvider {

    /**
     * Generate wallet file and return generated Ethereum address
     */
    override fun getAddress(): Result<String, Exception> = Result.of {
        logger.info { "New Ethereum address ${wallet}" }

        wallet
    }

    /** Free addresses amount is quite large */
    override fun getAddressCount(): Result<Int, Exception> = Result.of { 1 }

    /**
     * Logger
     */
    companion object : KLogging()
}
