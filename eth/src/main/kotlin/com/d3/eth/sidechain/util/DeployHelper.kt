/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.sidechain.util

import com.d3.commons.util.createPrettyScheduledThreadPool
import com.d3.eth.helper.encodeFunction
import contract.*
import integration.eth.config.EthereumConfig
import integration.eth.config.EthereumPasswords
import mu.KLogging
import okhttp3.*
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.Transfer
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.utils.Convert
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

const val ENDPOINT_ETHEREUM = "eth"
const val ATTEMPTS_DEFAULT = 240

/**
 * Authenticator class for basic access authentication
 * @param nodePassword config with Ethereum node credentials
 */
class BasicAuthenticator(private val nodeLogin: String?, private val nodePassword: String?) :
    Authenticator {
    constructor(ethereumPasswords: EthereumPasswords) : this(
        ethereumPasswords.nodeLogin,
        ethereumPasswords.nodePassword
    )

    override fun authenticate(route: Route, response: Response): Request {
        val credential = Credentials.basic(nodeLogin!!, nodePassword!!)
        return response.request().newBuilder().header("Authorization", credential).build()
    }
}

/**
 * Build DeployHelper in more granular level
 * @param ethereumConfig config with Ethereum network parameters
 * @param nodeLogin - Ethereum node login
 * @param nodePassword - Ethereum node password
 * @param credentials - Ethereum credentials
 */
class DeployHelperBuilder(
    ethereumConfig: EthereumConfig,
    nodeLogin: String?,
    nodePassword: String?,
    val credentials: org.web3j.crypto.Credentials,
    private val attempts: Int = ATTEMPTS_DEFAULT
) {
    /**
     * Helper class for contracts deploying
     * @param ethereumConfig config with Ethereum network parameters
     * @param ethereumPasswords config with Ethereum passwords
     */
    constructor(ethereumConfig: EthereumConfig, ethereumPasswords: EthereumPasswords) :
            this(
                ethereumConfig,
                ethereumPasswords.nodeLogin,
                ethereumPasswords.nodePassword,
                WalletUtils.loadCredentials(
                    ethereumPasswords.credentialsPassword,
                    ethereumPasswords.credentialsPath
                ),
                ATTEMPTS_DEFAULT
            )

    private val deployHelper =
        DeployHelper(ethereumConfig, nodeLogin, nodePassword, credentials, attempts)

    /**
     * Specify fast transaction manager to send multiple transactions one by one.
     */
    fun setFastTransactionManager(): DeployHelperBuilder {
        deployHelper.transactionManager = AttemptsCustomizableFastRawTransactionManager(
            deployHelper.web3,
            credentials,
            attempts
        )
        return this
    }

    fun build(): DeployHelper {
        return deployHelper
    }
}

/**
 * Helper class for contracts deploying
 * @param ethereumConfig config with Ethereum network parameters
 * @param nodeLogin - Ethereum node login
 * @param nodePassword
 * @param attempts attempts amount to poll transaction status
 */
