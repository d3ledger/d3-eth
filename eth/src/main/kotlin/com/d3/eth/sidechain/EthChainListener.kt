/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.sidechain

import com.d3.commons.sidechain.ChainListener
import com.d3.commons.sidechain.provider.LastReadBlockProvider
import com.d3.commons.util.createPrettySingleThreadPool
import com.github.kittinunf.result.Result
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import mu.KLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.EthBlock
import java.math.BigInteger

/**
 * Implementation of [ChainListener] for Ethereum sidechain
 * @param web3 - notary.endpoint of Ethereum client
 * @param confirmationPeriod - number of block to consider block final
 */
class EthChainListener(
    private val web3: Web3j,
    private val confirmationPeriod: BigInteger,
    startBlock: BigInteger,
    private val lastReadBlockProvider: LastReadBlockProvider
) : ChainListener<EthBlock> {

    /** Keep counting blocks to prevent double emitting in case of chain reorganisation */
    var lastBlockNumber = maxOf(lastReadBlockProvider.getLastBlockHeight(), startBlock)
        private set

    init {
        logger.info {
            "Init EthChainListener. Start with block number ${lastBlockNumber}, " +
                    "confirmation period $confirmationPeriod"
        }
    }

    override fun getBlockObservable(): Result<Observable<EthBlock>, Exception> {
        return Result.of {
            web3.replayPastAndFutureBlocksFlowable(
                DefaultBlockParameter.valueOf(lastBlockNumber.plus(confirmationPeriod)),
                false
            )
                .toObservable()
                .observeOn(
                    Schedulers.from(
                        createPrettySingleThreadPool("eth-deposit", "eth-event-handler")
                    )
                )
                // skip up to confirmationPeriod blocks in case of chain reorganisation
                .filter { lastBlockNumber <= it.block.number }
                .map { topBlock ->
                    logger.info { "Ethereum chain listener got block ${topBlock.block.number}" }

                    val topBlockNumber = topBlock.block.number.minus(confirmationPeriod)
                    val blocks = mutableListOf<EthBlock>()
                    while (lastBlockNumber <= topBlockNumber) {
                        val block = web3.ethGetBlockByNumber(
                            DefaultBlockParameter.valueOf(lastBlockNumber), true
                        ).send()

                        logger.info { "Ethereum chain listener loaded block ${block.block.number}" }

                        blocks.add(block)
                        lastBlockNumber = lastBlockNumber.inc()
                    }
                    lastReadBlockProvider.saveLastBlockHeight(lastBlockNumber)
                    blocks
                }
                .flatMapIterable { blocks -> blocks }
        }
    }

    /**
     * @return a block as soon as it is committed to Ethereum
     */
    override suspend fun getBlock(): EthBlock {
        return getBlockObservable().get().blockingFirst()
    }

    override fun close() {
        web3.shutdown()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
