/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.sidechain

import com.d3.commons.sidechain.ChainHandler
import com.d3.commons.sidechain.SideChainEvent
import com.d3.notifications.event.AckEthWithdrawalProofEvent
import com.github.kittinunf.result.fanout
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.soranet.eth.abi.AbiDecoder
import jp.co.soramitsu.soranet.eth.abi.AbiGsonHelper.ETH_PREFIX
import jp.co.soramitsu.soranet.eth.mq.EthNotificationMqProducer
import jp.co.soramitsu.soranet.eth.provider.*
import mu.KLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.Transaction
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Implementation of [ChainHandler] for Ethereum side chain.
 * Extract interesting transactions from Ethereum block.
 * Supports three kinds of event:
 * 1) deposit from any address to `relay` address
 * 2) deposit from `wallet` address to master address
 * 3) withdrawal from `wallet` address using master contract
 * @param web3 - notary.endpoint of Ethereum client
 * @param ethWalletProvider - provider of observable wallets
 * @param ethTokensProvider - provider of observable tokens
 */
class EthChainHandler(
    private val web3: Web3j,
    private val masterAddres: String,
    private val ethWalletProvider: EthAddressProvider,
    private val ethTokensProvider: EthTokensProvider,
    private val ethNotificationMqProducer: EthNotificationMqProducer,
    masterContractAbi: String
) : ChainHandler<EthBlock> {

    private val masterContractAbiDecoder = AbiDecoder()

    init {
        logger.info { "Initialization of EthChainHandler with master $masterAddres" }
        masterContractAbiDecoder.addAbi(masterContractAbi)
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
                    if (BigInteger(it.data.drop(2), 16) > BigInteger.ZERO) {
                        true
                    } else {
                        logger.warn { "Transaction ${tx.hash} from Ethereum with 0 ERC20 amount" }
                        false
                    }
                }
                .filter {
                    // second and third topics are addresses from and to
                    val from = ETH_PREFIX + it.topics[1].drop(26).toLowerCase()
                    val to = ETH_PREFIX + it.topics[2].drop(26).toLowerCase()
                    // transfer from wallet to master or deposit to relay
                    to == masterAddres && wallets.containsKey(from)
                }
                .map {
                    ethTokensProvider.getTokenPrecision(tokenName)
                        .fold(
                            { precision ->
                                // second and third topics are addresses from and to
                                val from = ETH_PREFIX + it.topics[1].drop(26).toLowerCase()
                                val to = ETH_PREFIX + it.topics[2].drop(26).toLowerCase()
                                // amount of transfer is stored in data
                                val amount = BigInteger(it.data.drop(2), 16)

                                val clientId = wallets[from]!!

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
     * Tries to parse transaction as a master contract method call
     * @param transaction Ethereum block transaction
     * @return list of notary events on Ethereum withdrawal finalization
     */
    private fun handleWithdrawal(
        transaction: Transaction,
        time: BigInteger
    ): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Handle Ethereum master contract call tx ${transaction.hash}" }

        val receipt = web3.ethGetTransactionReceipt(transaction.hash).send()

        if (!receipt.transactionReceipt.get().isStatusOK) {
            logger.warn { "Transaction ${transaction.hash} from Ethereum has FAIL status" }
        } else {
            try {
                val decodeMethod = masterContractAbiDecoder.decodeMethod(transaction.input)
                when (decodeMethod.name) {
                    abiXorWithdrawalMethodName, abiOtherWithdrawalMethodName -> {
                        val param = decodeMethod.params.find {
                            it.name == txHashAbiParameterName &&
                                    it.type == txHashAbiParameterType
                        }
                        if (param == null) {
                            logger.warn { "Transaction ${transaction.hash} contains unexpected method parameters" }
                            return emptyList()
                        }
                        val irohaTxHash = Utils.toHex(param.value as ByteArray)
                        ethNotificationMqProducer.enqueue(
                            AckEthWithdrawalProofEvent(
                                irohaTxHash,
                                "$irohaTxHash$withdrawalAckId",
                                time.toLong(),
                                transaction.blockNumber.toLong(),
                                transaction.transactionIndex.toInt()
                            )
                        )
                    }
                    else -> {
                        logger.warn { "Transaction ${transaction.hash} contains unexpected method call" }
                    }
                }
            } catch (e: Exception) {
                logger.error("An error during contract call processing occured", e)
            }
        }
        // no need to process the event in Iroha
        return emptyList()
    }

    /**
     * Parse [EthBlock] for transactions.
     * @return List of transation we are interested in
     */
    override fun parseBlock(block: EthBlock): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Ethereum chain handler for block ${block.block.number}" }
        val addresses = ethWalletProvider.getAddresses()
        val tokens = ethTokensProvider.getEthAnchoredTokens().fanout {
            ethTokensProvider.getIrohaAnchoredTokens()
        }
        return addresses.fanout { tokens }
            .fold(
                { (wallets, tokens) ->
                    val (ethAnchoredTokens, irohaAnchoredTokens) = tokens
                    // Eth time in seconds, convert ot milliseconds
                    val time = block.block.timestamp.multiply(thousand)
                    block.block.transactions
                        .map { it.get() as Transaction }
                        .flatMap { transaction ->
                            val toAddress = transaction.to
                            if (toAddress != null) {
                                when {
                                    // TODO think how to proof withdrawals for other ERC20 if needed
                                    transaction.input != ETH_PREFIX && toAddress == masterAddres -> {
                                        logger.info { "Contract method call of master $masterAddres" }
                                        handleWithdrawal(transaction, time)
                                    }
                                    transaction.input == ETH_PREFIX && wallets.containsKey(transaction.from) && toAddress == masterAddres -> {
                                        val account = wallets[transaction.from]!!
                                        logger.info { "Ether deposit from wallet ${transaction.from} ($account) to master $masterAddres" }
                                        handleEther(transaction, time, account)
                                    }
                                    ethAnchoredTokens.containsKey(toAddress) -> {
                                        handleErc20(
                                            transaction,
                                            time,
                                            wallets,
                                            ethAnchoredTokens[toAddress]!!,
                                            false
                                        )
                                    }
                                    irohaAnchoredTokens.containsKey(toAddress) -> {
                                        handleErc20(
                                            transaction,
                                            time,
                                            wallets,
                                            irohaAnchoredTokens[toAddress]!!,
                                            true
                                        )
                                    }
                                    else -> listOf()
                                }
                            } else listOf()
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
    companion object : KLogging() {
        private val thousand: BigInteger = BigInteger.valueOf(1000)
        private const val abiXorWithdrawalMethodName = "mintTokensByPeers"
        private const val abiOtherWithdrawalMethodName = "withdraw"
        private const val txHashAbiParameterName = "txHash"
        private const val txHashAbiParameterType = "bytes32"
        private const val withdrawalAckId = "_withdrawal_ack"
    }
}
