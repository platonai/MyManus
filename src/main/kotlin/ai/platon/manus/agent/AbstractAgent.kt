package ai.platon.manus.agent

import ai.platon.manus.api.service.LlmService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.tool.ToolCallback

enum class AgentState(private val state: String) {
    IDLE("IDLE"), RUNNING("RUNNING"), FINISHED("FINISHED"), ERROR("ERROR");
    override fun toString() = state
}

abstract class AbstractAgent(
    protected var llmService: LlmService
): AutoCloseable {
    private val logger: Logger = LoggerFactory.getLogger(AbstractAgent::class.java)

    protected val conversationLogger = LoggerFactory.getLogger("ai.platon.manus.conversation")

    var conversationId: String = ""

    var state = AgentState.IDLE

    /**
     * Do not exceed the max steps.
     * */
    private val maxSteps = 8

    private var currentStep = 0

    abstract val name: String

    abstract val description: String

    open var data: Map<String, Any?> = HashMap()

    protected abstract fun addThinkPromptTo(messages: MutableList<Message>): Message

    /**
     * The next step message for the tool call agent.
     * The agent should provide a clear next step and should not use the default.
     * */
    protected abstract val nextStepMessage: Message

    abstract val toolCallbacks: List<ToolCallback>

    /**
     * Run the agent with the given data. Returns the information of each step.
     *
     * @param data The data to run the agent with.
     * @return The information of each step.
     * */
    fun run(data: Map<String, Any>): String {
        return runStepByStep(data)
    }

    /**
     * Perform the next step
     * */
    protected abstract fun step(): String

    override fun close() {

    }

    @Synchronized
    private fun runStepByStep(data: Map<String, Any>): String {
        check(state == AgentState.IDLE) { "Cannot run agent from state: $state" }

        this.data = data
        currentStep = 0
        state = AgentState.RUNNING

        val results = mutableListOf<String>()

        try {
            while (currentStep < maxSteps && state != AgentState.FINISHED) {
                currentStep++
                logger.info("""🔥 Executing step $currentStep (limit $maxSteps)""")
                val stepResult = step()
                if (isHung()) {
                    handleAgentHung()
                }
                results.add("Step $currentStep: $stepResult")
            }

            if (currentStep >= maxSteps) {
                results.add("Terminated. Step: $currentStep (limit $maxSteps)")
            }
        } finally {
            state = AgentState.IDLE
        }

        return results.joinToString("\n")
    }

    private fun handleAgentHung() {
        logger.warn("""💔 Agent hanging - no tool calls""")
        state = AgentState.FINISHED

        val message = """
No tool calls were detected in the agent's response. All responses must include at least 
one necessary tool call to proceed with the task.

- Current step: %d
- Current Execution State: terminated forcibly

"""

        logger.warn(message.trimIndent().format(currentStep))
    }

    private fun isHung(): Boolean {
        // take the last 10 round messages
        val count = llmService.agentMemory.get(conversationId)
            .filterIsInstance<AssistantMessage>().count { it.toolCalls.isEmpty() }
        return count >= 3
    }
}
