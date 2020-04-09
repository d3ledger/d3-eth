/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge

import com.d3.commons.model.D3ErrorException
import com.d3.commons.provider.NotaryPeerListProvider
import com.github.kittinunf.result.Result
import com.squareup.moshi.Moshi
import jp.co.soramitsu.soranet.eth.bridge.endpoint.EthNotaryResponse
import jp.co.soramitsu.soranet.eth.bridge.endpoint.EthNotaryResponseMoshiAdapter
import jp.co.soramitsu.soranet.eth.bridge.endpoint.IrohaTransactionHashType
import jp.co.soramitsu.soranet.eth.sidechain.util.extractVRS
import mu.KLogging
import java.math.BigInteger

/**
 * Approval for adding of a new peer
 */
data class AddPeerProof(
    val peerEthereumAddress: String,
    val irohaHash: String,
    val r: ArrayList<ByteArray>,
    val s: ArrayList<ByteArray>,
    val v: ArrayList<BigInteger>
)

/**
 * Collect proofs of notaries for ethereum contracts.
 */
class ProofCollector(
    private val notaryPeerListProvider: NotaryPeerListProvider
) {

    /**
     * Gather proof from notaries for add peer
     * @param peerEthereumAddress - new peer EthereumAddress
     * @param irohaTxHash - iroha hash of trigger tx
     */
    fun collectProofForAddPeer(
        peerEthereumAddress: String,
        irohaTxHash: IrohaTransactionHashType
    ): Result<AddPeerProof, Exception> {
        return Result.of {
            val vv = ArrayList<BigInteger>()
            val rr = ArrayList<ByteArray>()
            val ss = ArrayList<ByteArray>()

            notaryPeerListProvider.getPeerList().forEach { peer ->
                logger.info { "Query $peer for add peer proof" }

                val res: khttp.responses.Response
                try {
                    res = khttp.get("$peer/ethereum/proof/add_peer/$irohaTxHash")
                } catch (e: Exception) {
                    logger.warn { "Exception was thrown while refund server request: server $peer" }
                    logger.warn { e.localizedMessage }
                    return@forEach
                }
                if (res.statusCode != 200) {
                    logger.warn { "Error happened while refund server request: server $peer, error ${res.statusCode}" }
                    return@forEach
                }

                val moshi = Moshi.Builder().add(EthNotaryResponseMoshiAdapter()).build()!!
                val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!

                when (val response = ethNotaryAdapter.fromJson(res.jsonObject.toString())) {
                    is EthNotaryResponse.Error -> {
                        logger.warn { "EthNotaryResponse.Error: ${response.reason}" }
                        return@forEach
                    }

                    is EthNotaryResponse.Successful -> {
                        val signature = response.ethSignature
                        val vrs = extractVRS(signature)
                        vv.add(vrs.v)
                        rr.add(vrs.r)
                        ss.add(vrs.s)
                    }
                }
            }

            if (vv.size == 0) {
                throw D3ErrorException.fatal(
                    failedOperation = "Add peer",
                    description = "Not a single valid response was received from any refund server"
                )
            }

            AddPeerProof(
                peerEthereumAddress,
                irohaTxHash,
                rr,
                ss,
                vv
            )
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
