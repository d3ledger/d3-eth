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
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.sidechain.provider.FileBasedLastReadBlockProvider
import com.d3.commons.util.createPrettyFixThreadPool
import com.d3.commons.util.createPrettyScheduledThreadPool
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
import jp.co.soramitsu.soranet.eth.sidechain.util.BasicAuthenticator
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

    private val withdrawalProofHandler = WithdrawalProofHandler(
        ethDepositConfig.notaryCredential.accountId,
        ethTokensProvider,
        ethWalletProvider,
        ethDepositConfig,
        passwordsConfig,
        irohaAPI
    )

    private val masterContractAbi = Files.readString(File(ethDepositConfig.masterContractAbiPath))

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
        return initEthChain()
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
    private fun initEthChain(): Result<Observable<SideChainEvent.PrimaryBlockChainEvent>, Exception> {
        logger.info { "Init Eth chain" }

        val builder = OkHttpClient().newBuilder()
        builder.authenticator(BasicAuthenticator(passwordsConfig))
        val web3 = Web3j.build(
            HttpService(ethDepositConfig.ethereum.url, builder.build(), false),
            JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME.toLong(),
            createPrettyScheduledThreadPool(ETH_DEPOSIT_SERVICE_NAME, "web3j")
        )

        /** List of all observable wallets */
        val ethHandler = EthChainHandler(
            web3,
            ethDepositConfig.ethMasterAddress,
            ethWalletProvider,
            ethTokensProvider,
            ethNotificationMqProducer,
            masterContractAbi
        )
        return EthChainListener(
            web3,
            BigInteger.valueOf(ethDepositConfig.ethereum.confirmationPeriod),
            ethDepositConfig.startEthereumBlock,
            FileBasedLastReadBlockProvider(ethDepositConfig.lastEthereumReadBlockFilePath),
            ethDepositConfig.ignoreStartBlock
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
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
