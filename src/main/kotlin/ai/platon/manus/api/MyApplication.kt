package ai.platon.manus.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.awt.GraphicsEnvironment

@SpringBootApplication
class MyManusApplication

fun main(args: Array<String>) {
    val additionalProfiles = mutableListOf("private")

    // disable interactive mode if the environment is headless, such as docker container
    val isHeadless = GraphicsEnvironment.isHeadless()
    if (!isHeadless) {
        additionalProfiles.add("interactive")
    }

    runApplication<MyManusApplication>(*args) {
        setAdditionalProfiles(*additionalProfiles.toTypedArray())
        setRegisterShutdownHook(true)
        setLogStartupInfo(true)
    }
}
