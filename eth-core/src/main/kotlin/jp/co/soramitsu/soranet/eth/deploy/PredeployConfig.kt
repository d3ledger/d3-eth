/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.deploy

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig
import jp.co.soramitsu.soranet.eth.config.EthereumConfig

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
