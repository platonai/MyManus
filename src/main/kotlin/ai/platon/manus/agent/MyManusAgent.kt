package ai.platon.manus.agent

import ai.platon.manus.api.service.LlmService
import ai.platon.manus.tool.*
import org.apache.commons.lang3.SystemUtils
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.tool.ToolCallback

class MyManusAgent(
    llmService: LlmService,
    toolCallingManager: ToolCallingManager,
    private val workingDirectory: String
) : ToolCallAgent(llmService, toolCallingManager) {
    companion object {
        const val NAME = "MyManus"
    }

    override val name: String = NAME

    override val description = "A faithful agent that can solve various tasks using multiple tools"

    override val toolCallList: List<ToolCallback> = prepareToolCallList()

    private fun prepareToolCallList(): List<ToolCallback> {
        val tools = listOf(
            GoogleSearch.functionToolCallback,
            FileSaver.functionToolCallback,
            PythonTool.functionToolCallback,
            BrowserUseTool.getFunctionToolCallback(),
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
