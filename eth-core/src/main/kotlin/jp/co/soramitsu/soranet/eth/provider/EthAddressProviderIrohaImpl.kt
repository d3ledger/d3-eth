/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import iroha.protocol.QryResponses
import jp.co.soramitsu.iroha.java.ErrorResponseException
import mu.KLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap

const val ETH_WALLET = "ethereum_wallet"

/**
 * Implementation of [EthAddressProvider] with Iroha storage.
 *
 * @param queryHelper - Iroha queries network layer
 * @param storageAccountId - account that contains details
 * @param setterAccountId - account that has set details
 */
class EthAddressProviderIrohaImpl(
    private val queryHelper: IrohaQueryHelper,
    private val storageAccountId: String,
    private val setterAccountId: String,
    private val key: String
) : EthAddressProvider {

    private val registeredAddresses: MutableMap<String, String>

    init {
        logger.info {
            "Init address provider with storage account '$storageAccountId' and setter account '$setterAccountId'"
        }
        registeredAddresses = ConcurrentHashMap(
            queryHelper.getAccountDetails(
                storageAccountId,
                setterAccountId
            ).get()
        )
    }

    /**
     * Gets all non free relay wallets
     *
     * @return map<eth_wallet -> iroha_account> in success case or exception otherwise
     */
    override fun getAddresses(): Result<Map<String, String>, Exception> {
        return Result.of { registeredAddresses }
    }

    /** Get address belonging to [irohaAccountId] */
    override fun getAddressByAccountId(irohaAccountId: String): Result<Optional<String>, Exception> =
        Result.of {
            queryHelper.getAccountDetails(
                irohaAccountId,
                setterAccountId,
                key
            ).fold(
                { address ->
                    if (!address.isPresent) {
                        Optional.empty()
                    } else {
                        Optional.of(address.get())
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

    override fun addNewAddress(wallet: String, irohaAccountId: String) {
        registeredAddresses[wallet] = irohaAccountId
        logger.info("Added new eth address to the storage: $wallet : $irohaAccountId")
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
