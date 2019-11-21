/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.hex
import com.d3.commons.util.irohaEscape
import com.d3.eth.provider.EthAddressProvider
import com.d3.eth.provider.EthTokensProvider
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.extractVRS
import com.d3.eth.sidechain.util.hashToMint
import com.d3.eth.sidechain.util.hashToWithdraw
import integration.eth.config.EthereumPasswords
import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.web3j.crypto.WalletUtils
import java.math.BigDecimal
import java.math.BigInteger

const val ETH_WITHDRAWAL_PROOF_DOMAIN = "ethWithdrawalProof"
const val WITHDRAWAL_ACCOUNT_PUBLIC_KEY =
    "0000000000000000000000000000000000000000000000000000000000000000"

/**
 * Class responsible for withdrawal approval for wallet accounts
 */
class WithdrawalProofHandler(
    private val withrdawalTriggerAccountId: String,
    private val tokensProvider: EthTokensProvider,
    private val walletsProvider: EthAddressProvider,
    ethDepositConfig: EthDepositConfig,
    passwordsConfig: EthereumPasswords,
    irohaAPI: IrohaAPI
) {
    private val gson = GsonInstance.get()

    init {
        logger.info { "Initialization of WithdrawalProofHandler withrdawalTriggerAccountId=$withrdawalTriggerAccountId" }
    }

    private val deployHelper = DeployHelper(ethDepositConfig.ethereum, passwordsConfig)

    private val queryHelper =
        IrohaQueryHelperImpl(irohaAPI, ethDepositConfig.withdrawalCredential)

    private val irohaConsumer = IrohaConsumerImpl(
        IrohaCredential(ethDepositConfig.withdrawalCredential),
        irohaAPI
    )

    private val ethCredential = WalletUtils.loadCredentials(
        passwordsConfig.credentialsPassword,
        passwordsConfig.credentialsPath
    )

    /**
     * Filter expansion trigger event and call expansion logic
     * @param block - iroha block
     */
    fun proceedBlock(block: BlockOuterClass.Block) {
        block.blockV1.payload.transactionsList
            // Get commands
            .flatMap { tx ->
                val txHash = String.hex(Utils.hash(tx))
                tx.payload.reducedPayload.commandsList
                    .filter { command -> command.hasTransferAsset() }
                    .map { command -> command.transferAsset }
                    .filter { transfer -> transfer.destAccountId == withrdawalTriggerAccountId }
                    // check if description is valid Ethereum address
                    .filter { transfer -> WalletUtils.isValidAddress(transfer.description) }
                    .filter { transfer ->
                        // check token is eth token
                        tokensProvider.getEthTokens().get().containsValue(transfer.assetId)
                    }
                    .filter { transfer ->
                        // check account is registered in Ethereum
                        walletsProvider.getAddressByAccountId(transfer.srcAccountId).get().isPresent
                    }
                    .map { transfer ->
                        logger.info { "Withdrawal event from=${transfer.srcAccountId}, to=${transfer.destAccountId}, descr=${transfer.description}, asset=${transfer.assetId}, amount=${transfer.amount}" }

                        // create account if not exists
                        val proofAccountName = txHash.take(32).toLowerCase()
                        val proofAccountId = "$proofAccountName@$ETH_WITHDRAWAL_PROOF_DOMAIN"
                        createAccountIfNotExists(proofAccountName)

                        // write proof
                        val key = ethCredential.address
                        val proof = createProof(
                            transfer.srcAccountId,
                            transfer.assetId,
                            transfer.amount,
                            transfer.description,
                            txHash
                        )
                        ModelUtil.setAccountDetail(irohaConsumer, proofAccountId, key, proof)
                            .get()
                    }
            }
    }

    private fun createAccountIfNotExists(proofAccountName: String) {
        if (!queryHelper.isRegistered(
                proofAccountName,
                ETH_WITHDRAWAL_PROOF_DOMAIN,
                WITHDRAWAL_ACCOUNT_PUBLIC_KEY
            ).get()
        ) {
            ModelUtil.createAccount(
                irohaConsumer,
                proofAccountName,
                ETH_WITHDRAWAL_PROOF_DOMAIN,
                Utils.parseHexPublicKey(WITHDRAWAL_ACCOUNT_PUBLIC_KEY),
                emptyList()
            ).get()
        }
    }

    private fun createProof(
        accountId: String,
        assetId: String,
        amount: String,
        beneficiary: String,
        txHash: String
    ): String {
        val ethTokenAddress = tokensProvider.getTokenAddress(assetId).get()
        val tokenPrecision = tokensProvider.getTokenPrecision(assetId).get()
        val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(tokenPrecision).toPlainString()
        val hash: String
        if (tokensProvider.isIrohaAnchored(assetId).get())
            hash = hashToMint(
                ethTokenAddress,
                decimalAmount,
                beneficiary,
                txHash,
                beneficiary
            )
        else
            hash = hashToWithdraw(
                ethTokenAddress,
                decimalAmount,
                beneficiary,
                txHash,
                beneficiary
            )
        val signatureString = deployHelper.signUserData(hash)
        val vrs = extractVRS(signatureString)
        val signature = VRSSignarute(vrs.v, BigInteger(1, vrs.r), BigInteger(1, vrs.s))

        val withdrawalProof = WithdrawalProof(
            accountId,
            ethTokenAddress,
            decimalAmount,
            beneficiary,
            txHash,
            beneficiary,
            signature
        )
        return gson.toJson(withdrawalProof).irohaEscape()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
