/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.vacuum

import com.d3.commons.config.loadLocalConfigs
import integration.eth.config.loadEthPasswords
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import kotlin.test.assertEquals

class RelayVacuumTest {

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
        environmentVariables.set("RELAY-VACUUM_CREDENTIALSPATH", credentialsPath)
        val credentialsPassword = "credentials_password..."
        environmentVariables.set("RELAY-VACUUM_CREDENTIALSPASSWORD", credentialsPassword)

        val ethPasswords =
            loadEthPasswords("relay-vacuum", "/eth/ethereum_password.properties").get()
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
        val registrationServiceIrohaAccount = "registration@account"
        environmentVariables.set(
            "RELAY-VACUUM_REGISTRATIONSERVICEIROHAACCOUNT",
            registrationServiceIrohaAccount
        )
        val ethAnchoredTokenStorageAccount = "eth_anchored@storage"
        environmentVariables.set(
            "RELAY-VACUUM_ETHANCHOREDTOKENSTORAGEACCOUNT",
            ethAnchoredTokenStorageAccount
        )
        val ethAnchoredTokenSetterAccount = "eth_anchored@setter"
        environmentVariables.set(
            "RELAY-VACUUM_ETHANCHOREDTOKENSETTERACCOUNT",
            ethAnchoredTokenSetterAccount
        )
        val irohaAnchoredTokenStorageAccount = "iroha_anchored@storage"
        environmentVariables.set(
            "RELAY-VACUUM_IROHAANCHOREDTOKENSTORAGEACCOUNT",
            irohaAnchoredTokenStorageAccount
        )
        val irohaAnchoredTokenSetterAccount = "iroha_anchored@setter"
        environmentVariables.set(
            "RELAY-VACUUM_IROHAANCHOREDTOKENSETTERACCOUNT",
            irohaAnchoredTokenSetterAccount
        )
        val relayStorageAccount = "notary@account"
        environmentVariables.set("RELAY-VACUUM_RELAYSTORAGEACCOUNT", relayStorageAccount)
        val irohaHostname = "iroha.host"
        environmentVariables.set("RELAY-VACUUM_IROHA_HOSTNAME", irohaHostname)
        val irohaPort = "4040"
        environmentVariables.set("RELAY-VACUUM_IROHA_PORT", irohaPort)
        val ethereumUrl = "ethereum/node/url:2000"
        environmentVariables.set("RELAY-VACUUM_ETHEREUM_URL", ethereumUrl)
        val ethereumGasPrice = "20000000"
        environmentVariables.set("RELAY-VACUUM_ETHEREUM_GASPRICE", ethereumGasPrice)
        val ethereumGasLimit = "65000000"
        environmentVariables.set("RELAY-VACUUM_ETHEREUM_GASLIMIT", ethereumGasLimit)
        val etehereumConfirmationPeriod = "123"
        environmentVariables.set(
            "RELAY-VACUUM_ETHEREUM_CONFIRMATIONPERIOD",
            etehereumConfirmationPeriod
        )
        val vacuumCredentialAccountId = "vacuum_credentials@account"
        environmentVariables.set(
            "RELAY-VACUUM_VACUUMCREDENTIAL_ACCOUNTID",
            vacuumCredentialAccountId
        )
        val vacuumCredentialPubkey = "vacuum_public_key..."
        environmentVariables.set("RELAY-VACUUM_VACUUMCREDENTIAL_PUBKEY", vacuumCredentialPubkey)
        val vacuumCredentialPrivkey = "vacuum_private_key..."
        environmentVariables.set("RELAY-VACUUM_VACUUMCREDENTIAL_PRIVKEY", vacuumCredentialPrivkey)

        // load configs
        val relayConfig = loadLocalConfigs(
            "relay-vacuum",
            RelayVacuumConfig::class.java,
            "vacuum.properties"
        ).get()

        assertEquals(registrationServiceIrohaAccount, relayConfig.registrationServiceIrohaAccount)
        assertEquals(ethAnchoredTokenStorageAccount, relayConfig.ethAnchoredTokenStorageAccount)
        assertEquals(ethAnchoredTokenSetterAccount, relayConfig.ethAnchoredTokenSetterAccount)
        assertEquals(irohaAnchoredTokenStorageAccount, relayConfig.irohaAnchoredTokenStorageAccount)
        assertEquals(irohaAnchoredTokenSetterAccount, relayConfig.irohaAnchoredTokenSetterAccount)
        assertEquals(relayStorageAccount, relayConfig.relayStorageAccount)
        assertEquals(irohaHostname, relayConfig.iroha.hostname)
        assertEquals(irohaPort.toInt(), relayConfig.iroha.port)
        assertEquals(ethereumUrl, relayConfig.ethereum.url)
        assertEquals(ethereumGasPrice.toLong(), relayConfig.ethereum.gasPrice)
        assertEquals(ethereumGasLimit.toLong(), relayConfig.ethereum.gasLimit)
        assertEquals(etehereumConfirmationPeriod.toLong(), relayConfig.ethereum.confirmationPeriod)
        assertEquals(vacuumCredentialAccountId, relayConfig.vacuumCredential.accountId)
        assertEquals(vacuumCredentialPubkey, relayConfig.vacuumCredential.pubkey)
        assertEquals(vacuumCredentialPrivkey, relayConfig.vacuumCredential.privkey)
    }
}
