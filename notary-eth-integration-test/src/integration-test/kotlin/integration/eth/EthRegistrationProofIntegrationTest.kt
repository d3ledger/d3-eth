/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import com.d3.commons.model.IrohaCredential
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.eth.provider.ETH_CLIENT_WALLET
import com.d3.eth.provider.EthTokensProvider
import com.d3.eth.provider.EthWalletProviderIrohaImpl
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.hashToRegistration
import com.d3.eth.withdrawal.withdrawalservice.ProofCollector
import com.nhaarman.mockitokotlin2.mock
import integration.helper.ContractTestHelper
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.*
import org.web3j.utils.Numeric
import java.time.Duration
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthRegistrationProofIntegrationTest {
    private lateinit var cth: ContractTestHelper
    private val integrationHelper = EthIntegrationHelperUtil()
    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    private val ethDeposit: Job
    private val depositConfig = integrationHelper.configHelper.createEthDepositConfig()
    private val withdrawalConfig =
        integrationHelper.configHelper.createWithdrawalConfig(String.getRandomString(3))
    private val deployHelper =
        DeployHelper(depositConfig.ethereum, integrationHelper.configHelper.ethPasswordConfig)

    init {
        // run notary
        ethDeposit = GlobalScope.launch {
            integrationHelper.runEthDeposit(ethDepositConfig = depositConfig)
        }
        registrationTestEnvironment.registrationInitialization.init()
        Thread.sleep(10_000)
    }

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @BeforeEach
    fun setup() {
        cth = ContractTestHelper()
    }

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
                IrohaConsumerImpl(
                    IrohaCredential("$clientIrohaAccount@d3", keyPair),
                    integrationHelper.irohaAPI
                )
            val txHash =
                ModelUtil.setAccountDetail(
                    clientIrohaConsumer,
                    clientIrohaConsumer.creator,
                    ETH_CLIENT_WALLET,
                    ethAddress,
                    quorum = 2
                ).get()
            res =
                khttp.get("http://127.0.0.1:${depositConfig.refund.port}/ethereum/proof/registration/$txHash")
            assertEquals(200, res.statusCode)
            val proof = res.jsonObject.get("ethSignature")
            val hash =
                hashToRegistration(
                    accountId = "$clientIrohaAccount@d3",
                    address = ethAddress,
                    irohaHash = txHash
                )
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
                IrohaConsumerImpl(
                    IrohaCredential("$clientIrohaAccount@d3", keyPair),
                    integrationHelper.irohaAPI
                )
            val txHash =
                ModelUtil.setAccountDetail(
                    clientIrohaConsumer,
                    clientIrohaConsumer.creator,
                    "wrong_key",
                    ethAddress,
                    quorum = 2
                ).get()
            res =
                khttp.get("http://127.0.0.1:${depositConfig.refund.port}/ethereum/proof/registration/$txHash")
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
            val res =
                khttp.get("http://127.0.0.1:${depositConfig.refund.port}/ethereum/proof/registration/123")
            assertNotEquals(200, res.statusCode)
        }
    }

    /**
     * @given running registration proof collector service
     * @when an existing client gets proof of registration after setting his address in account details
     * and sends it to Ethereum
     * @then the Ethereum registration service tracks the event and registers client in iroha with
     * provided Ethereum address
     */
    @Test
    fun correctRegistration() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val keyPair = ModelUtil.generateKeypair()
            val clientIrohaAccount = String.getRandomString(5)
            val clientIrohaAccountId = "$clientIrohaAccount@d3"
            val ethAddress = cth.deployHelper.credentials.address
            // 1st - register at notary registration - create iroha account
            val res = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount,
                keyPair.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res.statusCode)

            // 2nd - send ethereum address to Iroha
            val clientIrohaConsumer = IrohaConsumerImpl(
                IrohaCredential(clientIrohaAccountId, keyPair),
                integrationHelper.irohaAPI
            )
            val txHash = ModelUtil.setAccountDetail(
                clientIrohaConsumer,
                clientIrohaConsumer.creator,
                ETH_CLIENT_WALLET,
                ethAddress,
                quorum = 2
            ).get()

            val tokensProvider = mock<EthTokensProvider>()
            val withdrawalQueryHelper = IrohaQueryHelperImpl(
                integrationHelper.irohaAPI,
                withdrawalConfig.withdrawalCredential
            )
            val notaryPeerListProvider = NotaryPeerListProviderImpl(
                withdrawalQueryHelper,
                withdrawalConfig.notaryListStorageAccount,
                withdrawalConfig.notaryListSetterAccount
            )

            // get proofs
            val proofCollector = ProofCollector(
                withdrawalQueryHelper,
                withdrawalConfig,
                tokensProvider,
                notaryPeerListProvider
            )

            val proof = proofCollector.collectProofForRegistration(
                ethAddress,
                clientIrohaAccountId,
                txHash
            ).get()

            // register in Ethereum
            val tx = integrationHelper.masterContract.register(
                proof.ethAddress,
                proof.irohaAccountId.toByteArray(),
                Numeric.hexStringToByteArray(proof.irohaHash),
                proof.v,
                proof.r,
                proof.s
            ).send()

            assertTrue { tx.isStatusOK }

            // let notary discover Ethereum tx and handle it
            Thread.sleep(15_000)

            // check registration address
            val clientAddressProvider = EthWalletProviderIrohaImpl(
                withdrawalQueryHelper,
                withdrawalConfig.notaryIrohaAccount,
                withdrawalConfig.notaryIrohaAccount,
                ETH_CLIENT_WALLET
            ) { _, _ -> true }

            assertEquals(
                ethAddress,
                clientAddressProvider.getAddressByAccountId(clientIrohaAccountId).get().get()
            )
        }
    }
}
