/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth.config

import com.d3.commons.config.loadConfigs
import com.github.kittinunf.result.Result

//Environment variable that holds Ethereum credentials path
const val ETH_CREDENTIALS_PATH_ENV = "ETH_CREDENTIALS_PATH"
//Environment variable that holds Ethereum credentials password
const val ETH_CREDENTIALS_PASSWORD_ENV = "ETH_CREDENTIALS_PASSWORD"
//Environment variable that holds Ethereum node login
const val ETH_NODE_LOGIN_ENV = "ETH_NODE_LOGIN"
//Environment variable that holds Ethereum node password
const val ETH_NODE_PASSWORD_ENV = "ETH_NODE_PASSWORD"
//Environment variable that holds current application profile

/**
 * Ethereum configurations
 */
interface EthereumConfig {
    val url: String
    val gasPrice: Long
    val gasLimit: Long
    val confirmationPeriod: Long
}

/**
 * Ethereum passwords
 */
interface EthereumPasswords {
    val credentialsPath: String
    val credentialsPassword: String
    val nodeLogin: String?
    val nodePassword: String?
}

/**
 * Loads ETH passwords. Lookup priority: environment variables>property file
 * TODO: implement command line argument parsing
 */
fun loadEthPasswords(
    prefix: String,
    filename: String
): Result<EthereumPasswords, Exception> {
    var config = loadConfigs(prefix, EthereumPasswords::class.java, filename).get()
    config = object : EthereumPasswords {
        override val credentialsPath =
            System.getenv(ETH_CREDENTIALS_PATH_ENV) ?: config.credentialsPath
        override val credentialsPassword =
            System.getenv(ETH_CREDENTIALS_PASSWORD_ENV) ?: config.credentialsPassword
        override val nodeLogin = System.getenv(ETH_NODE_LOGIN_ENV) ?: config.nodeLogin
        override val nodePassword = System.getenv(ETH_NODE_PASSWORD_ENV) ?: config.nodePassword
    }

    return Result.of(config)
}
