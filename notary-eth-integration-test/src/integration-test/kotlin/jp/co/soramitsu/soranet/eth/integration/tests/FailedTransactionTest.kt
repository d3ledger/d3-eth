/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import integration.helper.D3_DOMAIN
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationHelperUtil
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationTestEnvironment
import jp.co.soramitsu.soranet.eth.provider.ETH_DOMAIN
import jp.co.soramitsu.soranet.eth.token.EthTokenInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.protocol.exceptions.TransactionException
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FailedTransactionTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment

    val integrationHelper = ethIntegrationTestEnvironment.integrationHelper

    private val registrationTestEnvironment = ethIntegrationTestEnvironment.registrationTestEnvironment

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    init {
        ethIntegrationTestEnvironment.init()
    }

    @AfterAll
    fun dropDown() {
        ethIntegrationTestEnvironment.close()
    }

    /**
     * Reverted Ether transfer transaction test
     * @given Ethereum node, Iroha and notary are running, failer contract is deployed and registered as relay,
     * new user registers in Iroha
     * @when send Ether to relay account
     * @then user account in Iroha has 0 balance of Ether
     */
    @Test
    fun failedEtherTransferTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val failerAddress = integrationHelper.deployFailer()
            val clientAccount = String.getRandomString(9)
            val irohaKeyPair = Ed25519Sha3().generateKeypair()
            val ethKeyPair = Keys.createEcKeyPair()
            // register client in Iroha
            val res = integrationHelper.sendRegistrationRequest(
                clientAccount,
                irohaKeyPair.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            Assertions.assertEquals(200, res.statusCode)
            integrationHelper.registerEthereumWallet(
                "$clientAccount@$D3_DOMAIN",
                irohaKeyPair,
                ethKeyPair
            )
            integrationHelper.sendEth(BigInteger.valueOf(10000000), Keys.getAddress(ethKeyPair))
            integrationHelper.sendEth(BigInteger.valueOf(1), failerAddress, Credentials.create(ethKeyPair))

            assertEquals(BigInteger.ZERO, integrationHelper.getEthBalance(failerAddress))
            val irohaBalance =
                integrationHelper.getIrohaAccountBalance(
                    "$clientAccount@$D3_DOMAIN",
                    "ether#ethereum"
                )
            assertEquals(BigDecimal.ZERO, BigDecimal(irohaBalance))
        }
    }

    /**
     * Reverted Ether transfer transaction test
     * @given Ethereum node, Iroha and notary are running, failer contract is deployed twice,
     * one of them is registered as relay, another one is registered as ERC-20 token and
     * new user registers in Iroha
     * @when send tokens to relay account
     * @then user account in Iroha has 0 balance of ERC-20 token
     */
    @Test
    fun failedTokenTransferTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val failerAddress = integrationHelper.deployFailer()
            val anotherFailerAddress = integrationHelper.deployFailer()
            val clientAccount = String.getRandomString(9)
            val irohaKeyPair = Ed25519Sha3().generateKeypair()
            val ethKeyPair = Keys.createEcKeyPair()
            // register client in Iroha
            val res = integrationHelper.sendRegistrationRequest(
                clientAccount,
                irohaKeyPair.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            Assertions.assertEquals(200, res.statusCode)
            integrationHelper.registerEthereumWallet(
                "$clientAccount@$D3_DOMAIN",
                irohaKeyPair,
                ethKeyPair
            )
            val coinName = String.getRandomString(9)
            integrationHelper.addEthAnchoredERC20Token(
                anotherFailerAddress,
                EthTokenInfo(coinName, ETH_DOMAIN, 0)
            )

            // web3j throws exception in case of contract function call revert
            // so let's catch and ignore it
            try {
                integrationHelper.sendERC20Token(
                    anotherFailerAddress,
                    BigInteger.valueOf(1),
                    Keys.getAddress(ethKeyPair)
                )
                integrationHelper.sendERC20Token(
                    anotherFailerAddress,
                    BigInteger.valueOf(1),
                    failerAddress,
                    Credentials.create(ethKeyPair)
                )
            } catch (e: TransactionException) {
            }

            // actually this test passes even without transaction status check
            // it's probably impossible to get some money deposit to iroha
            // because logs are empty for reverted transactions
            // but let's leave it for a rainy day
            val irohaBalance = integrationHelper.getIrohaAccountBalance(
                "$clientAccount@$D3_DOMAIN",
                "$coinName#ethereum"
            )
            assertEquals(BigDecimal.ZERO, BigDecimal(irohaBalance))
        }
    }
}
