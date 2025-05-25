package ai.platon.manus.tool.support.serpapi

import ai.platon.manus.tool.GoogleSearch.Companion.SERP_API_KEY
import ai.platon.pulsar.common.config.ImmutableConfig
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.assertTrue

@EnabledIfEnvironmentVariable(named = "manus.serp.api.key", matches = ".+")
class SerpApiServiceTest {

    companion object {
        private val conf = ImmutableConfig(loadDefaults = true)

        val apiKey = conf.get("manus.serp.api.key") ?: ""

        @BeforeAll
        @JvmStatic
        fun setup() {
            Assumptions.assumeTrue(apiKey.isNotBlank())
        }
    }

    @Test
    fun testSerpApiService() {
        val service = SerpApiService(SERP_API_KEY, "google")
        val result = service.call(SerpApiService.Request("browser-use"))
        assertTrue { result.isNotEmpty() }
    }
}
