package ai.platon.manus.tool

import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

abstract class AbstractTool : Tool {
    override fun apply(args: Any): ToolExecuteResult {
        return when (args) {
            is String -> run(pulsarObjectMapper().readValue(args))
            is Map<*, *> -> run(args as Map<String, Any?>)
            else -> ToolExecuteResult("Invalid argument type | $args")
        }
    }

    abstract fun run(args: Map<String, Any?>): ToolExecuteResult

    override fun close() {

    }
}
