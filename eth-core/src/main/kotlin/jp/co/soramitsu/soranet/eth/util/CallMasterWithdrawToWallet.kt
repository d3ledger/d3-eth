/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("CallMasterWithdrawToWallet")

package jp.co.soramitsu.soranet.eth.util

import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.irohaUnEscape
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import jp.co.soramitsu.soranet.eth.config.EthereumConfig
import jp.co.soramitsu.soranet.eth.config.loadEthPasswords
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
import jp.co.soramitsu.soranet.eth.sidechain.util.VRSSignature
import mu.KLogging
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*
import kotlin.system.exitProcess


private val logger = KLogging().logger


data class WithdrawalProof(
    // Client Iroha account id
    val accountId: String,

    // ethereum address of token contract
    val tokenContractAddress: String,

    // amount of assets to withdraw
    val amount: String,

    // beneficiary ethereum address
    val beneficiary: String,

    // initial iroha withdrawal transaction
    val irohaHash: String,

    // caller ethereum address
    val relay: String,

    // ethereum notary signature
    val signature: VRSSignature
)

/**
 * Task that checks if Ethereum address is a Master peer.
 */
fun main() {
    val gson = GsonInstance.get()

    val masterContractAddress = "0x4732c999ad17382522807dfa9451c53d002d576e" // < dev soranet
    // "0xc41f90922b508658425f9b485eba5679938cd585" // < test soranet


    val ethTokenAddress = "0x0000000000000000000000000000000000000000" // < ether
    // "0x2f68c8e42f6f2b866b2b57e41eab304b3930cc0b" // < dev soranet xor
    // "0x087457fae2d66fd1d466f3fd880a99b6c28566e5" // < test soranet xor

    val decimalAmount = BigInteger("500000000000000000")
    val beneficiary = "0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33"
    val txHash = "5D1FB9696E85DD8433A16A8E964306D8FB44F0DF453247724FA4CBE38E7AB3E8"
    val proofs = listOf(
        "{\\\"accountId\\\":\\\"bogdan@sora\\\",\\\"tokenContractAddress\\\":\\\"0x0000000000000000000000000000000000000000\\\",\\\"amount\\\":\\\"500000000000000000\\\",\\\"beneficiary\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"irohaHash\\\":\\\"5D1FB9696E85DD8433A16A8E964306D8FB44F0DF453247724FA4CBE38E7AB3E8\\\",\\\"relay\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"signature\\\":{\\\"v\\\":\\\"1b\\\",\\\"r\\\":\\\"159f465fc477289bd744f3a7ec4efbb583fa106dfea1c9a3ae197849d3057d6c\\\",\\\"s\\\":\\\"6dd8b0766b4b39ff8d84d84c6fe593d5a2111ab37b9c9b6c91427c50fdb47722\\\"}}",
        "{\\\"accountId\\\":\\\"bogdan@sora\\\",\\\"tokenContractAddress\\\":\\\"0x0000000000000000000000000000000000000000\\\",\\\"amount\\\":\\\"500000000000000000\\\",\\\"beneficiary\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"irohaHash\\\":\\\"5D1FB9696E85DD8433A16A8E964306D8FB44F0DF453247724FA4CBE38E7AB3E8\\\",\\\"relay\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"signature\\\":{\\\"v\\\":\\\"1c\\\",\\\"r\\\":\\\"791872f2d1f16c075129f1ea0c19a768963ee6c6b1dd7bcf4bf8132e95227bb8\\\",\\\"s\\\":\\\"54a32b396f395b751e07f3ee3ada10df3980d57dedde2462a70368ce0ca5746f\\\"}}",
        "{\\\"accountId\\\":\\\"bogdan@sora\\\",\\\"tokenContractAddress\\\":\\\"0x0000000000000000000000000000000000000000\\\",\\\"amount\\\":\\\"500000000000000000\\\",\\\"beneficiary\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"irohaHash\\\":\\\"5D1FB9696E85DD8433A16A8E964306D8FB44F0DF453247724FA4CBE38E7AB3E8\\\",\\\"relay\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"signature\\\":{\\\"v\\\":\\\"1c\\\",\\\"r\\\":\\\"1394b2c5c97b92d5538b8ef420fa45838295027751dc6ca4721ee32f0c4dba91\\\",\\\"s\\\":\\\"04968c03378fee272ca9bc8985a5ea5536eb347a89caa510bd990e4883f5f178\\\"}}",
        "{\\\"accountId\\\":\\\"bogdan@sora\\\",\\\"tokenContractAddress\\\":\\\"0x0000000000000000000000000000000000000000\\\",\\\"amount\\\":\\\"500000000000000000\\\",\\\"beneficiary\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"irohaHash\\\":\\\"5D1FB9696E85DD8433A16A8E964306D8FB44F0DF453247724FA4CBE38E7AB3E8\\\",\\\"relay\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"signature\\\":{\\\"v\\\":\\\"1b\\\",\\\"r\\\":\\\"59e8f1705782d6a341eb3ab53770607086a3bddb6dd950217df943601628316b\\\",\\\"s\\\":\\\"2665f7c623674949983e8de3cfb163217c30a694032c2ff21bf614e87452a6eb\\\"}}",
        "{\\\"accountId\\\":\\\"bogdan@sora\\\",\\\"tokenContractAddress\\\":\\\"0x0000000000000000000000000000000000000000\\\",\\\"amount\\\":\\\"500000000000000000\\\",\\\"beneficiary\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"irohaHash\\\":\\\"5D1FB9696E85DD8433A16A8E964306D8FB44F0DF453247724FA4CBE38E7AB3E8\\\",\\\"relay\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"signature\\\":{\\\"v\\\":\\\"1c\\\",\\\"r\\\":\\\"547af739e847b410068272b810627f0250c69c0103673fc5766c9679aeba2a9f\\\",\\\"s\\\":\\\"186eabc2cce610e4e6d9e83145c924986d0dfd59568ba800c74d1d8369a288a6\\\"}}",
        "{\\\"accountId\\\":\\\"bogdan@sora\\\",\\\"tokenContractAddress\\\":\\\"0x0000000000000000000000000000000000000000\\\",\\\"amount\\\":\\\"500000000000000000\\\",\\\"beneficiary\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"irohaHash\\\":\\\"5D1FB9696E85DD8433A16A8E964306D8FB44F0DF453247724FA4CBE38E7AB3E8\\\",\\\"relay\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"signature\\\":{\\\"v\\\":\\\"1b\\\",\\\"r\\\":\\\"07b4a7cedb5a23275f46e7917a8089ed87aa5ba39256ee1366adb66ba7d1f9b4\\\",\\\"s\\\":\\\"6ba377c9083234105bc92c58c40ba5f5e25e7d0c545346d9acb40ce9ce24385e\\\"}}",
        "{\\\"accountId\\\":\\\"bogdan@sora\\\",\\\"tokenContractAddress\\\":\\\"0x0000000000000000000000000000000000000000\\\",\\\"amount\\\":\\\"500000000000000000\\\",\\\"beneficiary\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"irohaHash\\\":\\\"5D1FB9696E85DD8433A16A8E964306D8FB44F0DF453247724FA4CBE38E7AB3E8\\\",\\\"relay\\\":\\\"0x3A8aCBAe9576256Ad7EA0C2dC222d59a18A92E33\\\",\\\"signature\\\":{\\\"v\\\":\\\"1b\\\",\\\"r\\\":\\\"90d9cf920549362b243641c0d1ae9ca3165eb150316cf6495cb9be315f64658d\\\",\\\"s\\\":\\\"7ac2a195a58e1823978e5191a6e39967639a299bdc7c38f033ee3390371765d6\\\"}}"
    )

    val vv = ArrayList<BigInteger>()
    val rr = ArrayList<ByteArray>()
    val ss = ArrayList<ByteArray>()
    proofs.map { withdrawalProofJson ->
        val withdrawalProof =
            gson.fromJson(withdrawalProofJson.irohaUnEscape(), WithdrawalProof::class.java)
        vv.add(BigInteger(withdrawalProof.signature.v, 16))
        rr.add(Numeric.hexStringToByteArray(withdrawalProof.signature.r))
        ss.add(Numeric.hexStringToByteArray(withdrawalProof.signature.s))
    }

    loadLocalConfigs("predeploy.ethereum", EthereumConfig::class.java, "predeploy.properties")
        .fanout { loadEthPasswords("predeploy", "/eth/ethereum_password.properties") }
        .map { (ethereumConfig, passwordConfig) ->
            DeployHelper(
                ethereumConfig,
                passwordConfig
            )
        }
        .map { deployHelper ->
            logger.info { "Load master contract at $masterContractAddress" }
            val master = deployHelper.loadMasterContract(masterContractAddress)
            master.withdraw(
                ethTokenAddress,
                decimalAmount,
                beneficiary,
                Numeric.hexStringToByteArray(txHash),
                vv,
                rr,
                ss,
                beneficiary
            ).send()
        }
        .failure { ex ->
            logger.error("Master contract call exception", ex)
            exitProcess(1)
        }
}
