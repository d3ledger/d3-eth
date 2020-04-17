/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import integration.helper.D3_DOMAIN
import integration.helper.IrohaConfigHelper
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationTestEnvironment
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import java.time.Duration

/**
 * Requires Iroha is running
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthAddressProviderIrohaTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment

    val integrationHelper = ethIntegrationTestEnvironment.integrationHelper

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    init {
        ethIntegrationTestEnvironment.init()
    }

    @AfterAll
    fun dropDown() {
        ethIntegrationTestEnvironment.close()
    }

    /**
     * @given ethereum relay wallets are stored in the system
     * @when getAddresses() is called
     * @then not free wallets are returned in a map
     */
    @Test
    fun testStorage() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)

            val clientIrohaAccount = String.getRandomString(9)
            val irohaKeyPair = Ed25519Sha3().generateKeypair()
            val ethKeyPair = Keys.createEcKeyPair()
            val res = ethIntegrationTestEnvironment.registrationTestEnvironment.register(
                clientIrohaAccount,
                irohaKeyPair.public.toHexString()
            )
            assertEquals(200, res.statusCode)
            val clientId = "$clientIrohaAccount@$D3_DOMAIN"
            integrationHelper.registerEthereumWallet(
                clientId,
                irohaKeyPair,
                ethKeyPair
            )

            Thread.sleep(3_000)

            integrationHelper.ethWalletsProvider().getAddresses()
                .fold(
                    {
                        assertTrue(it.entries.any {
                            it.key == Numeric.prependHexPrefix(Keys.getAddress(ethKeyPair))
                                    && it.value == clientId
                        })
                    },
                    { ex -> fail("cannot get addresses", ex) }
                )
        }
    }

    /**
     * @given There is no relay accounts registered (we use test accountId as relay holder)
     * @when getAddresses() is called
     * @then empty map is returned
     */
    @Test
    // We cannot predict the state of Iroha for now
    @Disabled
    fun testEmptyStorage() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            integrationHelper.ethWalletsProvider().getAddresses()
                .fold(
                    { assert(it.isEmpty()) },
                    { ex -> fail("result has exception", ex) }
                )
        }
    }

    @Test
    fun testGetByAccountNotFound() {
        val res = integrationHelper.ethWalletsProvider().getAddressByAccountId("nonexist@domain")
        assertFalse(res.get().isPresent)
    }
}
