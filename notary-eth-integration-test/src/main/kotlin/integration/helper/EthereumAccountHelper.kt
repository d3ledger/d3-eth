/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import jp.co.soramitsu.iroha.java.IrohaAPI

class EthereumAccountHelper(irohaApi: IrohaAPI) : IrohaAccountHelper(irohaApi) {

    val withdrawalAccount = createTesterAccount("eth_wthdr_srv", "withdrawal")

    val ethWithdrawalBillingAccount = createTesterAccount("eth_wthdr_bln", "billing")
}
