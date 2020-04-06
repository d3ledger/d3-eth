/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.abi

import com.d3.eth.helper.hexStringToByteArray
import com.google.gson.*
import org.web3j.utils.Numeric.toHexString
import java.lang.reflect.Type

internal object AbiGsonHelper {
    val customGson: Gson by lazy {
        GsonBuilder().registerTypeHierarchyAdapter(
            ByteArray::class.java,
            ByteArrayToEthHexTypeAdapter()
        ).create()
    }

    private class ByteArrayToEthHexTypeAdapter : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): ByteArray {
            return hexStringToByteArray(json.asString)
        }

        override fun serialize(
            src: ByteArray,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonPrimitive(toHexString(src))
        }
    }

    const val ETH_PREFIX = "0x"
}
