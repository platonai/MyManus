package ai.platon.manus.agent

import ai.platon.manus.api.service.LlmService
import ai.platon.manus.tool.FileSaver
import ai.platon.manus.tool.GoogleSearch
import ai.platon.manus.tool.PythonTool
import ai.platon.manus.tool.Summary
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
) : ThinkAndActAgent(llmService) {
    private val logger: Logger = LoggerFactory.getLogger(ToolCallAgent::class.java)

    private var response: ChatResponse? = null

    private lateinit var userPrompt: Prompt

    override val description: String = "The tool call agent manages tool calls"

    override val name: String = "ToolCallAgent"

    override fun think(): Boolean {
        val retry = 0
        return doThinkWithRetry(retry)
    }

    override fun addThinkPrompt(messages: MutableList<Message>): Message {
        // super class' behavior, doing nothing in this case
        super.addThinkPrompt(messages)
        return SystemPromptTemplate(TOOL_CALL_AGENT_STEP_PROMPT).createMessage(data).also { messages.add(it) }
    }

    override val nextStepMessage: Message get() = UserMessage(TOOL_CALL_AGENT_NEXT_STEP_PROMPT)

    private fun doThinkWithRetry(retry: Int): Boolean {
        try {
            val messages = mutableListOf<Message>()
            addThinkPrompt(messages)

            val chatOptions = ToolCallingChatOptions.builder().internalToolExecutionEnabled(false).build()
            val nextStepMessage = nextStepMessage
            messages.add(nextStepMessage)

//            println("-----")
//            println("ToolCallAgent think() | $name | $data")
//            println(messages.joinToString("\n"))
//            println(">>>>>")

            userPrompt = Prompt(messages, chatOptions)

            response = llmService.agentClient.prompt(userPrompt)
                .advisors { it.param(CONVERSATION_ID, conversationId) }
                .toolCallbacks(toolCallbacks)
                .call()
                .chatResponse()

            val thoughts = response ?: return false
            val toolCalls = thoughts.result.output.toolCalls

            logger.info("""😇 {}'s thoughts: 🗯{}🗯""", name, thoughts.result.output)
            logger.info("🛠️ {} selected {} tools to use | {}", name, toolCalls.size, toolCalls.map { it.name })

            val answer = thoughts.result.output.text
            if (!answer.isNullOrEmpty()) {
                logger.info("""✨ {}'s thoughts: 🗯{}🗯""", name, answer)
            }

            if (toolCalls.isNotEmpty()) {
                logger.info("""🎯 Tools prepared: {}""", toolCalls.map { it.name })
            }

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

    override fun act(): String {
        val response0 = response ?: return "Illegal state: null response"

        try {
            val results: MutableList<String> = ArrayList()

            val result = toolCallingManager.executeToolCalls(userPrompt, response0)
            val index = result.conversationHistory().size - 1
            val responseMessage = result.conversationHistory()[index] as ToolResponseMessage
            llmService.agentMemory.add(conversationId, responseMessage)

            val llmCallResponse = responseMessage.responses[0].responseData()

            results.add(llmCallResponse)

            logger.info("🔧 Tool response | {} | {}", name, StringUtils.abbreviate(llmCallResponse, 1000))

            return results.joinToString("\n\n")
        } catch (e: Exception) {
            val toolCall = response0.result.output.toolCalls[0]
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
