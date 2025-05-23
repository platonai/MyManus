package ai.platon.manus.examples

import ai.platon.manus.api.SimpleAgentTaskRunner
import org.springframework.boot.runApplication

fun main() {
    val task = """
create a simple python script that prints "Hello, World!" to the console.
    """
    runApplication<SimpleAgentTaskRunner>(task)
}
