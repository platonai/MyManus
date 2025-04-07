package ai.platon.manus.agent

import ai.platon.manus.api.service.LlmService
import ai.platon.manus.tool.Bash
import ai.platon.manus.tool.FileSaver
import ai.platon.manus.tool.Summary
import org.apache.commons.lang3.SystemUtils
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.tool.ToolCallback
import java.util.concurrent.atomic.AtomicReference

class FileAgent(
    llmService: LlmService, toolCallingManager: ToolCallingManager, private val workingDirectory: String
) : ToolCallAgent(llmService, toolCallingManager) {
    private val currentFileState = AtomicReference<Map<String, Any>>()

    override val name = "FILE_AGENT"

    override val description = """
A file system agent capable of performing read/write operations across multiple file formats

- Supports: Reading | Writing | Format Conversion
- Compatible Formats: Text (TXT, CSV), Documents (PDF, DOCX), Data (JSON, XML), Binary Files, etc.

        """.trimIndent()

    override val nextStepMessage: Message
        get() = PromptTemplate(FILE_AGENT_NEXT_STEP_PROMPT).createMessage(data)

    override fun act(): String {
        val result = super.act()
        updateFileState("file_operation", result)
        return result
    }

    override fun addThinkPrompt(messages: MutableList<Message>): Message {
        super.addThinkPrompt(messages)
        return SystemPromptTemplate(FILE_AGENT_SYSTEM_PROMPT).createMessage(data).also { messages.add(it) }
    }

    override val toolCallList = prepareToolCallList()

    override var data: Map<String, Any?>
        get() {
            val moreData = super.data.toMutableMap()

            val state = currentFileState.get()
            moreData["working_directory"] = workingDirectory
            moreData["last_operation"] = state?.get("operation") ?: "No previous operation"
            moreData["operation_result"] = state?.get("result")

            return moreData
        }
        set(data) {
            super.data = data
        }

    private fun updateFileState(operation: String, result: String) {
        val state: MutableMap<String, Any> = HashMap()
        state["operation"] = operation
        state["result"] = result
        currentFileState.set(state)
    }

    private fun prepareToolCallList(): List<ToolCallback> {
        val tools = listOf<ToolCallback>(
            FileSaver.functionToolCallback,
            Summary.getFunctionToolCallback(this, llmService.memory, conversationId)
        )

        val moreTools = when {
            SystemUtils.IS_OS_LINUX -> listOf(
                Bash.getFunctionToolCallback(workingDirectory)
            )

            else -> emptyList()
        }

        return tools + moreTools
    }
}
