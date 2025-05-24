package ai.platon.manus.agent.plan

import ai.platon.manus.agent.MyAgent
import org.springframework.ai.tool.ToolCallback

enum class StepStatus(
    val value: String,
    val mark: String,
    val emoji: String
) {
    NOT_STARTED("not_started", "[ ]", """🔜"""),
    IN_PROGRESS("in_progress", "[→]", """🚀"""),
    COMPLETED("completed", "[✓]", "✅"),
    BLOCKED("blocked", "[!]", "❗");

    override fun toString() = value

    companion object {
        val allStatuses = entries.map { it.value }

        val activeStatuses = listOf(
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

    abstract val toolCallbacks: List<ToolCallback>
}
