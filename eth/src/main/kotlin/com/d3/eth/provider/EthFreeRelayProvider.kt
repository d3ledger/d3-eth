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
 * @param storageAccount - account in Iroha to write down the information about free relay wallets has been added
 */
// TODO Prevent double relay accounts usage (in perfect world it is on Iroha side with custom code). In real world
// on provider side with some synchronization.
class EthFreeRelayProvider(
    private val queryHelper: IrohaQueryHelper,
    private val storageAccount: String,
    private val setterAccount: String
) {

    private val freeRelayPredicate = { _: String, value: String -> value == "free" }

    init {
        logger.info {
            "Init free relay provider with holder account '$storageAccount' and setter account '$setterAccount'"
        }
    }

    /**
     * Get first free Ethereum relay wallet.
     * @return free Ethereum relay wallet
     */
    fun getRelay(): Result<String, Exception> {
        return queryHelper.getAccountDetailsFirst(
            storageAccount,
            setterAccount,
            freeRelayPredicate
        ).map { freeWallet ->
            if (!freeWallet.isPresent)
                throw IllegalStateException("EthFreeRelayProvider - no free relay wallets created by $setterAccount")
            freeWallet.get().key
        }
    }

    /**
     * Get all free Ethereum relay wallets
     * @return free Ethereum relay wallets
     */
    fun getRelays(): Result<Set<String>, Exception> {
        return queryHelper.getAccountDetailsFilter(
            storageAccount,
            setterAccount,
            freeRelayPredicate
        ).map { relays ->
            relays.keys
        }
    }

    /**
     * Get number of all free Ethereum relay wallets
     * @return number of free Ethereum relay wallets
     */
    fun getRelaysCount(): Result<Int, Exception> {
        return queryHelper.getAccountDetailsCount(
            storageAccount,
            setterAccount,
            freeRelayPredicate
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
