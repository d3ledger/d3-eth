/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import jp.co.soramitsu.iroha.java.IrohaAPI

/**
 * Creates Iroha accounts for Ethereum subsystem
 */
class EthereumAccountHelper(irohaApi: IrohaAPI) : IrohaAccountHelper(irohaApi) {

    /** Withdrawal account */
    val withdrawalAccount = createTesterAccount("eth_wthdr_srv", "withdrawal")

    /** Withdrawal billing account to send fee to */
    val ethWithdrawalBillingAccount = createTesterAccount("eth_wthdr_bln", "billing")

    /** Account that stores Ethereum addresses */
    val ethAddressesStorage = createTesterAccount("eth_addr_storage")

    /** Account that sets Ethereum addresses */
    val ethAddressesWriter = createTesterAccount("eth_addr_writer", "tester")
}