/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge.endpoint

import com.d3.commons.expansion.ExpansionDetails
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.irohaUnEscape
import com.github.kittinunf.result.map
import jp.co.soramitsu.soranet.eth.sidechain.util.hashToAddAndRemovePeer
import jp.co.soramitsu.soranet.eth.sidechain.util.signUserData
import mu.KLogging
import org.web3j.crypto.ECKeyPair

interface EthAddPeerStrategy {
    fun performAddPeer(irohaTxHash: IrohaTransactionHashType): EthNotaryResponse
}

/**
 * Strategy to check add peer Iroha transaction and give Ethereum signed proof
 */
class EthAddPeerStrategyImpl(
    private val queryHelper: IrohaQueryHelper,
    private val ecKeyPair: ECKeyPair,
    private val expansionTriggerAccountId: String,
    private val expansionTriggerCreatorAccountId: String
) : EthAddPeerStrategy {
    private val gson = GsonInstance.get()

    /**
     * Check and give proof
     */
    override fun performAddPeer(irohaTxHash: IrohaTransactionHashType): EthNotaryResponse {
        val irohaTx = queryHelper.getSingleTransaction(irohaTxHash)

        return irohaTx.map { tx ->
            if (tx.payload.reducedPayload.creatorAccountId != expansionTriggerCreatorAccountId)
                throw IllegalArgumentException("Wrong iroha tx creator account id ${tx.payload.reducedPayload.creatorAccountId}, expected ${expansionTriggerCreatorAccountId}")

            val validatedTxs = tx.payload.reducedPayload.commandsList
                .filter { it.hasSetAccountDetail() }
                .map { it.setAccountDetail }
                .filter { it.accountId == expansionTriggerAccountId }

            if (validatedTxs.count() == 0)
                throw IllegalArgumentException("Expansion command not found in transaction $irohaTxHash")

            val expansionDetails = gson.fromJson(
                validatedTxs.first().value.irohaUnEscape(),
                ExpansionDetails::class.java
            )

            val finalHash = hashToAddAndRemovePeer(
                expansionDetails.additionalData["eth_address"]!!,
                irohaTxHash
            )
            val signature = signUserData(ecKeyPair, finalHash)
            EthNotaryResponse.Successful(signature)
        }.fold(
            { it },
            { ex -> throw ex }
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
