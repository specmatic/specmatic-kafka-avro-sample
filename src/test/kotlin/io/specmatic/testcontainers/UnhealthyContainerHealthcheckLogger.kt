package io.specmatic.testcontainers

import com.github.dockerjava.api.DockerClient
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

class UnhealthyContainerHealthcheckLogger(val dockerClient: DockerClient) : InvocationInterceptor {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun interceptBeforeAllMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        invocation.dumpHealthchecksOnFailure()
    }

    override fun interceptBeforeEachMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        invocation.dumpHealthchecksOnFailure()
    }

    override fun interceptTestMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        invocation.dumpHealthchecksOnFailure()
    }

    override fun interceptTestTemplateMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        invocation.dumpHealthchecksOnFailure()
    }

    override fun interceptAfterEachMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        invocation.dumpHealthchecksOnFailure()
    }

    override fun interceptAfterAllMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        invocation.dumpHealthchecksOnFailure()
    }

    private fun <T> Invocation<T>.dumpHealthchecksOnFailure(): T {
        try {
            return proceed()
        } catch (e: Throwable) {
            val unhealthyContainerLogs = logUnhealthyContainerHealthchecks()
            if (unhealthyContainerLogs.isEmpty()) throw e
            throw UnhealthyContainerException(unhealthyContainerLogs.joinToString("\n\n"), e)
        }
    }

    private fun logUnhealthyContainerHealthchecks(): List<String> {
        val unhealthy = dockerClient.listContainersCmd()
            .withShowAll(true)
            .withFilter("health", listOf("unhealthy"))
            .exec()
        return unhealthy.map { container ->
            val name = container.names.firstOrNull()?.trimStart('/') ?: container.id
            val healthLog = dockerClient.inspectContainerCmd(container.id).exec()
                .state.health?.log.orEmpty()
                .joinToString("\n") { "[exit=${it.exitCodeLong}] ${it.output?.trim()}" }
            val message = "Healthcheck output for unhealthy container '$name':\n$healthLog"
            logger.error(message)
            message
        }
    }

    private class UnhealthyContainerException(message: String, cause: Throwable) : Exception(message, cause) {
        override fun fillInStackTrace(): Throwable = this
    }
}
