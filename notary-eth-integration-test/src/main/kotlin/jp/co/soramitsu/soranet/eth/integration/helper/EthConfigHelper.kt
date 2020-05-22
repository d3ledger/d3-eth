/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.helper

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig
import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.util.getRandomString
import integration.helper.IrohaConfigHelper
import jp.co.soramitsu.soranet.eth.bridge.EthDepositConfig
import jp.co.soramitsu.soranet.eth.bridge.RefundConfig
import jp.co.soramitsu.soranet.eth.config.EthereumConfig
import jp.co.soramitsu.soranet.eth.config.EthereumPasswords
import jp.co.soramitsu.soranet.eth.config.loadEthPasswords
import jp.co.soramitsu.soranet.eth.registration.EthRegistrationConfig
import java.math.BigInteger

/**
 *Class that handles all the configuration objects.
 */
open class EthConfigHelper(
    private val accountHelper: EthereumAccountHelper,
    open val masterContractAddress: String,
    open val xorTokenAddress: String,
    val lastEthereumReadBlockFilePath: String = "deploy/eth-deposit/last_eth_read_block.txt"
) : IrohaConfigHelper() {

    /** Ethereum password configs */
    val ethPasswordConfig = loadEthPasswords("test", "/eth/ethereum_password.properties").get()

    /** Configuration for deposit instance */
    private val ethDepositConfig by lazy {
        loadLocalConfigs(
            "eth-deposit",
            EthDepositConfig::class.java,
            "deposit.properties"
        ).get()
    }

    /**
     * Test configuration for refund endpoint
     * Create unique port for refund for every call
     */
    fun createRefundConfig(): RefundConfig {
        return object : RefundConfig {
            override val port = portCounter.incrementAndGet()
        }
    }

    /** Test configuration of Deposit with runtime dependencies */
    fun createEthDepositConfig(
        testName: String = String.getRandomString(9),
        irohaConfig: IrohaConfig = createIrohaConfig(),
        ethereumConfig: EthereumConfig = object : EthereumConfig {
            override val url = ethDepositConfig.ethereum.url
            override val gasPrice = ethDepositConfig.ethereum.gasPrice
            override val gasLimit = ethDepositConfig.ethereum.gasLimit
            override val confirmationPeriod = ethDepositConfig.ethereum.confirmationPeriod
        },
        notaryCredential_: IrohaCredentialRawConfig = accountHelper.createCredentialRawConfig(
            accountHelper.notaryAccount
        ),
        xorTokenAddress: String = this@EthConfigHelper.xorTokenAddress,
        xorExchangeContractAddress: String = masterContractAddress

    ): EthDepositConfig {
        return object : EthDepositConfig {
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val expansionTriggerAccount = accountHelper.expansionTriggerAccount.accountId
            override val expansionTriggerCreatorAccountId = accountHelper.superuserAccount.accountId
            override val ethAnchoredTokenStorageAccount =
                accountHelper.ethAnchoredTokenStorageAccount.accountId
            override val ethAnchoredTokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val irohaAnchoredTokenStorageAccount =
                accountHelper.irohaAnchoredTokenStorageAccount.accountId
            override val irohaAnchoredTokenSetterAccount =
                accountHelper.tokenSetterAccount.accountId
            override val notaryCredential = notaryCredential_
            override val withdrawalCredential =
                accountHelper.createCredentialRawConfig(accountHelper.withdrawalAccount)
            override val refund = createRefundConfig()
            override val iroha = irohaConfig
            override val lastEthereumReadBlockFilePath =
                this@EthConfigHelper.lastEthereumReadBlockFilePath
            override val startEthereumBlock = BigInteger.ZERO
            override val ignoreStartBlock = true
            override val ethereum = ethereumConfig
            override val ethIrohaDepositQueue = testName
            override val ethMasterAddress = masterContractAddress
            override val ethereumWalletStorageAccount = accountHelper.ethereumWalletStorageAccount.accountId
            override val ethereumWalletSetterAccount = accountHelper.notaryAccount.accountId
            override val masterContractAbiPath = "deploy/ethereum/contract/abi/Master.abi"
            override val withdrawalLimitStorageAccount = accountHelper.xorLimitsStorageAccount.accountId
            override val xorTokenAddress = xorTokenAddress
            override val xorExchangeContractAddress = xorExchangeContractAddress
            override val isXorToken = false
        }
    }

    /** Test configuration of Registration with runtime dependencies */
    fun createEthRegistrationConfig(): EthRegistrationConfig {
        return object : EthRegistrationConfig {
            override val port = portCounter.incrementAndGet()
            override val relayRegistrationIrohaAccount = accountHelper.registrationAccount.accountId
            override val relayStorageAccount = accountHelper.ethereumWalletStorageAccount.accountId
            override val iroha = createIrohaConfig()
            override val registrationCredential =
                accountHelper.createCredentialRawConfig(accountHelper.registrationAccount)
        }
    }

    /**
     * Creates new Ethereum config with given credentials path
     * @return EthereumConfig object
     */
    fun createEthereumConfig(): EthereumConfig {
        return object : EthereumConfig {
            override val confirmationPeriod = ethDepositConfig.ethereum.confirmationPeriod
            override val gasLimit = ethDepositConfig.ethereum.gasLimit
            override val gasPrice = ethDepositConfig.ethereum.gasPrice
            override val url = ethDepositConfig.ethereum.url
        }
    }

    /**
     * Creates new Ethereum passwords config with given credentials path
     * @param credentialsPath path to Ethereum credentials file (.key)
     * @return EthereumConfig object
     */
    fun createEthereumPasswords(credentialsPath: String = ethPasswordConfig.credentialsPath): EthereumPasswords {
        return object : EthereumPasswords {
            override val credentialsPath = credentialsPath
            override val credentialsPassword = ethPasswordConfig.credentialsPassword
            override val nodeLogin = ethPasswordConfig.nodeLogin
            override val nodePassword = ethPasswordConfig.nodePassword
        }
    }

}
