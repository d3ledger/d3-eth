/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.abi

import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.soranet.eth.abi.AbiGsonHelper.ETH_PREFIX
import jp.co.soramitsu.soranet.eth.abi.AbiGsonHelper.customGson
import org.ethereum.solidity.Abi
import java.util.*
import javax.xml.bind.DatatypeConverter

data class AbiDecoder(val methodIDs: HashMap<String, Abi.Entry> = HashMap()) {
    private val savedAbis = mutableListOf<Abi.Entry>()

    data class DecodedMethod(
        val name: String,
        val params: List<Param>
    ) {
        override fun toString(): String {
            return customGson.toJson(this)
        }
    }

    data class Param(
        val name: String,
        val type: String,
        val value: Any
    ) {
        override fun toString(): String {
            return customGson.toJson(this)
        }
    }

    fun addAbi(json: String) {
        val abi = Abi.fromJson(json)
        for (entry in abi) {
            if (entry == null) {
                continue
            }
            if (entry.name != null) {
                val methodSignature = entry.encodeSignature()
                methodIDs[Utils.toHex(methodSignature)] = entry
            }
            savedAbis.add(entry)
        }
    }

    fun removeAbi(json: String) {
        val abi = Abi.fromJson(json)
        for (entry in abi) {
            if (entry == null) {
                continue
            }
            if (entry.name != null) {
                val methodSignature = entry.encodeSignature()
                methodIDs.remove(Utils.toHex(methodSignature))
            }
            savedAbis.remove(entry)
        }
    }

    fun decodeMethod(data: String): DecodedMethod {
        val noPrefix = data.removePrefix(ETH_PREFIX)
        val bytes = DatatypeConverter.parseHexBinary(noPrefix.toUpperCase())
        val methodBytes = bytes.sliceArray(0..3)
        val entry = methodIDs[Utils.toHex(methodBytes)]
        if (entry is Abi.Function) {
            val decoded = entry.decode(bytes)
            val params = mutableListOf<Param>()
            for (i in decoded.indices) {
                val name = entry.inputs[i].name
                val type = entry.inputs[i].type.toString()
                val value = decoded[i]!!
                val param = Param(name, type, value)
                params.add(param)
            }
            return DecodedMethod(entry.name, params)
        }
        throw IllegalStateException("Input data is not processable by the AbiDecoder instance. Add the corresponding Abi first");
    }
}
