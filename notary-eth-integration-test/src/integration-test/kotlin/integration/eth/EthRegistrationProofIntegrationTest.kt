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
import com.d3.commons.util.irohaEscape
import com.d3.eth.deposit.endpoint.EthSignature
import com.d3.eth.provider.ETH_WALLET
import com.d3.eth.registration.wallet.ETH_FAILED_REGISTRATION_KEY
import com.d3.eth.registration.wallet.ETH_REGISTRATION_KEY
import com.d3.eth.registration.wallet.EthereumRegistrationProof
import com.d3.eth.registration.wallet.createRegistrationProof
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.extractVRS
import com.d3.eth.sidechain.util.hashToRegistration
import integration.helper.ContractTestHelper
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.*
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import java.math.BigInteger
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
     * Check whether ethereum wallet was registered for client
     * @param clientId - iroha account id
     * @param ethereumAddress - ethereum address as "0x123..."
     */
    private fun checkRegisteredEthereumAddress(clientId: String, ethereumAddress: String) {
        val registeredAddress = integrationHelper.queryHelper.getAccountDetails(
            clientId,
            integrationHelper.accountHelper.notaryAccount.accountId,
            ETH_WALLET
        ).get().get()
        assertEquals(ethereumAddress, registeredAddress)

        val registeredAccount = integrationHelper.queryHelper.getAccountDetails(
            integrationHelper.accountHelper.ethereumWalletStorageAccount.accountId,
            integrationHelper.accountHelper.notaryAccount.accountId,
            ethereumAddress
        ).get().get()
        assertEquals(clientId, registeredAccount)
    }

    /**
     * @given notary service is running and client with iroha account and ethereum wallet
     * @when the client sends ethereum wallet registration request
     * @then ethereum address is registered for client
     */
    @Test
    fun correctWalletRegistration() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val keyPair = ModelUtil.generateKeypair()
            val clientIrohaAccount = String.getRandomString(5)
            val ethKeypair = Keys.createEcKeyPair()
            val clientId = "$clientIrohaAccount@d3"
            val ethereumAddress = "0x${Keys.getAddress(ethKeypair.publicKey)}"

            // register iroha account
            val res = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount,
                keyPair.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res.statusCode)

            // register in Ethereum
            integrationHelper.registerEthereumWallet(clientId, keyPair, ethKeypair)

            Thread.sleep(3_000)
            checkRegisteredEthereumAddress(clientId, ethereumAddress)
        }
    }

    /**
     * @given notary service is running and client with iroha account and ethereum wallet
     * @when the client sends ethereum wallet registration request with wrong proof
     * @then failed registration record is sent to iroha,
     */
    @Test
    fun correctAfterIncorrectWalletRegistration() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val keyPair = ModelUtil.generateKeypair()
            val clientIrohaAccount = String.getRandomString(5)
            val ethKeypair = Keys.createEcKeyPair()
            val clientId = "$clientIrohaAccount@d3"
            val ethereumAddress = "0x${Keys.getAddress(ethKeypair.publicKey)}"

            // register iroha account
            val res = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount,
                keyPair.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res.statusCode)

            // send wrong signature
            val clientIrohaConsumer = IrohaConsumerImpl(
                IrohaCredential(clientId, keyPair),
                integrationHelper.irohaAPI
            )
            val signature = createRegistrationProof(ethKeypair)
            val wrongSignature = EthereumRegistrationProof(signature.signature, BigInteger.ZERO)
            integrationHelper.setAccountDetail(
                clientIrohaConsumer,
                integrationHelper.accountHelper.registrationAccount.accountId,
                ETH_REGISTRATION_KEY,
                wrongSignature.toJson().irohaEscape(),
                quorum = 2
            )
            Thread.sleep(3_000)

            // check wrong registration
            assertTrue {
                integrationHelper.queryHelper.getAccountDetails(
                    clientId,
                    integrationHelper.accountHelper.notaryAccount.accountId,
                    ETH_FAILED_REGISTRATION_KEY
                ).get().isPresent
            }

            // register in Ethereum again
            integrationHelper.registerEthereumWallet(clientId, keyPair, ethKeypair)

            Thread.sleep(3_000)
            checkRegisteredEthereumAddress(clientId, ethereumAddress)
        }
    }

    /**
     * @given
     * @when
     * @then
     */
    @Test
    fun doubleWalletRegistration() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val ethKeypair = Keys.createEcKeyPair()
            val ethereumAddress = "0x${Keys.getAddress(ethKeypair.publicKey)}"

            val keyPair1 = ModelUtil.generateKeypair()
            val clientIrohaAccount1 = String.getRandomString(5)
            val clientId1 = "$clientIrohaAccount1@d3"

            val keyPair2 = ModelUtil.generateKeypair()
            val clientIrohaAccount2 = String.getRandomString(5)
            val clientId2 = "$clientIrohaAccount2@d3"

            // register iroha accounts
            val res1 = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount1,
                keyPair1.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res1.statusCode)

            val res2 = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount2,
                keyPair2.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res2.statusCode)

            // register in Ethereum client1
            integrationHelper.registerEthereumWallet(clientId1, keyPair1, ethKeypair)
            Thread.sleep(3_000)

            checkRegisteredEthereumAddress(clientId1, ethereumAddress)

            // register in Ethereum client2 with the same Ethereum address
            integrationHelper.registerEthereumWallet(clientId2, keyPair2, ethKeypair)
            Thread.sleep(3_000)

            // check wrong registration
            assertTrue {
                integrationHelper.queryHelper.getAccountDetails(
                    clientId2,
                    integrationHelper.accountHelper.notaryAccount.accountId,
                    ETH_FAILED_REGISTRATION_KEY
                ).get().isPresent
            }
        }
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
                    "eth_address",
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
                    "abc",
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
            var res = integrationHelper.sendRegistrationRequest(
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
                "eth_address",
                ethAddress,
                quorum = 2
            ).get()
            res =
                khttp.get("http://127.0.0.1:${depositConfig.refund.port}/ethereum/proof/registration/$txHash")
            assertEquals(200, res.statusCode)
            val signature = res.jsonObject.get("ethSignature") as EthSignature
            val hash = hashToRegistration(
                address = ethAddress,
                accountId = clientIrohaAccountId,
                irohaHash = txHash
            )
            assertEquals(deployHelper.signUserData(hash), signature)

            val vv = ArrayList<BigInteger>()
            val rr = ArrayList<ByteArray>()
            val ss = ArrayList<ByteArray>()

            val vrs = extractVRS(signature)
            vv.add(vrs.v)
            rr.add(vrs.r)
            ss.add(vrs.s)

            // register in Ethereum
            val tx = integrationHelper.masterContract.register(
                ethAddress,
                clientIrohaAccountId.toByteArray(),
                Numeric.hexStringToByteArray(txHash),
                vv,
                rr,
                ss
            ).send()

            println(tx.transactionHash)
            println(tx.isStatusOK)

            Thread.sleep(10_000)

            // todo check registration
        }
    }
}
