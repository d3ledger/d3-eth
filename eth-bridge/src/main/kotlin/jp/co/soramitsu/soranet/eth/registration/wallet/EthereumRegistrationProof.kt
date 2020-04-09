/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.registration.wallet

import com.google.gson.Gson
import jp.co.soramitsu.soranet.eth.sidechain.util.VRSSignature
import org.web3j.crypto.Keys
import java.math.BigInteger

const val ETH_REGISTRATION_KEY = "register_wallet"

/**
 * Proof that Ethereum account belongs to the owner of the private key.
 * EC Signature of an address generated from public key.
 */
data class EthereumRegistrationProof(
    val signature: VRSSignature,
    val publicKey: BigInteger
) {
    /**
     * Ethereum address derived from a public key
     */
    fun getAddress(): String = Keys.getAddress(publicKey)

    /**
     * Serialize this to json
     */
    fun toJson(): String = Gson().toJson(this)
}
