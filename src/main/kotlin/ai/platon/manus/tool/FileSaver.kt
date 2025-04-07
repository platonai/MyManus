package ai.platon.manus.tool

import ai.platon.manus.tool.support.ToolExecuteResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.function.FunctionToolCallback
import java.nio.file.Files
import java.nio.file.Paths

class FileSaver : AbstractTool() {
    override fun run(args: Map<String, Any?>): ToolExecuteResult {
        log.info("FileSaver | $args")

        try {
            val content = args["content"]?.toString() ?: return ToolExecuteResult("Content is empty")
            val filePath = args["file_path"]?.toString() ?: return ToolExecuteResult("File path is empty")

            val path = Paths.get(filePath)
            Files.createDirectories(path.parent)
            Files.writeString(path, content)

            return ToolExecuteResult("Content successfully saved | $filePath")
        } catch (e: Throwable) {
            return ToolExecuteResult("Failed to save file | " + e.message)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FileSaver::class.java)

        private val PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "content": {
			            "type": "string",
			            "description": "(required) The content to save to the file."
			        },
			        "file_path": {
			            "type": "string",
			            "description": "(required) The target file path, including the filename and extension, where the content will be written."
			        }
			    },
			    "required": ["content", "file_path"]
			}
			
			""".trimIndent()

        private const val name = "file_saver"

        private val description = """
### Save Content to a Local File

Use this tool to save text, code, or other generated content to a specified file on the local filesystem.

It accepts two parameters: 
- `content`: The content to be saved.
- `file_path`: The target file path, the content is then written to the location.

			""".trimIndent()

        val functionToolCallback: FunctionToolCallback<*, *>
            get() = FunctionToolCallback.builder(name, FileSaver())
                .description(description)
                .inputSchema(PARAMETERS)
                .inputType(Map::class.java)
                .build()
    }
}
