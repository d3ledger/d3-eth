/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.endpoints.config

import com.d3.commons.config.IrohaConfig

interface TestingEndpointConfig {
    /** Testing endpoints port */
    val port: Int

    /** Notary account in Iroha */
    val notaryIrohaAccount: String

    /** Iroha configuration */
    val iroha: IrohaConfig
}