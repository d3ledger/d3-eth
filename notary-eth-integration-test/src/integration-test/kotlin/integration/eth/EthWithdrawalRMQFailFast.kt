/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import integration.helper.ContainerHelper
import integration.helper.DEFAULT_RMQ_PORT
import org.junit.jupiter.api.*
import org.testcontainers.containers.BindMode

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthWithdrawalRMQFailFast {
    private val containerHelper = ContainerHelper()
    private val dockerfile = "${containerHelper.userDir}/eth-withdrawal/build/docker/Dockerfile"
    private val contextFolder = "${containerHelper.userDir}/eth-withdrawal/build/docker/"

    // Create withdrawal container
    private val withdrawalContainer = containerHelper.createSoraPluginContainer(contextFolder, dockerfile)

    @BeforeAll
    fun startUp() {
        // Mount Ethereum keys
        withdrawalContainer.addFileSystemBind(
            "${containerHelper.userDir}/deploy/ethereum/keys",
            "/deploy/ethereum/keys",
            BindMode.READ_WRITE
        )

        // Mount Ethereum configs
        withdrawalContainer.addFileSystemBind(
            "${containerHelper.userDir}/configs/eth",
            "/configs/eth",
            BindMode.READ_WRITE
        )

        // Start Iroha
        containerHelper.irohaContainer.start()

        // Start RMQ
        containerHelper.rmqContainer.start()

        withdrawalContainer.addEnv(
            "WITHDRAWAL_IROHA_HOSTNAME",
            containerHelper.irohaContainer.toriiAddress.host
        )
        withdrawalContainer.addEnv(
            "WITHDRAWAL_IROHA_PORT",
            containerHelper.irohaContainer.toriiAddress.port.toString()
        )
        withdrawalContainer.addEnv("WITHDRAWAL_ETHEREUM_URL", "http://127.0.0.1:8545")
        withdrawalContainer.addEnv("RMQ_HOST", "127.0.0.1")
        withdrawalContainer.addEnv("RMQ_PORT", containerHelper.rmqContainer.getMappedPort(DEFAULT_RMQ_PORT).toString())
        // Start service
        withdrawalContainer.start()
    }

    @AfterAll
    fun tearDown() {
        containerHelper.close()
        withdrawalContainer.stop()
    }

    /**
     * @given withdrawal and RMQ services being started
     * @when RMQ dies
     * @then withdrawal dies as well
     */
    @Test
    fun testFailFast() {
        // Let service work a little
        Thread.sleep(15_000)
        Assertions.assertTrue(containerHelper.isServiceHealthy(withdrawalContainer))
        // Kill Iroha
        containerHelper.rmqContainer.stop()
        // Wait a little
        Thread.sleep(15_000)
        // Check that the service is dead
        Assertions.assertTrue(containerHelper.isServiceDead(withdrawalContainer))
    }
}
