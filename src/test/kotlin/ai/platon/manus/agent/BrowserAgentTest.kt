package ai.platon.manus.agent

import ai.platon.manus.MyTestApplication
import ai.platon.manus.api.service.LlmService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [MyTestApplication::class])
class BrowserAgentTest {
    @Autowired
    lateinit var llmService: LlmService

    @Autowired
    lateinit var toolCallingManager: ToolCallingManager

    private lateinit var browserAgent: BrowserAgent

    @BeforeEach
    fun setUp() {
        browserAgent = BrowserAgent(llmService, toolCallingManager)
    }

    @Test
    fun testBrowserAgent() {
        val params = mapOf("action" to "navigate", "url" to "https://example.com")
//        val result = browserAgent.run(params)
//        println(result)
    }
}
