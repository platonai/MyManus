package ai.platon.manus.agent.plan

import ai.platon.manus.agent.MyAgent
import org.springframework.ai.tool.ToolCallback

enum class StepStatus(
    val value: String,
    val mark: String,
    val emoji: String
) {
    NOT_STARTED("not_started", "[ ]", "\uD83D\uDD1C"),
    IN_PROGRESS("in_progress", "[→]", "\uD83D\uDE80"),
    COMPLETED("completed", "[✓]", "✅"),
    BLOCKED("blocked", "[!]", "❗");

    override fun toString(): String {
        return value
    }

    companion object {
        val allStatuses: List<String> = entries.map { it.value }.toList()

        val activeStatuses: List<String> = listOf(
            NOT_STARTED.value, IN_PROGRESS.value
        )

        fun fromValue(value: String?, defaultValue: StepStatus = NOT_STARTED): StepStatus {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: defaultValue
        }
    }
}

abstract class FlowBase(
    var agents: List<MyAgent>,
    val data: MutableMap<String, Any>
) {
    init {
        data["agents"] = agents
    }

    abstract fun execute(inputText: String): String

    abstract val tools: List<ToolCallback>
}
