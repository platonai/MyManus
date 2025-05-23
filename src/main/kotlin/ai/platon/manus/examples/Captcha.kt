package ai.platon.manus.examples

import ai.platon.manus.api.SimpleAgentTaskRunner
import org.springframework.boot.runApplication

fun main() {
    val task = "go to https://captcha.com/demos/features/captcha-demo.aspx and solve the captcha"
    runApplication<SimpleAgentTaskRunner>(task)
}
