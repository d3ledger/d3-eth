/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.helper

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.config.loadConfigs
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.expansion.ExpansionDetails
import com.d3.commons.expansion.ExpansionUtils
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.provider.FileBasedLastReadBlockProvider
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.getRandomString
import com.d3.commons.util.irohaEscape
import com.d3.commons.util.irohaUnEscape
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.success
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import integration.helper.D3_DOMAIN
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.soranet.eth.bridge.ETH_WITHDRAWAL_PROOF_DOMAIN
import jp.co.soramitsu.soranet.eth.bridge.EthDepositConfig
import jp.co.soramitsu.soranet.eth.bridge.WithdrawalProof
import jp.co.soramitsu.soranet.eth.bridge.executeDeposit
import jp.co.soramitsu.soranet.eth.config.EthereumPasswords
import jp.co.soramitsu.soranet.eth.constants.ETH_MASTER_ADDRESS_KEY
import jp.co.soramitsu.soranet.eth.mq.EthNotificationMqProducer.Companion.EVENTS_QUEUE_NAME
import jp.co.soramitsu.soranet.eth.provider.ETH_DOMAIN
import jp.co.soramitsu.soranet.eth.provider.ETH_WALLET
import jp.co.soramitsu.soranet.eth.provider.EthAddressProviderIrohaImpl
import jp.co.soramitsu.soranet.eth.provider.EthTokensProviderImpl
import jp.co.soramitsu.soranet.eth.registration.EthRegistrationConfig
import jp.co.soramitsu.soranet.eth.registration.wallet.ETH_REGISTRATION_KEY
import jp.co.soramitsu.soranet.eth.registration.wallet.createRegistrationProof
import jp.co.soramitsu.soranet.eth.sidechain.EthChainListener
import jp.co.soramitsu.soranet.eth.token.EthTokenInfo
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.WalletUtils
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.security.KeyPair
import java.util.*

/**
 * Utility class that makes testing more comfortable.
 * Class lazily creates new master contract in Ethereum and master account in Iroha.
 */
object EthIntegrationHelperUtil : IrohaIntegrationHelperUtil() {
    private val logger = logger()
    val gson = GsonInstance.get()

    val ethTestConfig =
        loadConfigs("test", TestEthereumConfig::class.java, "/test.properties").get()

    /** Ethereum utils */
    private val contractTestHelper = ContractTestHelper()

    override val accountHelper = EthereumAccountHelper(irohaAPI)

    private val tokenProviderIrohaConsumer = IrohaConsumerImpl(accountHelper.tokenSetterAccount, irohaAPI)

    /** Iroha consumer to set Ethereum contract addresses in Iroha */
    private val ethAddressWriterIrohaConsumer = IrohaConsumerImpl(accountHelper.ethAddressesWriter, irohaAPI)

    /** New master ETH master contract*/
    val masterContract by lazy {
        val contract = contractTestHelper.master
        ModelUtil.setAccountDetail(
            ethAddressWriterIrohaConsumer,
            accountHelper.ethAddressesStorage.accountId,
            ETH_MASTER_ADDRESS_KEY,
            contract.contractAddress
        )
        logger.info("master eth wallet ${contract.contractAddress} was deployed ")
        contract
    }

    override val configHelper = EthConfigHelper(
        accountHelper,
        masterContract.contractAddress
    )

    var ethDepositConfig: EthDepositConfig = configHelper.createEthDepositConfig()

    val ethRegistrationConfig by lazy { configHelper.createEthRegistrationConfig() }

    val ethListener = EthChainListener(
        contractTestHelper.deployHelper.web3,
        BigInteger.valueOf(ethTestConfig.ethereum.confirmationPeriod),
        BigInteger.ZERO,
        FileBasedLastReadBlockProvider(configHelper.lastEthereumReadBlockFilePath),
        false
    )

    /** Provider that is used to store/fetch tokens*/
    fun ethTokensProvider() = EthTokensProviderImpl(
        queryHelper,
        ethDepositConfig.ethAnchoredTokenStorageAccount,
        ethDepositConfig.ethAnchoredTokenSetterAccount,
        ethDepositConfig.irohaAnchoredTokenStorageAccount,
        ethDepositConfig.irohaAnchoredTokenSetterAccount
    )

    /** Provider of ETH wallets created by registrationAccount*/
    fun ethWalletsProvider() = EthAddressProviderIrohaImpl(
        queryHelper,
        ethDepositConfig.ethereumWalletStorageAccount,
        ethDepositConfig.ethereumWalletSetterAccount,
        ETH_WALLET
    )

    private val rmqEvents = Collections.synchronizedList(ArrayList<JSONObject>())

    private val consumerTags = ArrayList<String>()
    private val connectionFactory = ConnectionFactory()
    private var connection: Connection? = null
    private var channel: Channel? = null

