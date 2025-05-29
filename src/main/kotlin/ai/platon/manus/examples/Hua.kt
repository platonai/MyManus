package ai.platon.manus.examples

import ai.platon.manus.api.SimpleAgentTaskRunner
import org.springframework.boot.runApplication

fun main() {
    val task = "打开 hua.com"
    runApplication<SimpleAgentTaskRunner>(task)
}
