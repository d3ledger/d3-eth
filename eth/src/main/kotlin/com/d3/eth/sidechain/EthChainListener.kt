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
import io.reactivex.subjects.PublishSubject
import mu.KLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.EthBlock
import java.math.BigInteger
import kotlin.system.exitProcess

/**
 * Implementation of [ChainListener] for Ethereum sidechain
 * @param web3 - notary.endpoint of Ethereum client
 * @param confirmationPeriod - number of block to consider block final
 */
class EthChainListener(
    private val web3: Web3j,
    private val confirmationPeriod: BigInteger,
    startBlock: BigInteger,
    private val lastReadBlockProvider: LastReadBlockProvider,
    private val ignoreStartBlock: Boolean
) : ChainListener<EthBlock> {

    /** Keep counting blocks to prevent double emitting in case of chain reorganisation */
    var lastBlockNumber = maxOf(lastReadBlockProvider.getLastBlockHeight(), startBlock)
        private set

    private val scheduler = Schedulers.from(createPrettySingleThreadPool("eth-deposit", "eth-event-handler"))
    private val ethBlocksSubject: PublishSubject<EthBlock> = PublishSubject.create()
    private val ethBlocksObservable = ethBlocksSubject.share().subscribeOn(scheduler).doOnSubscribe {
        runBlockSubjectProducer()
    }

    init {
        logger.info {
            "Init EthChainListener. Start with block number $lastBlockNumber, " +
                    "confirmation period $confirmationPeriod" + " and ignorance of first block: $ignoreStartBlock"
        }
    }

    override fun getBlockObservable(): Result<Observable<EthBlock>, Exception> =
        Result.of { ethBlocksObservable }

    private fun runBlockSubjectProducer() {
        if (ignoreStartBlock) {
            lastBlockNumber = web3.blockFlowable(true).toObservable().blockingFirst().block.number
        }
        getEthBlockObservable()
            .observeOn(scheduler)
            .subscribeOn(scheduler)
            // skip up to confirmationPeriod blocks in case of chain reorganisation
            .filter { lastBlockNumber <= it.block.number }
            .subscribe({ topBlock ->
                logger.info { "Ethereum chain listener got block ${topBlock.block.number}" }

                val topBlockNumber = topBlock.block.number.minus(confirmationPeriod)
                while (lastBlockNumber < topBlockNumber) {
                    val block = web3.ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(lastBlockNumber), true
                    ).send()

                    logger.info { "Ethereum chain listener loaded block ${block.block.number}" }

                    publishEthBlockAndSaveHeight(block)
                }
                publishEthBlockAndSaveHeight(topBlock)
            }, { ex ->
                logger.error("Ethereum blocks observable error", ex)
                exitProcess(1)
            })
    }

    /**
     * @return a block as soon as it is committed to Ethereum
     */
    override suspend fun getBlock(): EthBlock {
        return getBlockObservable().get().blockingFirst()
    }

    override fun close() {
        scheduler.shutdown()
        web3.shutdown()
    }

    /**
     * Rethrows an EthBlock into target publish subject and saves last read block value
     */
    private fun publishEthBlockAndSaveHeight(ethBlock: EthBlock) {
        ethBlocksSubject.onNext(ethBlock)
        val height = ethBlock.block.number.inc()
        lastReadBlockProvider.saveLastBlockHeight(height)
        lastBlockNumber = height
    }

    private fun getEthBlockObservable(): Observable<EthBlock> =
        web3.replayPastAndFutureBlocksFlowable(
            DefaultBlockParameter.valueOf(lastBlockNumber.plus(confirmationPeriod)),
            true
        ).toObservable()

    /**
     * Logger
     */
    companion object : KLogging()
}
