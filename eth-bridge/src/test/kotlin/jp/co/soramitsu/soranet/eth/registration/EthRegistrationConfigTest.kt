/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.registration

import com.d3.commons.config.loadLocalConfigs
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import kotlin.test.assertEquals

class EthRegistrationConfigTest {

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
        val port = "1334"
        environmentVariables.set("ETH-REGISTRATION_PORT", port)
        val relayRegistrationIrohaAccount = "relay_registration@account"
        environmentVariables.set(
            "ETH-REGISTRATION_RELAYREGISTRATIONIROHAACCOUNT",
            relayRegistrationIrohaAccount
        )
        val relayStorageAccount = "notary@account"
        environmentVariables.set("ETH-REGISTRATION_RELAYSTORAGEACCOUNT", relayStorageAccount)
        val registrationCredentialAccountId = "registration@credential"
        environmentVariables.set(
            "ETH-REGISTRATION_REGISTRATIONCREDENTIAL_ACCOUNTID",
            registrationCredentialAccountId
        )
        val registrationCredentialPubkey = "pubkey..."
        environmentVariables.set(
            "ETH-REGISTRATION_REGISTRATIONCREDENTIAL_PUBKEY",
            registrationCredentialPubkey
        )
        val registrationCredentialPrivkey = "privkey..."
        environmentVariables.set(
            "ETH-REGISTRATION_REGISTRATIONCREDENTIAL_PRIVKEY",
            registrationCredentialPrivkey
        )
        val irohaHostname = "iroha.host"
        environmentVariables.set("ETH-REGISTRATION_IROHA_HOSTNAME", irohaHostname)
        val irohaPort = "4040"
        environmentVariables.set("ETH-REGISTRATION_IROHA_PORT", irohaPort)

        // load configs
        val registrationConfig =
            loadLocalConfigs(
                "eth-registration",
                EthRegistrationConfig::class.java,
                "registration.properties"
            ).get()

        assertEquals(port.toInt(), registrationConfig.port)
        assertEquals(relayRegistrationIrohaAccount, registrationConfig.relayRegistrationIrohaAccount)
        assertEquals(relayStorageAccount, registrationConfig.relayStorageAccount)
        assertEquals(registrationCredentialAccountId, registrationConfig.registrationCredential.accountId)
        assertEquals(registrationCredentialPubkey, registrationConfig.registrationCredential.pubkey)
        assertEquals(registrationCredentialPrivkey, registrationConfig.registrationCredential.privkey)
        assertEquals(irohaHostname, registrationConfig.iroha.hostname)
        assertEquals(irohaPort.toInt(), registrationConfig.iroha.port)
    }
}
