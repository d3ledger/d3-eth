/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.withdrawalservice

import com.d3.commons.model.D3ErrorException
import com.d3.commons.provider.NotaryPeerListProvider
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.eth.deposit.endpoint.BigIntegerMoshiAdapter
import com.d3.eth.deposit.endpoint.EthNotaryResponse
import com.d3.eth.deposit.endpoint.EthNotaryResponseMoshiAdapter
import com.d3.eth.deposit.endpoint.IrohaTransactionHashType
import com.d3.eth.provider.EthTokensProvider
import com.d3.eth.sidechain.util.extractVRS
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import com.squareup.moshi.Moshi
import mu.KLogging
import java.math.BigDecimal
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
 * Approval to be passed to the Ethereum for refund
 * @param tokenContractAddress Ethereum address of ERC-20 token (or 0x0000000000000000000000000000000000000000 for ether)
 * @param amount amount of token/ether to transfer
 * @param account target account
 * @param irohaHash hash of approving TransferAsset transaction in Iroha
 * @param r array of r-components of notary signatures
 * @param s array of s-components of notary signatures
 * @param v array of v-components of notary signatures
 * @param relay Ethereum address of user relay contract
 */
data class RollbackApproval(
    val tokenContractAddress: String,
    val amount: String,
    val account: String,
    val irohaHash: String,
    val r: ArrayList<ByteArray>,
    val s: ArrayList<ByteArray>,
    val v: ArrayList<BigInteger>,
    val relay: String
)

/**
 * Collect proofs of notaries for ethereum contracts.
 */
class ProofCollector(
    private val queryHelper: IrohaQueryHelper,
    private val withdrawalServiceConfig: WithdrawalServiceConfig,
    private val tokensProvider: EthTokensProvider,
    private val notaryPeerListProvider: NotaryPeerListProvider
) {
    private val relayStorageAccount = withdrawalServiceConfig.relayStorageAccount

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
                val response = ethNotaryAdapter.fromJson(res.jsonObject.toString())

                when (response) {
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
                    failedOperation = WITHDRAWAL_OPERATION,
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

    fun collectProofForWithdrawal(event: SideChainEvent.IrohaEvent.SideChainTransfer): Result<RollbackApproval, Exception> {
        // description field holds target account address
        return tokensProvider.getTokenAddress(event.asset)
            .fanout { tokensProvider.getTokenPrecision(event.asset) }
            .fanout { findInAccDetail(relayStorageAccount, event.srcAccount) }
            .map { (tokenInfo, relayAddress) ->
                val hash = event.hash
                val amount = event.amount
                if (!event.asset.contains("#ethereum") && !event.asset.contains("#sora")) {
                    throw D3ErrorException.warning(
                        failedOperation = WITHDRAWAL_OPERATION,
                        description = "Incorrect asset name in Iroha event: " + event.asset
                    )
                }

                val address = event.description
                val vv = ArrayList<BigInteger>()
                val rr = ArrayList<ByteArray>()
                val ss = ArrayList<ByteArray>()

                notaryPeerListProvider.getPeerList().forEach { peer ->
                    logger.info { "Query $peer for proof for hash $hash" }
                    val res: khttp.responses.Response
                    try {
                        res = khttp.get("$peer/eth/$hash")
                    } catch (e: Exception) {
                        logger.warn("Exception was thrown while refund server request: server $peer", e)
                        return@forEach
                    }
                    if (res.statusCode != 200) {
                        logger.warn { "Error happened while refund server request: server $peer, error ${res.statusCode}" }
                        return@forEach
                    }

                    val moshi = Moshi
                        .Builder()
                        .add(EthNotaryResponseMoshiAdapter())
                        .add(BigInteger::class.java, BigIntegerMoshiAdapter())
                        .build()!!
                    val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
                    val response = ethNotaryAdapter.fromJson(res.jsonObject.toString())

                    when (response) {
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
                    throw D3ErrorException.warning(
                        failedOperation = WITHDRAWAL_OPERATION,
                        description = "Not a single valid response was received from any refund server"
                    )
                }

                val (coinAddress, precision) = tokenInfo
                val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(precision).toPlainString()
                RollbackApproval(
                    coinAddress,
                    decimalAmount,
                    address,
                    hash,
                    rr,
                    ss,
                    vv,
                    relayAddress
                )
            }
    }

    private fun findInAccDetail(acc: String, name: String): Result<String, Exception> {
        return queryHelper.getAccountDetailsFirst(
            acc,
            withdrawalServiceConfig.registrationIrohaAccount
        ) { _, value -> value == name }.map { relay ->
            if (!relay.isPresent)
                throw D3ErrorException.fatal(
                    failedOperation = WITHDRAWAL_OPERATION,
                    description = "No relay address in account details $acc bind to $name"
                )
            else
                relay.get().key
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
