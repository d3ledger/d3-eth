/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.helper

import com.d3.commons.config.loadConfigs
import jp.co.soramitsu.soranet.eth.config.EthereumPasswords
import jp.co.soramitsu.soranet.eth.contract.SoraToken
import jp.co.soramitsu.soranet.eth.helper.hexStringToByteArray
import jp.co.soramitsu.soranet.eth.sidechain.util.*
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.math.BigInteger
import kotlin.test.assertEquals

/**
 * Helper class for Ethereum contracts.
 * Deploys contracts on demand, contains set of functions to work with ethereum contracts.
 */
class ContractTestHelper {
    private val testConfig =
        loadConfigs("test", TestEthereumConfig::class.java, "/test.properties").get()
    private val passwordConfig =
        loadConfigs(
            "test",
            EthereumPasswords::class.java,
            "/eth/ethereum_password.properties"
        ).get()

    val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    // ganache-cli ether custodian
    val accMain = deployHelper.credentials.address
    // some ganache-cli account
    val accGreen = testConfig.ethTestAccount

    val keypair = deployHelper.credentials.ecKeyPair
    val token by lazy { deployHelper.deployERC20TokenSmartContract() }
    val master by lazy {
        deployHelper.deployUpgradableMasterSmartContract(
            listOf(accMain)
        )
    }
    val xorAddress by lazy {
        master.xorTokenInstance().send()
    }

    val etherAddress = "0x0000000000000000000000000000000000000000"
    val defaultIrohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))
    val defaultByteHash = hexStringToByteArray(defaultIrohaHash)

    data class SigsData(
        val vv: ArrayList<BigInteger>,
        val rr: ArrayList<ByteArray>,
        val ss: ArrayList<ByteArray>
    )

    fun prepareSignatures(amount: Int, keypairs: List<ECKeyPair>, toSign: String): SigsData {
        val vv = ArrayList<BigInteger>()
        val rr = ArrayList<ByteArray>()
        val ss = ArrayList<ByteArray>()

        for (i in 0 until amount) {
            val signature = signUserData(keypairs[i], toSign)
            val vrs = extractVRS(signature)
            vv.add(vrs.v)
            rr.add(vrs.r)
            ss.add(vrs.s)
        }
        return SigsData(
            vv,
            rr,
            ss
        )
    }

    fun transferTokensToMaster(amount: BigInteger) {
        token.transfer(master.contractAddress, amount).send()
        assertEquals(amount, token.balanceOf(master.contractAddress).send())
    }

    /**
     * Withdraw assets
     * @param amount how much to withdraw
     * @return receipt of a transaction was sent
     */
    fun withdraw(amount: BigInteger): TransactionReceipt {
        return withdraw(amount, accGreen)
    }

    /**
     * Withdraw assets
     * @param amount how much to withdraw
     * @param to account address
     * @return receipt of a transaction was sent
     */
    fun withdraw(amount: BigInteger, to: String): TransactionReceipt {
        return withdraw(amount, to, token.contractAddress)
    }

    /**
     * Withdraw assets
     * @param amount how much to withdraw
     * @param to destination address
     * @param tokenAddress Ethereum address of ERC-20 token
     * @return receipt of a transaction was sent
     */
    fun withdraw(
        amount: BigInteger,
        to: String,
        tokenAddress: String
    ): TransactionReceipt {
        val finalHash = hashToWithdraw(
            tokenAddress,
            amount.toString(),
            to,
            defaultIrohaHash,
            master.contractAddress
        )
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)

        return master.withdraw(
            tokenAddress,
            amount,
            to,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            master.contractAddress
        ).send()
    }

    fun addPeerByPeer(newPeer: String): TransactionReceipt {
        val finalHash = hashToAddAndRemovePeer(newPeer, defaultIrohaHash)
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)

        return master.addPeerByPeer(
            newPeer,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss
        ).send()
    }

    fun removePeerByPeer(peer: String): TransactionReceipt {
        val finalHash = hashToAddAndRemovePeer(peer, defaultIrohaHash)
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)

        return master.removePeerByPeer(
            peer,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss
        ).send()
    }

    fun mintByPeer(beneficiary: String, amount: Long): TransactionReceipt {
        val finalHash = hashToMint(
            xorAddress,
            amount.toString(),
            beneficiary,
            defaultIrohaHash,
            master.contractAddress
        )
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)

        return master.mintTokensByPeers(
            xorAddress,
            BigInteger.valueOf(amount),
            beneficiary,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            master.contractAddress
        ).send()
    }

    fun sendEthereum(amount: BigInteger, to: String) {
        deployHelper.sendEthereum(amount, to)
    }

    fun sendERC20Token(contractAddress: String, amount: BigInteger, toAddress: String) {
        deployHelper.sendERC20(contractAddress, toAddress, amount)
    }

    fun sendEthereum(amount: BigInteger, to: String, credentials: Credentials) {
        deployHelper.sendEthereum(amount, to, credentials)
    }

    fun sendERC20Token(
        contractAddress: String,
        amount: BigInteger,
        toAddress: String,
        credentials: Credentials
    ) {
        deployHelper.sendERC20(contractAddress, toAddress, amount, credentials)
    }

    fun getETHBalance(whoAddress: String): BigInteger {
        return deployHelper.getETHBalance(whoAddress)
    }

    fun getERC20TokenBalance(contractAddress: String, whoAddress: String): BigInteger {
        return deployHelper.getERC20Balance(contractAddress, whoAddress)
    }

    /**
     * Generating KeyPairs for signing the data and array of public keys (Ethereum address of initial peers)
     * @param amount of keyPairs
     * @return pair of keyPairs and public keys
     */
    fun getKeyPairsAndPeers(amount: Int): Pair<List<ECKeyPair>, List<String>> {
        val keyPairs = ArrayList<ECKeyPair>()
        val peers = ArrayList<String>()

        for (i in 0 until amount) {
            val keypair = Keys.createEcKeyPair()
            keyPairs.add(keypair)
            peers.add("0x" + Keys.getAddress(keypair))
        }
        return Pair(keyPairs, peers)
    }

    fun getToken(tokenAddress: String): SoraToken {
        return deployHelper.loadSoraTokenSmartContract(tokenAddress)
    }

    fun deployFailer(): String {
        return deployHelper.deployFailerContract().contractAddress
    }
}
