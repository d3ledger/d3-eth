/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawalservice

import com.d3.commons.config.loadLocalConfigs
import com.d3.eth.withdrawal.withdrawalservice.WithdrawalServiceConfig
import integration.eth.config.loadEthPasswords
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import kotlin.test.assertEquals

class WithdrawalServiceConfigTest {

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
        environmentVariables.set("WITHDRAWAL_CREDENTIALSPATH", credentialsPath)
        val credentialsPassword = "credentials_password..."
        environmentVariables.set("WITHDRAWAL_CREDENTIALSPASSWORD", credentialsPassword)

        val ethPasswords =
            loadEthPasswords("withdrawal", "/eth/ethereum_password.properties").get()
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
        val port = "133700"
        environmentVariables.set("WITHDRAWAL_PORT", port)
        val relayStorageAccount = "ethereum_relays@account"
        environmentVariables.set("WITHDRAWAL_RELAYSTORAGEACCOUNT", relayStorageAccount)
        val ethAnchoredTokenStorageAccount = "eth_anchored@storage"
        environmentVariables.set(
            "WITHDRAWAL_ETHANCHOREDTOKENSTORAGEACCOUNT",
            ethAnchoredTokenStorageAccount
        )
        val ethAnchoredTokenSetterAccount = "eth_anchored@setter"
        environmentVariables.set(
            "WITHDRAWAL_ETHANCHOREDTOKENSETTERACCOUNT",
            ethAnchoredTokenSetterAccount
        )
        val irohaAnchoredTokenStorageAccount = "iroha_anchored@storage"
        environmentVariables.set(
            "WITHDRAWAL_IROHAANCHOREDTOKENSTORAGEACCOUNT",
            irohaAnchoredTokenStorageAccount
        )
        val irohaAnchoredTokenSetterAccount = "iroha_anchored@setter"
        environmentVariables.set(
            "WITHDRAWAL_IROHAANCHOREDTOKENSETTERACCOUNT",
            irohaAnchoredTokenSetterAccount
        )
        val notaryListStorageAccount = "notary_list@storage"
        environmentVariables.set("WITHDRAWAL_NOTARYLISTSTORAGEACCOUNT", notaryListStorageAccount)
        val notaryListSetterAccount = "notary_list@setter"
        environmentVariables.set("WITHDRAWAL_NOTARYLISTSETTERACCOUNT", notaryListSetterAccount)
        val registrationIrohaAccount = "registration@service"
        environmentVariables.set(
            "WITHDRAWAL_REGISTRATIONIROHAACCOUNT",
            registrationIrohaAccount
        )
        val expansionTriggerAccount = "expansion@account"
        environmentVariables.set("WITHDRAWAL_EXPANSIONTRIGGERACCOUNT", expansionTriggerAccount)
        val expansionTriggerCreatorAccountId = "expansion_trigger@account"
        environmentVariables.set(
            "WITHDRAWAL_EXPANSIONTRIGGERCREATORACCOUNTID",
            expansionTriggerCreatorAccountId
        )
        val ethMasterAddress = "0xMasterAddress..."
        environmentVariables.set("WITHDRAWAL_ETHMASTERADDRESS", ethMasterAddress)

        val withdrawalCredentialAccountId = "withdrawal_credentials@account"
        environmentVariables.set(
            "WITHDRAWAL_WITHDRAWALCREDENTIAL_ACCOUNTID",
            withdrawalCredentialAccountId
        )
        val withdrawalCredentialPubkey = "withdrawal_public_key..."
        environmentVariables.set(
            "WITHDRAWAL_WITHDRAWALCREDENTIAL_PUBKEY",
            withdrawalCredentialPubkey
        )
        val withdrawalCredentialPrivkey = "withdrawal_private_key..."
        environmentVariables.set(
            "WITHDRAWAL_WITHDRAWALCREDENTIAL_PRIVKEY",
            withdrawalCredentialPrivkey
        )
        val irohaHostname = "iroha.host"
        environmentVariables.set("WITHDRAWAL_IROHA_HOSTNAME", irohaHostname)
        val irohaPort = "4040"
        environmentVariables.set("WITHDRAWAL_IROHA_PORT", irohaPort)
        val ethereumUrl = "ethereum/node/url:2000"
        environmentVariables.set("WITHDRAWAL_ETHEREUM_URL", ethereumUrl)
        val ethereumGasPrice = "20000000"
        environmentVariables.set("WITHDRAWAL_ETHEREUM_GASPRICE", ethereumGasPrice)
        val ethereumGasLimit = "65000000"
        environmentVariables.set("WITHDRAWAL_ETHEREUM_GASLIMIT", ethereumGasLimit)
        val etehereumConfirmationPeriod = "123"
        environmentVariables.set(
            "WITHDRAWAL_ETHEREUM_CONFIRMATIONPERIOD",
            etehereumConfirmationPeriod
        )
        val ethIrohaDepositQueue = "iroha-withdrawal-rmq-queue-name"
        environmentVariables.set("WITHDRAWAL_ETHIROHAWITHDRAWALQUEUE", ethIrohaDepositQueue)

        // load configs
        val withdrawalConfig = loadLocalConfigs(
            "withdrawal",
            WithdrawalServiceConfig::class.java,
            "withdrawal.properties"
        ).get()

        assertEquals(port.toInt(), withdrawalConfig.port)
        assertEquals(relayStorageAccount, withdrawalConfig.relayStorageAccount)

        assertEquals(
            ethAnchoredTokenStorageAccount,
            withdrawalConfig.ethAnchoredTokenStorageAccount
        )
        assertEquals(ethAnchoredTokenSetterAccount, withdrawalConfig.ethAnchoredTokenSetterAccount)
        assertEquals(
            irohaAnchoredTokenStorageAccount,
            withdrawalConfig.irohaAnchoredTokenStorageAccount
        )
        assertEquals(
            irohaAnchoredTokenSetterAccount,
            withdrawalConfig.irohaAnchoredTokenSetterAccount
        )
        assertEquals(notaryListStorageAccount, withdrawalConfig.notaryListStorageAccount)
        assertEquals(notaryListSetterAccount, withdrawalConfig.notaryListSetterAccount)
        assertEquals(registrationIrohaAccount, withdrawalConfig.registrationIrohaAccount)
        assertEquals(expansionTriggerAccount, withdrawalConfig.expansionTriggerAccount)
        assertEquals(
            expansionTriggerCreatorAccountId,
            withdrawalConfig.expansionTriggerCreatorAccountId
        )
        assertEquals(ethMasterAddress, withdrawalConfig.ethMasterAddress)
        assertEquals(withdrawalCredentialAccountId, withdrawalConfig.withdrawalCredential.accountId)
        assertEquals(withdrawalCredentialPubkey, withdrawalConfig.withdrawalCredential.pubkey)
        assertEquals(withdrawalCredentialPrivkey, withdrawalConfig.withdrawalCredential.privkey)
        assertEquals(irohaHostname, withdrawalConfig.iroha.hostname)
        assertEquals(irohaPort.toInt(), withdrawalConfig.iroha.port)
        assertEquals(ethereumUrl, withdrawalConfig.ethereum.url)
        assertEquals(ethereumGasPrice.toLong(), withdrawalConfig.ethereum.gasPrice)
        assertEquals(ethereumGasLimit.toLong(), withdrawalConfig.ethereum.gasLimit)
        assertEquals(
            etehereumConfirmationPeriod.toLong(),
            withdrawalConfig.ethereum.confirmationPeriod
        )
        assertEquals(ethIrohaDepositQueue, withdrawalConfig.ethIrohaWithdrawalQueue)
    }
}
