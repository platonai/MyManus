package ai.platon.manus.tool.support.serpapi

import ai.platon.manus.common.BROWSER_USER_AGENT
import ai.platon.manus.common.SERP_API_URL
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient

class SerpApiService(
    var apikey: String,
    var engine: String
) {
    private val webClient = WebClient.builder()
        .baseUrl(SERP_API_URL)
        .defaultHeader(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT)
        .codecs { it.defaultCodecs().maxInMemorySize(MAX_MEMORY_SIZE) }
        .build()

    fun call(request: Request): Map<String, Any> {
        if (request.query.isBlank()) {
            return mapOf()
        }

        try {
            val responseMono = webClient.method(HttpMethod.GET)
                .uri { it.queryParam("api_key", apikey).queryParam("engine", engine)
                    .queryParam("q", request.query).build() }
                .retrieve()
                .bodyToMono(String::class.java)
            val response = checkNotNull(responseMono.block()) { "Response from SERP should not be null" }

            logger.info("SerpAPI search: {}", request.query)
            logger.debug("Result:\n{}", response)

            return pulsarObjectMapper().readValue(response)
        } catch (e: Exception) {
            logger.warn("Failed to search via SerpAPI | {}", e.message)
            return mapOf()
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("serpapi search request")
    data class Request(
        @field:JsonPropertyDescription("The query keyword, e.g. MyManus")
        @field:JsonProperty(required = true, value = "query")
        @param:JsonProperty(required = true, value = "query")
        @param:JsonPropertyDescription("The query keyword, e.g. MyManus")
        val query: String = ""
    )

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SerpApiService::class.java)
        private const val MEMORY_SIZE = 5
        private const val BYTE_SIZE = 1024
        private const val MAX_MEMORY_SIZE = MEMORY_SIZE * BYTE_SIZE * BYTE_SIZE
    }
}
