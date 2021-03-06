/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import com.d3.commons.config.loadConfigs
import com.d3.eth.helper.hexStringToByteArray
import com.d3.eth.sidechain.util.*
import contract.SoraToken
import integration.eth.config.EthereumPasswords
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.math.BigInteger
import kotlin.test.assertEquals

/** Helper class for Ethereum contracts.
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
    val relayRegistry by lazy { deployHelper.deployUpgradableRelayRegistrySmartContract() }
    val token by lazy { deployHelper.deployERC20TokenSmartContract() }
    val master by lazy {
        deployHelper.deployUpgradableMasterSmartContract(
            listOf(accMain)
        )
    }
    val xorAddress = master.xorTokenInstance().send()
    val relayImplementation by lazy { deployHelper.deployRelaySmartContract(master.contractAddress) }
    val relay by lazy {
        deployHelper.deployUpgradableRelaySmartContract(
            relayImplementation.contractAddress,
            master.contractAddress
        )
    }

    val etherAddress = "0x0000000000000000000000000000000000000000"
    val defaultIrohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))
    val defaultByteHash = hexStringToByteArray(defaultIrohaHash)

    data class sigsData(
        val vv: ArrayList<BigInteger>,
        val rr: ArrayList<ByteArray>,
        val ss: ArrayList<ByteArray>
    )

    fun prepareSignatures(amount: Int, keypairs: List<ECKeyPair>, toSign: String): sigsData {
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
        return sigsData(vv, rr, ss)
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
        val tokenAddress = token.contractAddress
        val to = accGreen

        val finalHash = hashToWithdraw(
            tokenAddress,
            amount.toString(),
            to,
            defaultIrohaHash,
            relay.contractAddress
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
            relay.contractAddress
        ).send()
    }

    /**
     * Withdraw assets
     * @param amount how much to withdraw
     * @param to account address
     * @return receipt of a transaction was sent
     */
    fun withdraw(amount: BigInteger, to: String): TransactionReceipt {
        val tokenAddress = token.contractAddress

        val finalHash = hashToWithdraw(
            tokenAddress,
            amount.toString(),
            to,
            defaultIrohaHash,
            relay.contractAddress
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
            relay.contractAddress
        ).send()
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
            relay.contractAddress
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
            relay.contractAddress
        ).send()
    }

    /**
     * Withdraw assets
     * @param amount how much to withdraw
     * @param to destination address
     * @param tokenAddress Ethereum address of ERC-20 token
     * @param fromMaster true if withdraw should proceeded from master contract
     * @return receipt of a transaction was sent
     */
    fun withdraw(
        amount: BigInteger,
        tokenAddress: String,
        to: String,
        fromMaster: Boolean
    ): TransactionReceipt {
        val finalHash = hashToWithdraw(
            tokenAddress,
            amount.toString(),
            to,
            defaultIrohaHash,
            relay.contractAddress
        )
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)
        if (fromMaster) {
            return master.withdraw(
                tokenAddress,
                amount,
                to,
                defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                relay.contractAddress
            ).send()
        } else {
            addWhiteListToRelayRegistry(relay.contractAddress, listOf(to))
            return relay.withdraw(
                tokenAddress,
                amount,
                to,
                defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                relay.contractAddress
            ).send()
        }
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
            relay.contractAddress
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
            relay.contractAddress
        ).send()
    }

    /**
     * Save white list in the contract
     * @param relayAddress relay contract address
     * @param whiteList list of addresses allowed to withdraw
     * @return receipt of a transaction was sent
     */
    fun addWhiteListToRelayRegistry(
        relayAddress: String,
        whiteList: List<String>
    ): TransactionReceipt {
        return relayRegistry.addNewRelayAddress(relayAddress, whiteList).send()
    }

    fun sendEthereum(amount: BigInteger, to: String) {
        deployHelper.sendEthereum(amount, to)
    }

    fun sendERC20Token(contractAddress: String, amount: BigInteger, toAddress: String) {
        deployHelper.sendERC20(contractAddress, toAddress, amount)
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
        return deployHelper.loadTokenSmartContract(tokenAddress)
    }

    fun deployFailer(): String {
        return deployHelper.deployFailerContract().contractAddress
    }
}
