/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.abi

import com.google.common.io.Resources
import jp.co.soramitsu.iroha.java.Utils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

class AbiDecoderTest {

    private val abiFileName = "TestAbi.abi"

    @Test
    fun testDecoder() {
        val abiJson = Resources.toString(Resources.getResource(abiFileName), StandardCharsets.UTF_8)
        val testData =
            "0xeea29e3e0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000016345785d8a0000000000000000000000000000c3ce1c359669be9e8a9a5f2334e46589b39a5c649cda23c271071130dd45f7d26a3832e90da1a1c556432d39dce2ae09f23e8f46000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001c00000000000000000000000000000000000000000000000000000000000000280000000000000000000000000c3ce1c359669be9e8a9a5f2334e46589b39a5c640000000000000000000000000000000000000000000000000000000000000005000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001b0000000000000000000000000000000000000000000000000000000000000005307a419a8eaaf545fa4f4b38fa68873c34420c822a3ec42789dd1b74ed987153b0c61b652ba67b8f68306a05a0052a41783491bffe0e2845b94f237983cf3780a721bebd62cd7e8dcacd24a1737a3518e810ebdb1663b8338ff833c29571e0f038a40e08a483559126aef3404698b263d6c9772824cffb6bf233d31610ce7a98a18b80ce9bfb0205aa3d4926b42bfb3dfc5e43081b7be9f0ce6fd5470a63e0dd000000000000000000000000000000000000000000000000000000000000000516eb040ad5e932eb9fefdf46f2ed0b0b0ea2f46b481aca98dc528772b16b89dd49173d9bedff9bc40421691eeb4c2b4383c9bc9cfa35776bc7dd7dfad433847c6371551b31f19137a02217b58f4cbe0216c4218572f65b51c74eb5cd441f366247a6019e4b6d8dedc1afd1caa4fffe2b73ec3ddff97874955ba3231cbad80c7944d724ba2f3b4987be8d7443da127014bf34e1c81718e0cbdbf05961282d1554"
        val decoder = AbiDecoder()
        decoder.addAbi(abiJson)
        val decodedMethod = decoder.decodeMethod(testData)

        assertEquals("withdraw", decodedMethod.name)
        assertEquals(8, decodedMethod.params.size)
        assertEquals("tokenAddress", decodedMethod.params[0].name)
        assertEquals("address", decodedMethod.params[0].type)
        assertEquals(
            "0000000000000000000000000000000000000000",
            Utils.toHex(decodedMethod.params[0].value as ByteArray)
        )

        assertEquals("amount", decodedMethod.params[1].name)
        assertEquals("uint256", decodedMethod.params[1].type)
        assertEquals(BigInteger("100000000000000000"), decodedMethod.params[1].value)

        assertEquals("to", decodedMethod.params[2].name)
        assertEquals("address", decodedMethod.params[2].type)
        assertEquals(
            "C3CE1C359669BE9E8A9A5F2334E46589B39A5C64",
            Utils.toHex(decodedMethod.params[2].value as ByteArray)
        )

        assertEquals("txHash", decodedMethod.params[3].name)
        assertEquals("bytes32", decodedMethod.params[3].type)
        assertEquals(
            "9CDA23C271071130DD45F7D26A3832E90DA1A1C556432D39DCE2AE09F23E8F46",
            Utils.toHex(decodedMethod.params[3].value as ByteArray)
        )

        assertEquals("v", decodedMethod.params[4].name)
        assertEquals("uint8[]", decodedMethod.params[4].type)
        val actualV = decodedMethod.params[4].value as Array<*>
        assertEquals(5, actualV.size)
        val big27 = BigInteger("27")
        assertEquals(BigInteger("28"), actualV[0])
        assertEquals(big27, actualV[1])
        assertEquals(big27, actualV[2])
        assertEquals(big27, actualV[3])
        assertEquals(big27, actualV[4])

        assertEquals("r", decodedMethod.params[5].name)
        assertEquals("bytes32[]", decodedMethod.params[5].type)
        val actualR = decodedMethod.params[5].value as Array<*>
        assertEquals(5, actualR.size)
        assertEquals(
            "307A419A8EAAF545FA4F4B38FA68873C34420C822A3EC42789DD1B74ED987153",
            Utils.toHex(actualR[0] as ByteArray)
        )
        assertEquals(
            "B0C61B652BA67B8F68306A05A0052A41783491BFFE0E2845B94F237983CF3780",
            Utils.toHex(actualR[1] as ByteArray)
        )
        assertEquals(
            "A721BEBD62CD7E8DCACD24A1737A3518E810EBDB1663B8338FF833C29571E0F0",
            Utils.toHex(actualR[2] as ByteArray)
        )
        assertEquals(
            "38A40E08A483559126AEF3404698B263D6C9772824CFFB6BF233D31610CE7A98",
            Utils.toHex(actualR[3] as ByteArray)
        )
        assertEquals(
            "A18B80CE9BFB0205AA3D4926B42BFB3DFC5E43081B7BE9F0CE6FD5470A63E0DD",
            Utils.toHex(actualR[4] as ByteArray)
        )

        assertEquals("s", decodedMethod.params[6].name)
        assertEquals("bytes32[]", decodedMethod.params[6].type)
        val actualS = decodedMethod.params[6].value as Array<*>
        assertEquals(5, actualS.size)
        assertEquals(
            "16EB040AD5E932EB9FEFDF46F2ED0B0B0EA2F46B481ACA98DC528772B16B89DD",
            Utils.toHex(actualS[0] as ByteArray)
        )
        assertEquals(
            "49173D9BEDFF9BC40421691EEB4C2B4383C9BC9CFA35776BC7DD7DFAD433847C",
            Utils.toHex(actualS[1] as ByteArray)
        )
        assertEquals(
            "6371551B31F19137A02217B58F4CBE0216C4218572F65B51C74EB5CD441F3662",
            Utils.toHex(actualS[2] as ByteArray)
        )
        assertEquals(
            "47A6019E4B6D8DEDC1AFD1CAA4FFFE2B73EC3DDFF97874955BA3231CBAD80C79",
            Utils.toHex(actualS[3] as ByteArray)
        )
        assertEquals(
            "44D724BA2F3B4987BE8D7443DA127014BF34E1C81718E0CBDBF05961282D1554",
            Utils.toHex(actualS[4] as ByteArray)
        )

        assertEquals("from", decodedMethod.params[7].name)
        assertEquals("address", decodedMethod.params[7].type)
        assertEquals(
            "C3CE1C359669BE9E8A9A5F2334E46589B39A5C64",
            Utils.toHex(decodedMethod.params[7].value as ByteArray)
        )
    }

