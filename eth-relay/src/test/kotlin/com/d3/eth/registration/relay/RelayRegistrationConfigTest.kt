/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration.relay

import com.d3.commons.config.loadLocalConfigs
import integration.eth.config.loadEthPasswords
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import kotlin.test.assertEquals

class RelayRegistrationConfigTest {

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
        environmentVariables.set("RELAY-REGISTRATION_CREDENTIALSPATH", credentialsPath)
        val credentialsPassword = "credentials_password..."
        environmentVariables.set("RELAY-REGISTRATION_CREDENTIALSPASSWORD", credentialsPassword)

        val ethPasswords =
            loadEthPasswords("relay-registration", "/eth/ethereum_password.properties").get()
        assertEquals(credentialsPath, ethPasswords.credentialsPath)
        assertEquals(credentialsPassword, ethPasswords.credentialsPassword)
    }

    /**
     * @given properties configuration file and environment variables that overrides properties
     * @when configs are loaded
     * @then new overridden configurations are returned
     */
    @Test
    fun allEnvironmentVariablesRelayRegistrationConfigTest() {
        val number = "12"
        environmentVariables.set("RELAY-REGISTRATION_NUMBER", number)
        val replenishmentPeriod = "1334"
        environmentVariables.set("RELAY-REGISTRATION_REPLENISHMENTPERIOD", replenishmentPeriod)
        val ethMasterAddress = "0xMasterAddress..."
        environmentVariables.set("RELAY-REGISTRATION_ETHMASTERADDRESS", ethMasterAddress)
        val ethRelayImplementationAddress = "0xRelayImplementationAddress..."
        environmentVariables.set(
            "RELAY-REGISTRATION_ETHRELAYIMPLEMENTATIONADDRESS",
            ethRelayImplementationAddress
        )
        val relayStorageAccount = "ethereum_relays@account"
        environmentVariables.set("RELAY-REGISTRATION_RELAYSTORAGEACCOUNT", relayStorageAccount)

        val relayRegistrationCredentialAccountId = "notary_credentials@account"
        environmentVariables.set(
            "RELAY-REGISTRATION_RELAYREGISTRATIONCREDENTIAL_ACCOUNTID",
            relayRegistrationCredentialAccountId
        )
        val relayRegistrationCredentialPubkey = "notary_public_key..."
        environmentVariables.set(
            "RELAY-REGISTRATION_RELAYREGISTRATIONCREDENTIAL_PUBKEY",
            relayRegistrationCredentialPubkey
        )
        val relayRegistrationCredentialPrivkey = "notary_private_key..."
        environmentVariables.set(
            "RELAY-REGISTRATION_RELAYREGISTRATIONCREDENTIAL_PRIVKEY",
            relayRegistrationCredentialPrivkey
        )
        val irohaHostname = "iroha.host"
        environmentVariables.set("RELAY-REGISTRATION_IROHA_HOSTNAME", irohaHostname)
        val irohaPort = "4040"
        environmentVariables.set("RELAY-REGISTRATION_IROHA_PORT", irohaPort)
        val ethereumUrl = "ethereum/node/url:2000"
        environmentVariables.set("RELAY-REGISTRATION_ETHEREUM_URL", ethereumUrl)
        val ethereumGasPrice = "20000000"
        environmentVariables.set("RELAY-REGISTRATION_ETHEREUM_GASPRICE", ethereumGasPrice)
        val ethereumGasLimit = "65000000"
        environmentVariables.set("RELAY-REGISTRATION_ETHEREUM_GASLIMIT", ethereumGasLimit)
        val etehereumConfirmationPeriod = "123"
        environmentVariables.set(
            "RELAY-REGISTRATION_ETHEREUM_CONFIRMATIONPERIOD",
            etehereumConfirmationPeriod
        )

        // load configs
        val relayConfig =
            loadLocalConfigs(
                "relay-registration",
                RelayRegistrationConfig::class.java,
                "relay_registration.properties"
            ).get()

        assertEquals(number.toInt(), relayConfig.number)
        assertEquals(replenishmentPeriod.toLong(), relayConfig.replenishmentPeriod)
        assertEquals(ethMasterAddress, relayConfig.ethMasterAddress)
        assertEquals(ethRelayImplementationAddress, relayConfig.ethRelayImplementationAddress)
        assertEquals(relayStorageAccount, relayConfig.relayStorageAccount)
        assertEquals(
            relayRegistrationCredentialAccountId,
            relayConfig.relayRegistrationCredential.accountId
        )
        assertEquals(
            relayRegistrationCredentialPubkey,
            relayConfig.relayRegistrationCredential.pubkey
        )
        assertEquals(
            relayRegistrationCredentialPrivkey,
            relayConfig.relayRegistrationCredential.privkey
        )
        assertEquals(irohaHostname, relayConfig.iroha.hostname)
        assertEquals(irohaPort.toInt(), relayConfig.iroha.port)
        assertEquals(ethereumUrl, relayConfig.ethereum.url)
        assertEquals(ethereumGasPrice.toLong(), relayConfig.ethereum.gasPrice)
        assertEquals(ethereumGasLimit.toLong(), relayConfig.ethereum.gasLimit)
        assertEquals(
            etehereumConfirmationPeriod.toLong(),
            relayConfig.ethereum.confirmationPeriod
        )
    }
}
