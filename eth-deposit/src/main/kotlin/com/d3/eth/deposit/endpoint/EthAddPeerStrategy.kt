/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit.endpoint

import com.d3.commons.expansion.ExpansionDetails
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.util.irohaUnEscape
import com.d3.eth.sidechain.util.hashToAddAndRemovePeer
import com.d3.eth.sidechain.util.signUserData
import com.github.kittinunf.result.map
import com.google.gson.Gson
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
    private val gson = Gson()

    /**
     * Check and give proof
     */
    override fun performAddPeer(irohaTxHash: IrohaTransactionHashType): EthNotaryResponse {
        val irohaTx = queryHelper.getSingleTransaction(irohaTxHash)

        return irohaTx.map { tx ->
            if (tx.payload.reducedPayload.creatorAccountId != expansionTriggerCreatorAccountId)
                throw IllegalArgumentException("Wrong iroha tx creator account id ${tx.payload.reducedPayload.creatorAccountId}, expected ${expansionTriggerCreatorAccountId}")
            val setAccountDetail = tx.payload.reducedPayload.getCommands(0).setAccountDetail
            if (setAccountDetail.accountId != expansionTriggerAccountId)
                throw IllegalArgumentException("Wrong iroha tx account id ${setAccountDetail.accountId}, expected ${expansionTriggerAccountId}")

            val expansionDetails = gson.fromJson(
                setAccountDetail.value.irohaUnEscape(),
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
