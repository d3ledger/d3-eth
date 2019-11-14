/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import iroha.protocol.QryResponses
import jp.co.soramitsu.iroha.java.ErrorResponseException
import mu.KLogging
import java.util.*

const val ETH_RELAY = "ethereum_relay"
const val ETH_WALLET = "ethereum_wallet"

/**
 * Implementation of [EthAddressProvider] with Iroha storage.
 *
 * @param queryHelper - Iroha queries network layer
 * @param notaryAccount - account that contains details
 * @param registrationAccount - account that has set details
 */
class EthAddressProviderIrohaImpl(
    private val queryHelper: IrohaQueryHelper,
    private val storageAccountId: String,
    private val setterAccountId: String,
    private val key: String
) : EthAddressProvider {
    init {
        logger.info {
            "Init relay provider with storage account '$storageAccountId' and setter account '$setterAccountId'"
        }
    }

    private val nonFreeRelayPredicate = { _: String, value: String -> value != "free" }

    /**
     * Gets all non free relay wallets
     *
     * @return map<eth_wallet -> iroha_account> in success case or exception otherwise
     */
    override fun getAddresses(): Result<Map<String, String>, Exception> {
        return queryHelper.getAccountDetailsFilter(
            storageAccountId,
            setterAccountId,
            nonFreeRelayPredicate
        )
    }

    /** Get relay belonging to [irohaAccountId] */
    override fun getAddressByAccountId(irohaAccountId: String): Result<Optional<String>, Exception> = Result.of {
        queryHelper.getAccountDetails(
            irohaAccountId,
            setterAccountId,
            key
        ).fold(
            { relay ->
                if (!relay.isPresent) {
                    Optional.empty()
                } else {
                    Optional.of(relay.get())
                }
            }, { ex ->
                if (ex is ErrorResponseException && ex.errorResponse.reason == QryResponses.ErrorResponse.Reason.NO_ACCOUNT_DETAIL) {
                    // if no account was found
                    Optional.empty()
                } else {
                    // if another error or exception occurred
                    throw ex
                }
            })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
