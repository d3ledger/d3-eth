/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.token

/** Information about token - token [name], [domain] and [precision] */
data class EthTokenInfo(val name: String, val domain: String, val precision: Int)
