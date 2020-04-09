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
import jp.co.soramitsu.soranet.eth.provider.ETH_PRECISION
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.math.BigDecimal
import java.math.BigInteger
import java.security.KeyPair
import java.time.Duration

/**
 * Integration tests for deposit case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepositIntegrationTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment
    /** Utility functions for integration tests */
    private val integrationHelper = ethIntegrationTestEnvironment.integrationHelper
    private val registrationTestEnvironment = ethIntegrationTestEnvironment.registrationTestEnvironment

    /** Ethereum assetId in Iroha */
    private val etherAssetId = "ether#ethereum"

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    init {
        ethIntegrationTestEnvironment.init()
    }

    private fun registerClient(accountName: String, keypair: KeyPair, ecKeyPair: ECKeyPair) {
        // register client in Iroha
        val res = registrationTestEnvironment.register(
            accountName,
            keypair.public.toHexString()
        )
        Assertions.assertEquals(200, res.statusCode)

        // register Ethereum wallet
        integrationHelper.registerEthereumWallet("$accountName@$D3_DOMAIN", keypair, ecKeyPair)
        Thread.sleep(3_000)
    }

    @AfterAll
    fun dropDown() {
        ethIntegrationTestEnvironment.close()
    }

    /**
     * Test US-001 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and notary running and user registered with ethereum wallet
     * @when user transfers 1234000000000 Wei to the master contract
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositOfETH() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)

            val clientIrohaAccount = String.getRandomString(9)
            val irohaKeyPair = Ed25519Sha3().generateKeypair()
            val ethKeyPair = Keys.createEcKeyPair()

            registerClient(
                clientIrohaAccount,
                irohaKeyPair,
                ethKeyPair
            )

            val clientIrohaAccountId = "$clientIrohaAccount@$D3_DOMAIN"
            val ethAddress = Keys.getAddress(ethKeyPair.publicKey)

            val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
            val amount = BigInteger.valueOf(1_234_000_000_000)
            // send ETH
            integrationHelper.sendEth(amount.multiply(BigInteger.valueOf(5)), ethAddress)
            integrationHelper.purgeAndwaitOneIrohaBlock {
                integrationHelper.sendEth(
                    amount,
                    integrationHelper.masterContract.contractAddress,
                    Credentials.create(ethKeyPair)
                )
            }

            Thread.sleep(2_000)

            Assertions.assertEquals(
                BigDecimal(amount, ETH_PRECISION).add(BigDecimal(initialAmount)),
                BigDecimal(
                    integrationHelper.getIrohaAccountBalance(
                        clientIrohaAccountId,
                        etherAssetId
                    )
                )
            )
        }
    }

    /**
     * Test US-002 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and notary running
     * @when "fromAddress" transfers 51 coin to "relayWallet"
     * @then Associated Iroha account balance is increased on 51 coin
     */
    @Test
    fun depositOfERC20() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            Thread.currentThread().name = this::class.simpleName
            val (tokenInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(2)
            val assetId = "${tokenInfo.name}#ethereum"

            val clientIrohaAccount = String.getRandomString(9)
            val irohaKeyPair = Ed25519Sha3().generateKeypair()
            val ethKeyPair = Keys.createEcKeyPair()

            registerClient(
                clientIrohaAccount,
                irohaKeyPair,
                ethKeyPair
            )

            val clientIrohaAccountId = "$clientIrohaAccount@$D3_DOMAIN"
            val ethAddress = Keys.getAddress(ethKeyPair.publicKey)
            val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
            val amount = BigInteger.valueOf(1_234_000_000_000)

            integrationHelper.sendERC20Token(tokenAddress, amount, ethAddress)
            integrationHelper.sendEth(amount, ethAddress)
            integrationHelper.purgeAndwaitOneIrohaBlock {
                integrationHelper.sendERC20Token(
                    tokenAddress,
                    amount,
                    integrationHelper.masterContract.contractAddress,
                    Credentials.create(ethKeyPair)
                )
            }

            Thread.sleep(2_000)

            Assertions.assertEquals(
                BigDecimal(amount, tokenInfo.precision).add(BigDecimal(initialAmount)),
                BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))
            )
        }
    }
}
