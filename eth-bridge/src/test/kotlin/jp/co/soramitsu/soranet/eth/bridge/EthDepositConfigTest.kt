/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge

import com.d3.commons.config.loadLocalConfigs
import jp.co.soramitsu.soranet.eth.config.loadEthPasswords
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import kotlin.test.assertEquals

class EthDepositConfigTest {

    @Rule
    @JvmField
    val environmentVariables = EnvironmentVariables()

    /**
     * @given properties configuration file and environment variables that overrides properties
     * @when configs are loaded
     * @then new overridden configurations are returned
     */
    @Test
    fun allEnvironmentVariablesEthPasswordTest() {
        val credentialsPath = "credentials/path/..."
        environmentVariables.set("ETH-DEPOSIT_CREDENTIALSPATH", credentialsPath)
        val credentialsPassword = "credentials_password..."
        environmentVariables.set("ETH-DEPOSIT_CREDENTIALSPASSWORD", credentialsPassword)

        val ethPasswords =
            loadEthPasswords("eth-deposit", "/eth/ethereum_password.properties").get()
        assertEquals(credentialsPath, ethPasswords.credentialsPath)
        assertEquals(credentialsPassword, ethPasswords.credentialsPassword)
    }

    /**
     * @given properties configuration file and environment variables that overrides properties
     * @when configs are loaded
     * @then new overridden configurations are returned
     */
    @Test
    fun allEnvironmentVariablesTest() {
        // set all variables
        val registrationServiceIrohaAccount = "registration@service"
        environmentVariables.set(
            "ETH-DEPOSIT_REGISTRATIONSERVICEIROHAACCOUNT",
            registrationServiceIrohaAccount
        )
        val notaryListStorageAccount = "notary_list@storage"
        environmentVariables.set("ETH-DEPOSIT_NOTARYLISTSTORAGEACCOUNT", notaryListStorageAccount)
        val ethAnchoredTokenStorageAccount = "eth_anchored@storage"
        environmentVariables.set(
            "ETH-DEPOSIT_ETHANCHOREDTOKENSTORAGEACCOUNT",
            ethAnchoredTokenStorageAccount
        )
        val ethAnchoredTokenSetterAccount = "eth_anchored@setter"
        environmentVariables.set(
            "ETH-DEPOSIT_ETHANCHOREDTOKENSETTERACCOUNT",
            ethAnchoredTokenSetterAccount
        )
        val irohaAnchoredTokenStorageAccount = "iroha_anchored@storage"
        environmentVariables.set(
            "ETH-DEPOSIT_IROHAANCHOREDTOKENSTORAGEACCOUNT",
            irohaAnchoredTokenStorageAccount
        )
        val irohaAnchoredTokenSetterAccount = "iroha_anchored@setter"
        environmentVariables.set(
            "ETH-DEPOSIT_IROHAANCHOREDTOKENSETTERACCOUNT",
            irohaAnchoredTokenSetterAccount
        )
        val notaryCredentialAccountId = "notary_credentials@account"
        environmentVariables.set(
            "ETH-DEPOSIT_NOTARYCREDENTIAL_ACCOUNTID",
            notaryCredentialAccountId
        )
        val notaryCredentialPubkey = "notary_public_key..."
        environmentVariables.set("ETH-DEPOSIT_NOTARYCREDENTIAL_PUBKEY", notaryCredentialPubkey)
        val notaryCredentialPrivkey = "notary_private_key..."
        environmentVariables.set("ETH-DEPOSIT_NOTARYCREDENTIAL_PRIVKEY", notaryCredentialPrivkey)
        val refundPort = "5000"
        environmentVariables.set("ETH-DEPOSIT_REFUND_PORT", refundPort)
        val irohaHostname = "iroha.host"
        environmentVariables.set("ETH-DEPOSIT_IROHA_HOSTNAME", irohaHostname)
        val irohaPort = "4040"
        environmentVariables.set("ETH-DEPOSIT_IROHA_PORT", irohaPort)
        val lastEthereumReadBlockFilePath = "last/ethereum/read/block/path..."
        environmentVariables.set(
            "ETH-DEPOSIT_LASTETHEREUMREADBLOCKFILEPATH",
            lastEthereumReadBlockFilePath
        )
        val startEthereumBlock = "0"
        environmentVariables.set("ETH-DEPOSIT_STARTETHEREUM_BLOCK", startEthereumBlock)
        val ethereumUrl = "ethereum/node/url:2000"
        environmentVariables.set("ETH-DEPOSIT_ETHEREUM_URL", ethereumUrl)
        val ethereumGasPrice = "20000000"
        environmentVariables.set("ETH-DEPOSIT_ETHEREUM_GASPRICE", ethereumGasPrice)
        val ethereumGasLimit = "65000000"
        environmentVariables.set("ETH-DEPOSIT_ETHEREUM_GASLIMIT", ethereumGasLimit)
        val etehereumConfirmationPeriod = "123"
        environmentVariables.set(
            "ETH-DEPOSIT_ETHEREUM_CONFIRMATIONPERIOD",
            etehereumConfirmationPeriod
        )
        val expansionTriggerAccount = "expansion@account"
        environmentVariables.set("ETH-DEPOSIT_EXPANSIONTRIGGERACCOUNT", expansionTriggerAccount)
        val expansionTriggerCreatorAccountId = "expansion_trigger@account"
        environmentVariables.set(
            "ETH-DEPOSIT_EXPANSIONTRIGGERCREATORACCOUNTID",
            expansionTriggerCreatorAccountId
        )
        val ethIrohaDepositQueue = "iroha-deposit-rmq-queue-name"
        environmentVariables.set("ETH-DEPOSIT_ETHIROHADEPOSITQUEUE", ethIrohaDepositQueue)

        // load configs
        val depositConfig = loadLocalConfigs(
            "eth-deposit",
            EthDepositConfig::class.java,
            "deposit.properties"
        ).get()

        assertEquals(notaryListStorageAccount, depositConfig.notaryListStorageAccount)
        assertEquals(ethAnchoredTokenStorageAccount, depositConfig.ethAnchoredTokenStorageAccount)
        assertEquals(ethAnchoredTokenSetterAccount, depositConfig.ethAnchoredTokenSetterAccount)
        assertEquals(
            irohaAnchoredTokenStorageAccount,
            depositConfig.irohaAnchoredTokenStorageAccount
        )
        assertEquals(irohaAnchoredTokenSetterAccount, depositConfig.irohaAnchoredTokenSetterAccount)
        assertEquals(notaryCredentialAccountId, depositConfig.notaryCredential.accountId)
        assertEquals(notaryCredentialPubkey, depositConfig.notaryCredential.pubkey)
        assertEquals(notaryCredentialPrivkey, depositConfig.notaryCredential.privkey)
        assertEquals(refundPort.toInt(), depositConfig.refund.port)
        assertEquals(irohaHostname, depositConfig.iroha.hostname)
        assertEquals(irohaPort.toInt(), depositConfig.iroha.port)
        assertEquals(lastEthereumReadBlockFilePath, depositConfig.lastEthereumReadBlockFilePath)
        assertEquals(startEthereumBlock.toBigInteger(), depositConfig.startEthereumBlock)
        assertEquals(ethereumUrl, depositConfig.ethereum.url)
        assertEquals(ethereumGasPrice.toLong(), depositConfig.ethereum.gasPrice)
        assertEquals(ethereumGasLimit.toLong(), depositConfig.ethereum.gasLimit)
        assertEquals(
            etehereumConfirmationPeriod.toLong(),
            depositConfig.ethereum.confirmationPeriod
        )
        assertEquals(expansionTriggerAccount, depositConfig.expansionTriggerAccount)
        assertEquals(
            expansionTriggerCreatorAccountId,
            depositConfig.expansionTriggerCreatorAccountId
        )
        assertEquals(ethIrohaDepositQueue, depositConfig.ethIrohaDepositQueue)
    }
}