    /**
     * Returns the last posted RMQ event
     */
    fun getLastRmqEvent(): JSONObject = rmqEvents[rmqEvents.size - 1]

    /**
     * Get relay address of an account.
     */
    fun getWalletByAccount(clientId: String): Optional<String> {
        return ethWalletsProvider().getAddressByAccountId(clientId).get()
    }

    /**
     * Returns ETH balance for a given address
     */
    fun getEthBalance(address: String): BigInteger {
        return contractTestHelper.getETHBalance(address)
    }

    /**
     * Deploys randomly named ERC20 token
     * @return pair (tokenName, tokenAddress)
     */
    fun deployRandomERC20Token(precision: Int = 0): Pair<EthTokenInfo, String> {
        val name = String.getRandomString(5)
        return Pair(
            EthTokenInfo(name, ETH_DOMAIN, precision),
            deployERC20Token(
                name,
                precision
            )
        )
    }

    /**
     * Deploy ERC20 token and register it to the notary system:
     * - create asset in Iroha
     * - add to Token provider service
     * - add to master contract
     * @return token name in iroha and address of ERC20 smart contract
     */
    fun deployERC20Token(name: String, precision: Int): String {
        logger.info { "create $name ERC20 token" }
        val tokenAddress =
            contractTestHelper.deployHelper.deployERC20TokenSmartContract().contractAddress
        addEthAnchoredERC20Token(
            tokenAddress,
            EthTokenInfo(name, ETH_DOMAIN, precision)
        )
        masterContract.addToken(tokenAddress).send()
        return tokenAddress
    }

    /**
     * Deploys smart contract which always fails. Use only if you know why you need it.
     * @return contract address
     */
    fun deployFailer(): String {
        val address = contractTestHelper.deployFailer()
        logger.info { "Created Failer contract at $address" }
        return address
    }

    /**
     * Add token to Iroha token provider
     * @param tokenAddress - token ERC20 smart contract address
     * @param tokenInfo - token info
     */
    fun addIrohaAnchoredERC20Token(tokenAddress: String, tokenInfo: EthTokenInfo) {
        ModelUtil.createAsset(irohaConsumer, tokenInfo.name, tokenInfo.domain, tokenInfo.precision)
        ModelUtil.setAccountDetail(
            tokenProviderIrohaConsumer,
            accountHelper.irohaAnchoredTokenStorageAccount.accountId,
            tokenAddress,
            "${tokenInfo.name}#${tokenInfo.domain}"
        ).success {
            logger.info { "token ${tokenInfo.name}#${tokenInfo.domain} was added to ${accountHelper.irohaAnchoredTokenStorageAccount.accountId} by ${tokenProviderIrohaConsumer.creator}" }
        }
    }

    /**
     * Add token to Iroha token provider
     * @param tokenAddress - token ERC20 smart contract address
     * @param tokenInfo - token info
     */
    fun addEthAnchoredERC20Token(tokenAddress: String, tokenInfo: EthTokenInfo) {
        ModelUtil.createAsset(irohaConsumer, tokenInfo.name, tokenInfo.domain, tokenInfo.precision)
        ModelUtil.setAccountDetail(
            tokenProviderIrohaConsumer,
            accountHelper.ethAnchoredTokenStorageAccount.accountId,
            tokenAddress,
            "${tokenInfo.name}#${tokenInfo.domain}"
        ).success {
            logger.info { "token ${tokenInfo.name}#${tokenInfo.domain} was added to ${accountHelper.ethAnchoredTokenStorageAccount.accountId} by ${tokenProviderIrohaConsumer.creator}" }
        }
    }

    /**
     * Transfer [amount] ERC20 deployed at [contractAddress] tokens to [toAddress]
     * @param contractAddress - address of ERC20 contract
     * @param amount - amount to transfer
     * @param toAddress - destination address
     */
    fun sendERC20Token(contractAddress: String, amount: BigInteger, toAddress: String) {
        logger.info { "send ERC20 $contractAddress $amount to $toAddress" }
        contractTestHelper.sendERC20Token(contractAddress, amount, toAddress)
    }

    /**
     * Transfer [amount] ERC20 deployed at [contractAddress] tokens to [toAddress]
     * @param contractAddress - address of ERC20 contract
     * @param amount - amount to transfer
     * @param toAddress - destination address
     * @param credentials - destination address
     */
    fun sendERC20Token(
        contractAddress: String,
        amount: BigInteger,
        toAddress: String,
        credentials: Credentials
    ) {
        logger.info { "send ERC20 $contractAddress $amount from ${credentials.address} to $toAddress" }
        contractTestHelper.sendERC20Token(contractAddress, amount, toAddress, credentials)
    }

