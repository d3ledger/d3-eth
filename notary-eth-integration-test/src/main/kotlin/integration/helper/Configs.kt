/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig
import integration.eth.config.EthereumConfig

/**
 * Ethereum configurations
 */
interface TestEthereumConfig {
    val ethTestAccount: String
    val ethereum: EthereumConfig
    val iroha: IrohaConfig
    val testCredentialConfig: IrohaCredentialRawConfig
    val testQueue: String
    val clientStorageAccount: String
    val brvsAccount: String
}
