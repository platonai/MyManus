package ai.platon.manus.tool

import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.manus.tool.support.llmbash.BashProcess
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.function.FunctionToolCallback
import java.nio.file.Path
import java.nio.file.Paths

class Bash(
    private var workingDirectory: Path
) : AbstractTool() {

    constructor(workingDirectory: String): this(Paths.get(workingDirectory))

    override fun run(args: Map<String, Any?>): ToolExecuteResult {
        logger.info("Bash: $args")

        val command = args["command"]?.toString()
            ?: return ToolExecuteResult("""{"output":"", "exitCode":-1}""")

        val commandList = mutableListOf(command)
        val result = BashProcess.executeCommand(commandList, workingDirectory.toString())

        return ToolExecuteResult(pulsarObjectMapper().writeValueAsString(result))
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Bash::class.java)

        private val PARAMETERS = """
			{
				"type": "object",
				"properties": {
					"command": {
						"type": "string",
						"description": "The Bash command to execute. This field can be left empty to retrieve additional logs if the previous exit code was -1, or set to ctrl+c to interrupt the currently running process."
					}
				},
				"required": ["command"]
			}
			
			""".trimIndent()

        private const val name = "bash"

        private val description = """
### Execute a Bash Command in the Terminal

**Long-Running Commands:**  
For commands that may run indefinitely, execute them in the background and redirect their output to a file.  
Example:  
```bash
command = "python3 app.py > server.log 2>&1 &"
```

**Interactive Commands:**  
If a Bash command returns an exit code of `-1`, it indicates that the process is still running. In this case, the assistant must take one of the following actions:
- Send a second request to the terminal with an empty `command` field to retrieve additional logs.
- Send additional input text by setting the `command` field to the desired text, which will be written to the STDIN of the running process.
- Send `command = "ctrl+c"` to interrupt and terminate the running process.

**Timeout Handling:**  
If the execution result indicates `"Command timed out. Sending SIGINT to the process"`, the assistant should retry the command execution by running it in the background.

			""".trimIndent()

        fun getFunctionToolCallback(workingDirectory: String): FunctionToolCallback<*, *> {
            return FunctionToolCallback.builder(name, Bash(workingDirectory))
                .description(description)
                .inputSchema(PARAMETERS)
                .inputType(Map::class.java).build()
        }
    }
}
