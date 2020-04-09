/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge.endpoint

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.math.BigInteger

/**
 * Type of transaction hash in Iroha
 */
typealias IrohaTransactionHashType = String


/**
 * Adapter of [BigInteger] class for moshi
 */
class BigIntegerMoshiAdapter : JsonAdapter<BigInteger>() {

    override fun fromJson(reader: JsonReader?): BigInteger? {
        return BigInteger(reader?.nextString(), 10)
    }

    override fun toJson(writer: JsonWriter?, value: BigInteger?) {
        writer?.value(value?.toString(10))
    }
}
