package ai.platon.manus.tool

import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.manus.tool.support.serpapi.SerpApiService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.function.FunctionToolCallback

class GoogleSearch : AbstractTool() {
    private val logger = LoggerFactory.getLogger(GoogleSearch::class.java)

    val serp by lazy { SerpApiService(SERP_API_KEY, "google") }

    override fun run(args: Map<String, Any?>): ToolExecuteResult {
        return try {
            performSearch(args)
        } catch (e: Exception) {
            ToolExecuteResult("Failed to google | ${e.message}")
        }
    }

    private fun performSearch(toolInput: Map<String, Any?>): ToolExecuteResult {
        logger.info("GoogleSearch | $toolInput")

        val query = toolInput["query"] as? String ?: return ToolExecuteResult("Invalid query")

        val request = SerpApiService.Request(query)
        val response = serp.call(request).toMutableMap()

        if (response.containsKey("answer_box") && response["answer_box"] is List<*>) {
            response["answer_box"] = (response["answer_box"] as List<*>)[0]!!
        }

        val result: String = when {
            response.containsKey("answer_box") && (response["answer_box"] as? Map<*, *>)?.containsKey("answer") == true -> {
                (response["answer_box"] as Map<*, *>)["answer"].toString()
            }

            response.containsKey("answer_box") && (response["answer_box"] as? Map<*, *>)?.containsKey("snippet") == true -> {
                (response["answer_box"] as Map<*, *>)["snippet"].toString()
            }

            response.containsKey("answer_box") && (response["answer_box"] as? Map<*, *>)?.containsKey("snippet_highlighted_words") == true -> {
                ((response["answer_box"] as Map<*, *>)["snippet_highlighted_words"] as List<String>)[0]
            }

            response.containsKey("sports_results") && (response["sports_results"] as? Map<*, *>)?.containsKey("game_spotlight") == true -> {
                (response["sports_results"] as Map<*, *>)["game_spotlight"].toString()
            }

            response.containsKey("shopping_results") && (response["shopping_results"] as? List<Map<String, Any>>)?.get(0)
                ?.containsKey("title") == true -> {
                val shoppingResults = response["shopping_results"] as List<Map<String, Any>>
                val subList = shoppingResults.subList(0, minOf(3, shoppingResults.size))
                subList.toString()
            }

            response.containsKey("knowledge_graph") && (response["knowledge_graph"] as? Map<*, *>)?.containsKey("description") == true -> {
                (response["knowledge_graph"] as Map<*, *>)["description"].toString()
            }

            (response["organic_results"] as? List<Map<String, Any>>)?.get(0)?.containsKey("snippet") == true -> {
                (response["organic_results"] as List<Map<String, Any>>)[0]["snippet"].toString()
            }

            (response["organic_results"] as? List<Map<String, Any>>)?.get(0)?.containsKey("link") == true -> {
                (response["organic_results"] as List<Map<String, Any>>)[0]["link"].toString()
            }

            response.containsKey("images_results") && (response["images_results"] as? List<Map<String, Any>>)?.get(0)
                ?.containsKey("thumbnail") == true -> {
                val thumbnails: MutableList<String> = ArrayList()
                val imageResults = response["images_results"] as List<Map<String, Any>>
                for (item in imageResults.subList(0, minOf(10, imageResults.size))) {
                    thumbnails.add(item["thumbnail"].toString())
                }
                thumbnails.toString()
            }

            else -> {
                "No good search result found"
            }
        }

        logger.info("SerpAPI result: $result")

        return ToolExecuteResult(result)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(GoogleSearch::class.java)

        // TODO: a better way to manage the SERP_API_KEY
        var SERP_API_KEY = System.getenv("SERP_API_KEY") ?: ""

        private val PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "query": {
			            "type": "string",
			            "description": "(required) The search query to submit to Google."
			        },
			        "num_results": {
			            "type": "integer",
			            "description": "(optional) The number of search results to return. Default is 10.",
			            "default": 10
			        }
			    },
			    "required": ["query"]
			}
			
			""".trimIndent()

        private const val name = "google_search"

        private val description = """
### Google Search

Use this tool to search the web for information, retrieve the latest data, or explore specific topics.
It performs a Google search based on the provided query and returns a list of relevant URLs matching the search criteria.

			""".trimIndent()

        val INSTANCE = GoogleSearch()

        val functionToolCallback: FunctionToolCallback<*, *>
            get() = FunctionToolCallback.builder(name, INSTANCE)
                .description(description)
                .inputSchema(PARAMETERS)
                .inputType(Map::class.java)
                .build()
    }
}
