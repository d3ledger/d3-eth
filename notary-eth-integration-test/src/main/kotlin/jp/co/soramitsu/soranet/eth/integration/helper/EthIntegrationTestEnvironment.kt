/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.helper

import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.soranet.eth.bridge.EthDepositConfig
import jp.co.soramitsu.soranet.eth.provider.ETH_ADDRESS
import jp.co.soramitsu.soranet.eth.sidechain.WithdrawalLimitProvider.Companion.XOR_PRECISION
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.Closeable
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object EthIntegrationTestEnvironment : Closeable {
    private const val classesCount = 8
    private val isInitialized = AtomicBoolean()
    private val classesExecuted = AtomicInteger()
    val integrationHelper = EthIntegrationHelperUtil
    val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    var ethDepositConfig = getNewEthDepositConfig()
    lateinit var relevantTokenAddress: String
    val amount = BigInteger("500000")

    private lateinit var ethDeposit: Job

    fun init() {
        if (!isInitialized.get()) {
            integrationHelper.sendERC20Token(
                relevantTokenAddress,
                amount,
                ETH_ADDRESS
            )
            // run notary
            ethDeposit = GlobalScope.launch {
                integrationHelper.runEthDeposit(ethDepositConfig = ethDepositConfig)
            }
            registrationTestEnvironment.registrationInitialization.init()
            integrationHelper.runEthNotificationRmqConsumer()
            Thread.sleep(5_000)
            isInitialized.set(true)
        }
        classesExecuted.incrementAndGet()
    }

    // TODO Rework this completely in XNET-82
    fun refresh() {
        if (isInitialized.get()) {
            ethDeposit.cancel()
            Thread.sleep(2_000)
            ethDepositConfig = getNewEthDepositConfig()
            integrationHelper.sendERC20Token(
                relevantTokenAddress,
                amount,
                ETH_ADDRESS
            )
            // run notary
            ethDeposit = GlobalScope.launch {
                integrationHelper.runEthDeposit(ethDepositConfig = ethDepositConfig)
            }
            Thread.sleep(5_000)
            isInitialized.set(true)
        }
    }

    override fun close() {
        if (classesExecuted.get() == classesCount && isInitialized.get()) {
            ethDeposit.cancel()
            registrationTestEnvironment.close()
            isInitialized.set(false)
        }
    }

    private fun getNewEthDepositConfig(): EthDepositConfig {
        relevantTokenAddress = integrationHelper.deployRandomERC20Token(XOR_PRECISION).second
        return EthIntegrationHelperUtil.configHelper.createEthDepositConfig(
            xorTokenAddress = relevantTokenAddress,
            xorExchangeContractAddress = ETH_ADDRESS
        )
    }
}
