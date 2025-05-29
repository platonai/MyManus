package ai.platon.manus.examples.todo

import ai.platon.manus.api.SimpleAgentTaskRunner
import org.springframework.boot.runApplication

fun main() {
//    val task = "go to hacker news, search the page content, find out the latest news about AI agents, read each article, and summarize the result"
    val task = "go to hacker news, click the 10-th link"
    runApplication<SimpleAgentTaskRunner>(task)
}
