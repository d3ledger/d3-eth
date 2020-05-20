/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.hex
import com.d3.commons.util.irohaEscape
import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.soranet.eth.config.EthereumPasswords
import jp.co.soramitsu.soranet.eth.provider.EthAddressProvider
import jp.co.soramitsu.soranet.eth.provider.EthTokensProvider
import jp.co.soramitsu.soranet.eth.sidechain.util.*
import mu.KLogging
import org.apache.commons.codec.binary.Hex
import org.web3j.crypto.WalletUtils
import java.math.BigDecimal

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
    private val deployHelper: DeployHelper,
    private val queryHelper: IrohaQueryHelper,
    private val irohaConsumer: IrohaConsumer,
    passwordsConfig: EthereumPasswords
) {
    private val gson = GsonInstance.get()

    init {
        logger.info { "Wallet Withdrawal: Initialization of WithdrawalProofHandler withrdawalTriggerAccountId=$withrdawalTriggerAccountId" }
    }

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
            .forEach { tx ->
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
                        logger.info { "Wallet Withdrawal: Withdrawal event from=${transfer.srcAccountId}, to=${transfer.destAccountId}, descr=${transfer.description}, asset=${transfer.assetId}, amount=${transfer.amount}" }

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
                        ModelUtil.setAccountDetail(irohaConsumer, proofAccountId, key, proof).get()
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
            ).fold(
                { logger.info { "Wallet Withdrawal: create account \"$proofAccountName@$ETH_WITHDRAWAL_PROOF_DOMAIN\" for proofs" } },
                { logger.info { "Wallet Withdrawal: account \"$proofAccountName@$ETH_WITHDRAWAL_PROOF_DOMAIN\" exists: ${it.message}" } }
            )
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
        val hash =
            if (tokensProvider.isIrohaAnchored(assetId).get())
                hashToMint(
                    ethTokenAddress,
                    decimalAmount,
                    beneficiary,
                    txHash,
                    beneficiary
                )
            else
                hashToWithdraw(
                    ethTokenAddress,
                    decimalAmount,
                    beneficiary,
                    txHash,
                    beneficiary
                )
        val signatureString = deployHelper.signUserData(hash)
        val vrs = extractVRS(signatureString)
        val v = vrs.v.toString(16).replace("0x", "")
        val r = Hex.encodeHexString(vrs.r).replace("0x", "")
        val s = Hex.encodeHexString(vrs.s).replace("0x", "")
        val signature = VRSSignature(v, r, s)

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
