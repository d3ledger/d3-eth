/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import com.d3.commons.config.IrohaCredentialRawConfig
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.hex
import com.d3.commons.util.toHexString
import integration.helper.D3_DOMAIN
import integration.helper.IrohaConfigHelper
import jp.co.soramitsu.soranet.eth.config.loadEthPasswords
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationTestEnvironment
import jp.co.soramitsu.soranet.eth.provider.ETH_PRECISION
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelperBuilder
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

/**
 * Integration tests with multiple notaries for deposit case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
class DepositMultiIntegrationTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment
    /** Utility functions for integration tests */
    private val integrationHelper = ethIntegrationTestEnvironment.integrationHelper
    private val keyPair2 = ModelUtil.generateKeypair()

    /** Ethereum assetId in Iroha */
    private val etherAssetId = "ether#ethereum"

    private val registrationTestEnvironment = ethIntegrationTestEnvironment.registrationTestEnvironment
    private val ethDeposit2: Job

    val ethereumPasswords = loadEthPasswords("test", "/eth/ethereum_password.properties").get()

    init {
        ethIntegrationTestEnvironment.init()
        ethIntegrationTestEnvironment.refresh()
        // create 2nd notary config
        val irohaCredential = object : IrohaCredentialRawConfig {
            override val pubkey = String.hex(keyPair2.public.encoded).toLowerCase()
            override val privkey = String.hex(keyPair2.private.encoded).toLowerCase()
            override val accountId = integrationHelper.accountHelper.notaryAccount.accountId
        }

        val ethereumConfig =
            integrationHelper.configHelper.createEthereumConfig()
        val depositConfig =
            integrationHelper.configHelper.createEthDepositConfig(
                ethereumConfig = ethereumConfig,
                notaryCredential_ = irohaCredential
            )

        val notary2IrohaPublicKey = keyPair2.public.toHexString()
        val notary2EthereumCredentials =
            DeployHelper(depositConfig.ethereum, ethereumPasswords).credentials
        val notary2EthereumAddress = notary2EthereumCredentials.address
        val notary2Name = "notary_name_" + String.getRandomString(5)
        val notary2EndpointAddress = "http://127.0.0.1:${depositConfig.refund.port}"
        // wait for expansion is finished
        Thread.sleep(5_000)

        integrationHelper.triggerExpansion(
            integrationHelper.accountHelper.notaryAccount.accountId,
            notary2IrohaPublicKey,
            2,
            notary2EthereumAddress,
            notary2Name,
            notary2EndpointAddress
        )
        Thread.sleep(5_000)

        // run 2nd instance of notary
        ethDeposit2 = GlobalScope.launch {
            integrationHelper.runEthDeposit(
                ethereumPasswords,
                depositConfig,
                registrationConfig = integrationHelper.configHelper.createEthRegistrationConfig()
            )
        }
    }

    /** Iroha client account */
    private val clientIrohaAccount = String.getRandomString(9)
    private val clientIrohaAccountId = "$clientIrohaAccount@$D3_DOMAIN"
    private val clientKeyPair = ModelUtil.generateKeypair()

    init {
        // register client in Iroha
        val res = integrationHelper.sendRegistrationRequest(
            clientIrohaAccount,
            clientKeyPair.public.toHexString(),
            registrationTestEnvironment.registrationConfig.port
        )
        Assertions.assertEquals(200, res.statusCode)
    }

    val clientEthereumKeypair = Keys.createEcKeyPair()
    val walletAddress = registerWallet()

    private fun registerWallet(): String {
        // register in Ethereum client1
        integrationHelper.registerEthereumWallet(
            clientIrohaAccountId,
            clientKeyPair,
            clientEthereumKeypair
        )
        return "0x${Keys.getAddress(clientEthereumKeypair.publicKey)}"

    }

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterAll
    fun dropDown() {
        ethDeposit2.cancel()
        ethIntegrationTestEnvironment.close()
    }

    /**
     * @given Ethereum and Iroha networks running and client ethereum wallet registered and has 1234000000000 Wei
     * @when the client transfers 0 Wei from wallet to "master" and then transfers 1234000000000 Wei
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun walletDepositMultisig() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            Thread.currentThread().name = this::class.simpleName
            val initialAmount =
                integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
            val amount = BigInteger.valueOf(1_234_000_000_000)
            // send ETH
            runBlocking { delay(2000) }
            integrationHelper.purgeAndwaitOneIrohaBlock {
                // refill wallet
                val amountWithFee = BigInteger.valueOf(1_234_500_000_000)
                integrationHelper.sendEth(amountWithFee, walletAddress)

                // send from wallet to master
                val credentials = Credentials.create(clientEthereumKeypair)
                val ethHelper = DeployHelperBuilder(
                    integrationHelper.ethTestConfig.ethereum,
                    ethereumPasswords.nodeLogin,
                    ethereumPasswords.nodePassword,
                    credentials
                ).build()
                ethHelper.sendEthereum(amount, integrationHelper.masterContract.contractAddress)
            }
            runBlocking { delay(15_000) }

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
     * @given Ethereum and Iroha networks running and client ethereum wallet registered and has 51 coin
     * @when the client transfers 0 coins from wallet to "master" and then transfers 51 coin
     * @then Associated Iroha account balance is increased on 51 coin
     */
    @Test
    fun walletDepositMultisigERC20() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val (tokenInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(2)
            val assetId = "${tokenInfo.name}#ethereum"
            val initialAmount =
                integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
            val amount = BigInteger.valueOf(51)

            // send ETH
            integrationHelper.purgeAndwaitOneIrohaBlock {
                // refill wallet
                val amountForFee = BigInteger.valueOf(500_000_000)
                integrationHelper.sendEth(amountForFee, walletAddress)
                integrationHelper.sendERC20Token(tokenAddress, amount, walletAddress)

                // send from wallet to master
                val credentials = Credentials.create(clientEthereumKeypair)
                val ethHelper = DeployHelperBuilder(
                    integrationHelper.ethTestConfig.ethereum,
                    ethereumPasswords.nodeLogin,
                    ethereumPasswords.nodePassword,
                    credentials
                ).build()
                ethHelper.sendERC20(tokenAddress, integrationHelper.masterContract.contractAddress, amount)
            }
            runBlocking { delay(15_000) }

            Assertions.assertEquals(
                BigDecimal(amount, tokenInfo.precision).add(BigDecimal(initialAmount)),
                BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))
            )
        }
    }
}
