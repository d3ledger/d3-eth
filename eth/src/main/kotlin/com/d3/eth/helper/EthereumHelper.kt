/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.helper

import com.d3.commons.util.unHex
import org.web3j.abi.datatypes.Type
import org.web3j.utils.Numeric
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
 * Convert hex string to byte array
 */
fun hexStringToByteArray(irohaHash: String) = Numeric.hexStringToByteArray(irohaHash)
