/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.sidechain.provider.ChainAddressProvider
import com.github.kittinunf.result.Result
import iroha.protocol.QryResponses
import jp.co.soramitsu.iroha.java.ErrorResponseException
import mu.KLogging
import java.util.*

const val ETH_RELAY = "ethereum_relay"
const val ETH_CLIENT_WALLET = "ethereum_client_wallet"

/**
 * Implementation of [ChainAddressProvider] with Iroha storage.
 *
 * @param queryHelper - Iroha queries network layer
 * @param storageAccount - account that contains details
 * @param registrationAccount - account that has set details
 */
class EthWalletProviderIrohaImpl(
    private val queryHelper: IrohaQueryHelper,
    private val storageAccount: String,
    private val registrationAccount: String,
    private val ethKey: String = ETH_RELAY,
    private val filterPredicate: (String, String) -> Boolean = { _: String, value: String -> value != "free" }
) : ChainAddressProvider {
    init {
        logger.info {
            "Init relay provider with storage account '$storageAccount' and registration account '$registrationAccount'"
        }
    }

    /**
     * Gets all non free relay wallets
     *
     * @return map<eth_wallet -> iroha_account> in success case or exception otherwise
     */
    override fun getAddresses(): Result<Map<String, String>, Exception> {
        return queryHelper.getAccountDetailsFilter(
            storageAccount,
            registrationAccount,
            filterPredicate
        )
    }

    /** Get relay belonging to [irohaAccountId] */
    override fun getAddressByAccountId(irohaAccountId: String): Result<Optional<String>, Exception> =
        Result.of {
            queryHelper.getAccountDetails(
                irohaAccountId,
                registrationAccount,
                ethKey
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
