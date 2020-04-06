/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import com.d3.commons.sidechain.iroha.FEE_DESCRIPTION
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.eth.provider.ETH_PRECISION
import com.d3.eth.token.EthTokenInfo
import com.d3.notifications.event.SoraAckEthWithdrawalProofEvent
import integration.helper.D3_DOMAIN
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.*
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.math.BigDecimal
import java.math.BigInteger
import java.security.KeyPair
import java.time.Duration
import kotlin.test.assertEquals

/**
 * Integration tests for withdrawal service.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WalletWithdrawalPipelineIntegrationTest {

    /** Integration tests util */
    private val integrationHelper = EthIntegrationHelperUtil()

    /** Test Deposit configuration */
    private val depositConfig = integrationHelper.configHelper.createEthDepositConfig()

    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)

    /** Test Registration configuration */
    private val registrationConfig = registrationTestEnvironment.registrationConfig

    /** Test EthRegistration configuration */
    private val ethRegistrationConfig = integrationHelper.ethRegistrationConfig

    /** Ethereum test address where we want to withdraw to */
    private val toAddress = integrationHelper.ethTestConfig.ethTestAccount

    /** Notary account in Iroha */
    private val withdrawalAccountId = integrationHelper.accountHelper.notaryAccount.accountId

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    private val ethDeposit: Job

    init {
        registrationTestEnvironment.registrationInitialization.init()
        ethDeposit = GlobalScope.launch {
            integrationHelper.runEthDeposit(
                ethDepositConfig = depositConfig,
                registrationConfig = ethRegistrationConfig
            )
        }
        integrationHelper.runEthNotificationRmqConsumer()
    }

    lateinit var clientName: String
    lateinit var clientId: String
    lateinit var keypair: KeyPair
    lateinit var ethKeypair: ECKeyPair

    @BeforeEach
    fun setup() {
        integrationHelper.nameCurrentThread(this::class.simpleName!!)

        // generate client name and key
        clientName = String.getRandomString(9)
        clientId = "$clientName@$D3_DOMAIN"
        keypair = ModelUtil.generateKeypair()
        ethKeypair = Keys.createEcKeyPair()
        registerClient()
    }

    @AfterAll
    fun dropDown() {
        registrationTestEnvironment.close()
        ethDeposit.cancel()
        integrationHelper.close()
    }

    private fun registerClient() {
        // register client in Iroha
        val res = integrationHelper.sendRegistrationRequest(
            clientName,
            keypair.public.toHexString(),
            registrationConfig.port
        )
        Assertions.assertEquals(200, res.statusCode)

        // register Ethereum wallet
        integrationHelper.registerEthereumWallet(clientId, keypair, ethKeypair)
        Thread.sleep(3_000)
    }

    /**
     * Full withdrawal pipeline test
     * @given iroha and withdrawal services are running, free relays available, user account has 125 Wei in Iroha
     * @when user transfers 125 Wei to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 Wei
     */
    @Test
    fun testFullWithdrawalPipeline() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val amount = BigInteger.valueOf(1251400000000)

            // make sure master has enough assets
            integrationHelper.sendEth(amount, integrationHelper.masterContract.contractAddress)
            val masterBalanceInitial =
                integrationHelper.getEthBalance(integrationHelper.masterContract.contractAddress)

            val initialBalance = integrationHelper.getEthBalance(toAddress)
            val decimalAmount = BigDecimal(amount, ETH_PRECISION)
            val assetId = "ether#ethereum"

            // add Ether to user in Iroha
            integrationHelper.addIrohaAssetTo(clientId, assetId, decimalAmount)
            val ininialClientIrohaBalance =
                integrationHelper.getIrohaAccountBalance(clientId, assetId)

            // transfer Ether from user to notary master account
            val txHash = integrationHelper.transferAssetIrohaFromClient(
                clientId,
                keypair,
                clientId,
                withdrawalAccountId,
                assetId,
                toAddress,
                decimalAmount.toPlainString()
            )
            Thread.sleep(7_000)

            // gather proof and withdraw
            integrationHelper.withdrawToWallet(txHash)

            // check Ether balance of client in Iroha
            assertEquals(
                ininialClientIrohaBalance.toDouble() - decimalAmount.toDouble(),
                integrationHelper.getIrohaAccountBalance(clientId, assetId).toDouble()
            )

            // check Ether balance of client in Ethereum
            assertEquals(
                initialBalance.add(amount),
                integrationHelper.getEthBalance(toAddress)
            )

            // check notary balance in Ethereum
            val masterBalanceActual =
                integrationHelper.getEthBalance(integrationHelper.masterContract.contractAddress)
            assertEquals(
                masterBalanceInitial.subtract(amount),
                masterBalanceActual
            )

            Thread.sleep(15_000)

            // check notification published
            val notification = integrationHelper.gson.fromJson(
                integrationHelper.getLastRmqEvent().toString(),
                SoraAckEthWithdrawalProofEvent::class.java
            )
            assertEquals(txHash, notification.irohaTxHash)
        }
    }

    /**
     * Full withdrawal pipeline test for ERC20 token
     * @given iroha and withdrawal services are running, free relays available, user account has 125 OMG tokens in Iroha
     * @when user transfers 125 OMG to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 OMG
     */
    @Test
    fun testFullWithdrawalPipelineErc20() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val precision = 2

            // create ERC20 token and transfer to master
            val (assetInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(precision)

            val bigIntegerValue = BigInteger.valueOf(125)
            integrationHelper.sendERC20Token(
                tokenAddress,
                bigIntegerValue,
                integrationHelper.masterContract.contractAddress
            )
            val masterBalanceInitial = integrationHelper.getERC20TokenBalance(
                tokenAddress,
                integrationHelper.masterContract.contractAddress
            )

            val amount = BigDecimal(1.25)
            val assetId = "${assetInfo.name}#${assetInfo.domain}"

            val initialBalance = integrationHelper.getERC20TokenBalance(tokenAddress, toAddress)

            // add assets to user
            integrationHelper.addIrohaAssetTo(clientId, assetId, amount)

            // transfer assets from user to notary master account
            val txHash = integrationHelper.transferAssetIrohaFromClient(
                clientId,
                keypair,
                clientId,
                withdrawalAccountId,
                assetId,
                toAddress,
                amount.toPlainString()
            )
            Thread.sleep(7_000)

            // gather proof and withdraw
            integrationHelper.withdrawToWallet(txHash)

            assertEquals(
                initialBalance.add(bigIntegerValue),
                integrationHelper.getERC20TokenBalance(tokenAddress, toAddress)
            )

            val masterBalanceActual =
                integrationHelper.getERC20TokenBalance(
                    tokenAddress,
                    integrationHelper.masterContract.contractAddress
                )
            assertEquals(
                masterBalanceInitial.subtract(bigIntegerValue),
                masterBalanceActual
            )

            Thread.sleep(15_000)

            // check notification published
            val notification = integrationHelper.gson.fromJson(
                integrationHelper.getLastRmqEvent().toString(),
                SoraAckEthWithdrawalProofEvent::class.java
            )
            assertEquals(txHash, notification.irohaTxHash)
        }
    }

    /**
     * @given all services running and client has iroha anchored token xor#sora
     * @when client withdraws
     * @then new tokens are minted in Ethereum
     */
    @Test
    fun testIrohaAnchoredWithdrawalPipeline() {
        val amount = "2.34"
        val assetId = "xor#sora"
        val tokenAddress = integrationHelper.masterContract.xorTokenInstance().send()
        val tokenInfo = EthTokenInfo("xor", "sora", 18)
        integrationHelper.addIrohaAnchoredERC20Token(tokenAddress, tokenInfo)

        // add assets to user
        integrationHelper.addIrohaAssetTo(clientId, assetId, amount)

        val initialWithdrawalBalance = integrationHelper.getIrohaAccountBalance(withdrawalAccountId, assetId)
        val initialClientIrohaBalance = integrationHelper.getIrohaAccountBalance(clientId, assetId)
        val initialEthereumBalance = integrationHelper.getERC20TokenBalance(tokenAddress, toAddress)

        // transfer assets from user to notary master account
        val txHash = integrationHelper.transferAssetIrohaFromClient(
            clientId,
            keypair,
            clientId,
            withdrawalAccountId,
            assetId,
            toAddress,
            amount
        )
        Thread.sleep(7_000)

        // gather proof and withdraw
        integrationHelper.withdrawToWallet(txHash)

        // check balance of client in Iroha
        assertEquals(
            initialClientIrohaBalance.toDouble() - amount.toDouble(),
            integrationHelper.getIrohaAccountBalance(clientId, assetId).toDouble()
        )

        // check client balance in Ethereum
        val bigIntAmount = amount.toBigDecimal().multiply(10.toBigDecimal().pow(18)).toBigInteger()
        assertEquals(
            initialEthereumBalance + bigIntAmount,
            integrationHelper.getERC20TokenBalance(tokenAddress, toAddress)
        )

        // check notary balance in Ethereum
        val masterBalanceActual = integrationHelper.getERC20TokenBalance(
            tokenAddress,
            integrationHelper.masterContract.contractAddress
        )
        assertEquals(
            BigInteger.ZERO,
            masterBalanceActual
        )

        // check withdrawal service balance in Iroha
        assertEquals(
            initialWithdrawalBalance.toBigDecimal() + amount.toBigDecimal(),
            integrationHelper.getIrohaAccountBalance(withdrawalAccountId, assetId).toBigDecimal()
        )

        Thread.sleep(15_000)

        // check notification published
        val notification = integrationHelper.gson.fromJson(
            integrationHelper.getLastRmqEvent().toString(),
            SoraAckEthWithdrawalProofEvent::class.java
        )
        assertEquals(txHash, notification.irohaTxHash)
    }

    /**
     * Full withdrawal pipeline test with fee
     * @given iroha and withdrawal services are running, free relays available, user account has 125 Wei in Iroha
     * @when user transfers 125 Wei to Iroha master account and 34 XOR as fee
     * @then balance of user's wallet in Ethereum increased by 125 Wei and billing account gets 34
     * XOR
     */
    @Test
    fun testFullWithdrawalPipelineWithFee() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val amount = BigInteger.valueOf(1251400000000)
            val feeAmount = BigInteger.valueOf(3400000000)

            // make sure master has enough assets
            integrationHelper.sendEth(amount, integrationHelper.masterContract.contractAddress)
            val masterBalanceInitial =
                integrationHelper.getEthBalance(integrationHelper.masterContract.contractAddress)

            val initialBalance = integrationHelper.getEthBalance(toAddress)
            val decimalAmount = BigDecimal(amount, ETH_PRECISION)
            val feeDecimalAmount = BigDecimal(feeAmount, ETH_PRECISION)
            val assetId = "ether#ethereum"
            val xorAssetId = "xor#sora"

            // add Iroha assets to user
            integrationHelper.addIrohaAssetTo(clientId, assetId, decimalAmount)
            integrationHelper.addIrohaAssetTo(clientId, xorAssetId, feeDecimalAmount)

            val initialWithdrawalBalance =
                integrationHelper.getIrohaAccountBalance(withdrawalAccountId, xorAssetId)
            val ininialClientIrohaBalance =
                integrationHelper.getIrohaAccountBalance(clientId, assetId)
            val initialClientXorBalance =
                integrationHelper.getIrohaAccountBalance(clientId, xorAssetId)

            // transfer Ether from user to notary master account
            val txHash = integrationHelper.transferAssetIrohaFromClientWithFee(
                clientId,
                keypair,
                clientId,
                withdrawalAccountId,
                assetId,
                toAddress,
                decimalAmount.toPlainString(),
                xorAssetId,
                feeDecimalAmount.toPlainString(),
                FEE_DESCRIPTION
            )
            Thread.sleep(7_000)

            // gather proof and withdraw
            integrationHelper.withdrawToWallet(txHash)

            // check Ether balance of client in Iroha
            assertEquals(
                ininialClientIrohaBalance.toDouble() - decimalAmount.toDouble(),
                integrationHelper.getIrohaAccountBalance(clientId, assetId).toDouble()
            )
            // check client XOR balance in Iroha
            assertEquals(
                initialClientXorBalance.toDouble() - feeDecimalAmount.toDouble(),
                integrationHelper.getIrohaAccountBalance(clientId, xorAssetId).toDouble()
            )

            // check Ether balance of client in Ethereum
            assertEquals(
                initialBalance.add(amount),
                integrationHelper.getEthBalance(toAddress)
            )

            // check notary balance in Ethereum
            val masterBalanceActual =
                integrationHelper.getEthBalance(integrationHelper.masterContract.contractAddress)
            assertEquals(
                masterBalanceInitial.subtract(amount),
                masterBalanceActual
            )

            assertEquals(
                initialWithdrawalBalance.toBigDecimal() + decimalAmount,
                integrationHelper.getIrohaAccountBalance(withdrawalAccountId, assetId).toBigDecimal()
            )

            Thread.sleep(15_000)

            // check notification published
            val notification = integrationHelper.gson.fromJson(
                integrationHelper.getLastRmqEvent().toString(),
                SoraAckEthWithdrawalProofEvent::class.java
            )
            assertEquals(txHash, notification.irohaTxHash)
        }
    }
}
