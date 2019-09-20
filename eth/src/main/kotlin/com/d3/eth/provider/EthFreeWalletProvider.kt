/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.github.kittinunf.result.Result
import mu.KLogging
import org.web3j.crypto.WalletUtils
import java.io.File
import java.io.FileNotFoundException
import java.util.*

/**
 * Generates new wallets with given password
 * @param password - ethereum wallet file secret password
 * @param fileStorage - storage of wallet files
 */
class EthFreeWalletProvider(private val password: String, private val fileStorage: String) :
    EthFreeClientAddressProvider {

    /**
     * Generate wallet file and return generated Ethereum address
     */
    override fun getAddress(): Result<String, Exception> = Result.of {
        // create secret file
        val filename = WalletUtils.generateNewWalletFile(
            password,
            File(fileStorage)
        )

        // rename to <UUID>.json where <UUID> is generated from address
        val generatedWalletFile = File(filename)
        val credentials = WalletUtils.loadCredentials(
            password,
            generatedWalletFile
        )
        val ethereumAddress = credentials.address
        val uuid = UUID.nameUUIDFromBytes(ethereumAddress.toByteArray())
        val uuidFileName = File(fileStorage, "$uuid.json")
        generatedWalletFile.renameTo(uuidFileName)

        logger.info { "New Ethereum address $ethereumAddress is generated and saved to ${uuidFileName.canonicalPath}" }

        ethereumAddress
    }

    /** Free addresses amount is quite large */
    override fun getAddressCount(): Result<Int, Exception> = Result.of { Int.MAX_VALUE }

    /**
     * Get wallet file by Ethereum address
     */
    fun getWalletByAddress(ethereumAddress: String): Result<File, java.lang.Exception> = Result.of {
        val uuid = UUID.nameUUIDFromBytes(ethereumAddress.toByteArray())
        val walletFile = File(fileStorage, "$uuid.json")
        if (!walletFile.exists())
            throw FileNotFoundException("Wallet ${walletFile.canonicalPath} for address $ethereumAddress not found")
        walletFile
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
