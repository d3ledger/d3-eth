/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.helper

import integration.helper.IrohaAccountHelper
import jp.co.soramitsu.iroha.java.IrohaAPI

/**
 * Creates Iroha accounts for Ethereum subsystem
 */
class EthereumAccountHelper(irohaApi: IrohaAPI) : IrohaAccountHelper(irohaApi) {

    /** Withdrawal account */
    val withdrawalAccount = createTesterAccount("eth_wthdr_srv", listOf("withdrawal"))

    /** Account that stores Ethereum addresses */
    val ethAddressesStorage = createTesterAccount("eth_addr_storage")

    /** Account that sets Ethereum addresses */
    val ethAddressesWriter = createTesterAccount("eth_addr_writer", listOf("tester"))

    /** list of registered ethereum wallets */
    val ethereumWalletStorageAccount = createTesterAccount("ethereum_wallets")

}
