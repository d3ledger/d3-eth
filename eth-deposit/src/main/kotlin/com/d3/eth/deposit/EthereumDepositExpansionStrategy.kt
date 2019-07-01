/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit

import com.d3.commons.expansion.ExpansionDetails
import com.d3.commons.expansion.ServiceExpansion
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.MultiSigIrohaConsumer
import com.d3.commons.util.unHex
import com.github.kittinunf.result.Result
import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import mu.KLogging

/**
 * Class responsible for ethereum expansion
 */
class EthereumDepositExpansionStrategy(
    private val notaryCredential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val ethDepositConfig: EthDepositConfig
) {

    init {
        logger.info {
            "Init deposit expansion strategy, " +
                    "expansionTriggerAccount=${ethDepositConfig.expansionTriggerAccount}, " +
                    "expansionTriggerCreatorAccountId=${ethDepositConfig.expansionTriggerCreatorAccountId}"
        }
    }

    private val expansionService = ServiceExpansion(
        ethDepositConfig.expansionTriggerAccount,
        ethDepositConfig.expansionTriggerCreatorAccountId,
        irohaAPI
    )

    /**
     * Filter expansion trigger event and call expansion logic
     * @param block - iroha block
     */
    fun filterAndExpand(block: BlockOuterClass.Block) {
        expansionService.expand(block) { expansionDetails, _, triggerTime ->
            if (expansionDetails.accountIdToExpand == notaryCredential.accountId) {
                logger.info {
                    "Add new peer with iroha pubkey ${expansionDetails.publicKey} " +
                            "endpoint ${expansionDetails.additionalData["notary_endpoint"]}"
                }
                addPeer(expansionDetails, triggerTime)
            } else {
                logger.info {
                    "Expansion account ${expansionDetails.accountIdToExpand} is " +
                            "different from ${notaryCredential.accountId}"
                }
            }
        }
    }

    /**
     * Send expansion transaction to Iroha.
     * - add signature to notary account
     * - add new notary endpoint to notaries list
     * @param expansionDetails - details with expansion data
     * @param triggerTime - time of transaction trigger for Iroha multisig
     */
    private fun addPeer(
        expansionDetails: ExpansionDetails,
        triggerTime: Long
    ): Result<String, Exception> {
        val consumer = MultiSigIrohaConsumer(notaryCredential, irohaAPI)
        val notaryName = expansionDetails.additionalData["notary_name"]
        val notaryEndpoint = expansionDetails.additionalData["notary_endpoint"]
        return consumer.send(
            Transaction.builder(notaryCredential.accountId, triggerTime)
                .addSignatory(
                    notaryCredential.accountId,
                    String.unHex(expansionDetails.publicKey.toLowerCase())
                )
                .setAccountQuorum(notaryCredential.accountId, expansionDetails.quorum)
                .setQuorum(consumer.getConsumerQuorum().get())
                .setAccountDetail(
                    ethDepositConfig.notaryListStorageAccount,
                    notaryName,
                    notaryEndpoint
                )
                .build()
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
