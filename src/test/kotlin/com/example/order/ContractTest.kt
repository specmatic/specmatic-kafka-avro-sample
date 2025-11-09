package com.example.order

import io.specmatic.async.core.constants.AVAILABLE_SERVERS
import io.specmatic.async.core.constants.SCHEMA_REGISTRY_KIND
import io.specmatic.async.core.constants.SCHEMA_REGISTRY_PASSWORD
import io.specmatic.async.core.constants.SCHEMA_REGISTRY_URL
import io.specmatic.async.core.constants.SCHEMA_REGISTRY_USERNAME
import io.specmatic.async.core.constants.SchemaRegistryKind
import io.specmatic.kafka.test.SpecmaticKafkaContractTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.io.File
import java.time.Duration

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContractTest: SpecmaticKafkaContractTest {
    @Value("\${spring.kafka.properties.schema.registry.url}")
    lateinit var schemaRegistryUrl: String

    @Value("\${spring.kafka.bootstrap-servers}")
    lateinit var kafkaBootstrapServers: String

    private val schemaRegistry = schemaRegistry()

    @BeforeAll
    fun setup() {
        schemaRegistry.start()
        System.setProperty(SCHEMA_REGISTRY_URL, schemaRegistryUrl)
        System.setProperty(SCHEMA_REGISTRY_KIND, SchemaRegistryKind.CONFLUENT.name)
        System.setProperty(AVAILABLE_SERVERS, kafkaBootstrapServers)
        System.setProperty(SCHEMA_REGISTRY_USERNAME, "admin")
        System.setProperty(SCHEMA_REGISTRY_PASSWORD, "admin-secret")
    }

    @AfterAll
    fun tearDown() {
        schemaRegistry.stop()
    }

    private fun schemaRegistry(): ComposeContainer {
        return ComposeContainer(DOCKER_COMPOSE_FILE)
            .withLocalCompose(true).waitingFor(
                REGISTER_SCHEMAS_SERVICE,
                LogMessageWaitStrategy()
                    .withRegEx(SCHEMA_REGISTERED_REGEX)
                    .withStartupTimeout(Duration.ofSeconds(60))
            )
    }

    companion object {
        private val DOCKER_COMPOSE_FILE = File("docker-compose.yaml")
        private const val REGISTER_SCHEMAS_SERVICE = "register-schemas"
        private const val SCHEMA_REGISTERED_REGEX = ".*(?i)schemas registered.*"
    }
}