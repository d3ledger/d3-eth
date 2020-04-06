/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.config.loadConfigs
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.expansion.ExpansionDetails
import com.d3.commons.expansion.ExpansionUtils
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.sidechain.provider.FileBasedLastReadBlockProvider
import com.d3.commons.util.*
import com.d3.eth.constants.ETH_MASTER_ADDRESS_KEY
import com.d3.eth.constants.ETH_RELAY_REGISTRY_KEY
import com.d3.eth.deposit.ETH_WITHDRAWAL_PROOF_DOMAIN
import com.d3.eth.deposit.EthDepositConfig
import com.d3.eth.deposit.WithdrawalProof
import com.d3.eth.deposit.executeDeposit
import com.d3.eth.mq.EthNotificationMqProducer.Companion.EVENTS_QUEUE_NAME
import com.d3.eth.provider.*
import com.d3.eth.registration.EthRegistrationConfig
import com.d3.eth.registration.EthRegistrationStrategyImpl
import com.d3.eth.registration.executeRegistration
import com.d3.eth.registration.relay.RelayRegistration
import com.d3.eth.registration.wallet.ETH_REGISTRATION_KEY
import com.d3.eth.registration.wallet.createRegistrationProof
import com.d3.eth.sidechain.EthChainListener
import com.d3.eth.token.EthTokenInfo
import com.d3.eth.vacuum.RelayVacuumConfig
import com.d3.eth.withdrawal.withdrawalservice.WithdrawalServiceConfig
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.success
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import integration.eth.config.EthereumPasswords
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.json.JSONObject
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
class EthIntegrationHelperUtil : IrohaIntegrationHelperUtil() {
    val gson = GsonInstance.get()

    val ethTestConfig =
        loadConfigs("test", TestEthereumConfig::class.java, "/test.properties").get()

    override val accountHelper by lazy { EthereumAccountHelper(irohaAPI) }

    /** Ethereum utils */
    private val contractTestHelper by lazy { ContractTestHelper() }

