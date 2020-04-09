/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import integration.helper.IrohaConfigHelper
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.soranet.eth.bridge.endpoint.EthAddPeerStrategyImpl
import jp.co.soramitsu.soranet.eth.bridge.endpoint.EthServerEndpoint
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationHelperUtil
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationTestEnvironment
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
import jp.co.soramitsu.soranet.eth.sidechain.util.ENDPOINT_ETHEREUM
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.crypto.Keys
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
class EthExpansionTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment
    private val integrationHelper = ethIntegrationTestEnvironment.integrationHelper
    private val depositConfig = ethIntegrationTestEnvironment.ethDepositConfig

    init {
        ethIntegrationTestEnvironment.init()
    }

    @AfterAll
    fun dropDown() {
        ethIntegrationTestEnvironment.refresh()
        ethIntegrationTestEnvironment.close()
    }

    /**
     * @given deposit and withdrawal services are running
     * @when expansion transaction is triggered
     * @then
     */
    @Test
    fun expansionTest() {
        val publicKey = Ed25519Sha3().generateKeypair().public.toHexString()
        val ecKeyPair = Keys.createEcKeyPair()
        val ethAddress = "0x${Keys.getAddress(ecKeyPair)}"
        val notaryName = "notary_name_" + String.getRandomString(5)
        val port = IrohaConfigHelper.portCounter.incrementAndGet()
        val notaryEndpointAddress = "http://localhost:$port"

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

        val newServerBundle = ServerInitializationBundle(port, ENDPOINT_ETHEREUM)
        val ethServerEndpoint = EthServerEndpoint(
            newServerBundle,
            EthAddPeerStrategyImpl(
                integrationHelper.queryHelper,
                ecKeyPair,
                depositConfig.expansionTriggerAccount,
                depositConfig.expansionTriggerCreatorAccountId
            )
        )

        Thread.sleep(5_000)

        assertTrue { masterContract.isPeer(ethAddress).send() }
        assertEquals(notaryEndpointAddress, integrationHelper.getNotaries()[notaryName])
        assertTrue {
            integrationHelper.getSignatories(integrationHelper.accountHelper.notaryAccount.accountId)
                .map { it.toLowerCase() }.contains(publicKey.toLowerCase())
        }
        ethServerEndpoint.close()
    }
}
