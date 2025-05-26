package ai.platon.manus.examples

import ai.platon.manus.api.SimpleAgentTaskRunner
import org.springframework.boot.runApplication

fun main() {
    val task = "go to google and search for 'PulsarRPA', then summarize the result"
    runApplication<SimpleAgentTaskRunner>(task)
}
