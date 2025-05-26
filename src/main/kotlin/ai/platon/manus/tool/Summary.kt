package ai.platon.manus.tool

import ai.platon.manus.agent.AgentState
import ai.platon.manus.agent.AbstractAgent
import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.tool.function.FunctionToolCallback
import org.springframework.ai.tool.metadata.ToolMetadata

class Summary(
    private val agent: AbstractAgent,
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

        private const val NAME = "summary"

        private const val DESCRIPTION = """
Terminate the current execution step with a comprehensive summary message.
This message will be passed as the final output of the current step and should include:

- Detailed execution results and status
- All relevant facts and data collected
- Key findings and observations
- Important insights and conclusions
- Any actionable recommendations

The summary should be thorough enough to provide complete context for subsequent steps or other agents.

        """

        fun getFunctionToolCallback(
            agent: AbstractAgent, chatMemory: ChatMemory, conversationId: String
        ): FunctionToolCallback<*, *> {
            return FunctionToolCallback.builder(NAME, Summary(agent, chatMemory, conversationId))
                .description(DESCRIPTION)
                .inputSchema(PARAMETERS)
                .inputType(Map::class.java)
                .toolMetadata(ToolMetadata.builder().returnDirect(true).build()).build()
        }
    }
}
