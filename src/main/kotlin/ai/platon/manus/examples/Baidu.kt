package ai.platon.manus.examples

import ai.platon.manus.api.SimpleAgentTaskRunner
import org.springframework.boot.runApplication

fun main() {
    val task = "打开baidu.com，搜索框输入 PulsarRPA, 点击回车，搜索结果打开后，总结结果"
    runApplication<SimpleAgentTaskRunner>(task)
}
