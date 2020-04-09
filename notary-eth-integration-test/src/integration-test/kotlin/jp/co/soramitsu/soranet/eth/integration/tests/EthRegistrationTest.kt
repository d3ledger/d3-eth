/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationHelperUtil
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationTestEnvironment
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthRegistrationTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment

    /** Integration tests util */
    private val integrationHelper = ethIntegrationTestEnvironment.integrationHelper

    private val registrationServiceEnvironment = ethIntegrationTestEnvironment.registrationTestEnvironment

    init {
        ethIntegrationTestEnvironment.init()
    }

    @AfterAll
    fun dropDown() {
        ethIntegrationTestEnvironment.close()
    }

    /**
     * Test registration
     * @given Registration service is up and running
     * @when POST query is sent to register a user with `name` and `pubkey`
     * @then new account is created in Iroha
     */
    @Test
    fun correctRegistration() {
        val name = String.getRandomString(7)
        val keypair = Ed25519Sha3().generateKeypair()
        val pubkey = keypair.public.toHexString()
        val clientId = "$name@d3"
        val ethKeyPair = Keys.createEcKeyPair()

        // register in Iroha
        val res = registrationServiceEnvironment.register(name, pubkey)
        assertEquals(200, res.statusCode)
        // register in Eth
        integrationHelper.registerEthereumWallet(
            clientId,
            keypair,
            ethKeyPair
        )

        Thread.sleep(5_000)

        // check relay address
        assertEquals(
            Numeric.prependHexPrefix(Keys.getAddress(ethKeyPair)),
            integrationHelper.getWalletByAccount(clientId).get()
        )
    }

    /**
     * Test registration
     * @given Registration service is up and running
     * @when POST query is sent to register a user with `name` and `pubkey` where user with 'name' already exists
     * @then error response that userId already exists returned
     */
    @Test
    fun doubleRegistration() {
        val name = String.getRandomString(7)
        val keypair = Ed25519Sha3().generateKeypair()
        val pubkey = keypair.public.toHexString()
        val clientId = "$name@d3"
        val ethKeyPair = Keys.createEcKeyPair()

        // register in Iroha
        val res = registrationServiceEnvironment.register(name, pubkey)
        assertEquals(200, res.statusCode)
        // register in Eth
        integrationHelper.registerEthereumWallet(
            clientId,
            keypair,
            ethKeyPair
        )

        Thread.sleep(5_000)

        // check relay address
        assertEquals(
            Numeric.prependHexPrefix(Keys.getAddress(ethKeyPair)),
            integrationHelper.getWalletByAccount(clientId).get()
        )

        // try to register with the same name
        integrationHelper.registerEthereumWallet(
            clientId,
            keypair,
            ethKeyPair
        )

        // check relay address the same
        assertEquals(
            Numeric.prependHexPrefix(Keys.getAddress(ethKeyPair)),
            integrationHelper.getWalletByAccount(clientId).get()
        )
    }
}
