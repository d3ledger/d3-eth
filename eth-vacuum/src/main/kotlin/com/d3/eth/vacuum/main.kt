/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("VacuumRelayMain")

package com.d3.eth.vacuum

import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import integration.eth.config.loadEthPasswords
import com.github.kittinunf.result.*
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging

private const val RELAY_VACUUM_PREFIX = "relay-vacuum"
private val logger = KLogging().logger
/**
 * Entry point for moving all currency from relay contracts to master contract
 */
fun main(args: Array<String>) {
    loadLocalConfigs(RELAY_VACUUM_PREFIX, RelayVacuumConfig::class.java, "vacuum.properties")
        .flatMap { relayVacuumConfig ->
            executeVacuum(relayVacuumConfig, args)
        }
        .failure { ex ->
            logger.error("Cannot run vacuum", ex)
            System.exit(1)
        }
}

fun executeVacuum(
    relayVacuumConfig: RelayVacuumConfig,
    args: Array<String> = emptyArray()
): Result<Unit, Exception> {
    logger.info { "Run relay vacuum" }
    return Result.of {
        val keyPair = Utils.parseHexKeypair(
            relayVacuumConfig.vacuumCredential.pubkey,
            relayVacuumConfig.vacuumCredential.privkey
        )
        IrohaCredential(relayVacuumConfig.vacuumCredential.accountId, keyPair)
    }.fanout {
        Result.of { IrohaAPI(relayVacuumConfig.iroha.hostname, relayVacuumConfig.iroha.port) }
    }.map { (credential, irohaAPI) ->
        IrohaQueryHelperImpl(irohaAPI, credential.accountId, credential.keyPair)
    }.fanout {
        loadEthPasswords(RELAY_VACUUM_PREFIX, "/eth/ethereum_password.properties")
    }.flatMap { (queryHelper, passwordConfig) ->
        RelayVacuum(relayVacuumConfig, passwordConfig, queryHelper).vacuum()
    }
}
