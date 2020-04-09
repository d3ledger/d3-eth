/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.squareup.moshi.Moshi
import integration.helper.D3_DOMAIN
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.soranet.eth.config.loadEthPasswords
import jp.co.soramitsu.soranet.eth.bridge.EthDepositConfig
import jp.co.soramitsu.soranet.eth.bridge.endpoint.BigIntegerMoshiAdapter
import jp.co.soramitsu.soranet.eth.bridge.endpoint.EthNotaryResponse
import jp.co.soramitsu.soranet.eth.bridge.endpoint.EthNotaryResponseMoshiAdapter
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationHelperUtil
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationTestEnvironment
import jp.co.soramitsu.soranet.eth.provider.ETH_PRECISION
import jp.co.soramitsu.soranet.eth.provider.ETH_WALLET
import jp.co.soramitsu.soranet.eth.provider.EthAddressProviderIrohaImpl
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
import jp.co.soramitsu.soranet.eth.sidechain.util.ENDPOINT_ETHEREUM
import jp.co.soramitsu.soranet.eth.sidechain.util.hashToWithdraw
import jp.co.soramitsu.soranet.eth.sidechain.util.signUserData
import khttp.get
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.*
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
class WithdrawalMultinotaryIntegrationTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment

    /** Utility functions for integration tests */
    private val integrationHelper = ethIntegrationTestEnvironment.integrationHelper

    private val keyPair2 = ModelUtil.generateKeypair()

    private val depositConfig1: EthDepositConfig

    private val depositConfig2: EthDepositConfig

    private val keypair1: ECKeyPair

    private val ethKeyPair2: ECKeyPair

    private val ethereumPasswords =
        loadEthPasswords("eth-deposit", "/eth/ethereum_password.properties").get()

    private val withdrawalAccountId = integrationHelper.accountHelper.notaryAccount.accountId

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    private val ethDeposit2: Job

    init {
        ethIntegrationTestEnvironment.init()
        ethIntegrationTestEnvironment.refresh()
        val ethKeyPath = ethereumPasswords.credentialsPath

        // create 1st deposit config
        depositConfig1 = ethIntegrationTestEnvironment.ethDepositConfig
        val ethereumPasswordsConfig1 =
            integrationHelper.configHelper.createEthereumPasswords(ethKeyPath)

        // run 1st instance of deposit
        keypair1 = DeployHelper(depositConfig1.ethereum, ethereumPasswordsConfig1).credentials.ecKeyPair

        // create 2nd deposit config
        val ethereumPasswordsConfig2 = integrationHelper.configHelper
            .createEthereumPasswords(ethKeyPath.split(".key").first() + "2.key")
        depositConfig2 = integrationHelper.configHelper.createEthDepositConfig()

        val notary2IrohaPublicKey = keyPair2.public.toHexString()
        val notary2EthereumCredentials =
            DeployHelper(depositConfig2.ethereum, ethereumPasswordsConfig2).credentials
        val notary2EthereumAddress = notary2EthereumCredentials.address
        ethKeyPair2 = notary2EthereumCredentials.ecKeyPair
        val notary2Name = "notary_name_" + String.getRandomString(5)
        val notary2EndpointAddress = "http://127.0.0.1:${depositConfig2.refund.port}"

        integrationHelper.triggerExpansion(
            integrationHelper.accountHelper.notaryAccount.accountId,
            notary2IrohaPublicKey,
            2,
            notary2EthereumAddress,
            notary2Name,
            notary2EndpointAddress
        )

        Thread.sleep(5_000)

        // run 2nd instance of deposit
        ethDeposit2 = GlobalScope.launch {
            integrationHelper.runEthDeposit(
                ethereumPasswords = ethereumPasswordsConfig2,
                ethDepositConfig = depositConfig2
            )
        }
    }

    @AfterAll
    fun dropDown() {
        ethDeposit2.cancel()
        ethIntegrationTestEnvironment.close()
    }

    /**
     * Test US-003 Withdrawal of ETH token
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha is running, 2 notaries are running and user has sent 64203 Ether to master
     * @when withdrawal service queries notary1 and notary2
     * @then both notaries reply with valid refund information and signature
     */
    @Test
    fun testRefundEndpoints() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val amount = "64203"
            val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(ETH_PRECISION)
            val assetId = "ether#ethereum"
            val ethWallet = "0x1334"
            val ethKeyPair = Keys.createEcKeyPair()

            // create
            val client = String.getRandomString(9)
            // register client in Iroha
            val res = ethIntegrationTestEnvironment.registrationTestEnvironment.register(
                client,
                ModelUtil.generateKeypair().public.toHexString()
            )
            Assertions.assertEquals(200, res.statusCode)
            val clientId = "$client@$D3_DOMAIN"
            integrationHelper.registerEthereumWallet(
                clientId,
                integrationHelper.testCredential.keyPair,
                ethKeyPair
            )

            integrationHelper.addIrohaAssetTo(clientId, assetId, decimalAmount)

            Thread.sleep(5_000)
            val relay = EthAddressProviderIrohaImpl(
                integrationHelper.queryHelper,
                integrationHelper.accountHelper.ethereumWalletStorageAccount.accountId,
                integrationHelper.accountHelper.notaryAccount.accountId,
                ETH_WALLET
            ).getAddresses().get().filter {
                it.value == clientId
            }.keys.first()

            // transfer assets from user to notary master account
            val hash = integrationHelper.transferAssetIrohaFromClient(
                clientId,
                integrationHelper.testCredential.keyPair,
                clientId,
                withdrawalAccountId,
                assetId,
                ethWallet,
                amount
            )

            // query 1
            val res1 =
                get("http://127.0.0.1:${depositConfig1.refund.port}/$ENDPOINT_ETHEREUM/$hash")

            val moshi = Moshi
                .Builder()
                .add(EthNotaryResponseMoshiAdapter())
                .add(BigInteger::class.java, BigIntegerMoshiAdapter())
                .build()!!
            val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
            val response1 = ethNotaryAdapter.fromJson(res1.jsonObject.toString())

            assert(response1 is EthNotaryResponse.Successful)
            response1 as EthNotaryResponse.Successful

            Assertions.assertEquals(
                signUserData(
                    keypair1,
                    hashToWithdraw(
                        "0x0000000000000000000000000000000000000000",
                        decimalAmount.toPlainString(),
                        ethWallet,
                        hash,
                        relay
                    )
                ), response1.ethSignature
            )

            // query 2
            val res2 =
                get("http://127.0.0.1:${depositConfig2.refund.port}/$ENDPOINT_ETHEREUM/$hash")

            val response2 = ethNotaryAdapter.fromJson(res2.jsonObject.toString())

            assert(response2 is EthNotaryResponse.Successful)
            response2 as EthNotaryResponse.Successful

            Assertions.assertEquals(
                signUserData(
                    ethKeyPair2,
                    hashToWithdraw(
                        "0x0000000000000000000000000000000000000000",
                        decimalAmount.toPlainString(),
                        ethWallet,
                        hash,
                        relay
                    )
                ), response2.ethSignature
            )
        }
    }
}
