package ai.platon.manus.tool

import ai.platon.manus.tool.support.CodeExecutor
import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.pulsar.common.AppPaths
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.tool.function.FunctionToolCallback

class PythonTool : AbstractTool() {

    override fun run(args: Map<String, Any?>): ToolExecuteResult {
        val code = (args["code"] as String?) ?: return ToolExecuteResult("No code provided | $args")

        val output = CodeExecutor.execute(code, "python", AppPaths.random("my.", ".py"))

        val result = output.logs ?: ""
        return ToolExecuteResult(result)
    }

    companion object {

        private val PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "code": {
			            "type": "string",
			            "description": "The Python code to execute."
			        }
			    },
			    "required": ["code"]
			}
			
			""".trimIndent()

        private const val name = "python_execute"

        private val description = """
### Execute Python Code

Executes a Python code string in a controlled environment.
**Note:** Only output from `print` statements is visible. Return values from functions are not captured.
Use `print` statements to display results.

			""".trimIndent()

        val functionToolCallback: FunctionToolCallback<*, *>
            get() = FunctionToolCallback.builder(name, PythonTool())
                .description(description)
                .inputSchema(PARAMETERS)
                .inputType(Map::class.java)
                .build()
    }
}
