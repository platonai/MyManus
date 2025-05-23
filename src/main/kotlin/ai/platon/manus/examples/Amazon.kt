package ai.platon.manus.examples

import ai.platon.manus.api.SimpleAgentTaskRunner
import org.springframework.boot.runApplication

fun main() {
    val task = "go to amazon and find the best selling book in the last 30 days, and summarize it in 3 sentences"
    runApplication<SimpleAgentTaskRunner>(task)
}
