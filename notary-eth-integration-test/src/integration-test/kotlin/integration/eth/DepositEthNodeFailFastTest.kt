package integration.eth

import integration.helper.ContainerHelper
import integration.helper.KGenericContainerImage
import org.junit.jupiter.api.*
import org.testcontainers.containers.BindMode
import org.testcontainers.images.builder.ImageFromDockerfile
import java.io.File

private const val ETHEREUM_PORT = 8545

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepositEthNodeFailFastTest {

    private val containerHelper = ContainerHelper()
    private val dockerfile = "${containerHelper.userDir}/docker/Dockerfile"
    private val jarFile = "${containerHelper.userDir}/eth/build/libs/eth-all.jar"
    private val ethNodeDockerFile = containerHelper.userDir + "/deploy/ethereum/ganache.dockerfile"
    private val ethNodeContextFolder = containerHelper.userDir + "/deploy/ethereum"

    // Create deposit container
    private val depositContainer = containerHelper.createContainer(jarFile, dockerfile)

    /**
     * Creates test container for Ethereum node in the 'ganache' mode
     */
    private fun createEthNodeContainer(): KGenericContainerImage {
        val ethNodeContainer = KGenericContainerImage(
            ImageFromDockerfile()
                .withFileFromFile("", File(ethNodeContextFolder))
                .withFileFromFile("Dockerfile", File(ethNodeDockerFile))
        )
            .withLogConsumer { outputFrame -> print(outputFrame.utf8String) }
            .withExposedPorts(ETHEREUM_PORT)
            .withEnv("NODE", "0")
        ethNodeContainer.addFileSystemBind(ethNodeContextFolder, "/eth", BindMode.READ_WRITE)
        return ethNodeContainer
    }

    private val ethNodeContainer = createEthNodeContainer()

    @BeforeAll
    fun setUp() {
        // Mount Ethereum keys
        depositContainer.addFileSystemBind(
            "${containerHelper.userDir}/deploy/ethereum/keys",
            "/opt/notary/deploy/ethereum/keys",
            BindMode.READ_WRITE
        )

        // Mount Ethereum configs
        depositContainer.addFileSystemBind(
            "${containerHelper.userDir}/configs/eth",
            "/opt/notary/configs/eth",
            BindMode.READ_WRITE
        )

        // Mount Iroha keys
        depositContainer.addFileSystemBind(
            "${containerHelper.userDir}/deploy/iroha/keys",
            "/opt/notary/deploy/iroha/keys",
            BindMode.READ_WRITE
        )
        // Start Iroha
        containerHelper.irohaContainer.start()
        // Start Ethereum node
        ethNodeContainer.start()

        depositContainer.addEnv(
            "ETH-DEPOSIT_IROHA_HOSTNAME",
            containerHelper.irohaContainer.toriiAddress.host
        )
        depositContainer.addEnv(
            "ETH-DEPOSIT_IROHA_PORT",
            containerHelper.irohaContainer.toriiAddress.port.toString()
        )
        depositContainer.addEnv(
            "ETH-DEPOSIT_ETHEREUM_URL",
            "http://127.0.0.1:${ethNodeContainer.getMappedPort(ETHEREUM_PORT)}"
        )
        // Start service
        depositContainer.start()
    }

    @AfterAll
    fun tearDown() {
        containerHelper.close()
        ethNodeContainer.stop()
        depositContainer.stop()
    }

    /**
     * @given deposit and Ethereum services being started
     * @when Ethereum dies
     * @then deposit dies as well
     */
    @Test
    fun testFailFast() {
        // Let service work a little
        Thread.sleep(15_000)
        Assertions.assertTrue(containerHelper.isServiceHealthy(depositContainer))
        // Kill Ethereum node
        ethNodeContainer.stop()
        // Wait a little
        Thread.sleep(15_000)
        // Check that the service is dead
        Assertions.assertTrue(containerHelper.isServiceDead(depositContainer))
    }
}
