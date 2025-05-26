package ai.platon.manus.agent

import ai.platon.manus.api.service.LlmService
import ai.platon.manus.tool.FileSaver
import ai.platon.manus.tool.GoogleSearch
import ai.platon.manus.tool.PythonTool
import ai.platon.manus.tool.Summary
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.DefaultChatClient
import org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.tool.ToolCallback

open class ToolCallAgent(
    llmService: LlmService, private val toolCallingManager: ToolCallingManager
) : ReActAgent(llmService) {
    private val logger: Logger = LoggerFactory.getLogger(ToolCallAgent::class.java)

    private var response: ChatResponse? = null

    private lateinit var userPrompt: Prompt

    override val description: String = "The tool call agent manages tool calls"

    override val name: String = "ToolCallAgent"

    override fun think(): Boolean {
        val retry = 0
        return doThinkWithRetry(retry)
    }

    override fun addThinkPromptTo(messages: MutableList<Message>): Message {
        // super class' behavior, doing nothing in ToolCallAgent
        super.addThinkPromptTo(messages)
        return SystemPromptTemplate(TOOL_CALL_AGENT_SYSTEM_PROMPT).createMessage(data).also { messages.add(it) }
    }

    override val nextStepMessage: Message get() = UserMessage(TOOL_CALL_AGENT_NEXT_STEP_PROMPT)

    private fun doThinkWithRetry(retry: Int): Boolean {
        try {
            val messages = mutableListOf<Message>()
            addThinkPromptTo(messages)

            val chatOptions = ToolCallingChatOptions.builder().internalToolExecutionEnabled(false).build()
            val nextStepMessage = nextStepMessage
            messages.add(nextStepMessage)

            userPrompt = Prompt(messages, chatOptions)

            val request = llmService.agentClient.prompt(userPrompt)
                .advisors { it.param(CONVERSATION_ID, conversationId) }
                .toolCallbacks(toolCallbacks)

            if (request is DefaultChatClient.DefaultChatClientRequestSpec) {
//                conversationLogger.info("\n\n-------------------\nMyManus:\n{}\n{}\n{}",
//                    request.messages.joinToString("\n"), data,
//                    request.messages.joinToString("\n") { it.text })

                val requestText = request.messages.joinToString("\n") { it.text }
                conversationLogger.info("===========================================================================" +
                        "\nMyManus:\n\n{}\n\n\n", requestText)
            }

            response = request.call().chatResponse()

            val thoughts = response ?: return false
            val toolCalls = thoughts.result.output.toolCalls

            conversationLogger.info("AI:\n\n{}\n{}", thoughts, thoughts.result.output)
            conversationLogger.info("🛠️ agent has selected tools to use | [{}] {} | {}",
                name, toolCalls.size, toolCalls.map { it.name })
            conversationLogger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")

            reportLLMThoughtsAndChosenToolCalls(thoughts, verbose = false)

            return toolCalls.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            logger.warn("I'm stuck in my thought process 😭 | {} | {}\n{}", name, e.message, data)
            if (retry < REPLY_MAX) {
                return doThinkWithRetry(retry + 1)
            }
            return false
        }
    }

    private fun reportLLMThoughtsAndChosenToolCalls(response: ChatResponse, verbose: Boolean) {
        val thoughts = response
        val toolCalls = thoughts.result.output.toolCalls

        if (verbose) {
            logger.info("""😇 agent's thoughts | {} | 🗯{}🗯""", name, thoughts.result.output)
            logger.info("🛠️ agent has selected tools to use | [{}] {} | {}", name, toolCalls.size, toolCalls.map { it.name })

            return
        }

        val answer = thoughts.result.output.text
        if (!answer.isNullOrEmpty()) {
            logger.info("""✨ {}'s answer: 🗯{}🗯""", name, answer)
        }
        if (toolCalls.isNotEmpty()) {
            logger.info("""🎯 Tools prepared: {}""", toolCalls.map { it.name })
        }
    }

    override fun act(): String {
        val response0 = requireNotNull(response)
        val results: MutableList<String> = ArrayList()
        val toolCalls = response0.result.output.toolCalls
        if (toolCalls.isEmpty()) {
            logger.warn("No tool calls found in response | {} | {}", name, response0.result.output)
            return "No tool calls found in response"
        }

        val toolCall = toolCalls[0]
        logger.info("🔧 Performing tool call | {} | {} {}", name, toolCall.name, toolCall.arguments)

        try {
            val toolCallResult = toolCallingManager.executeToolCalls(userPrompt, response0)
            val index = toolCallResult.conversationHistory().size - 1
            val responseMessage = toolCallResult.conversationHistory()[index] as ToolResponseMessage
            llmService.agentMemory.add(conversationId, responseMessage)

            val llmCallResponse = responseMessage.responses[0].responseData()
            results.add(llmCallResponse)

            val responseText = results.joinToString("\n\n")

            conversationLogger.info("AI:\n\n{}\n", results.joinToString("\n"))
            conversationLogger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")

            logger.info("🔧 Tool response | {} | {}", name, StringUtils.abbreviate(responseText, 1000))

            return responseText
        } catch (e: Exception) {
            val response = ToolResponse(toolCall.id(), toolCall.name(), "Error: " + e.message)
            val responseMessage = ToolResponseMessage(listOf(response), mapOf())
            llmService.agentMemory.add(conversationId, responseMessage)

            logger.warn("""Act failed 😔 | {}""", e.message)
            return String.format("""Act failed 😔 | %s""", e.message)
        }
    }

    override val toolCallbacks = listOf<ToolCallback>(
        GoogleSearch.functionToolCallback,
        FileSaver.functionToolCallback,
        PythonTool.functionToolCallback,
        Summary.getFunctionToolCallback(this, llmService.agentMemory, conversationId)
    )

    companion object {
        private const val REPLY_MAX = 3
    }
}