    /**
     * Get [whoAddress] balance of ERC20 tokens with [contractAddress]
     * @param contractAddress - address of ERC20 smart contract
     * @param whoAddress - address of client
     */
    fun getERC20TokenBalance(contractAddress: String, whoAddress: String): BigInteger {
        return contractTestHelper.getERC20TokenBalance(contractAddress, whoAddress)
    }

    /**
     * Returns master contract ETH balance
     */
    fun getMasterEthBalance(): BigInteger {
        return getEthBalance(
            masterContract.contractAddress
        )
    }

    /**
     * Waits for exactly one Ethereum block
     */
    fun waitOneEtherBlock() {
        runBlocking {
            logger.info { "Waiting for Ethereum block. Last block ${ethListener.lastBlockNumber}" }
            val block = ethListener.getBlock()
            logger.info { "Waiting for Ethereum block ${block.block.number} is over." }
        }
    }

    /**
     * Send ETH with given amount to the destination address
     */
    fun sendEth(amount: BigInteger, to: String) {
        logger.info { "send $amount Wei to $to " }
        contractTestHelper.sendEthereum(amount, to)
    }

    /**
     * Send ETH with given amount to the destination address from provided credentials account
     */
    fun sendEth(amount: BigInteger, to: String, credentials: Credentials) {
        logger.info { "send $amount Wei from ${credentials.address} to $to " }
        contractTestHelper.sendEthereum(amount, to, credentials)
    }

    /**
     * Send HTTP POST request to registration service to register user
     * @param name - user name
     * @param pubkey - user public key in hexidecimal representation
     * @param port - port of registration service
     */
    fun sendRegistrationRequest(
        name: String,
        pubkey: String,
        port: Int
    ): khttp.responses.Response {
        return khttp.post(
            "http://127.0.0.1:$port/users",
            data = mapOf(
                "name" to name,
                "pubkey" to pubkey,
                "domain" to D3_DOMAIN
            )
        )
    }

    /**
     * Send transaction from client to trigger Ethereum registration.
     * The transaction contains ethereum address proof from client.
     * @param clientId - Iroha client id
     * @param irohaKeyPair - iroha client keypair
     * @param ethereumKeyPair - ethereum client keypair
     * @return result of tx sent with hash
     */
    fun registerEthereumWallet(
        clientId: String,
        irohaKeyPair: KeyPair,
        ethereumKeyPair: ECKeyPair
    ): Result<String, Exception> {
        val address = "0x${Keys.getAddress(ethereumKeyPair.publicKey)}"
        logger.info { "Send request to register wallet $address for client $clientId" }
        // register in Ethereum
        val clientIrohaConsumer = IrohaConsumerImpl(
            IrohaCredential(clientId, irohaKeyPair),
            irohaAPI
        )
        val signature = createRegistrationProof(ethereumKeyPair)
        return setAccountDetail(
            clientIrohaConsumer,
            accountHelper.registrationAccount.accountId,
            ETH_REGISTRATION_KEY,
            signature.toJson().irohaEscape(),
            quorum = 2
        )
    }


    /**
     * Run Ethereum notary process
     */
    fun runEthDeposit(
        ethereumPasswords: EthereumPasswords = configHelper.ethPasswordConfig,
        ethDepositConfig: EthDepositConfig = this.ethDepositConfig,
        rmqConfig: RMQConfig = loadRawLocalConfigs(
            "rmq",
            RMQConfig::class.java, "rmq.properties"
        ),
        registrationConfig: EthRegistrationConfig = configHelper.createEthRegistrationConfig()

    ) {
        val name = String.getRandomString(9)
        val address = "http://localhost:${ethDepositConfig.refund.port}"
        addNotary(name, address)

        executeDeposit(ethereumPasswords, ethDepositConfig, rmqConfig, registrationConfig)

        logger.info { "Notary $name is started on $address" }
    }

    /**
     * Run RMQ notifications consumer
     */
    fun runEthNotificationRmqConsumer(
        rmqConfig: RMQConfig = loadRawLocalConfigs(
            "rmq",
            RMQConfig::class.java, "rmq.properties"
        )
    ) {
        connectionFactory.host = rmqConfig.host
        connectionFactory.port = rmqConfig.port
        connection = connectionFactory.newConnection()
        channel = connection!!.createChannel()
        val arguments = hashMapOf(
            // enable deduplication
            Pair("x-message-deduplication", true),
            // save deduplication data on disk rather that memory
            Pair("x-cache-persistence", "disk"),
            // save deduplication data 1 day
            Pair("x-cache-ttl", 60_000 * 60 * 24)
        )
        channel!!.queueDeclare(EVENTS_QUEUE_NAME, true, false, false, arguments)
        consumerTags.add(
            channel!!.basicConsume(
                EVENTS_QUEUE_NAME,
                true,
                { _: String, delivery: Delivery ->
                    run {
                        val element = JSONObject(String(delivery.body))
                        logger.info("Consumed event $element from notifications MQ")
                        rmqEvents.add(element)
                    }
                },
                { _ -> }
            )
        )
    }