    @Test
    fun testDecoderFail() {
        assertThrows<IllegalStateException> {
            val testData =
                "0xeea29e3e0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000016345785d8a0000000000000000000000000000c3ce1c359669be9e8a9a5f2334e46589b39a5c649cda23c271071130dd45f7d26a3832e90da1a1c556432d39dce2ae09f23e8f46000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001c00000000000000000000000000000000000000000000000000000000000000280000000000000000000000000c3ce1c359669be9e8a9a5f2334e46589b39a5c640000000000000000000000000000000000000000000000000000000000000005000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001b0000000000000000000000000000000000000000000000000000000000000005307a419a8eaaf545fa4f4b38fa68873c34420c822a3ec42789dd1b74ed987153b0c61b652ba67b8f68306a05a0052a41783491bffe0e2845b94f237983cf3780a721bebd62cd7e8dcacd24a1737a3518e810ebdb1663b8338ff833c29571e0f038a40e08a483559126aef3404698b263d6c9772824cffb6bf233d31610ce7a98a18b80ce9bfb0205aa3d4926b42bfb3dfc5e43081b7be9f0ce6fd5470a63e0dd000000000000000000000000000000000000000000000000000000000000000516eb040ad5e932eb9fefdf46f2ed0b0b0ea2f46b481aca98dc528772b16b89dd49173d9bedff9bc40421691eeb4c2b4383c9bc9cfa35776bc7dd7dfad433847c6371551b31f19137a02217b58f4cbe0216c4218572f65b51c74eb5cd441f366247a6019e4b6d8dedc1afd1caa4fffe2b73ec3ddff97874955ba3231cbad80c7944d724ba2f3b4987be8d7443da127014bf34e1c81718e0cbdbf05961282d1554"
            AbiDecoder().decodeMethod(testData)
        }
    }
}
