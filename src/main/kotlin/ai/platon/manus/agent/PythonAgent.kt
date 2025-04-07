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

    override val name: String
        get() = "PYTHON_AGENT"

    override val description: String
        get() = """
**PYTHON AGENT**

The Python Agent can directly execute Python code and return the results in a single step.  
It supports popular libraries such as `math`, `numpy`, `numexpr`, and others.

**Usage:**
- One agent step allows you to write and execute Python code without separating the process.
- **Input:** Describe the task you want the Python code to perform.
- **Output:** The result produced by executing the code.

				""".trimIndent()

    override val nextStepMessage: Message
        get() = PromptTemplate(PYTHON_AGENT_NEXT_STEP_PROMPT).createMessage(data)

    override fun addThinkPrompt(messages: MutableList<Message>): Message {
        super.addThinkPrompt(messages)
        return SystemPromptTemplate(PYTHON_AGENT_SYSTEM_PROMPT).createMessage(data).also { messages.add(it) }
    }

    override fun act(): String {
        val result = super.act()
        updateExecutionState(result)
        return result
    }

    override val toolCallList: List<ToolCallback>
        get() = listOf<ToolCallback>(
            PythonTool.functionToolCallback, Summary.getFunctionToolCallback(
                this, llmService.memory, conversationId
            )
        )

    override var data: Map<String, Any?>
        get() {
            val data: MutableMap<String, Any?> = HashMap()
            val parentData = super.data
            data.putAll(parentData)

            data["working_directory"] = workingDirectory
            data["last_result"] = (if (lastResult != null) lastResult else "No previous execution")!!

            return data
        }
        set(data) {
            super.data = data
        }

    private fun updateExecutionState(result: String?) {
        this.lastResult = result
    }
}
