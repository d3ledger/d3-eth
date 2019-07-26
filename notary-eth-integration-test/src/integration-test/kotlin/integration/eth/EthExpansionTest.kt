/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.eth.sidechain.util.DeployHelper
import integration.helper.EthIntegrationHelperUtil
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthExpansionTest {
    private val integrationHelper = EthIntegrationHelperUtil()
    private val depositService: Job
    private val withdrawalService: Job

    init {
        depositService = GlobalScope.launch {
            integrationHelper.runEthDeposit(ethDepositConfig = integrationHelper.configHelper.createEthDepositConfig())
        }
        withdrawalService = GlobalScope.launch {
            integrationHelper.runEthWithdrawalService(
                integrationHelper.configHelper.createWithdrawalConfig(
                    "EthExpansionTest" + String.getRandomString(9)
                )
            )
        }
        Thread.sleep(15_000)
    }

    /**
     * @given deposit and withdrawal services are running
     * @when expansion transaction is triggered
     * @then
     */
    @Test
    fun expansionTest() {
        val publicKey = Ed25519Sha3().generateKeypair().public.toHexString()
        val ethAddress = "0x0000000000000000000000000000000000000000"
        val notaryName = "notary_name_" + String.getRandomString(5)
        val notaryEndpointAddress = "http://localhost:20001"

        val masterContract = DeployHelper(
            integrationHelper.configHelper.createEthereumConfig(),
            integrationHelper.configHelper.ethPasswordConfig
        ).loadMasterContract(integrationHelper.masterContract.contractAddress)
        assertFalse { masterContract.isPeer(ethAddress).send() }

        integrationHelper.triggerExpansion(
            integrationHelper.accountHelper.notaryAccount.accountId,
            publicKey,
            2,
            ethAddress,
            notaryName,
            notaryEndpointAddress
        )
        Thread.sleep(5_000)

        assertTrue { masterContract.isPeer(ethAddress).send() }
        assertEquals(notaryEndpointAddress, integrationHelper.getNotaries()[notaryName])
        assertTrue {
            integrationHelper.getSignatories(integrationHelper.accountHelper.notaryAccount.accountId)
                .map { it.toLowerCase() }.contains(publicKey.toLowerCase())
        }

    }
}
