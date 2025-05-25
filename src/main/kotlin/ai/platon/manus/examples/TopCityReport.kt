package ai.platon.manus.examples

import ai.platon.manus.api.SimpleAgentTaskRunner
import org.springframework.boot.runApplication

fun main() {
    val task = """
**Task: Report China's top 10 cities by GDP including:**

- Nominal GDP
- Population
- GDP per capita
- Major industries

**Additional requirements:**
- Provide analysis of the results
- Create and save a bar chart in PNG format
    """.trimIndent()
    runApplication<SimpleAgentTaskRunner>(task)
}
