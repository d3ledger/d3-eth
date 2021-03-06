/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.notary

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.github.kittinunf.result.Result
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigInteger
import kotlin.test.assertEquals

/**
 * Test business logic of Notary.
 */
class NotaryTest {
    val creatorId = "creator@iroha"
    private val irohaCredential = IrohaCredential(creatorId, ModelUtil.generateKeypair())
    private val irohaConsumer = mock<IrohaConsumer> {
        on { creator } doReturn creatorId
        on { getConsumerQuorum() } doReturn Result.of { 1 }
    }

    /**
     * Check transactions in ordered batch emitted on deposit event.
     * @param expectedAmount amount of assets to deposit
     * @param expectedAssetId asset id
     * @param expectedCreatorId creator of transactions
     * @param expectedHash - hash of ethereum transaction
     * @param expectedUserId - destination wallet address
     */
    private fun checkEthereumDepositResult(
        expectedAmount: String,
        expectedAssetId: String,
        expectedUserId: String,
        expectedFrom: String,
        expectedTime: BigInteger,
        result: Observable<IrohaTransaction>
    ) {
        val observer = TestObserver<IrohaTransaction>()
        result.subscribe(observer)

        observer.assertNoErrors()
        observer.assertComplete()
        observer.assertValueCount(1)
        observer.assertValue { tx ->
            val commands = tx.commands
            assertEquals(expectedTime, tx.createdTime)
            assertEquals(2, commands.size)
            var cmd = commands[0]
            if (cmd is IrohaCommand.CommandAddAssetQuantity) {
                assertEquals(expectedAmount, cmd.amount)
                assertEquals(expectedAssetId, cmd.assetId)
            } else {
                fail { "Wrong IrohaCommand type" }
            }
            cmd = commands[1]
            if (cmd is IrohaCommand.CommandTransferAsset) {
                assertEquals(creatorId, cmd.srcAccountId)
                assertEquals(expectedUserId, cmd.destAccountId)
                assertEquals(expectedAssetId, cmd.assetId)
                assertEquals(expectedFrom, cmd.description)
                assertEquals(expectedAmount, cmd.amount)
            } else {
                fail { "Wrong IrohaCommand type" }
            }

            true
        }
    }

    /**
     * @given a custodian has 100 Wei with intention to deposit 100 Wei to Notary
     * @when a custodian transfer 100 Wei to a specified wallet and specifies Iroha wallet to deposit assets
     * @then an IrohaAtomicBatch is emitted with 2 transactions:
     * 1 - SetAccountDetail with hash
     * 2 - AddAssetQuantity with 100 Wei and TransferAsset with 100 Wei to specified account id
     */
    @Test
    fun depositEthereumTest() {
        val expectedAmount = "100"
        val expectedAssetId = "ether#ethereum"
        val expectedHash = "hash"
        val expectedUserId = "from"
        val expectedFrom = "eth_from"
        val expectedTime = BigInteger.TEN

        val custodianIntention =
            SideChainEvent.PrimaryBlockChainEvent.ChainAnchoredOnPrimaryChainDeposit(
                expectedHash,
                expectedTime,
                expectedUserId,
                expectedAssetId,
                expectedAmount,
                expectedFrom
            )

        // source of events from side chains
        val obsEth = Observable.just<SideChainEvent.PrimaryBlockChainEvent>(custodianIntention)
        val notary = NotaryImpl(irohaConsumer, irohaCredential, obsEth)
        val res = notary.irohaOutput()
        checkEthereumDepositResult(
            expectedAmount,
            expectedAssetId,
            expectedUserId,
            expectedFrom,
            expectedTime,
            res
        )
    }

    /**
     * @given a custodian has 100 "XOR" ERC20 tokens with intention to deposit 100 "XOR" tokens to Notary
     * @when a custodian transfer 100 "XOR" tokens to a specified wallet and specifies Iroha wallet to deposit assets
     * @then an IrohaAtomicBatch is emitted with 2 transactions:
     * 1 - SetAccountDetail with hash
     * 2 - AddAssetQuantity with 100 "XOR" and TransferAsset with 100 "XOR" to specified account id
     */
    @Test
    fun depositEthereumTokenTest() {
        val expectedAmount = "100"
        val expectedAssetId = "xor#ethereum"
        val expectedHash = "hash"
        val expectedUserId = "from"
        val expectedFrom = "eth_from"
        val expectedTime = BigInteger.TEN
        val custodianIntention =
            SideChainEvent.PrimaryBlockChainEvent.ChainAnchoredOnPrimaryChainDeposit(
                expectedHash,
                expectedTime,
                expectedUserId,
                expectedAssetId,
                expectedAmount,
                expectedFrom
            )

        // source of events from side chains
        val obsEth = Observable.just<SideChainEvent.PrimaryBlockChainEvent>(custodianIntention)
        val notary = NotaryImpl(irohaConsumer, irohaCredential, obsEth)
        val res = notary.irohaOutput()
        checkEthereumDepositResult(
            expectedAmount,
            expectedAssetId,
            expectedUserId,
            expectedFrom,
            expectedTime,
            res
        )
    }
}
