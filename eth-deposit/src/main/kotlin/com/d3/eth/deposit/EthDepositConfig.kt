/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig
import integration.eth.config.EthereumConfig
import java.math.BigInteger

/** Configuration of refund endpoint in Notary */
interface RefundConfig {
    val port: Int
}

/** Configuration of deposit */
interface EthDepositConfig {
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

    /** multisig notary credentials */
    val notaryCredential: IrohaCredentialRawConfig

    /** non-multisig withdrawal proof creator */
    val withdrawalCredential: IrohaCredentialRawConfig

    val refund: RefundConfig

    val iroha: IrohaConfig

    /** Path to storage of last read Ethereum block number */
    val lastEthereumReadBlockFilePath: String

    /** Ethereum block number to start listen for */
    val startEthereumBlock: BigInteger

    /** Property indicating if the service should care about last block on start */
    val ignoreStartBlock: Boolean

    val ethereum: EthereumConfig

    /** Iroha withdrawal account grant permission to */
    val withdrawalAccountId: String

    /** Account that trigger expansion */
    val expansionTriggerAccount: String

    /** Creator of expansion trigger transaction */
    val expansionTriggerCreatorAccountId: String

    val ethIrohaDepositQueue: String

    /** Address of master smart contract in Ethereum */
    val ethMasterAddress: String

    /** Ethereum wallet list storage account id */
    val ethereumWalletStorageAccount: String

    /** Ethereum wallet list setter account id */
    val ethereumWalletSetterAccount: String

    /** Ethereum relay list storage account id */
    val ethereumRelayStorageAccount: String

    /** Ethereum relay list setter account id */
    val ethereumRelaySetterAccount: String
}
