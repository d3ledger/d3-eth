/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.hashToRegistration
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import kotlin.test.assertNotEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthRegistrationProofIntegrationTest {
    private val integrationHelper = EthIntegrationHelperUtil()
    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    private val ethDeposit: Job
    private val depositConfig = integrationHelper.configHelper.createEthDepositConfig()
    private val deployHelper = DeployHelper(depositConfig.ethereum, integrationHelper.configHelper.ethPasswordConfig)

    init {
        // run notary
        ethDeposit = GlobalScope.launch {
            integrationHelper.runEthDeposit(ethDepositConfig = depositConfig)
        }
        registrationTestEnvironment.registrationInitialization.init()
        Thread.sleep(10_000)
    }

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterAll
    fun dropDown() {
        ethDeposit.cancel()
        integrationHelper.close()
    }

    /**
     * @given running registration proof collector service
     * @when an existing client gets proof of registration after setting his address in account details
     * @then the registration service returns a valid proof
     */
    @Test
    fun getRegistrationProof() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val keyPair = ModelUtil.generateKeypair()
            val clientIrohaAccount = String.getRandomString(5)
            val ethAddress = "0x123"
            var res = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount,
                keyPair.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res.statusCode)
            val clientIrohaConsumer =
                IrohaConsumerImpl(IrohaCredential("$clientIrohaAccount@d3", keyPair), integrationHelper.irohaAPI)
            val txHash =
                ModelUtil.setAccountDetail(
                    clientIrohaConsumer,
                    clientIrohaConsumer.creator,
                    "eth_address",
                    ethAddress,
                    quorum = 2
                ).get()
            res = khttp.get("http://127.0.0.1:${depositConfig.refund.port}/ethereum/proof/registration/$txHash")
            assertEquals(200, res.statusCode)
            val proof = res.jsonObject.get("ethSignature")
            val hash =
                hashToRegistration(accountId = "$clientIrohaAccount@d3", address = ethAddress, irohaHash = txHash)
            assertEquals(deployHelper.signUserData(hash), proof)
        }
    }

    /**
     * @given running registration proof collector service
     * @when the registration service is called with an invalid tx hash
     * @then the registration service returns an error
     */
    @Test
    fun getRegistrationProofBadTx() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val keyPair = ModelUtil.generateKeypair()
            val clientIrohaAccount = String.getRandomString(5)
            val ethAddress = "0x123"
            var res = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount,
                keyPair.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res.statusCode)
            val clientIrohaConsumer =
                IrohaConsumerImpl(IrohaCredential("$clientIrohaAccount@d3", keyPair), integrationHelper.irohaAPI)
            val txHash =
                ModelUtil.setAccountDetail(
                    clientIrohaConsumer,
                    clientIrohaConsumer.creator,
                    "abc",
                    ethAddress,
                    quorum = 2
                ).get()
            res = khttp.get("http://127.0.0.1:${depositConfig.refund.port}/ethereum/proof/registration/$txHash")
            assertNotEquals(200, res.statusCode)
        }
    }

    /**
     * @given running registration proof collector service
     * @when an existing client gets proof of registration after setting his address in account details incorrectly(bad key)
     * @then the registration service returns an error
     */
    @Test
    fun getRegistrationProofNonExistingTxHash() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val res = khttp.get("http://127.0.0.1:${depositConfig.refund.port}/ethereum/proof/registration/123")
            assertNotEquals(200, res.statusCode)
        }
    }
}
