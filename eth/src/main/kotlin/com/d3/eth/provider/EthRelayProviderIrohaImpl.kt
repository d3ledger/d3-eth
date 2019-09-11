/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import java.util.*

/**
 * Implementation of [EthRelayProvider] with Iroha storage.
 *
 * @param queryHelper - Iroha queries network layer
 * @param notaryAccount - account that contains details
 * @param registrationAccount - account that has set details
 */
class EthRelayProviderIrohaImpl(
    private val queryHelper: IrohaQueryHelper,
    private val notaryAccount: String,
    private val registrationAccount: String
) : EthRelayProvider {
    init {
        logger.info {
            "Init relay provider with notary account '$notaryAccount' and registration account '$registrationAccount'"
        }
    }

    private val nonFreeRelayPredicate = { _: String, value: String -> value != "free" }

    /**
     * Gets all non free relay wallets
     *
     * @return map<eth_wallet -> iroha_account> in success case or exception otherwise
     */
    override fun getRelays(): Result<Map<String, String>, Exception> {
        return queryHelper.getAccountDetailsFilter(
            notaryAccount,
            registrationAccount,
            nonFreeRelayPredicate
        )
    }

    /** Get relay belonging to [irohaAccountId] */
    override fun getRelayByAccountId(irohaAccountId: String): Result<Optional<String>, Exception> {
        return queryHelper.getAccountDetailsFirst(
            notaryAccount,
            registrationAccount
        ) { _, value -> value == irohaAccountId }.map { relay ->
            if (!relay.isPresent)
                Optional.empty()
            else
                Optional.of(relay.get().first)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
