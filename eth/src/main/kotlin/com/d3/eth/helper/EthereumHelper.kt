/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.helper

import com.d3.commons.util.unHex
import com.github.kittinunf.result.Result
import org.web3j.abi.datatypes.Type
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
