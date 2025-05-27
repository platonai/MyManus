package ai.platon.manus.agent

import ai.platon.manus.api.service.LlmService
import ai.platon.manus.tool.PythonTool
import ai.platon.manus.tool.Summary
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.tool.ToolCallback

class PythonAgent(
    llmService: LlmService,
    toolCallingManager: ToolCallingManager,
    private val workingDirectory: String
) : ToolCallAgent(llmService, toolCallingManager) {
    private var lastResult: String? = null

    override val name: String = "PYTHON_AGENT"

    override val description: String
        get() = """
Executes Python code string.
Note: 
- Only print outputs are visible, function return values are not captured. 
- Use print statements to see results.
				""".trimIndent()

    override val nextStepMessage: Message
        get() = PromptTemplate(PYTHON_AGENT_NEXT_STEP_PROMPT).createMessage(data)

    override fun addThinkPromptTo(messages: MutableList<Message>): Message {
        super.addThinkPromptTo(messages)
        return SystemPromptTemplate(PYTHON_AGENT_SYSTEM_PROMPT).createMessage(data).also { messages.add(it) }
    }

    override fun act(): List<String> {
        val results = super.act()
        updateExecutionState(results)
        return results
    }

    override val toolCallbacks: List<ToolCallback>
        get() = listOf<ToolCallback>(
            PythonTool.functionToolCallback,
            Summary.getFunctionToolCallback(this, llmService.agentMemory, conversationId)
        )

    override var data: Map<String, Any?>
        get() {
            val lr = lastResult ?: "No previous execution"
            return super.data + mapOf("last_result" to lr, "working_directory" to workingDirectory)
        }
        set(data) {
            super.data = data
        }

    private fun updateExecutionState(results: List<String>) {
        this.lastResult = results.joinToString("\n")
    }
}
