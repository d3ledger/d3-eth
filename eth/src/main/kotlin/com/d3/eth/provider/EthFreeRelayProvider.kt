/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging

/**
 * Provides with free ethereum relay wallet
 * @param queryHelper - iroha queries network layer
 * @param notaryIrohaAccount - Master notary account in Iroha to write down the information about free relay wallets has been added
 */
// TODO Prevent double relay accounts usage (in perfect world it is on Iroha side with custom code). In real world
// on provider side with some synchronization.
class EthFreeRelayProvider(
    private val queryHelper: IrohaQueryHelper,
    private val notaryIrohaAccount: String,
    private val registrationIrohaAccount: String
) : EthFreeClientAddressProvider {

    private val freeRelayPredicate = { _: String, value: String -> value == "free" }

    init {
        logger.info {
            "Init free relay provider with holder account '$notaryIrohaAccount' and setter account '$registrationIrohaAccount'"
        }
    }

    override fun getAddress(): Result<String, Exception> {
        return queryHelper.getAccountDetailsFirst(
            notaryIrohaAccount,
            registrationIrohaAccount,
            freeRelayPredicate
        ).map { freeWallet ->
            if (!freeWallet.isPresent)
                throw IllegalStateException("EthFreeRelayProvider - no free relay wallets created by $registrationIrohaAccount")
            freeWallet.get().key
        }
    }

    /**
     * Get number of all free Ethereum relay wallets
     * @return number of free Ethereum relay wallets
     */
    override fun getAddressCount(): Result<Int, Exception> {
        return queryHelper.getAccountDetailsCount(
            notaryIrohaAccount,
            registrationIrohaAccount,
            freeRelayPredicate
        )
    }

    /**
     * Get all free Ethereum relay wallets
     * @return free Ethereum relay wallets
     */
    fun getRelays(): Result<Set<String>, Exception> {
        return queryHelper.getAccountDetailsFilter(
            notaryIrohaAccount,
            registrationIrohaAccount,
            freeRelayPredicate
        ).map { relays ->
            relays.keys
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
