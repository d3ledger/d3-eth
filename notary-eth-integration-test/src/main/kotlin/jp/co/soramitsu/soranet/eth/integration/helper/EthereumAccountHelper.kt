/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.helper

import com.d3.commons.model.IrohaCredential
import integration.helper.IrohaAccountHelper
import jp.co.soramitsu.iroha.java.IrohaAPI

/**
 * Creates Iroha accounts for Ethereum subsystem
 */
class EthereumAccountHelper(irohaAPI: IrohaAPI) : IrohaAccountHelper(irohaAPI) {

    /** Withdrawal account */
    val withdrawalAccount by lazy { createTesterAccount("eth_wthdr_srv", listOf("withdrawal")) }

    /** Account that stores Ethereum addresses */
    val ethAddressesStorage by lazy { createTesterAccount("eth_addr_storage") }

    /** Account that sets Ethereum addresses */
    val ethAddressesWriter by lazy { createTesterAccount("eth_addr_writer", listOf("tester")) }

    /** list of registered ethereum wallets */
    val ethereumWalletStorageAccount by lazy { createTesterAccount("ethereum_wallets") }

    /** XOR limits storage account **/
    val xorLimitsStorageAccount by lazy {
        IrohaCredential(
            "xor_limits@notary",
            testCredential.keyPair
        )
    }

    override val registrationAccount by lazy {
        IrohaCredential(
            "registration_service@d3",
            testCredential.keyPair
        )
    }
}