    /**
     * Get list of all notary endpoints
     */
    fun getNotaries(): Map<String, String> {
        return getAccountDetails(
            accountHelper.notaryListStorageAccount.accountId,
            accountHelper.notaryListSetterAccount.accountId
        )
    }

    /**
     * Trigger Ethereum node expansion.
     * @param accountId - account to expand
     * @param publicKey - new public key to add
     * @param quorum - new quorum
     * @param ethereumAddress - Ethereum address of new node for contract checks
     */
    fun triggerExpansion(
        accountId: String,
        publicKey: String,
        quorum: Int,
        ethereumAddress: String,
        notaryName: String,
        notaryEndpointAddress: String
    ): String {
        logger.info {
            "Send expansion transaction publicKey=${publicKey} " +
                    "eth_address=${ethereumAddress}, " +
                    "notary_endpoint=${notaryEndpointAddress}"
        }
        val expansionDetails = ExpansionDetails(
            accountId,
            publicKey,
            quorum,
            mapOf(
                "eth_address" to ethereumAddress,
                "notary_name" to notaryName,
                "notary_endpoint" to notaryEndpointAddress
            )
        )

        IrohaConsumerImpl(
            accountHelper.superuserAccount,
            irohaAPI
        ).send(
            ExpansionUtils.createExpansionTriggerTx(
                accountHelper.superuserAccount.accountId,
                expansionDetails,
                accountHelper.expansionTriggerAccount.accountId
            )
        ).fold(
            { hash ->
                logger.info { "Expansion trigger transaction with hash $hash sent" }
                return hash
            },
            { ex -> throw ex }
        )
    }

    /**
     * Withdraw to wallet by provided proofs
     * @param txHash - hash of iroha initial tx
     */
    fun withdrawToWallet(
        txHash: String
    ) {
        val tx = queryHelper.getSingleTransaction(txHash).get()
            .payload.reducedPayload.commandsList
            .filter { it.hasTransferAsset() }
            .first { WalletUtils.isValidAddress(it.transferAsset.description) }
            .transferAsset
        val assetId = tx.assetId
        val amount = tx.amount
        val beneficiary = tx.description

        val ethTokensProvider = ethTokensProvider()
        val ethTokenAddress = ethTokensProvider.getTokenAddress(assetId).get()
        val tokenPrecision = ethTokensProvider.getTokenPrecision(assetId).get()
        val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(tokenPrecision).toBigInteger()

        // signatures
        val vv = ArrayList<BigInteger>()
        val rr = ArrayList<ByteArray>()
        val ss = ArrayList<ByteArray>()

        val proofAccountId = "${txHash.take(32).toLowerCase()}@$ETH_WITHDRAWAL_PROOF_DOMAIN"
        queryHelper.getAccountDetails(proofAccountId, accountHelper.withdrawalAccount.accountId).get()
            .map { (ethNotaryAddress, withdrawalProofJson) ->
                val withdrawalProof =
                    gson.fromJson(
                        withdrawalProofJson.irohaUnEscape(),
                        WithdrawalProof::class.java
                    )
                vv.add(BigInteger(withdrawalProof.signature.v, 16))
                rr.add(Numeric.hexStringToByteArray(withdrawalProof.signature.r))
                ss.add(Numeric.hexStringToByteArray(withdrawalProof.signature.s))
            }

        if (vv.size == 0) {
            throw Exception("No proofs for withdrawal")
        }
        val transactionResponse =
            if (ethTokensProvider.isIrohaAnchored(assetId).get())
                contractTestHelper.master.mintTokensByPeers(
                    ethTokenAddress,
                    decimalAmount,
                    beneficiary,
                    Numeric.hexStringToByteArray(txHash),
                    vv,
                    rr,
                    ss,
                    beneficiary
                ).send()
            else
                contractTestHelper.master.withdraw(
                    ethTokenAddress,
                    decimalAmount,
                    beneficiary,
                    Numeric.hexStringToByteArray(txHash),
                    vv,
                    rr,
                    ss,
                    beneficiary
                ).send()

        val ethTransaction = contractTestHelper.deployHelper.web3
            .ethGetTransactionByHash(transactionResponse.transactionHash).send()
        logger.info { "Gas used: ${ethTransaction.transaction.get().gas}" }
        logger.info { "Gas price: ${ethTransaction.transaction.get().gasPrice}" }
        logger.info { "Tx input hash: $txHash" }
        logger.info { "Tx Input: ${ethTransaction.transaction.get().input}" }
    }
}
