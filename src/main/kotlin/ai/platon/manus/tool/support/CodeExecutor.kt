package ai.platon.manus.tool.support

import ai.platon.manus.common.MyContext
import ai.platon.pulsar.common.ProcessLauncher
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists

object CodeExecutor {
    private val logger: Logger = LoggerFactory.getLogger(CodeExecutor::class.java)

    fun execute(
        code: String,
        lang: String,
        filename: String? = null,
        kwargs: Map<String, Any> = mapOf()
    ): CodeExecutionResult {
        logger.info("lang: $lang, file: $filename, args: $kwargs")

        var workDir = if (kwargs.containsKey("work_dir")) kwargs["work_dir"] as String? else null
        if (workDir == null) {
            workDir = MyContext.AGENT_WORKING_DIR
        }

        val destination = filename ?: "my.${RandomStringUtils.randomAlphanumeric(12)}.$lang"
        val path = Paths.get(workDir)
            .resolve("generated-sources/myManus")
            .resolve(destination)
        Files.createDirectories(path.parent)
        Files.writeString(path, code)

        check(path.exists())

        val result = when (lang) {
            "python" -> executeCommand("python", listOf("\"$path\""))
            "sh" -> executeCommand("sh", listOf("\"$path\""))
            "java" -> executeCommand("java", listOf("\"$path\""))
            else -> executeCommand(lang, listOf("\"$path\""))
        }

        return CodeExecutionResult(exitcode = result?.exitCode, logs = result?.output)
    }

    private fun executeCommand(executable: String, arguments: List<String>): ExecutionResult? {
        val process = ProcessLauncher.launch(executable, arguments)
        val output = waitFor(process)
        logger.info("Process output:\n{}", output.take(10).joinToString())
        return ExecutionResult(output.joinToString(), process.exitValue())
    }

    private fun waitFor(process: Process): List<String> {
        val processOutput = mutableListOf<String>()
        val readLineThread = Thread {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                // Wait for DevTools listening line and extract port number.
                var line: String? = reader.readLine()
//                var line: String? = String(reader.readLine().toByteArray(Charset.defaultCharset()))
                while (line != null) {
                    if (line.isNotBlank()) {
                        // logger.info("[output] - $line")
                    }

                    processOutput.add(line)

                    line = reader.readLine()
                }
            }
        }
        readLineThread.start()

        try {
            readLineThread.join(2 * 1000 * 60)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn("Interrupted while waiting for devtools server, close it", e)
            close(readLineThread)
        }

        return processOutput
    }

    private fun close(thread: Thread) {
        try {
            thread.join(2 * 1000 * 60)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun read(reader: BufferedReader): String {
        val lines: MutableList<String> = ArrayList()
        var response = StringUtils.EMPTY
        while (true) {
            try {
                if ((reader.readLine()?.also { response = it }) == null) {
                    break
                }
            } catch (e: IOException) {
            }

            lines.add(response)
        }
        return lines.joinToString("\n")
    }
}
