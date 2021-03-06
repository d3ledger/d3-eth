/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import com.d3.commons.util.getRandomString
import com.d3.eth.provider.ETH_DOMAIN
import com.d3.eth.provider.EthTokensProvider
import com.d3.eth.provider.EthTokensProviderImpl
import com.d3.eth.token.EthTokenInfo
import com.d3.eth.token.executeTokenRegistration
import com.google.gson.Gson
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.time.Duration

/**
 * Requires Iroha is running
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ERC20TokenRegistrationTest {
    private val integrationHelper = EthIntegrationHelperUtil()
    private val ethTokensFilePath = "eth-tokens-for-test.json"
    private val irohaTokensFilePath = "iroha-tokens-for-test.json"
    private val tokenRegistrationConfig =
        integrationHelper.configHelper.createERC20TokenRegistrationConfig(
            ethTokensFilePath,
            irohaTokensFilePath
        )

    private val ethTokensProvider = EthTokensProviderImpl(
        integrationHelper.queryHelper,
        tokenRegistrationConfig.ethAnchoredTokenStorageAccount,
        tokenRegistrationConfig.irohaCredential.accountId,
        tokenRegistrationConfig.irohaAnchoredTokenStorageAccount,
        tokenRegistrationConfig.irohaCredential.accountId
    )

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterEach
    fun clearFile() {
        File(ethTokensFilePath).delete()
        File(irohaTokensFilePath).delete()
    }

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
    }

    /**
     * Test US-001 ERC20 tokens registration
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha network is running and json file full ERC20 tokens exists
     * @when ERC20 tokens registration completes
     * @then all the tokens from file are registered in Iroha
     */
    @Test
    fun testTokenRegistration() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val ethTokens = createRandomTokens()
            val irohaTokens = createRandomTokens()
            createTokensFile(ethTokens, ethTokensFilePath)
            createTokensFile(irohaTokens, irohaTokensFilePath)

            executeTokenRegistration(tokenRegistrationConfig)

            val actualEthTokens =
                tokensMapToInfo(ethTokensProvider, ethTokensProvider.getEthAnchoredTokens().get())
            assertEquals(ethTokens.toList().sortedBy { it.first }, actualEthTokens)

            val actualIrohaTokens =
                tokensMapToInfo(ethTokensProvider, ethTokensProvider.getEthAnchoredTokens().get())
            assertEquals(ethTokens.toList().sortedBy { it.first }, actualIrohaTokens)
        }
    }

    /**
     * Test US-002 ERC20 tokens registration
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha network is running and json file with no tokens exists
     * @when ERC20 tokens registration completes
     * @then no tokens are registered in Iroha
     */
    @Test
    fun testTokenRegistrationEmptyTokenFile() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            createTokensFile(HashMap(), ethTokensFilePath)
            createTokensFile(HashMap(), irohaTokensFilePath)
            executeTokenRegistration(tokenRegistrationConfig)
            ethTokensProvider.getEthAnchoredTokens().fold({ tokensFromProvider ->
                assertTrue(tokensFromProvider.isEmpty())
            }, { ex -> fail("cannot fetch tokens", ex) })

            ethTokensProvider.getIrohaAnchoredTokens().fold({ tokensFromProvider ->
                assertTrue(tokensFromProvider.isEmpty())
            }, { ex -> fail("cannot fetch tokens", ex) })
        }
    }


    //Creates json file full of ERC20 tokens
    private fun createTokensFile(tokens: Map<String, EthTokenInfo>, tokensFilePath: String) {
        val tokensJson = Gson().toJson(tokens)
        val tokeFile = File(tokensFilePath)
        tokeFile.createNewFile()
        tokeFile.printWriter().use { out ->
            out.use { out.println(tokensJson) }
        }
    }

    //Creates randomly generated tokens as a map (token address -> token info)
    private fun createRandomTokens(): MutableMap<String, EthTokenInfo> {
        val tokensToCreate = 5
        val defaultPrecision = 15
        val tokens = HashMap<String, EthTokenInfo>()
        for (i in 1..tokensToCreate) {
            val tokenName = String.getRandomString(9)
            val tokenInfo = EthTokenInfo(tokenName, ETH_DOMAIN, defaultPrecision)
            val tokenAddress = String.getRandomString(16)
            tokens.put(tokenAddress, tokenInfo)
        }
        return tokens
    }

    private fun tokensMapToInfo(
        ethTokensProvider: EthTokensProvider,
        tokens: Map<String, String>
    ): List<Pair<String, EthTokenInfo>> {
        return tokens.map { (ethAddress, tokenId) ->
            val name = tokenId.split("#").first()
            val domain = tokenId.split("#").last()
            Pair(
                ethAddress,
                EthTokenInfo(
                    name,
                    domain,
                    ethTokensProvider.getTokenPrecision(tokenId).get()
                )
            )
        }.sortedBy { it.first }
    }
}
