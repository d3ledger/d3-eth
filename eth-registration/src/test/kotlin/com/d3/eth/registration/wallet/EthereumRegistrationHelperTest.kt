/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration.wallet

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EthereumRegistrationHelperTest {

    /**
     * @given proof formed from keypair
     * @when get address from proof
     * @then address is equal to address generated from public key
     */
    @Test
    fun testGetAddress() {
        val keypair = Keys.createEcKeyPair()
        val proof = createRegistrationProof(keypair)

        assertEquals(Keys.getAddress(keypair.publicKey), proof.getAddress())
    }

    /**
     * @given EC keypair
     * @when signature is generated
     * @then check returns true
     */
    @Test
    fun testCorrect() {
        val keypair = Keys.createEcKeyPair()
        val proof = createRegistrationProof(keypair)

        assertTrue { checkRegistrationProof(proof) }
    }

    /**
     * @given EC keypair and signature generated with wrong address
     * @when check signature
     * @then check returns false
     */
    @Test
    fun testIncorrectAddress() {
        val keypair = Keys.createEcKeyPair()

        // generate proof with wrong address
        val address = "0000000000000000000000000000000000000000"
        val esig = keypair.sign(Hash.sha3(address.toByteArray()))
        val proof =
            EthereumRegistrationProof(esig, keypair.publicKey)

        assertFalse { checkRegistrationProof(proof) }
    }

    /**
     * @given malformed EC keypair and signature
     * @when check signature
     * @then check throws IllegalArgumentException
     */
    @Test
    fun testIncorrectSignature() {
        val keypair = Keys.createEcKeyPair()
        val proof = EthereumRegistrationProof(
            ECDSASignature(
                BigInteger.ZERO,
                BigInteger.ZERO
            ), keypair.publicKey
        )

        assertThrows<IllegalArgumentException> {
            checkRegistrationProof(
                proof
            )
        }
    }
}
