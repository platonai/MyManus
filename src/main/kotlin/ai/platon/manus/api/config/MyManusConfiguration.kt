package ai.platon.manus.api.config

import ai.platon.manus.agent.*
import ai.platon.manus.agent.plan.PlanningFlow
import ai.platon.manus.api.service.LlmService
import ai.platon.manus.common.MyContext
import ai.platon.manus.tool.BrowserUseTool
import ai.platon.manus.tool.GoogleSearch
import jakarta.annotation.PreDestroy
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.util.Timeout
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.util.concurrent.TimeUnit

@Configuration
class MyManusConfiguration {
    @Value("\${manus.serp.api.key}")
    lateinit var serpApiKey: String

    @Bean
    fun planningFlow(llmService: LlmService, toolCallingManager: ToolCallingManager): PlanningFlow {
        GoogleSearch.INSTANCE.serp.apikey = serpApiKey

        val manusAgent = MyManusAgent(llmService, toolCallingManager, MyContext.AGENT_WORKING_DIR)
        val browserAgent = BrowserAgent(llmService, toolCallingManager)
        val fileAgent = FileAgent(llmService, toolCallingManager, MyContext.AGENT_WORKING_DIR)
        val pythonAgent = PythonAgent(llmService, toolCallingManager, MyContext.AGENT_WORKING_DIR)

        return PlanningFlow(llmService, manusAgent, browserAgent, fileAgent, pythonAgent)
    }

    @Bean
    fun restClient(): RestClient.Builder {
        val requestConfig = RequestConfig.custom()
            .setResponseTimeout(Timeout.of(10, TimeUnit.MINUTES))
            .setConnectionRequestTimeout(Timeout.of(10, TimeUnit.MINUTES)).build()

        val httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()

        val requestFactory = HttpComponentsClientHttpRequestFactory(httpClient)

        return RestClient.builder().requestFactory(requestFactory)
    }

    @PreDestroy
    @Throws(Exception::class)
    fun cleanup() {
        BrowserUseTool.close()
    }
}
