/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.withdrawalservice

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig
import integration.eth.config.EthereumConfig

/** Configuration of withdrawal service */
interface WithdrawalServiceConfig {
    /** Withdrawal endpoint port */
    val port: Int

    /** Notary account in Iroha */
    val relayStorageAccount: String

    /** Iroha account that stores Ethereum anchored ERC20 tokens */
    val ethAnchoredTokenStorageAccount: String

    /** Iroha account that sets Ethereum anchored ERC20 tokens */
    val ethAnchoredTokenSetterAccount: String

    /** Iroha account that stores Iroha anchored ERC20 tokens */
    val irohaAnchoredTokenStorageAccount: String

    /** Iroha account that sets Iroha anchored ERC20 tokens */
    val irohaAnchoredTokenSetterAccount: String

    /** Notary storage account in Iroha */
    val notaryListStorageAccount: String

    /** Account who sets account details */
    val notaryListSetterAccount: String

    /** Notary account in Iroha */
    val registrationIrohaAccount: String

    /** Account that trigger expansion */
    val expansionTriggerAccount: String

    /** Creator of expansion trigger transaction */
    val expansionTriggerCreatorAccountId: String

    /** Account id of withdrawal billing */
    val withdrawalBillingAccount: String

    /** Master contact Ethereum address */
    val ethMasterAddress: String

    val withdrawalCredential: IrohaCredentialRawConfig

    /** Iroha configuration */
    val iroha: IrohaConfig

    /** Ethereum config */
    val ethereum: EthereumConfig

    /** RMQ Iroha Block */
    val ethIrohaWithdrawalQueue: String
}