    private val tokenProviderIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.tokenSetterAccount, irohaAPI)
    }

    val tokensProvider = EthTokensProviderImpl(
        queryHelper,
        accountHelper.ethAnchoredTokenStorageAccount.accountId,
        accountHelper.tokenSetterAccount.accountId,
        accountHelper.irohaAnchoredTokenStorageAccount.accountId,
        accountHelper.tokenSetterAccount.accountId
    )

    /** Iroha consumer to set Ethereum contract addresses in Iroha */
    private val ethAddressWriterIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.ethAddressesWriter, irohaAPI)
    }

    val relayRegistryContract by lazy {
        val contract = contractTestHelper.relayRegistry
        ModelUtil.setAccountDetail(
            ethAddressWriterIrohaConsumer,
            accountHelper.ethAddressesStorage.accountId,
            ETH_RELAY_REGISTRY_KEY,
            contract.contractAddress
        )
        logger.info { "relay registry eth wallet ${contract.contractAddress} was deployed" }
        contract
    }

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

    override val configHelper by lazy {
        EthConfigHelper(
            accountHelper,
            relayRegistryContract.contractAddress,
            masterContract.contractAddress,
            contractTestHelper.relayImplementation.contractAddress
        )
    }

    val ethRegistrationConfig by lazy { configHelper.createEthRegistrationConfig() }

    val ethListener = EthChainListener(
        contractTestHelper.deployHelper.web3,
        BigInteger.valueOf(ethTestConfig.ethereum.confirmationPeriod),
        BigInteger.ZERO,
        FileBasedLastReadBlockProvider(configHelper.lastEthereumReadBlockFilePath),
        false
    )

    /** Provider that is used to store/fetch tokens*/
    val ethTokensProvider by lazy {
        EthTokensProviderImpl(
            queryHelper,
            accountHelper.ethAnchoredTokenStorageAccount.accountId,
            accountHelper.tokenSetterAccount.accountId,
            accountHelper.irohaAnchoredTokenStorageAccount.accountId,
            accountHelper.tokenSetterAccount.accountId
        )
    }

    private val registrationQueryHelper = IrohaQueryHelperImpl(
        irohaAPI,
        accountHelper.registrationAccount.accountId,
        accountHelper.registrationAccount.keyPair
    )

    /** Provider that is used to get free registered relays*/
    private val ethFreeRelayProvider by lazy {
        EthFreeRelayProvider(
            registrationQueryHelper,
            accountHelper.ethereumRelayStorageAccount.accountId,
            accountHelper.registrationAccount.accountId
        )
    }

    /** Provider of ETH wallets created by registrationAccount*/
    private val ethRelayProvider by lazy {
        EthAddressProviderIrohaImpl(
            registrationQueryHelper,
            accountHelper.ethereumRelayStorageAccount.accountId,
            accountHelper.registrationAccount.accountId,
            ETH_RELAY
        )
    }

    private val ethRegistrationStrategy by lazy {
        EthRegistrationStrategyImpl(
            ethFreeRelayProvider,
            ethRelayProvider,
            registrationConsumer,
            accountHelper.ethereumRelayStorageAccount.accountId
        )
    }

    private val relayRegistration by lazy {
        RelayRegistration(
            ethFreeRelayProvider,
            configHelper.createRelayRegistrationConfig(),
            masterContract.contractAddress,
            relayRegistryContract.contractAddress,
            accountHelper.registrationAccount,
            irohaAPI,
            configHelper.ethPasswordConfig
        )
    }

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
     * Get address of first free relay.
     */
    fun getFreeRelay(): String {
        return ethFreeRelayProvider.getRelay().get()
    }

    /**
     * Get relay address of an account.
     */
    fun getRelayByAccount(clientId: String): Optional<String> {
        return ethRelayProvider.getAddressByAccountId(clientId).get()
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
        return Pair(EthTokenInfo(name, ETH_DOMAIN, precision), deployERC20Token(name, precision))
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
        addEthAnchoredERC20Token(tokenAddress, EthTokenInfo(name, ETH_DOMAIN, precision))
        masterContract.addToken(tokenAddress).send()
        return tokenAddress
    }

    /**
     * Deploys smart contract which always fails. Use only if you know why do you need it.
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
        return getEthBalance(masterContract.contractAddress)
    }

    /**
     * Deploys relay contracts in Ethereum network
     */
    fun deployRelays(relaysToDeploy: Int) {
        relayRegistration.deploy(
            relaysToDeploy,
            contractTestHelper.relayImplementation.contractAddress,
            masterContract.contractAddress
        )
            .fold(
                {
                    logger.info("Relays were deployed by ${accountHelper.registrationAccount}")
                },
                {
                    logger.error("Relays were not deployed.", it)
                }
            )
    }

    /**
     * Import relays from given file
     */
    fun importRelays(filename: String) {
        relayRegistration.import(filename)
            .fold(
                {
                    logger.info("Relays were imported by ${accountHelper.registrationAccount}")
                },
                {
                    logger.error("Relays were not imported.", it)
                }
            )
    }

    /**
     * Registers relay account in Iroha with given address
     * @param address Ethereum address to register
     */
    fun registerRelayByAddress(address: String) {
        relayRegistration.registerRelayIroha(address)
        Thread.sleep(10_000)
    }

    /**
     * Deploys relay and registers first free relay contract in Iroha to the client with given [name] and public key
     */
    fun registerClientInEth(
        name: String,
        keypair: KeyPair = ModelUtil.generateKeypair()
    ): String {
        deployRelays(1)
        return registerClientWithoutRelay(name, keypair)
    }

    /**
     * Registers first free relay contract in Iroha to the client with given [name] and public key
     */
    fun registerClientWithoutRelay(
        name: String,
        keypair: KeyPair = ModelUtil.generateKeypair()
    ): String {
        ethRegistrationStrategy.register(name, D3_DOMAIN, keypair.public.toHexString())
            .fold({ registeredEthWallet ->
                logger.info("registered client $name with relay $registeredEthWallet")
                return registeredEthWallet
            },
                { ex -> throw RuntimeException("$name was not registered", ex) })
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
     * Send ETH with given amount to ethPublicKey
     */
    fun sendEth(amount: BigInteger, to: String) {
        logger.info { "send $amount Wei to $to " }
        contractTestHelper.sendEthereum(amount, to)
    }

    /**
     * Returns wallets registered by master account in Iroha
     */
    fun getRegisteredEthWallets(): Set<String> = ethRelayProvider.getAddresses().get().keys

    /**
     * Add list of [relays].
     * Set relays as details to NotaryAccount from RegistrationAccount
     */
    fun addRelaysToIroha(relays: Map<String, String>) {
        relays.map {
            // Set ethereum wallet as occupied by user id
            ModelUtil.setAccountDetail(
                registrationConsumer,
                accountHelper.notaryAccount.accountId,
                it.key,
                it.value
            )
        }
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
        logger.info { "Send request to register wallet ${address} for client ${clientId}" }
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
        ethDepositConfig: EthDepositConfig = configHelper.createEthDepositConfig(
            String.getRandomString(9)
        ),
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
     * Run ethereum registration config
     */
    fun runEthRegistrationService(registrationConfig: EthRegistrationConfig = ethRegistrationConfig) {
        executeRegistration(registrationConfig)
    }

    /**
     * Run withdrawal service
     */
    fun runEthWithdrawalService(
        withdrawalServiceConfig: WithdrawalServiceConfig = configHelper.createWithdrawalConfig(
            String.getRandomString(9)
        ),
        relayVacuumConfig: RelayVacuumConfig = configHelper.createRelayVacuumConfig(),
        rmqConfig: RMQConfig = loadRawLocalConfigs(
            "rmq",
            RMQConfig::class.java, "rmq.properties"
        )
    ) {
        masterContract // initialize lazy master contract
        com.d3.eth.withdrawal.withdrawalservice.executeWithdrawal(
            withdrawalServiceConfig,
            configHelper.ethPasswordConfig,
            relayVacuumConfig,
            rmqConfig
        )
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
                    rmqEvents.add(JSONObject(String(delivery.body)))
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
    ) {
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

        val ethTokenAddress = tokensProvider.getTokenAddress(assetId).get()
        val tokenPrecision = tokensProvider.getTokenPrecision(assetId).get()
        val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(tokenPrecision).toBigInteger()

        // signatures
        val vv = ArrayList<BigInteger>()
        val rr = ArrayList<ByteArray>()
        val ss = ArrayList<ByteArray>()

        val proofAccountId = "${txHash.take(32).toLowerCase()}@$ETH_WITHDRAWAL_PROOF_DOMAIN"
        queryHelper.getAccountDetails(proofAccountId, accountHelper.withdrawalAccount.accountId).get()
            .map { (ethNotaryAddress, withdrawalProofJson) ->
                val withdrawalProof =
                    gson.fromJson(withdrawalProofJson.irohaUnEscape(), WithdrawalProof::class.java)
                vv.add(BigInteger(withdrawalProof.signature.v, 16))
                rr.add(Numeric.hexStringToByteArray(withdrawalProof.signature.r))
                ss.add(Numeric.hexStringToByteArray(withdrawalProof.signature.s))
            }

        if (vv.size == 0) {
            throw Exception("No proofs for withdrawal")
        }
        val transactionResponse =
            if (tokensProvider.isIrohaAnchored(assetId).get())
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

    /**
     * Logger
     */
    companion object : KLogging()
}
