
package ai.platon.manus.tool.support.llmbash

import ai.platon.manus.tool.support.CodeExecutor

object BashProcess {

    fun executeCommand(commandList: List<String>, workingDirectory: String?): List<String?> {
        return commandList.map { commandLine ->
            CodeExecutor.execute(commandLine, "bash", workingDirectory)
            null
        }
    }
}
