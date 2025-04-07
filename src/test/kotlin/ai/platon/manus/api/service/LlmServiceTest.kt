package ai.platon.manus.api.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "spring.ai.openai.api-key", matches = ".+")
class LlmServiceTest {
    @Autowired
    lateinit var llmService: LlmService

    @Test
    fun testChatClient() {
        val inputText = "What is the capital of France?"
        val outputText = llmService.chatClient.prompt(inputText).call().content()
        println(outputText)

        assertNotNull(outputText)
        assertTrue { outputText.isNotEmpty() }
    }
}
