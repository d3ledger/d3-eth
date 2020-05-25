/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge

import com.d3.chainadapter.client.RMQConfig
import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.commons.expansion.ServiceExpansion
import com.d3.commons.model.IrohaCredential
import com.d3.commons.notary.Notary
import com.d3.commons.notary.NotaryImpl
import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.sidechain.provider.FileBasedLastReadBlockProvider
import com.d3.commons.util.createPrettyFixThreadPool
import com.d3.commons.util.createPrettySingleThreadPool
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.soranet.eth.bridge.endpoint.EthAddPeerStrategyImpl
import jp.co.soramitsu.soranet.eth.bridge.endpoint.EthServerEndpoint
import jp.co.soramitsu.soranet.eth.config.EthereumPasswords
import jp.co.soramitsu.soranet.eth.mq.EthNotificationMqProducer
import jp.co.soramitsu.soranet.eth.provider.EthAddressProvider
import jp.co.soramitsu.soranet.eth.provider.EthTokensProvider
import jp.co.soramitsu.soranet.eth.registration.wallet.EthereumWalletRegistrationHandler
import jp.co.soramitsu.soranet.eth.sidechain.EthChainHandler
import jp.co.soramitsu.soranet.eth.sidechain.EthChainListener
import jp.co.soramitsu.soranet.eth.sidechain.WithdrawalLimitProvider
import jp.co.soramitsu.soranet.eth.sidechain.util.BasicAuthenticator
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
import jp.co.soramitsu.soranet.eth.sidechain.util.ENDPOINT_ETHEREUM
import mu.KLogging
import okhttp3.OkHttpClient
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.JsonRpc2_0Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Files
import java.io.File
import java.math.BigInteger
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.exitProcess

/**
 * Class for deposit instantiation
 * @param ethWalletProvider - provides with white list of ethereum wallets
 * @param ethTokensProvider - provides with white list of ethereum ERC20 tokens
 * @param registrationHandler - iroha-based wallet registration handler
 */
