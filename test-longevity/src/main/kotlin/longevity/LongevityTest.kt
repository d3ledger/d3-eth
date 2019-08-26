/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package longevity

import com.d3.commons.config.IrohaCredentialRawConfig
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.hex
import com.d3.eth.provider.ETH_PRECISION
import com.github.kittinunf.result.Result
import integration.eth.config.loadEthPasswords
import integration.helper.EthIntegrationHelperUtil
import integration.helper.NotaryClient
import jp.co.soramitsu.iroha.java.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KLogging
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Class for longevity testing.
 * It is supposed to be used for long-running tests.
 */
class LongevityTest {
    private val integrationHelper = EthIntegrationHelperUtil()

    private val masterContract = integrationHelper.masterContract.contractAddress

    /**
     * Number of clients for tests.
     * Since clients keys for ethereum and iroha are files [totalClients] should be in [1..5]
     */
    private val totalClients = 1

    /** Create d3 clients */
    private val clients = (0..totalClients - 1).map { clientNumber ->
        NotaryClient(
            integrationHelper,
            integrationHelper.configHelper.createEthereumConfig(),
            loadEthPasswords("client$clientNumber", "/eth/ethereum_password.properties").get()
        )
    }

    /** Run 4 instances of notary */
    private fun runNotaries() {
        // run first instance with default configs
        integrationHelper.runEthDeposit(ethDepositConfig = integrationHelper.configHelper.createEthDepositConfig())

        // launch the rest
        (1..3).forEach {
            val keyPair = ModelUtil.generateKeypair()
            val irohaCredential = object : IrohaCredentialRawConfig {
                override val pubkey = String.hex(keyPair.public.encoded).toLowerCase()
                override val privkey = String.hex(keyPair.private.encoded).toLowerCase()
                override val accountId = integrationHelper.accountHelper.notaryAccount.accountId
            }

            val ethereumPasswords =
                loadEthPasswords("notary$it", "/eth/ethereum_password.properties").get()

            val ethereumConfig =
                integrationHelper.configHelper.createEthereumConfig()

            val depositConfig =
                integrationHelper.configHelper.createEthDepositConfig(
                    ethereumConfig = ethereumConfig,
                    notaryCredential_ = irohaCredential
                )

            integrationHelper.accountHelper.addNotarySignatory(
                Utils.parseHexKeypair(
                    irohaCredential.pubkey,
                    irohaCredential.privkey
                )
            )

            integrationHelper.runEthDeposit(ethereumPasswords, depositConfig)
        }
    }

    /**
     * Run all notary services.
     */
    private fun runServices() {
        runNotaries()
        GlobalScope.launch { integrationHelper.runEthRegistrationService() }
        GlobalScope.launch { integrationHelper.runEthWithdrawalService() }

        // wait until services are up
        Thread.sleep(10_000)
    }

    /**
     * Register all clients.
     */
    private fun registerClients(): Result<Unit, Exception> {
        integrationHelper.deployRelays(5)
        return Result.of {
            for (client in clients) {
                val status = client.signUp().statusCode
                if (status != 200)
                    throw Exception("Cannot register client ${client.accountId}, status code: $status")
            }
        }
    }

    /**
     * Run test strategy.
     */
    private fun runTest() {
        logger.info { "client count ${clients.size}" }

        clients.forEach { client ->
            GlobalScope.launch { delay(3_000) }
            GlobalScope.launch {
                logger.info { "start client ${client.accountId}" }
                while (true) {
                    val amount = BigInteger.valueOf(12_000_000_000)
                    logger.info { "Client ${client.name} perform deposit of $amount wei" }
                    client.deposit(amount)
                    launch { delay(20_000) }


                    val ethBalanceBefore = client.getEthBalance()
                    val irohaBalanceBefore = client.getIrohaBalance()
                    logger.info {
                        "Master eth balance ${integrationHelper.getEthBalance(
                            masterContract
                        )} after deposit"
                    }
                    logger.info { "Clietn ${client.name} eth balance: $ethBalanceBefore after deposit" }
                    logger.info { "Client ${client.name} iroha balance $irohaBalanceBefore after deposit" }

                    val decimalAmount = BigDecimal(amount, ETH_PRECISION)

                    if (!BigDecimal(irohaBalanceBefore).equals(decimalAmount))
                        logger.warn { "Client ${client.name} has wrong iroha balance. Expected ${decimalAmount.toPlainString()}, but got before: $irohaBalanceBefore" }

                    logger.info { "Client ${client.name} perform withdrawal of ${decimalAmount.toPlainString()}" }
                    client.withdraw(decimalAmount.toPlainString())
                    launch { delay(20_000) }

                    val ethBalanceAfter = client.getEthBalance()
                    val irohaBalanceAfter = client.getIrohaBalance()
                    logger.info {
                        "Master eth balance ${integrationHelper.getEthBalance(
                            masterContract
                        )} after withdrawal"
                    }
                    logger.info { "Clietn ${client.name} eth balance: $ethBalanceAfter after withdrawal" }
                    logger.info { "Client ${client.name} iroha balance $irohaBalanceAfter after withdrawal" }

                    // check balance
                    if (!ethBalanceBefore.add(amount).equals(ethBalanceAfter))
                        logger.warn { "Client ${client.name} has wrong eth balance. Expected equal but got before: $ethBalanceBefore, after: $ethBalanceAfter" }

                    if (!BigDecimal(irohaBalanceBefore).equals(BigDecimal(amount, ETH_PRECISION)))
                        logger.warn { "Client ${client.name} has wrong iroha balance. Expected 0, but got after: $irohaBalanceBefore" }

                    if (!BigDecimal(irohaBalanceAfter).equals(
                            BigDecimal(
                                BigInteger.ZERO,
                                ETH_PRECISION
                            )
                        )
                    )
                        logger.warn { "Client ${client.name} has wrong iroha balance. Expected 0, but got after: $irohaBalanceAfter" }
                }
            }
        }
    }

    /**
     * Run all.
     */
    fun run() {
        runServices()
        registerClients()

        // send ether to master account for fee
        val toMaster = BigInteger.valueOf(150_000_000_000_000)
        integrationHelper.sendEth(toMaster, integrationHelper.masterContract.contractAddress)

        runTest()
    }

    /**
     * Logger
     */
    companion object : KLogging()

}

