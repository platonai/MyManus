package ai.platon.manus

import org.apache.commons.lang3.SystemUtils
import org.junit.jupiter.api.Test

class BasicTest {

    @Test
    fun `check system environments`() {
        println(SystemUtils.USER_NAME)

        // System.getenv().forEach { (k: String?, v: String?) -> println("$k=$v") }

        var key = System.getenv("SERP_API_KEY")
        println(key)

        key = System.getenv("spring.ai.openai.api-key")
        println(key)
    }
}
