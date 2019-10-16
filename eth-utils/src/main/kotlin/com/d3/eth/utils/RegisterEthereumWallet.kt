/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("RegisterEthereumWallet")

package com.d3.eth.utils

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.loadConfigs
import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.eth.helper.generateWalletFile
import com.d3.eth.helper.getWalletByAddress
import com.d3.eth.helper.hexStringToByteArray
import com.d3.eth.provider.ETH_CLIENT_WALLET
import com.d3.eth.provider.EthTokensProviderImpl
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.withdrawal.withdrawalservice.ProofCollector
import com.d3.eth.withdrawal.withdrawalservice.WithdrawalServiceConfig
import integration.eth.config.EthereumConfig
import integration.eth.config.loadEthPasswords
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.web3j.crypto.WalletUtils
import java.math.BigInteger

private val logger = KLogging().logger

fun main() {
    // TODO get from arguments?
    val accountId = "alexey@d3"
    val publicKey = "092e71b031a51adae924f7cd944f0371ae8b8502469e32693885334dedcc6001"
    val privateKey = "e51123b78d658418d018e7d2486021209af3cff82714b4cb7925870fec6097dc"
    val keypair = Utils.parseHexKeypair(publicKey, privateKey)
    val credential = IrohaCredential(accountId, keypair)

    // 0. Generate Ethereum address
    val ethPasswordConfig = loadEthPasswords("test", "/eth/ethereum_password.properties").get()
    val ethAddress = generateWalletFile(ethPasswordConfig.credentialsPassword)

    val ethConfig =
        loadConfigs("test.ethereum", EthereumConfig::class.java, "/test.properties").get()

    val withdrawalConfig = loadLocalConfigs(
        "withdrawal",
        WithdrawalServiceConfig::class.java,
        "withdrawal.properties"
    ).get()

    val masterAddress = withdrawalConfig.ethMasterAddress

    val irohaConfig = loadConfigs("test.iroha", IrohaConfig::class.java, "/test.properties").get()

    val irohaAPI = IrohaAPI(
        irohaConfig.hostname,
        irohaConfig.port
    )

    // 1. Register in Iroha
    val irohaConsumer = IrohaConsumerImpl(credential, irohaAPI)
    val queryHelper = IrohaQueryHelperImpl(irohaAPI, credential)
    val clientQuorum = queryHelper.getAccountQuorum(accountId).get()
    val txHash = ModelUtil.setAccountDetail(
        irohaConsumer,
        accountId,
        ETH_CLIENT_WALLET,
        ethAddress,
        quorum = clientQuorum
    ).get()
    logger.info { "1 step - set $ethAddress to $accountId, tx hash $txHash" }

    // 2. Register in Ethereum
    val withdrawalCredentials = IrohaCredential(withdrawalConfig.withdrawalCredential)
    val withdrawalQueryHelper = IrohaQueryHelperImpl(irohaAPI, withdrawalCredentials)

    val tokensProvider = EthTokensProviderImpl(withdrawalQueryHelper, "", "", "", "")
    val notaryPeerListProvider = NotaryPeerListProviderImpl(
        withdrawalQueryHelper,
        withdrawalConfig.notaryListStorageAccount,
        withdrawalConfig.notaryListSetterAccount
    )

    // get proofs
    val proofCollector = ProofCollector(
        withdrawalQueryHelper,
        withdrawalConfig,
        tokensProvider,
        notaryPeerListProvider
    )

    val proof = proofCollector.collectProofForRegistration(
        ethAddress,
        accountId,
        txHash
    ).get()

    val testDeployHelper = DeployHelper(ethConfig, ethPasswordConfig)
    testDeployHelper.sendEthereum(BigInteger.valueOf(100_000_000), ethAddress)

    val ethCredentialsFile = getWalletByAddress(".", ethAddress).get()
    val ethCredentials =
        WalletUtils.loadCredentials(ethPasswordConfig.credentialsPassword, ethCredentialsFile)
    val deployHelper = DeployHelper(
        ethConfig,
        ethPasswordConfig.nodeLogin,
        ethPasswordConfig.nodePassword,
        ethCredentials
    )
    val master = deployHelper.loadMasterContract(masterAddress)

    val registrationResult = master.register(
        proof.ethAddress,
        proof.irohaAccountId.toByteArray(),
        hexStringToByteArray(proof.irohaHash),
        proof.v,
        proof.r,
        proof.s
    ).send()

    logger.info { " Ethereum registration tx hash: ${registrationResult.transactionHash}" }
}
