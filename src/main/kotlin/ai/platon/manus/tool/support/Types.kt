package ai.platon.manus.tool.support

data class ExecutionResult(
    var output: String? = null,
    var exitCode: Int? = null
)

data class CodeExecutionResult(
    var exitcode: Int? = null,
    var logs: String? = null,
    var image: String? = null
)

data class ToolExecuteResult(
    var message: String = "",
    var isInterrupted: Boolean = false
)
