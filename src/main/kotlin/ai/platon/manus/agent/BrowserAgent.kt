package ai.platon.manus.agent

import ai.platon.manus.api.service.LlmService
import ai.platon.manus.common.*
import ai.platon.manus.tool.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.tool.ToolCallback
import java.util.concurrent.atomic.AtomicReference

class BrowserAgent(
    llmService: LlmService, toolCallingManager: ToolCallingManager
) : ToolCallAgent(llmService, toolCallingManager) {
    companion object {
        // Agent name and description
        const val BROWSER_AGENT_NAME = "BROWSER_AGENT"
        const val BROWSER_AGENT_DESCRIPTION = "A browser agent that can control a browser to accomplish tasks"
    }

    private val logger: Logger = LoggerFactory.getLogger(BrowserAgent::class.java)
    private val currentStepBrowserCache = AtomicReference<Map<String, Any?>>()

    private val browserState: Map<String, Any?>
        get() = getBrowserStateWithCache()

    override val name = BROWSER_AGENT_NAME

    override val description = BROWSER_AGENT_DESCRIPTION

    override var data: Map<String, Any?>
        get() = doGetData()
        set(value) {
            super.data = value
        }

    override val nextStepMessage: Message
        get() = PromptTemplate(BROWSER_AGENT_NEXT_STEP_PROMPT).createMessage(data)

    override fun think(): Boolean {
        currentStepBrowserCache.set(null)
        return super.think()
    }

    override fun addThinkPrompt(messages: MutableList<Message>): Message {
        super.addThinkPrompt(messages)
        return SystemPromptTemplate(BROWSER_AGENT_SYSTEM_PROMPT).createMessage(data).also { messages.add(it) }
    }

    override val toolCallList: List<ToolCallback> = listOf(
        GoogleSearch.functionToolCallback,
        FileSaver.functionToolCallback,
        PythonTool.functionToolCallback,
        BrowserUseTool.getFunctionToolCallback(),
        Summary.getFunctionToolCallback(
            this,
            llmService.memory,
            conversationId
        )
    )

    private fun doGetData(): Map<String, Any?> {
        val data: MutableMap<String, Any?> = super.data.toMutableMap()

        val tabs = browserState[STATE_TABS] as? List<*>?

        data[PLACEHOLDER_URL] = String.format(FORMAT_URL_INFO, browserState[STATE_URL], browserState[STATE_TITLE])
        data[PLACEHOLDER_INTERACTIVE_ELEMENTS] = (browserState[STATE_INTERACTIVE_ELEMENTS] as? String?) ?: ""
        data[PLACEHOLDER_RESULTS] = ""
        data[PLACEHOLDER_TABS] = (if (tabs.isNullOrEmpty()) "" else String.format(FORMAT_TABS_INFO, tabs.size))

        val scrollInfo = browserState[STATE_SCROLL_INFO] as? Map<*, *>?
        val pixelsAbove = AnyNumberConvertor(scrollInfo?.get(SCROLL_PIXELS_ABOVE)).toIntOrNull()
        val pixelsBelow = AnyNumberConvertor(scrollInfo?.get(SCROLL_PIXELS_BELOW)).toIntOrNull()
        data[PLACEHOLDER_CONTENT_ABOVE] = pixelsAbove?.let { String.format(FORMAT_SCROLL_INFO, it) } ?: ""
        data[PLACEHOLDER_CONTENT_BELOW] = pixelsBelow?.let { String.format(FORMAT_SCROLL_INFO, it) } ?: ""

        data[STATE_HELP] = browserState[STATE_HELP]
        data[STATE_SCREENSHOT] = browserState[STATE_SCREENSHOT] as? String?

        return data
    }

    private fun getBrowserStateWithCache(): Map<String, Any?> {
        currentStepBrowserCache.compareAndSet(null, BrowserUseTool.INSTANCE.currentState)
        return currentStepBrowserCache.get()
    }
}
