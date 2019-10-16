/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("CreateNewWallet")

package com.d3.eth.deploy

import com.d3.eth.helper.generateWalletFile
import integration.eth.config.loadEthPasswords
import mu.KLogging

private val logger = KLogging().logger

fun main() {
    val ethereumPasswords =
        loadEthPasswords("eth-deposit", "/eth/ethereum_password.properties").get()

    val address = generateWalletFile(ethereumPasswords.credentialsPassword)

    logger.info { "Wallet address generated $address" }
}
