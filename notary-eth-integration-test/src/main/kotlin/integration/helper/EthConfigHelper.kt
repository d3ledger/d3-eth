/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig
import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.util.getRandomString
import integration.eth.config.EthereumConfig
import integration.eth.config.loadEthPasswords
import com.d3.eth.deposit.EthDepositConfig
import com.d3.eth.deposit.RefundConfig
import com.d3.eth.registration.EthRegistrationConfig
import com.d3.eth.registration.relay.RelayRegistrationConfig
import com.d3.eth.token.ERC20TokenRegistrationConfig
import com.d3.eth.vacuum.RelayVacuumConfig
import com.d3.eth.withdrawal.withdrawalservice.WithdrawalServiceConfig
import integration.eth.config.EthereumPasswords
import java.math.BigInteger

/**
 *Class that handles all the configuration objects.
 */
open class EthConfigHelper(
    private val accountHelper: EthereumAccountHelper,
    open val relayRegistryContractAddress: String,
    open val masterContractAddress: String,
    open val relayImplementaionContractAddress: String,
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

    /** Creates config for ERC20 tokens registration */
    fun createERC20TokenRegistrationConfig(
        ethTokensFilePath_: String,
        irohaTokensFilePath_: String
    ): ERC20TokenRegistrationConfig {
        val ethTokenRegistrationConfig = loadLocalConfigs(
            "token-registration",
            ERC20TokenRegistrationConfig::class.java,
            "token_registration.properties"
        ).get()

        return object : ERC20TokenRegistrationConfig {
            override val irohaCredential = ethTokenRegistrationConfig.irohaCredential
            override val iroha = createIrohaConfig()
            override val ethAnchoredTokensFilePath = ethTokensFilePath_
            override val irohaAnchoredTokensFilePath = irohaTokensFilePath_
            override val ethAnchoredTokenStorageAccount =
                accountHelper.ethAnchoredTokenStorageAccount.accountId
            override val irohaAnchoredTokenStorageAccount =
                accountHelper.irohaAnchoredTokenStorageAccount.accountId
        }
    }

    /** Creates config for ETH relays registration */
    fun createRelayRegistrationConfig(): RelayRegistrationConfig {
        val relayRegistrationConfig =
            loadLocalConfigs(
                "relay-registration",
                RelayRegistrationConfig::class.java,
                "relay_registration.properties"
            ).get()

        return object : RelayRegistrationConfig {
            override val number = relayRegistrationConfig.number
            override val replenishmentPeriod = relayRegistrationConfig.replenishmentPeriod
            override val ethMasterAddress = masterContractAddress
            override val ethRelayImplementationAddress = relayImplementaionContractAddress
            override val relayStorageAccount = accountHelper.ethereumRelayStorageAccount.accountId
            override val iroha = createIrohaConfig()
            override val ethereum = relayRegistrationConfig.ethereum
            override val relayRegistrationCredential =
                relayRegistrationConfig.relayRegistrationCredential
        }
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
        )
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
            override val withdrawalCredential = accountHelper.createCredentialRawConfig(accountHelper.withdrawalAccount)
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
            override val ethereumRelayStorageAccount = accountHelper.ethereumRelayStorageAccount.accountId
            override val ethereumRelaySetterAccount = accountHelper.registrationAccount.accountId
            override val masterContractAbiPath = "deploy/ethereum/contract/abi/Master.abi"
        }
    }

    /** Test configuration of Withdrawal service with runtime dependencies */
    fun createWithdrawalConfig(
        testName: String,
        useValidEthereum: Boolean = true
    ): WithdrawalServiceConfig {
        val withdrawalConfig =
            loadLocalConfigs(
                "withdrawal",
                WithdrawalServiceConfig::class.java,
                "withdrawal.properties"
            ).get()

        val ethereumConfig =
            if (useValidEthereum) withdrawalConfig.ethereum else getBrokenEthereumConfig(
                withdrawalConfig
            )

        return object : WithdrawalServiceConfig {
            override val relayStorageAccount = accountHelper.ethereumRelayStorageAccount.accountId
            override val ethAnchoredTokenStorageAccount =
                accountHelper.ethAnchoredTokenStorageAccount.accountId
            override val ethAnchoredTokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val irohaAnchoredTokenStorageAccount =
                accountHelper.irohaAnchoredTokenStorageAccount.accountId
            override val irohaAnchoredTokenSetterAccount =
                accountHelper.tokenSetterAccount.accountId
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val registrationIrohaAccount = accountHelper.registrationAccount.accountId
            override val expansionTriggerAccount = accountHelper.expansionTriggerAccount.accountId
            override val withdrawalCredential =
                accountHelper.createCredentialRawConfig(accountHelper.withdrawalAccount)
            override val expansionTriggerCreatorAccountId = accountHelper.superuserAccount.accountId
            override val withdrawalBillingAccount =
                accountHelper.ethWithdrawalBillingAccount.accountId
            override val ethMasterAddress = masterContractAddress
            override val port = portCounter.incrementAndGet()
            override val iroha = createIrohaConfig()
            override val ethereum = ethereumConfig
            override val ethIrohaWithdrawalQueue = testName
        }
    }

    fun getBrokenEthereumConfig(withdrawalServiceConfig: WithdrawalServiceConfig): EthereumConfig {
        return object : EthereumConfig {
            override val url = withdrawalServiceConfig.ethereum.url
            override val gasPrice = 0L
            override val gasLimit = 0L
            override val confirmationPeriod = 0L
        }
    }

    /** Test configuration of Registration with runtime dependencies */
    fun createEthRegistrationConfig(): EthRegistrationConfig {
        return object : EthRegistrationConfig {
            override val port = portCounter.incrementAndGet()
            override val relayRegistrationIrohaAccount = accountHelper.registrationAccount.accountId
            override val relayStorageAccount = accountHelper.ethereumRelayStorageAccount.accountId
            override val iroha = createIrohaConfig()
            override val registrationCredential =
                accountHelper.createCredentialRawConfig(accountHelper.registrationAccount)
        }
    }

    fun createRelayVacuumConfig(): RelayVacuumConfig {
        val vacuumConfig =
            loadLocalConfigs(
                "relay-vacuum",
                RelayVacuumConfig::class.java,
                "vacuum.properties"
            ).get()
        return object : RelayVacuumConfig {
            override val registrationServiceIrohaAccount =
                accountHelper.registrationAccount.accountId

            override val ethAnchoredTokenStorageAccount =
                accountHelper.ethAnchoredTokenStorageAccount.accountId
            override val ethAnchoredTokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val irohaAnchoredTokenStorageAccount =
                accountHelper.irohaAnchoredTokenStorageAccount.accountId
            override val irohaAnchoredTokenSetterAccount =
                accountHelper.tokenSetterAccount.accountId
            /** Notary Iroha account that stores relay register */
            override val relayStorageAccount = accountHelper.ethereumRelayStorageAccount.accountId

            override val vacuumCredential =
                accountHelper.createCredentialRawConfig(accountHelper.testCredential)
            /** Iroha configurations */
            override val iroha = createIrohaConfig()

            /** Ethereum configurations */
            override val ethereum = vacuumConfig.ethereum
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
    fun createEthereumPasswords(credentialsPath: String = ethPasswordConfig.credentialsPath) : EthereumPasswords {
        return object: EthereumPasswords {
            override val credentialsPath = credentialsPath
            override val credentialsPassword = ethPasswordConfig.credentialsPassword
            override val nodeLogin = ethPasswordConfig.nodeLogin
            override val nodePassword = ethPasswordConfig.nodePassword
        }
    }

}
