/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.sidechain

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.Transaction
import mu.KLogging
import org.web3j.contracts.eip20.generated.ERC20
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicLong

/**
 * Soranet to ETH XOR withdrawal limit provider.
 */
class WithdrawalLimitProvider(
    private val requesterQueryHelper: IrohaQueryHelper,
    private val setterIrohaConsumer: IrohaConsumer,
    val nextUpdateTime: AtomicLong,
    private val limitHolderAccount: String,
    private val limitUpdateTimeAccountKey: String,
    private val limitValueAccountKey: String,
    private val soraTokenContract: ERC20,
    private val xorExchangeAddress: String
) {
    private val creatorAccountId = setterIrohaConsumer.creator

    init {
        queryAndSetUpdateTime()
        logger.info("Initialized withdrawal limit provider with token contract - ${soraTokenContract.contractAddress} and exchange address - $xorExchangeAddress")
    }

    private fun queryAndSetUpdateTime() {
        nextUpdateTime.set(
            requesterQueryHelper.getAccountDetails(
                limitHolderAccount,
                creatorAccountId,
                limitUpdateTimeAccountKey
            ).map {
                if (it.isPresent) {
                    it.get().toLong()
                } else 0L
            }.get()
        )
        logger.info("Loaded limit provider next update time from Iroha: ${nextUpdateTime.get()}")
    }

    fun updateLimit(consequentUpdateTime: Long, limit: BigDecimal) {
        val stringLimit = limit
            .setScale(
                XOR_PRECISION,
                XOR_ROUNDING_MODE
            )
            .stripTrailingZeros()
            .toPlainString()

        logger.info("Going to update limits: time - $consequentUpdateTime limit - ${limit.toPlainString()}")

        setterIrohaConsumer.send(
            Transaction.builder(creatorAccountId)
                .compareAndSetAccountDetail(
                    limitHolderAccount,
                    limitUpdateTimeAccountKey,
                    consequentUpdateTime.toString(),
                    nextUpdateTime.get().toString()
                )
                .setAccountDetail(
                    limitHolderAccount,
                    limitValueAccountKey,
                    stringLimit
                )
                .build()
        ).fold(
            {
                logger.info("Successfully updated withdrawal limits")
                nextUpdateTime.set(consequentUpdateTime)
                logger.info("Set limit provider next update time after update: ${nextUpdateTime.get()}")
            },
            {
                logger.warn("Other node has updated withdrawal limits: ${it.message}")
                queryAndSetUpdateTime()
            }
        )
    }

    fun getXorExchangeLiquidity(): BigInteger {
        return soraTokenContract.balanceOf(xorExchangeAddress).send()
    }

    companion object : KLogging() {
        const val XOR_PRECISION = 18
        val XOR_ROUNDING_MODE = RoundingMode.HALF_UP
    }
}
