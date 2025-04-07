package ai.platon.manus

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [MyTestApplication::class])
@ActiveProfiles("private")
class SpringBootBaseTest {
    @Value("\${manus.serp.api.key}")
    lateinit var serpApiKey: String

    @Value("\${spring.ai.openai.api-key}")
    lateinit var llmApiKey: String

    @Test
    fun `check spring environment`() {
        println(serpApiKey)
        println(llmApiKey)
    }
}
