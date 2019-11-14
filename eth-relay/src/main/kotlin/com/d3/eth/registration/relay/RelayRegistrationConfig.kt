/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration.relay

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig
import integration.eth.config.EthereumConfig

/**
 * Interface represents configs for relay registration service for cfg4k
 */
interface RelayRegistrationConfig {

    /** Number of accounts to deploy */
    val number: Int

    /** How often run registration of new relays in seconds */
    val replenishmentPeriod: Long

    /** Address of master smart contract in Ethereum */
    val ethMasterAddress: String

    /** Address of implementation of Relay contract in Ethereum */
    val ethRelayImplementationAddress: String

    /** Notary Iroha account that stores relay register */
    val relayStorageAccount: String

    val relayRegistrationCredential: IrohaCredentialRawConfig

    /** Iroha configurations */
    val iroha: IrohaConfig

    /** Ethereum configurations */
    val ethereum: EthereumConfig
}