class DeployHelper(
    ethereumConfig: EthereumConfig,
    nodeLogin: String?,
    nodePassword: String?,
    val credentials: org.web3j.crypto.Credentials,
    attempts: Int = ATTEMPTS_DEFAULT
) {
    /**
     * Helper class for contracts deploying
     * @param ethereumConfig config with Ethereum network parameters
     * @param ethereumPasswords config with Ethereum passwords
     */
    constructor(ethereumConfig: EthereumConfig, ethereumPasswords: EthereumPasswords) :
            this(
                ethereumConfig,
                ethereumPasswords.nodeLogin,
                ethereumPasswords.nodePassword,
                WalletUtils.loadCredentials(
                    ethereumPasswords.credentialsPassword,
                    ethereumPasswords.credentialsPath
                ),
                ATTEMPTS_DEFAULT
            )

    val web3: Web3j

    init {
        val builder = OkHttpClient().newBuilder()
        builder.authenticator(BasicAuthenticator(nodeLogin, nodePassword))
        builder.readTimeout(1200, TimeUnit.SECONDS)
        builder.writeTimeout(1200, TimeUnit.SECONDS)
        web3 = Web3j.build(
            HttpService(ethereumConfig.url, builder.build(), false), DEFAULT_BLOCK_TIME.toLong(),
            createPrettyScheduledThreadPool(DeployHelper::class.simpleName!!, "web3j")
        )
    }

    /** transaction manager */
    var transactionManager = RawTransactionManager(web3, credentials, attempts, DEFAULT_BLOCK_TIME)

    /** Gas price */
    val gasPrice = BigInteger.valueOf(ethereumConfig.gasPrice)

    /** Max gas limit */
    val gasLimit = BigInteger.valueOf(ethereumConfig.gasLimit)

    /**
     * Sends given amount of ether from some predefined account to given account
     * @param amount amount of ether to send
     * @param to target account
     * @return transaction hash
     */
    fun sendEthereum(amount: BigInteger, to: String): String {
        val transfer = Transfer(web3, transactionManager)
        val transactionHash =
            transfer.sendFunds(to, BigDecimal(amount), Convert.Unit.WEI, gasPrice, gasLimit).send()
                .transactionHash
        logger.info("ETH $amount were sent to $to; tx hash $transactionHash")
        return transactionHash
    }

    /**
     * Deploy ERC20 token smart contract
     * @return token smart contract object
     */
    fun deployERC20TokenSmartContract(): BasicCoin {
        val tokenContract = BasicCoin.deploy(
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit),
            BigInteger.valueOf(Long.MAX_VALUE),
            credentials.address
        ).send()
        logger.info { "ERC20 token smart contract ${tokenContract.contractAddress} was deployed" }
        return tokenContract
    }

    /**
     * Deploy relay registry smart contract
     * @return master smart contract object
     */
    fun deployRelayRegistrySmartContract(): RelayRegistry {
        val relayRegistry = RelayRegistry.deploy(
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        ).send()
        logger.info { "Relay Registry smart contract ${relayRegistry.contractAddress} was deployed" }
        return relayRegistry
    }

    /**
     * Deploy [RelayRegistry] via [OwnedUpgradeabilityProxy].
     */
    fun deployUpgradableRelayRegistrySmartContract(): RelayRegistry {
        // deploy implementation
        val relayRegistry = deployRelayRegistrySmartContract()

        // deploy proxy
        val proxy = deployOwnedUpgradeabilityProxy()

        // call proxy set up
        val encoded = encodeFunction("initialize", Address(credentials.address) as Type<Any>)
        proxy.upgradeToAndCall(relayRegistry.contractAddress, encoded, BigInteger.ZERO).send()

        // load via proxy
        val proxiedRelayRegistry = RelayRegistry.load(
            proxy.contractAddress,
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        )
        logger.info { "Upgradable proxy to RelayRegistry contract ${proxiedRelayRegistry.contractAddress} was deployed" }

        return proxiedRelayRegistry
    }

    /**
     * Deploy master smart contract
     * @return master smart contract object
     */
    fun deployMasterSmartContract(peers: List<String>): Master {
        val master = Master.deploy(
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit),
            peers
        ).send()
        logger.info { "Master smart contract ${master.contractAddress} was deployed" }
        return master
    }

    /**
     * Deploy MasterRelayed smart contract with Relay contract
     * @return master smart contract object
     */
    fun deployMasterRelayedSmartContract(
        relayRegistry: String,
        peers: List<String>
    ): MasterRelayed {
        val master = MasterRelayed.deploy(
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit),
            relayRegistry,
            peers
        ).send()
        logger.info { "MasterRelayed smart contract ${master.contractAddress} was deployed" }
        return master
    }

    /**
     * Deploy [Master] via [OwnedUpgradeabilityProxy].
     */
    fun deployUpgradableMasterSmartContract(peers: List<String>): Master {
        // deploy implementation
        val master = deployMasterSmartContract(peers)

        // deploy proxy
        val proxy = deployOwnedUpgradeabilityProxy()

        // call proxy set up
        val encoded =
            encodeFunction(
                "initialize",
                Address(credentials.address) as Type<Any>,
                DynamicArray<Address>(Address::class.java, peers.map { Address(it) }) as Type<Any>
            )
        proxy.upgradeToAndCall(master.contractAddress, encoded, BigInteger.ZERO).send()

        // load via proxy
        val proxiedMaster = loadMasterContract(proxy.contractAddress)
        logger.info { "Upgradable proxy to Master contract ${proxiedMaster.contractAddress} was deployed" }

        return proxiedMaster
    }

    /**
     * Deploy [MasterRelayed] (Master with Relays) via [OwnedUpgradeabilityProxy].
     */
    fun deployUpgradableMasterRelayedSmartContract(
        relayRegistry: String,
        peers: List<String>
    ): MasterRelayed {
        // deploy implementation
        val master = deployMasterRelayedSmartContract(relayRegistry, peers)

        // deploy proxy
        val proxy = deployOwnedUpgradeabilityProxy()

        // call proxy set up
        val encoded =
            encodeFunction(
                "initialize",
                Address(credentials.address) as Type<Any>,
                Address(relayRegistry) as Type<Any>,
                DynamicArray<Address>(Address::class.java, peers.map { Address(it) }) as Type<Any>
            )
        proxy.upgradeToAndCall(master.contractAddress, encoded, BigInteger.ZERO).send()

        // load via proxy
        val proxiedMaster = loadMasterRelayedContract(proxy.contractAddress)
        logger.info { "Upgradable proxy to Master contract ${proxiedMaster.contractAddress} was deployed" }

        return proxiedMaster
    }

    /**
     * Load Master contract implementation
     * @param address - address of master contract
     * @return Master contract
     */
    fun loadMasterContract(address: String): Master {
        val proxiedMaster = Master.load(
            address,
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        )
        return proxiedMaster
    }

    /**
     * Load MasterRelayed contract implementation
     * @param address - address of master contract
     * @return Master contract
     */
    fun loadMasterRelayedContract(address: String): MasterRelayed {
        val proxiedMaster = MasterRelayed.load(
            address,
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        )
        return proxiedMaster
    }

    /**
     * Deploy relay smart contract
     * @param master notary master account
     * @return relay smart contract object
     */
    fun deployRelaySmartContract(master: String): Relay {
        val relay = Relay.deploy(
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit),
            master
        ).send()

        logger.info { "Relay smart contract ${relay.contractAddress} was deployed" }
        return relay
    }

    /**
     * Load Relay contract implementation
     * @param address - address of relay contract
     * @return Relay contract
     */
    fun loadRelayContract(address: String): Relay {
        return Relay.load(
            address,
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        )
    }

    /**
     * Load RelayRegistry contract implementation
     * @param address - address of relay registry contract
     * @return RelayRegistry contract
     */
    fun loadRelayRegistryContract(address: String): RelayRegistry {
        return RelayRegistry.load(
            address,
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        )
    }

    /**
     * Deploy upgradable proxy to relay contract.
     * @param relayImplementationAddress - address to deployed implementation of Relay contract
     * @param masterAddress - address of master contract
     * @return [OwnedUpgradeabilityProxy] to upgradable [Relay]
     */
    fun deployUpgradableRelaySmartContract(
        relayImplementationAddress: String,
        masterAddress: String
    ): Relay {
        // deploy proxy
        val proxy = deployOwnedUpgradeabilityProxy()

        // call proxy set up
        val encoded =
            encodeFunction("initialize", Address(masterAddress) as Type<Any>)
        proxy.upgradeToAndCall(relayImplementationAddress, encoded, BigInteger.ZERO).send()

        // load via proxy
        val proxiedRelay = Relay.load(
            proxy.contractAddress,
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        )
        logger.info {
            """Upgradable proxy to Relay contract ${proxiedRelay.contractAddress} """ +
                    """was deployed and initialized with master $masterAddress"""
        }

        return proxiedRelay
    }

    /**
     * Load Proxy contract
     * @param address - address of proxy contract
     * @return Proxy contract
     */
    fun loadOwnedUpgradabilityProxy(address: String): OwnedUpgradeabilityProxy {
        return OwnedUpgradeabilityProxy.load(
            address,
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        )
    }

    /**
     * Load Sora token smart contract
     * @return Sora token instance
     */
    fun loadTokenSmartContract(tokenAddress: String): SoraToken {
        val soraToken =
            SoraToken.load(
                tokenAddress,
                web3,
                transactionManager,
                StaticGasProvider(gasPrice, gasLimit)
            )
        logger.info { "Sora token contract ${soraToken.contractAddress} was loaded" }
        return soraToken
    }

    fun deployFailerContract(): Failer {
        val failer =
            Failer.deploy(web3, transactionManager, StaticGasProvider(gasPrice, gasLimit)).send()
        logger.info { "Failer smart contract ${failer.contractAddress} was deployed" }
        return failer
    }

    /**
     * Deploy TransferEthereum contract that sends Ethereum as internal transaction. Can be used for testing.
     */
    fun deployTransferEthereum(): TransferEthereum {
        val transferEthereum =
            TransferEthereum.deploy(web3, transactionManager, StaticGasProvider(gasPrice, gasLimit))
                .send()
        logger.info { "Transfer ethereum contract ${transferEthereum.contractAddress} was deployed" }
        return transferEthereum
    }

    /**
     * Deploy TestGreeter_v0 contract. The contract is used for upgradability testing, it is initial version.
     * @param greeting - greeting string
     * @return relay smart contract object
     */
    fun deployTestGreeter_v0(greeting: String): TestGreeter_v0 {
        val testGreeter_v0 = TestGreeter_v0.deploy(
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit),
            greeting
        ).send()
        logger.info { "TestGreeter_v0 was deployed at ${testGreeter_v0.contractAddress}" }
        return testGreeter_v0
    }

    /**
     * Deploy TestGreeter_v1 contract. The contract is used for upgradability testing, it is next version..
     * @return relay smart contract object
     */
    fun deployTestGreeter_v1(): TestGreeter_v1 {
        val testGreeter_v1 = TestGreeter_v1.deploy(
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        ).send()
        logger.info { "TestGreeter_v1 was deployed at ${testGreeter_v1.contractAddress}" }
        return testGreeter_v1
    }

    /**
     * Deploy OwnedUpgradabilityProxy contract. Contract is an upgradable proxy to another contract.
     */
    fun deployOwnedUpgradeabilityProxy(): OwnedUpgradeabilityProxy {
        val OwnedUpgradeabilityProxy = OwnedUpgradeabilityProxy.deploy(
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        ).send()
        logger.info { "OwnedUpgradeabilityProxy was deployed at ${OwnedUpgradeabilityProxy.contractAddress}" }
        return OwnedUpgradeabilityProxy
    }

    /**
     * Send ERC20 tokens
     * @param tokenAddress - address of token smart contract
     * @param toAddress - address transfer to
     * @param amount - amount of tokens
     */
    fun sendERC20(tokenAddress: String, toAddress: String, amount: BigInteger) {
        val token = BasicCoin.load(
            tokenAddress,
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        )
        token.transfer(toAddress, amount).send()
        logger.info { "ERC20 $amount with address $tokenAddress were sent to $toAddress" }
    }

    /**
     * Get ERC20 balance
     * @param tokenAddress - address of token smart contract
     * @param whoAddress - user address to check
     * @return user balance
     */
    fun getERC20Balance(tokenAddress: String, whoAddress: String): BigInteger {
        val token = BasicCoin.load(
            tokenAddress,
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        )
        return token.balanceOf(whoAddress).send()
    }

    /**
     * Get ETH balance
     * @param whoAddress - user address to check
     * @return user balance
     */
    fun getETHBalance(whoAddress: String): BigInteger {
        return web3.ethGetBalance(whoAddress, DefaultBlockParameterName.LATEST).send().balance
    }

    /**
     * Register relay to RelayRegistry
     * @ethRelayRegistryAddress - relay registry address
     * @freeEthWallet - wallet that set whitelist to
     */
    fun addRelayToRelayRegistry(ethRelayRegistryAddress: String, freeEthWallet: String) {
        logger.info { "Add new relay to relay registry relayRegistry=${ethRelayRegistryAddress}, freeWallet=$freeEthWallet, creator=${credentials.address}." }
        val relayRegistry = RelayRegistry.load(
            ethRelayRegistryAddress,
            web3,
            transactionManager,
            StaticGasProvider(gasPrice, gasLimit)
        )
        relayRegistry.addNewRelayAddress(freeEthWallet, emptyList()).send()
    }

    /**
     * Signs user-provided data with predefined account deployed on local Parity node
     * @param toSign data to sign
     * @return signed data
     */
    fun signUserData(toSign: String) = signUserData(credentials.ecKeyPair, toSign)

    /**
     * Logger
     */
    companion object : KLogging()
}

/**
 * Simple RawTransactionManager derivative that manages nonces to facilitate multiple transactions
 * per block. The implementation allows to set the attempts amount to modify default timeout.
 */
class AttemptsCustomizableFastRawTransactionManager(
    web3j: Web3j,
    credentials: org.web3j.crypto.Credentials,
    attempts: Int
) : RawTransactionManager(
    web3j,
    credentials,
    attempts,
    DEFAULT_BLOCK_TIME
) {

    @Volatile
    var currentNonce = BigInteger.valueOf(-1)!!
        private set

    @Synchronized
    @Throws(IOException::class)
    override fun getNonce(): BigInteger {
        currentNonce = if (currentNonce.signum() == -1) {
            // obtain lock
            super.getNonce()
        } else {
            currentNonce.add(BigInteger.ONE)
        }
        return currentNonce
    }

    @Synchronized
    @Throws(IOException::class)
    fun resetNonce() {
        currentNonce = super.getNonce()
    }

    @Synchronized
    fun setNonce(value: BigInteger) {
        currentNonce = value
    }
}
