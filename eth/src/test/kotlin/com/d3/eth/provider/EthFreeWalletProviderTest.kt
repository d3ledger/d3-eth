/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.d3.eth.helper.getWalletByAddress
import integration.eth.config.loadEthPasswords
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.web3j.crypto.WalletUtils
import java.io.FileNotFoundException
import kotlin.test.assertEquals

class EthFreeWalletProviderTest {

    private val ethereumPasswords =
        loadEthPasswords("eth-deposit", "/eth/ethereum_password.properties").get()

    val password = ethereumPasswords.credentialsPassword
    val fileStorage = "."

    val ethWalletProvider = EthFreeWalletProvider(password, fileStorage)

    /**
     * @given Ethereum wallet secret password
     * @when getAddress() called
     * @then new wallet with Ethereum address is generated
     */
    @Test
    fun generateWalletFile() {
        val address = ethWalletProvider.getAddress().get()

        val walletFile = getWalletByAddress(ethWalletProvider.fileStorage, address).get()
        val credentials = WalletUtils.loadCredentials(password, walletFile)
        assertEquals(address, credentials.address)

        walletFile.delete()
    }

    /**
     * @given EthFreeWalletProvider is created with wrong storage
     * @when getAddress() called
     * @then Exception is thrown
     */
    @Test
    fun generateWalletFileInvalidStorage() {
        val invalidEthWalletProvider = EthFreeWalletProvider(password, "wrong_storage_name")
        invalidEthWalletProvider.getAddress()
            .fold(
                { _ -> fail { "Exception is expected" } },
                { ex -> assert(ex is FileNotFoundException) }
            )
    }

    /**
     * @given EthWalletProvider
     * @when getWalletByAddress() is called with wrong address
     * @then exception is thrown
     */
    @Test
    fun loadInvalidWalletFile() {
        getWalletByAddress(ethWalletProvider.fileStorage, "wrong_address").fold(
            { _ -> fail { "Exception is expected" } },
            { ex -> assert(ex is FileNotFoundException) }
        )
    }
}
