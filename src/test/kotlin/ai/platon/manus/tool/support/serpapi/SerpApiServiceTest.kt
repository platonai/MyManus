package ai.platon.manus.tool.support.serpapi

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Value
import kotlin.test.assertTrue

@EnabledIfEnvironmentVariable(named = "manus.serp.api.key", matches = ".+")
class SerpApiServiceTest {

    companion object {
        // TODO: use spring property manus.serp.api.key
        val SERP_API_KEY: String = System.getenv("SERP_API_KEY")

        @BeforeAll
        @JvmStatic
        fun setup() {
            Assumptions.assumeTrue(SERP_API_KEY.isNotBlank())
        }
    }

    @Test
    fun testSerpApiService() {
        val service = SerpApiService(SERP_API_KEY, "google")
        val result = service.call(SerpApiService.Request("browser-use"))
        assertTrue { result.isNotEmpty() }
    }
}
