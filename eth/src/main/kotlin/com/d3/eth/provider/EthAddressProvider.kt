/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.github.kittinunf.result.Result
import java.util.*

/** Interface that provides relation between deployed Ethereum addresses and iroha accounts */
interface EthAddressProvider {

    /** Return all addresses in form of (ethereum address -> Iroha account id) */
    fun getAddresses(): Result<Map<String, String>, Exception>

    /**
     * Get relay belonging to [irohaAccountId]
     * @return address or [Optional.empty] if address is absent
     */
    fun getAddressByAccountId(irohaAccountId: String): Result<Optional<String>, Exception>
}
