/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.helper

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig
import jp.co.soramitsu.soranet.eth.config.EthereumConfig

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
