/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.withdrawalservice

import com.d3.chainadapter.client.RMQConfig
import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.commons.expansion.ServiceExpansion
import com.d3.commons.model.D3ErrorException
import com.d3.commons.model.IrohaCredential
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.FEE_DESCRIPTION
import com.d3.commons.sidechain.iroha.IrohaChainHandler
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.createPrettyFixThreadPool
import com.d3.commons.util.createPrettySingleThreadPool
import integration.eth.config.EthereumPasswords
import com.d3.eth.provider.EthTokensProviderImpl
import com.d3.eth.vacuum.RelayVacuumConfig
import com.d3.eth.withdrawal.consumer.EthConsumer
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging
import kotlin.system.exitProcess

// TODO restore these parameters in configs
const val master_address_env = "WITHDRAWAL_ETHMASTERWALLET"

/**
 * @param withdrawalConfig - configuration for withdrawal service
 * @param withdrawalEthereumPasswords - passwords for ethereum withdrawal account
 */
class WithdrawalServiceInitialization(
    private val withdrawalConfig: WithdrawalServiceConfig,
    private val credential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val withdrawalEthereumPasswords: EthereumPasswords,
    relayVacuumConfig: RelayVacuumConfig,
    rmqConfig: RMQConfig
) {

    private val chainListener = ReliableIrohaChainListener(
        rmqConfig,
        withdrawalConfig.ethIrohaWithdrawalQueue,
        createPrettySingleThreadPool(ETH_WITHDRAWAL_SERVICE_NAME, "rmq-consumer")
    )

    private val queryHelper by lazy {
        IrohaQueryHelperImpl(
            irohaAPI,
            credential.accountId,
            credential.keyPair
        )
    }

    private val tokensProvider = EthTokensProviderImpl(
        queryHelper,
        withdrawalConfig.ethAnchoredTokenStorageAccount,
        withdrawalConfig.ethAnchoredTokenSetterAccount,
        withdrawalConfig.irohaAnchoredTokenStorageAccount,
        withdrawalConfig.irohaAnchoredTokenSetterAccount
    )

    private val notaryPeerListProvider = NotaryPeerListProviderImpl(
        queryHelper,
        withdrawalConfig.notaryListStorageAccount,
        withdrawalConfig.notaryListSetterAccount
    )

    val ethConsumer = EthConsumer(
        withdrawalConfig.ethereum,
        withdrawalEthereumPasswords,
        relayVacuumConfig
    )

    private val expansionService = ServiceExpansion(
        withdrawalConfig.expansionTriggerAccount,
        withdrawalConfig.expansionTriggerCreatorAccountId,
        irohaAPI
    )

    private val proofCollector =
        ProofCollector(queryHelper, withdrawalConfig, tokensProvider, notaryPeerListProvider)

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<SideChainEvent.IrohaEvent>, Exception> {
        logger.info { "Init Iroha chain listener" }
        return chainListener.getBlockObservable().map { observable ->
            observable.flatMapIterable { (block, _) ->
                EthereumWithdrawalExpansionStrategy(
                    withdrawalConfig.ethereum,
                    withdrawalEthereumPasswords,
                    withdrawalConfig.ethMasterAddress,
                    expansionService,
                    proofCollector
                ).filterAndExpand(block)
                IrohaChainHandler(
                    credential.accountId,
                    FEE_DESCRIPTION
                ).parseBlock(block)
            }
        }
    }

    /**
     * Init Withdrawal Service
     */
    private fun initWithdrawalService(inputEvents: Observable<SideChainEvent.IrohaEvent>): WithdrawalService {
        return WithdrawalServiceImpl(
            withdrawalConfig,
            credential,
            irohaAPI,
            queryHelper,
            inputEvents,
            tokensProvider,
            proofCollector
        )
    }

    private fun initEthConsumer(withdrawalService: WithdrawalService): Result<Unit, Exception> {
        logger.info { "Init Ether withdrawal consumer" }

        return Result.of {
            withdrawalService.output()
                .observeOn(
                    Schedulers.from(
                        createPrettyFixThreadPool(
                            ETH_WITHDRAWAL_SERVICE_NAME,
                            "event-handler"
                        )
                    )
                )
                .subscribe(
                    { res ->
                        res.map { withdrawalEvents ->
                            withdrawalEvents.forEach { event ->
                                try {
                                    val transactionReceipt = ethConsumer.consume(event)
                                    if (transactionReceipt == null || transactionReceipt.status == FAILED_STATUS) {
                                        throw D3ErrorException.fatal(
                                            WITHDRAWAL_OPERATION,
                                            "Ethereum transaction has failed"
                                        )
                                    } else {
                                        withdrawalService.finalizeWithdrawal(event)
                                            .failure { ex ->
                                                throw D3ErrorException.fatal(
                                                    WITHDRAWAL_OPERATION,
                                                    "Cannot finalize withdrawal"
                                                )
                                            }
                                    }
                                } catch (e: Exception) {
                                    logger.error("Withdrawal error, perform rollback", e)
                                    withdrawalService.returnIrohaAssets(event).failure {
                                        logger.error("Rollback error", it)
                                    }
                                }
                            }
                        }.failure { ex ->
                            logger.error("Cannot consume withdrawal event", ex)
                        }
                        //TODO call ack()
                    }, { ex ->
                        logger.error("Withdrawal observable error", ex)
                        exitProcess(1)
                    }
                )
            Unit
        }
    }

    fun init(): Result<Unit, Exception> {
        logger.info {
            "Start withdrawal service init with iroha at ${withdrawalConfig.iroha.hostname}:${withdrawalConfig.iroha.port}"
        }
        return initIrohaChain()
            .map { initWithdrawalService(it) }
            .flatMap { initEthConsumer(it) }
            .map { WithdrawalServiceEndpoint(withdrawalConfig.port) }
            .flatMap { chainListener.listen() }
    }

    /**
     * Logger
     */
    companion object : KLogging() {
        private const val FAILED_STATUS = "0x0"
    }
}
