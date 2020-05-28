/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import jp.co.soramitsu.soranet.eth.bridge.XOR_LIMITS_TIME_KEY
import jp.co.soramitsu.soranet.eth.bridge.XOR_LIMITS_VALUE_KEY
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationTestEnvironment
import jp.co.soramitsu.soranet.eth.sidechain.EthChainHandler.Companion.threshold
import jp.co.soramitsu.soranet.eth.sidechain.WithdrawalLimitProvider.Companion.XOR_PRECISION
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class XorWithdrawalLimitsIntegrationTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment
    private val integrationHelper = ethIntegrationTestEnvironment.integrationHelper

    @BeforeAll
    fun init() {
        ethIntegrationTestEnvironment.init()
    }

    @AfterAll
    fun dropDown() {
        ethIntegrationTestEnvironment.close()
    }

    /**
     * @given notary service is running and no data for withdrawal limits assigned
     * @when an ethereum block appears
     * @then new relevant token limit and its time saved to Iroha
     */
    @Test
    fun correctXorWithdrawalLimitsUpdate() {
        val amount = ethIntegrationTestEnvironment.amount
        integrationHelper.waitOneEtherBlock {
            integrationHelper.deployRandomERC20Token()
        }
        Thread.sleep(5000)
        val limit = integrationHelper.queryHelper.getAccountDetails(
            integrationHelper.accountHelper.xorLimitsStorageAccount.accountId,
            integrationHelper.accountHelper.withdrawalAccount.accountId,
            XOR_LIMITS_VALUE_KEY
        ).get()
        assertTrue(limit.isPresent)
        val timeToLong = integrationHelper.queryHelper.getAccountDetails(
            integrationHelper.accountHelper.xorLimitsStorageAccount.accountId,
            integrationHelper.accountHelper.withdrawalAccount.accountId,
            XOR_LIMITS_TIME_KEY
        ).get().get().toLong()
        assertEquals(
            amount.toBigDecimal().divide(
                threshold,
                XOR_PRECISION,
                RoundingMode.HALF_UP
            ).stripTrailingZeros().toPlainString(),
            limit.get().toBigDecimal().stripTrailingZeros().toPlainString()
        )
        assertTrue(timeToLong > System.currentTimeMillis())
    }
}