class EthDepositInitialization(
    private val notaryCredential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val ethDepositConfig: EthDepositConfig,
    private val passwordsConfig: EthereumPasswords,
    rmqConfig: RMQConfig,
    private val ethWalletProvider: EthAddressProvider,
    private val ethTokensProvider: EthTokensProvider,
    private val registrationHandler: EthereumWalletRegistrationHandler
) {
    private var ecKeyPair: ECKeyPair = WalletUtils.loadCredentials(
        passwordsConfig.credentialsPassword,
        passwordsConfig.credentialsPath
    ).ecKeyPair

    private val queryHelper = IrohaQueryHelperImpl(irohaAPI, notaryCredential)

    private val irohaChainListener = ReliableIrohaChainListener(
        rmqConfig,
        ethDepositConfig.ethIrohaDepositQueue,
        consumerExecutorService = createPrettySingleThreadPool(
            ETH_DEPOSIT_SERVICE_NAME,
            "rmq-consumer"
        )
    )

    private val ethNotificationMqProducer = EthNotificationMqProducer(rmqConfig)

    private val expansionService = ServiceExpansion(
        ethDepositConfig.expansionTriggerAccount,
        ethDepositConfig.expansionTriggerCreatorAccountId,
        irohaAPI
    )

    private val irohaExpansionStrategy = EthereumBridgeIrohaExpansionStrategy(
        notaryCredential,
        irohaAPI,
        ethDepositConfig,
        expansionService
    )

    private val proofCollector = ProofCollector(
        NotaryPeerListProviderImpl(
            queryHelper,
            ethDepositConfig.notaryListStorageAccount,
            notaryCredential.accountId
        )
    )

    private val ethExpansionStrategy = EthereumBridgeEthExpansionStrategy(
        ethDepositConfig.ethereum,
        passwordsConfig,
        ethDepositConfig.ethMasterAddress,
        expansionService,
        proofCollector
    )

    private val deployHelper = DeployHelper(ethDepositConfig.ethereum, passwordsConfig)

    private val withdrawalQueryHelper = IrohaQueryHelperImpl(irohaAPI, ethDepositConfig.withdrawalCredential)

    private val withdrawalIrohaConsumer = IrohaConsumerImpl(
        IrohaCredential(ethDepositConfig.withdrawalCredential),
        irohaAPI
    )

    private val withdrawalProofHandler = WithdrawalProofHandler(
        ethDepositConfig.notaryCredential.accountId,
        ethTokensProvider,
        ethWalletProvider,
        deployHelper,
        withdrawalQueryHelper,
        withdrawalIrohaConsumer,
        passwordsConfig
    )

    private val masterContractAbi = Files.readString(File(ethDepositConfig.masterContractAbiPath))

    private val isHealthy = AtomicBoolean(true)

    private val withdrawalLimitsNextUpdateTimeHolder = AtomicLong()

    private val tokenSmartContract = deployHelper.loadTokenSmartContract(ethDepositConfig.xorTokenAddress)

    init {
        logger.info {
            "Init deposit ethAddress=" +
                    WalletUtils.loadCredentials(
                        passwordsConfig.credentialsPassword,
                        passwordsConfig.credentialsPath
                    ).address
        }
    }

    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Eth deposit initialization" }
        return initEthChain(isHealthy)
            .map { ethEvent -> initNotary(ethEvent) }
            .flatMap { notary -> notary.initIrohaConsumer() }
            .flatMap { irohaChainListener.getBlockObservable() }
            .flatMap { irohaObservable ->
                irohaObservable
                    .observeOn(
                        Schedulers.from(
                            createPrettyFixThreadPool(
                                ETH_DEPOSIT_SERVICE_NAME,
                                "event-handler"
                            )
                        )
                    ).subscribe(
                        { (block, _) ->
                            irohaExpansionStrategy.filterAndExpand(block)
                            ethExpansionStrategy.filterAndExpand(block)
                            registrationHandler.filterAndRegister(block)
                            ethTokensProvider.filterAndExpand(block)
                            withdrawalProofHandler.proceedBlock(block)
                        }, { ex ->
                            logger.error("Withdrawal observable error", ex)
                            exitProcess(1)
                        }
                    )
                irohaChainListener.listen()
            }
            .map { initRefund() }
    }

    /**
     * Init Ethereum chain listener
     * @return Observable on Ethereum sidechain events
     */
    private fun initEthChain(customHealthIndicator: AtomicBoolean): Result<Observable<SideChainEvent.PrimaryBlockChainEvent>, Exception> {
        logger.info { "Init Eth chain" }

        val builder = OkHttpClient().newBuilder()
        builder.authenticator(BasicAuthenticator(passwordsConfig))

        val web3jExecutorService = UnwrappingExceptionsScheduledThreadPoolExecutor(isHealthy)

        val web3 = Web3j.build(
            HttpService(ethDepositConfig.ethereum.url, builder.build(), false),
            JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME.toLong(),
            web3jExecutorService
        )

        val withdrawalLimitProvider = WithdrawalLimitProvider(
            queryHelper,
            withdrawalIrohaConsumer,
            withdrawalLimitsNextUpdateTimeHolder,
            ethDepositConfig.withdrawalLimitStorageAccount,
            XOR_LIMITS_TIME_KEY,
            XOR_LIMITS_VALUE_KEY,
            tokenSmartContract,
            ethDepositConfig.xorExchangeContractAddress
        )

        /** List of all observable wallets */
        val ethHandler = EthChainHandler(
            web3,
            ethDepositConfig.ethMasterAddress,
            ethWalletProvider,
            ethTokensProvider,
            ethNotificationMqProducer,
            masterContractAbi,
            withdrawalLimitProvider
        )
        return EthChainListener(
            web3,
            BigInteger.valueOf(ethDepositConfig.ethereum.confirmationPeriod),
            ethDepositConfig.startEthereumBlock,
            FileBasedLastReadBlockProvider(ethDepositConfig.lastEthereumReadBlockFilePath),
            ethDepositConfig.ignoreStartBlock,
            customHealthIndicator
        ).getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { ethHandler.parseBlock(it) }
            }
    }

    /**
     * Init Notary
     */
    private fun initNotary(
        ethEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>
    ): Notary {
        logger.info { "Init ethereum notary" }
        return NotaryImpl(notaryCredential, irohaAPI, ethEvents)
    }

    /**
     * Init refund notary.endpoint
     */
    private fun initRefund() {
        logger.info { "Init Refund endpoint" }
        val serverBundle =
            ServerInitializationBundle(ethDepositConfig.refund.port, ENDPOINT_ETHEREUM)
        EthServerEndpoint(
            serverBundle,
            EthAddPeerStrategyImpl(
                queryHelper,
                ecKeyPair,
                ethDepositConfig.expansionTriggerAccount,
                ethDepositConfig.expansionTriggerCreatorAccountId
            )
        ) { isHealthy.get() }
    }

    /**
     * Logger
     */
    companion object : KLogging() {
        private fun namedWithUnknownExceptionHandlingThreadFactory(): ThreadFactory {
            return object : ThreadFactory {
                private val threadCounter = AtomicInteger(0)
                override fun newThread(runnable: Runnable): Thread {
                    val thread = Executors.defaultThreadFactory().newThread(runnable)
                    thread.name =
                        "$ETH_DEPOSIT_SERVICE_NAME:web3j:th-${threadCounter.getAndIncrement()}:id-${thread.id}"
                    thread.uncaughtExceptionHandler = CriticalUncaughtExceptionHandler
                    return thread
                }
            }
        }
    }

    // TODO move to validator-commons ?
    internal class UnwrappingExceptionsScheduledThreadPoolExecutor(private val health: AtomicBoolean) :
        ScheduledThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            namedWithUnknownExceptionHandlingThreadFactory()
        ) {
        override fun afterExecute(r: Runnable?, t: Throwable?) {
            super.afterExecute(r, t)
            var toThrow = t
            if (toThrow == null && r is Future<*>) {
                if (r.isDone) {
                    try {
                        r.get()
                    } catch (t: Throwable) {
                        toThrow = t
                    }
                }
            }
            if (toThrow != null) {
                health.set(false)
            }
        }
    }

    internal object CriticalUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, t: Throwable) {
            logger.error("Encountered error in critical thread pool", t)
            exitProcess(1)
        }
    }
}
