/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import com.d3.commons.config.IrohaCredentialRawConfig
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.hex
import com.d3.commons.util.toHexString
import com.d3.eth.provider.ETH_PRECISION
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.DeployHelperBuilder
import integration.eth.config.loadEthPasswords
import integration.helper.D3_DOMAIN
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

/**
 * Integration tests with multiple notaries for deposit case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepositMultiIntegrationTest {
    /** Utility functions for integration tests */
    private val integrationHelper = EthIntegrationHelperUtil()
    private val keyPair2 = ModelUtil.generateKeypair()

    /** Ethereum assetId in Iroha */
    private val etherAssetId = "ether#ethereum"

    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    private val ethRegistrationService: Job
    private val ethDeposit1: Job
    private val ethDeposit2: Job

    val ethereumPasswords = loadEthPasswords("test", "/eth/ethereum_password.properties").get()

    init {
        // run notary
        ethDeposit1 = GlobalScope.launch {
            integrationHelper.runEthDeposit(
                ethDepositConfig = integrationHelper.configHelper.createEthDepositConfig(),
                registrationConfig = integrationHelper.configHelper.createEthRegistrationConfig()
            )
        }
        registrationTestEnvironment.registrationInitialization.init()
        ethRegistrationService = GlobalScope.launch {
            integrationHelper.runEthRegistrationService(integrationHelper.ethRegistrationConfig)
        }

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

        // wait for deposit service
        Thread.sleep(5_000)
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

    /** Ethereum address to transfer to */
    private val relayAddress = registerRelay()

    private fun registerRelay(): String {
        integrationHelper.deployRelays(1)
        return integrationHelper.registerClientInEth(clientIrohaAccount)
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
        ethDeposit1.cancel()
        ethDeposit2.cancel()
        ethRegistrationService.cancel()
        integrationHelper.close()
    }

    /**
     * Test US-001 Deposit of ETH with multiple notaries
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and 2 instances of notary running
     * @when "fromAddress" transfers 0 Wei to "relayWallet" and then "fromAddress" transfers 1234000000000 Wei
     * to "relayWallet"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun relayDepositMultisig() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            Thread.currentThread().name = this::class.simpleName
            val initialAmount =
                integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
            val amount = BigInteger.valueOf(1_234_000_000_000)
            // send ETH
            runBlocking { delay(2000) }
            integrationHelper.purgeAndwaitOneIrohaBlock {
                integrationHelper.sendEth(amount, relayAddress)
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
     * Test US-002 Deposit of ETH token with multiple notaries
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and 2 notaries running
     * @when "fromAddress" transfers 0 tokens to "relayWallet" and then "fromAddress" transfers 51 coin to "relayWallet"
     * @then Associated Iroha account balance is increased on 51 coin
     */
    @Test
    fun relayDepositMultisigERC20() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val (tokenInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(2)
            val assetId = "${tokenInfo.name}#ethereum"
            val initialAmount =
                integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
            val amount = BigInteger.valueOf(51)

            // send ETH
            integrationHelper.purgeAndwaitOneIrohaBlock {
                integrationHelper.sendERC20Token(tokenAddress, amount, relayAddress)
            }
            runBlocking { delay(15_000) }

            Assertions.assertEquals(
                BigDecimal(amount, tokenInfo.precision).add(BigDecimal(initialAmount)),
                BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))
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
