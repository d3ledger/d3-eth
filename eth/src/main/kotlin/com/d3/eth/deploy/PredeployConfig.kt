/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deploy

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig
import integration.eth.config.EthereumConfig

/** Configs for Predeploy of Ethereum contracts */
interface PredeployConfig {

    /** Ethereum configs */
    val ethereum: EthereumConfig

    /** Account to store contract addresses */
    val ethContractAddressStorageAccountId: String

    /** Iroha credentials */
    val irohaCredential: IrohaCredentialRawConfig

    /** Iroha config */
    val iroha: IrohaConfig
}
