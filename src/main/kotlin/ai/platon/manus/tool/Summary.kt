package ai.platon.manus.tool

import ai.platon.manus.agent.AgentState
import ai.platon.manus.agent.MyAgent
import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.tool.function.FunctionToolCallback
import org.springframework.ai.tool.metadata.ToolMetadata

class Summary(
    private val agent: MyAgent,
    private val chatMemory: ChatMemory,
    private val conversationId: String
) : AbstractTool() {

    override fun run(args: Map<String, Any?>): ToolExecuteResult {
        logger.info("Summary | $args")
        agent.state = AgentState.FINISHED
        return ToolExecuteResult(pulsarObjectMapper().writeValueAsString(args))
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Summary::class.java)

        private val PARAMETERS = """
			{
			  "type" : "object",
			  "properties" : {
			    "summary" : {
			      "type" : "string",
			      "description" : "The output of current step, better make a summary."
			    }
			  },
			  "required" : [ "summary" ]
			}
			""".trimIndent()

        private const val name = "summary"

        private const val description = "Record the summary of current step and terminate the current step"

        fun getFunctionToolCallback(
            agent: MyAgent, chatMemory: ChatMemory, conversationId: String
        ): FunctionToolCallback<*, *> {
            return FunctionToolCallback.builder(name, Summary(agent, chatMemory, conversationId))
                .description(description)
                .inputSchema(PARAMETERS)
                .inputType(Map::class.java)
                .toolMetadata(ToolMetadata.builder().returnDirect(true).build()).build()
        }
    }
}
