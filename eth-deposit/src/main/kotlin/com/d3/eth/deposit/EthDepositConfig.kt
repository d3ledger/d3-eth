/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig

/** Configuration of refund endpoint in Notary */
interface RefundConfig {
    val port: Int
}

/** Configuration of deposit */
interface EthDepositConfig {
    /** Iroha account that has registered wallets */
    val registrationServiceIrohaAccount: String

    /** Account that stores list of notaries endpoints */
    val notaryListStorageAccount: String

    /** Iroha account that stores Ethereum anchored ERC20 tokens */
    val ethAnchoredTokenStorageAccount: String

    /** Iroha account that sets Ethereum anchored ERC20 tokens */
    val ethAnchoredTokenSetterAccount: String

    /** Iroha account that stores Iroha anchored ERC20 tokens */
    val irohaAnchoredTokenStorageAccount: String

    /** Iroha account that sets Iroha anchored ERC20 tokens */
    val irohaAnchoredTokenSetterAccount: String

    val notaryCredential: IrohaCredentialRawConfig

    val refund: RefundConfig

    val iroha: IrohaConfig

    val ethereum: EthereumConfig

    /** Iroha withdrawal account grant permission to */
    val withdrawalAccountId: String

    /** Account that trigger expansion */
    val expansionTriggerAccount: String

    /** Creator of expansion trigger transaction */
    val expansionTriggerCreatorAccountId: String

    val ethIrohaDepositQueue: String
}
