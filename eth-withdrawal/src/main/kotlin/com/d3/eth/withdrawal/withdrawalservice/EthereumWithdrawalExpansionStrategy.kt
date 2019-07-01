/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.withdrawalservice

import com.d3.commons.config.EthereumPasswords
import com.d3.commons.expansion.ServiceExpansion
import com.d3.eth.sidechain.util.DeployHelper
import iroha.protocol.BlockOuterClass
import org.web3j.utils.Numeric

/**
 * Withdrawal service expansion strategy
 */
class EthereumWithdrawalExpansionStrategy(
    private val withdrawalConfig: WithdrawalServiceConfig,
    private val withdrawalEthereumPasswords: EthereumPasswords,
    private val expansionService: ServiceExpansion,
    private val proofCollector: ProofCollector
) {
    /**
     * Filter block for expansion trigger event and perform expansion logic:
     * - query proofs for expansion from all notaries
     * - send expansion transaction to Ethereum
     * @param block - iroha block
     */
    fun filterAndExpand(block: BlockOuterClass.Block) {
        expansionService.expand(block) { expansionDetails, triggerTxHash, _ ->
            val ethereumPeerAddress = expansionDetails.additionalData["eth_address"]!!
            val addPeerProof = proofCollector.collectProofForAddPeer(
                ethereumPeerAddress,
                triggerTxHash
            ).get()

            val masterContract = DeployHelper(
                withdrawalConfig.ethereum,
                withdrawalEthereumPasswords
            ).loadMasterContract(withdrawalConfig.ethMasterWallet)

            masterContract.addPeerByPeer(
                ethereumPeerAddress,
                Numeric.hexStringToByteArray(triggerTxHash),
                addPeerProof.v,
                addPeerProof.r,
                addPeerProof.s
            ).send()
        }
    }
}
