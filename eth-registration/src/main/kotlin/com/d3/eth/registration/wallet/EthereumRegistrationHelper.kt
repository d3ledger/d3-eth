/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration.wallet

import com.d3.eth.sidechain.util.VRSSignature
import com.d3.eth.sidechain.util.prepareDataToSign
import org.apache.commons.codec.binary.Hex
import org.web3j.crypto.*
import java.math.BigInteger

/**
 * Create signature proof by signing hash of address generated from public key.
 * @param ecKeyPair EC keypair
 * @return proof singed by EC keypair
 */
fun createRegistrationProof(ecKeyPair: ECKeyPair): EthereumRegistrationProof {
    val address = Keys.getAddress(ecKeyPair.publicKey)

    val dataToSign = prepareDataToSign(address)
    val sig = Sign.signMessage(dataToSign, ecKeyPair)
    val v = BigInteger(sig.v).toString(16).replace("0x", "")
    val r = Hex.encodeHexString(sig.r).replace("0x", "")
    val s = Hex.encodeHexString(sig.s).replace("0x", "")
    val vrs = VRSSignature(v, r, s)
    return EthereumRegistrationProof(vrs, ecKeyPair.publicKey)
}

/**
 * Check proof is actually signature of hash of address get from public key.
 * @param proof signature and public key
 * @return true if signature is correct, false otherwise
 */
fun checkRegistrationProof(proof: EthereumRegistrationProof): Boolean {
    val address = Keys.getAddress(proof.publicKey)

    // Iterate recId [0..3] while the correct way not found
    // there are 4 potential outputs including null values, thus we should check output
    // comparing to expecting public key
    val ecdsaSignature =
        ECDSASignature(BigInteger(proof.signature.r, 16), BigInteger(proof.signature.s, 16))
    for (i in 0..3) {
        // null is a valid result, skip it
        val res =
            Sign.recoverFromSignature(i, ecdsaSignature, Hash.sha3(prepareDataToSign(address)))
                ?: continue
        if (Keys.getAddress(res) == address)
            return true
    }
    return false
}
