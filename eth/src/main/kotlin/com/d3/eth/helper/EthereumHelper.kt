/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.helper

import com.d3.commons.util.unHex
import com.github.kittinunf.result.Result
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.WalletUtils
import org.web3j.utils.Numeric
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.util.*

/**
 * Encode solidity function call.
 * @param functionName - function to call
 * @param params - parameters of [Type]
 * @return encoded byte array to be
 */
fun encodeFunction(functionName: String, vararg params: Type<Any>): ByteArray {
    val function =
        org.web3j.abi.datatypes.Function(functionName, params.asList(), Collections.emptyList())
    return String.unHex(org.web3j.abi.FunctionEncoder.encode(function).drop(2))
}

/**
 * Get wallet file by Ethereum address
 * @param dir - directory where wallet file is stored
 * @param ethereumAddress - ethereum address to search wallet
 */
fun getWalletByAddress(dir: String, ethereumAddress: String): Result<File, Exception> = Result.of {
    val uuid = UUID.nameUUIDFromBytes(ethereumAddress.toByteArray())
    val walletFile = File(dir, "$uuid.json")
    if (!walletFile.exists())
        throw FileNotFoundException("Wallet ${walletFile.canonicalPath} for address $ethereumAddress not found")
    walletFile
}

/**
 * Convert hex string to byte array
 */
fun hexStringToByteArray(irohaHash: String) = Numeric.hexStringToByteArray(irohaHash)

/**
 * Generate Ethereum wallet and save to encrypted file
 * @param password - password for encryption
 * @return generated ethereum address
 */
fun generateWalletFile(password: String): String {
    val fileStorage = "."

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

    return ethereumAddress
}
