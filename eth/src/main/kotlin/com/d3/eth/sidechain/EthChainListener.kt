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
    private val lastReadBlockProvider: LastReadBlockProvider
) : ChainListener<EthBlock> {

    init {
        logger.info {
            "Init EthChainListener with confirmation period $confirmationPeriod, " + "" +
                    "last read block ${lastReadBlockProvider.getLastBlockHeight()}"
        }
    }

    /** Keep counting blocks to prevent double emitting in case of chain reorganisation */
    var lastBlock = lastReadBlockProvider.getLastBlockHeight()
        private set

    override fun getBlockObservable(): Result<Observable<EthBlock>, Exception> {
        return Result.of {
            web3.replayPastAndFutureBlocksFlowable(
                DefaultBlockParameter.valueOf(lastBlock.plus(confirmationPeriod)),
                false
            )
                .toObservable()
                .observeOn(
                    Schedulers.from(
                        createPrettySingleThreadPool("eth-deposit", "eth-event-handler")
                    )
                )
                // skip up to confirmationPeriod blocks in case of chain reorganisation
                .filter { lastBlock < it.block.number }
                .map {
                    if (lastBlock != it.block.number.minus(BigInteger.ONE))
                        throw IllegalArgumentException(
                            "Wrong block number. Expected ${lastBlock.add(BigInteger.ONE)}, got ${it.block.number}"
                        )
                    lastBlock = it.block.number
                    val block = web3.ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(
                            it.block.number - confirmationPeriod
                        ), true
                    ).send()
                    logger.info { "Ethereum chain listener got block ${block.block.number}" }
                    lastReadBlockProvider.saveLastBlockHeight(lastBlock)
                    block
                }
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
