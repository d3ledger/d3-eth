/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.abi

import com.google.gson.*
import jp.co.soramitsu.soranet.eth.helper.hexStringToByteArray
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
