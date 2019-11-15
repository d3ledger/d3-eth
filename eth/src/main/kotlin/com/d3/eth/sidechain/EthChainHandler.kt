/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.sidechain

import com.d3.commons.sidechain.ChainHandler
import com.d3.commons.sidechain.SideChainEvent
import com.d3.eth.provider.*
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.fanout
import mu.KLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.Transaction
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Implementation of [ChainHandler] for Ethereum side chain.
 * Extract interesting transactions from Ethereum block.
 * Supports two kinds of deposit transfers:
 * 1) from any address to `relay` address
 * 2) from `wallet` address to master address
 * @param web3 - notary.endpoint of Ethereum client
 * @param ethWalletProvider - provider of observable wallets
 * @param ethRelayProvider - provider of observable relays
 * @param ethTokensProvider - provider of observable tokens
 */
class EthChainHandler(
    val web3: Web3j,
    val masterAddres: String,
    val ethWalletProvider: EthAddressProvider,
    val ethRelayProvider: EthAddressProvider,
    val ethTokensProvider: EthTokensProvider
) :
    ChainHandler<EthBlock> {

    init {
        logger.info { "Initialization of EthChainHandler with master $masterAddres" }
    }

    /**
     * Process Ethereum ERC20 tokens
     * @param tx transaction in block
     * @return list of notary events on ERC20 deposit
     */
    private fun handleErc20(
        tx: Transaction,
        time: BigInteger,
        wallets: Map<String, String>,
        relays: Map<String, String>,
        tokenName: String,
        isIrohaAnchored: Boolean
    ): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Handle ERC20 tx ${tx.hash}" }

        // get receipt that contains data about solidity function execution
        val receipt = web3.ethGetTransactionReceipt(tx.hash).send()

        // if tx is committed successfully
        if (receipt.transactionReceipt.get().isStatusOK) {
            return receipt.transactionReceipt.get().logs
                .filter {
                    // filter out transfer
                    // the first topic is a hashed representation of a transfer signature call (the scary string)
                    it.topics[0] == "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"
                }
                .filter {
                    // check if amount > 0
                    if (BigInteger(it.data.drop(2), 16).compareTo(BigInteger.ZERO) > 0) {
                        true
                    } else {
                        logger.warn { "Transaction ${tx.hash} from Ethereum with 0 ERC20 amount" }
                        false
                    }
                }
                .filter {
                    // second and third topics are addresses from and to
                    val from = "0x" + it.topics[1].drop(26).toLowerCase()
                    val to = "0x" + it.topics[2].drop(26).toLowerCase()
                    // transfer from wallet to master or deposit to relay
                    (to == masterAddres && wallets.containsKey(from)) || (relays.containsKey(to))
                }
                .map {
                    ethTokensProvider.getTokenPrecision(tokenName)
                        .fold(
                            { precision ->
                                // second and third topics are addresses from and to
                                val from = "0x" + it.topics[1].drop(26).toLowerCase()
                                val to = "0x" + it.topics[2].drop(26).toLowerCase()
                                // amount of transfer is stored in data
                                val amount = BigInteger(it.data.drop(2), 16)

                                lateinit var clientId: String
                                if (to == masterAddres)
                                    clientId = wallets[from]!!
                                else
                                    clientId = relays[to]!!

                                if (isIrohaAnchored)
                                    SideChainEvent.PrimaryBlockChainEvent.IrohaAnchoredOnPrimaryChainDeposit(
                                        tx.hash,
                                        time,
                                        clientId,
                                        tokenName,
                                        BigDecimal(amount, precision).toPlainString(),
                                        from
                                    )
                                else
                                    SideChainEvent.PrimaryBlockChainEvent.ChainAnchoredOnPrimaryChainDeposit(
                                        tx.hash,
                                        time,
                                        clientId,
                                        tokenName,
                                        BigDecimal(amount, precision).toPlainString(),
                                        from
                                    )
                            },
                            { throw it }
                        )
                }
        } else {
            return listOf()
        }
    }

    /**
     * Process Ether deposit
     * @param tx transaction in block
     * @return list of notary events on Ether deposit
     */
    private fun handleEther(
        tx: Transaction,
        time: BigInteger,
        clientId: String
    ): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Handle Ethereum tx ${tx.hash}" }

        val receipt = web3.ethGetTransactionReceipt(tx.hash).send()

        return if (!receipt.transactionReceipt.get().isStatusOK) {
            logger.warn { "Transaction ${tx.hash} from Ethereum has FAIL status" }
            listOf()
        } else if (tx.value <= BigInteger.ZERO) {
            logger.warn { "Transaction ${tx.hash} from Ethereum with 0 ETH amount" }
            listOf()
        } else {
            // if tx amount > 0 and is committed successfully
            listOf(
                SideChainEvent.PrimaryBlockChainEvent.ChainAnchoredOnPrimaryChainDeposit(
                    tx.hash,
                    time,
                    // all non-existent keys were filtered out in parseBlock
                    clientId,
                    "$ETH_NAME#$ETH_DOMAIN",
                    BigDecimal(tx.value, ETH_PRECISION).toPlainString(),
                    tx.from
                )
            )
        }
    }

    /**
     * Parse [EthBlock] for transactions.
     * @return List of transation we are interested in
     */
    override fun parseBlock(block: EthBlock): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Ethereum chain handler for block ${block.block.number}" }
        val addresses = ethWalletProvider.getAddresses().fanout {
            ethRelayProvider.getAddresses()
        }
        val tokens = ethTokensProvider.getEthAnchoredTokens().fanout {
            ethTokensProvider.getIrohaAnchoredTokens()
        }
        return addresses.fanout { tokens }
            .fold(
                { (addresses, tokens) ->
                    val (wallets, relays) = addresses
                    val (ethAnchoredTokens, irohaAnchoredTokens) = tokens
                    // Eth time in seconds, convert ot milliseconds
                    val time = block.block.timestamp.multiply(BigInteger.valueOf(1000))
                    block.block.transactions
                        .map { it.get() as Transaction }
                        .flatMap {
                            if (wallets.containsKey(it.from) && it.to == masterAddres) {
                                val account = wallets[it.from]!!
                                logger.info { "Deposit from wallet ${it.from} ($account) to master ${masterAddres}" }
                                handleEther(it, time, account)
                            } else if (relays.containsKey(it.to)) {
                                val account = relays[it.to]!!
                                logger.info { "Deposit to relay ${it.to} ($account)" }
                                handleEther(it, time, account)
                            } else if (ethAnchoredTokens.containsKey(it.to))
                                handleErc20(it, time, wallets, relays, ethAnchoredTokens[it.to]!!, false)
                            else if (irohaAnchoredTokens.containsKey(it.to))
                                handleErc20(it, time, wallets, relays, irohaAnchoredTokens[it.to]!!, true)
                            else
                                listOf()
                        }
                }, { ex ->
                    logger.error("Cannot parse block", ex)
                    listOf()
                }
            )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
