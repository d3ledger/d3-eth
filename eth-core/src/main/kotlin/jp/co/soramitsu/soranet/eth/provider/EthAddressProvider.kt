/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.provider

import com.github.kittinunf.result.Result
import java.util.*

/** Interface of an instance that provides deployed ethereum relays */
interface EthAddressProvider {

    /** Returns relays in form of (ethereum wallet -> iroha user name) */
    fun getAddresses(): Result<Map<String, String>, Exception>

    /**
     * Get relay belonging to [irohaAccountId]
     * @return relay or null if relay is absent
     */
    fun getAddressByAccountId(irohaAccountId: String): Result<Optional<String>, Exception>
}
