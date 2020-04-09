/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.registration

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig

/**
 * Interface represents configs for registration service for cfg4k
 */
interface EthRegistrationConfig {
    /** Port of registration service */
    val port: Int

    /** Iroha account of relay registration service */
    val relayRegistrationIrohaAccount: String

    /** Iroha account of relay account register */
    val relayStorageAccount: String

    val registrationCredential: IrohaCredentialRawConfig

    /** Iroha configuration */
    val iroha: IrohaConfig
}
