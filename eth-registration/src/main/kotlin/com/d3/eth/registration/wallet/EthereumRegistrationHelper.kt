/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration.wallet

import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign

/**
 * Create signature proof by signing hash of address generated from public key.
 * @param ecKeyPair EC keypair
 * @return proof singed by EC keypair
 */
fun createRegistrationProof(ecKeyPair: ECKeyPair): EthereumRegistrationProof {
    val address = Keys.getAddress(ecKeyPair.publicKey)
    val esig = ecKeyPair.sign(Hash.sha3(address.toByteArray()))
    return EthereumRegistrationProof(esig, ecKeyPair.publicKey)
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
    for (i in 0..3) {
        // null is a valid result, skip it
        val res =
            Sign.recoverFromSignature(i, proof.signature, Hash.sha3(address.toByteArray()))
                ?: continue
        if (Keys.getAddress(res) == address)
            return true
    }
    return false
}
