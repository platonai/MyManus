package ai.platon.manus.agent.plan

import ai.platon.manus.agent.AbstractAgent
import ai.platon.manus.agent.FINALIZE_PLAN_PROMPT
import ai.platon.manus.agent.INITIAL_PLAN_PROMPT
import ai.platon.manus.agent.MyManusAgent
import ai.platon.manus.api.service.LlmService
import ai.platon.manus.tool.PlanningTool
import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.common.warnInterruptible
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.tool.ToolCallback
import java.util.*
import java.util.regex.Pattern

class PlanningFlow(
    var llmService: LlmService,
    agents: List<AbstractAgent>,
    data: MutableMap<String, Any> = mutableMapOf(),
) : FlowBase(agents, data) {
    private val logger = LoggerFactory.getLogger(PlanningFlow::class.java)
    protected val conversationLogger = LoggerFactory.getLogger("ai.platon.manus.conversation")

    private var planningTool: PlanningTool
    private var executorKeys: MutableList<String> = mutableListOf()
    private val finishedPlans = mutableSetOf<String>()

    var planId: String
        private set

    private var currentStepIndex: Int = -1

    override val toolCallbacks: MutableList<ToolCallback> = mutableListOf(PlanningTool.functionToolCallback)

    val currentPlanContent: String get() = getCurrentPlanContent(planId)

    init {
        if (executorKeys.isEmpty()) {
            agents.map { it.name.uppercase() }.toCollection(executorKeys)
        }

        planId = if (data.containsKey("plan_id")) {
            data.remove("plan_id") as String
        } else {
            "plan_" + System.currentTimeMillis()
        }

        this.planningTool = data.computeIfAbsent("planning_tool") { PlanningTool.INSTANCE } as PlanningTool
    }

    constructor(llmService: LlmService, vararg agents: AbstractAgent): this(llmService, agents.toList())

    fun newPlan(planID: String) {
        this.planId = planID
    }

    override fun execute(inputText: String): List<String> {
        if (inputText.isBlank()) {
            return listOf()
        }

        createInitialPlan(inputText)

        if (!planningTool.hasPlan(planId)) {
            val message = "Failed to create a plan. Plan not found | #$planId | $inputText"
            logger.warn(message)
            return listOf(message)
        }

        return try {
            executeStepByStep()
        } catch (e: Exception) {
            val message = "Failed to execute the planning flow | " + e.message
            warnInterruptible(this, e, message)
            listOf(message)
        } finally {
        }
    }

    private fun executeStepByStep(): List<String> {
        val results = mutableListOf<String>()

        while (true) {
            val info = currentStepInfo()
            if (info == null) {
                if (planningTool.hasPlan(planId)) {
                    logger.info("Plan not found | {}", planId)
                    results.add(finalizeConversation(planId))
                } else {
                    logger.info("Plan is already finished | {}", planId)
                }
                break
            }

            currentStepIndex = info.first
            val stepInfo = info.second

            val stepType = stepInfo["type"]
            val executor = chooseBestAgent(stepType)
            executor.conversationId = planId

            val stepResults = executeStep(executor, stepInfo)
            results.addAll(stepResults)
        }

        return results
    }

    internal fun askForAnInitialPlan(request: String): ChatResponse? {
        if (logger.isInfoEnabled) {
            val brief = request.replace("\\s+".toRegex(), " ")
                .replace("\\p{Cntrl}".toRegex(), " ")
                .let { StringUtils.abbreviate(it, 0, 80) }
            logger.info("Asking a initial plan | #{} | {}", planId, brief)
        }

        logger.info("Available agents: {}", agents.joinToString { it.name })

        val agentsInfo = agents.joinToString("\n", "Available agents:\n") {
            "- Agent name: ${it.name} description: ${it.description}"
        }
        val params = mapOf(
            "plan_id" to planId,
            "query" to request,
            "agents_info" to agentsInfo
        )

        val prompt = PromptTemplate(INITIAL_PLAN_PROMPT).create(params)
        val llmRequest = llmService.planningChatClient
            .prompt(prompt)
            .toolCallbacks(toolCallbacks)
            .user(request)
        // conversationLogger.info("\n\n-------------------\nMyManus:\n{}", llmRequest.messages)

        val useMemory = alwaysTrue()
        if (useMemory) {
            llmRequest.advisors(MessageChatMemoryAdvisor.builder(llmService.conversationMemory).build())
        }

        val llmResponse = llmRequest.call()

        val response = llmResponse.chatResponse()
        conversationLogger.info("\n\nAI:\n{}", prettyPulsarObjectMapper().writeValueAsString(response))

        return response
    }

    internal fun createInitialPlan(request: String) {
        val response = askForAnInitialPlan(request)

        // Notice: use `currentPlanContent` to see the content of AI's response
        val outputText = response?.result?.output?.text
        if (outputText != null) {
            val plan = outputText.replace("\\n+".toRegex(), "\\n")
            logger.info("Plan: $plan")
            return
        }

        val params = createDefaultPlanParameters(request)
        // planningTool.run(pulsarObjectMapper().writeValueAsString(params))
        planningTool.run(params)
    }

    private fun createDefaultPlanParameters(request: String): Map<String, Any?> {
        logger.warn("Creating default plan parameters")

        return mapOf<String, Any?>(
            "command" to "create",
            "plan_id" to planId,
            "title" to StringUtils.abbreviate("Plan for: $request", 100),
            "steps" to mutableListOf("Analyze request", "Execute task", "Verify results")
        )
    }

    private fun currentStepInfo(): Pair<Int, Map<String, String>>? {
        val planData = planningTool.plans[planId] ?: return null

        try {
            val steps = planData.getOrDefault("steps", ArrayList<String>()) as List<String>
            val stepStatuses = planData.getOrDefault("step_statuses", ArrayList<String>()) as MutableList<String>

            for (i in steps.indices) {
                val status = if (i >= stepStatuses.size) {
                    StepStatus.NOT_STARTED.value
                } else {
                    stepStatuses[i]
                }

                if (StepStatus.activeStatuses.contains(status)) {
                    val stepInfo: MutableMap<String, String> = HashMap()
                    stepInfo["text"] = steps[i]

                    val pattern = Pattern.compile("\\[([A-Z_]+)\\]")
                    val matcher = pattern.matcher(steps[i])
                    if (matcher.find()) {
                        stepInfo["type"] = matcher.group(1).lowercase(Locale.getDefault())
                    }

                    try {
                        val args = mapOf(
                            "command" to "mark_step",
                            "plan_id" to planId,
                            "step_index" to i,
                            "step_status" to StepStatus.IN_PROGRESS.value
                        )
                        planningTool.run(args)
                    } catch (e: Exception) {
                        logger.warn("Error marking step as in_progress", e)
                        if (i < stepStatuses.size) {
                            stepStatuses[i] = StepStatus.IN_PROGRESS.value
                        } else {
                            while (stepStatuses.size < i) {
                                stepStatuses.add(StepStatus.NOT_STARTED.value)
                            }
                            stepStatuses.add(StepStatus.IN_PROGRESS.value)
                        }
                        planData["step_statuses"] = stepStatuses
                    }

                    return Pair<Int, Map<String, String>>(i, stepInfo)
                }
            }

            return null
        } catch (e: Exception) {
            logger.warn("Error finding current step index: " + e.message)
            return null
        }
    }

    internal fun executeStep(agent: AbstractAgent, stepInfo: Map<String, String>): List<String> {
        try {
            val planStatus = currentPlanContent
            val stepText = stepInfo["text"] ?: "Step $currentStepIndex"

            try {
                val stepData = mapOf(
                    "planStatus" to planStatus, "currentStepIndex" to currentStepIndex, "stepText" to stepText
                )

                val stepResults = agent.run(stepData)

                markCurrentStepCompleted()

                return stepResults
            } catch (e: Exception) {
                val message = """Failed to execute step #$currentStepIndex 🫨"""
                if (logger.isDebugEnabled) {
                    logger.debug(message, e)
                } else {
                    logger.warn("$message | ${e.message}")
                }
                return listOf("$message | ${e.message}")
            }
        } catch (e: Exception) {
            logger.warn("Error preparing execution context: " + e.message)
            return listOf("Error preparing execution context: " + e.message)
        }
    }

    private fun markCurrentStepCompleted(): ToolExecuteResult? {
        if (currentStepIndex < 0) {
            logger.info("Plan flow not started yet")
            return null
        }

        try {
            val args = mapOf(
                "command" to "mark_step",
                "plan_id" to planId,
                "step_index" to currentStepIndex,
                "step_status" to StepStatus.COMPLETED.value
            )
            val result = planningTool.run(args)
            logger.info("Marked plan step as completed | $currentStepIndex | $planId")

            return result
        } catch (e: Exception) {
            logger.warn("Failed to update plan status: " + e.message)

            val plans = planningTool.plans
            val planData = plans[planId] ?: return null

            val stepStatuses = planData.getOrDefault("step_statuses", ArrayList<String>()) as MutableList<String>

            while (stepStatuses.size <= currentStepIndex) {
                stepStatuses.add(StepStatus.NOT_STARTED.value)
            }

            stepStatuses[currentStepIndex] = StepStatus.COMPLETED.value
            planData["step_statuses"] = stepStatuses
        }

        return null
    }

    internal fun chooseBestAgent(stepType: String?): AbstractAgent {
        val agent = agents.firstOrNull { it.name.equals(stepType, ignoreCase = true) }
        if (agent != null) {
            return agent
        }

        var backup = agents.firstOrNull { it.name.equals(MyManusAgent.NAME, ignoreCase = true) }
        if (backup != null) {
            return backup
        }

        backup = agents.firstOrNull()
        check(backup is AbstractAgent) { "No agents available in the system" }
        logger.warn("Pick the first agent as default | {}", backup.name)

        return backup
    }

    private fun generatePlanTextFromStorage(): String {
        try {
            val plans = planningTool.plans
            if (!plans.containsKey(planId)) {
                return "Error: Plan with ID $planId not found"
            }

            val planData = plans[planId]!!
            val title = planData.getOrDefault("title", "Untitled Plan") as String
            val steps = planData.getOrDefault("steps", ArrayList<String>()) as List<String>
            val stepStatuses = planData.getOrDefault("step_statuses", ArrayList<String>()) as MutableList<String>
            val stepNotes = planData.getOrDefault("step_notes", ArrayList<String>()) as MutableList<String>

            while (stepStatuses.size < steps.size) {
                stepStatuses.add(StepStatus.NOT_STARTED.value)
            }
            while (stepNotes.size < steps.size) {
                stepNotes.add("")
            }

            val statusCounts: MutableMap<String, Int> = HashMap()
            for (status in StepStatus.allStatuses) {
                statusCounts[status] = 0
            }

            for (status in stepStatuses) {
                statusCounts[status] = statusCounts.getOrDefault(status, 0) + 1
            }

            val completed = statusCounts[StepStatus.COMPLETED.value]!!
            val total = steps.size
            val progress = if (total > 0) (completed / total.toDouble()) * 100 else 0.0

            val planText = StringBuilder()
            planText.append("Plan: ").append(title).append(" (ID: ").append(planId).append(")\n")

            for (i in 0..<planText.length - 1) {
                planText.append("=")
            }
            planText.append("\n\n")

            planText.append(String.format("Progress: %d/%d steps completed (%.1f%%)\n", completed, total, progress))
            planText.append(
                String.format(
                    "Status: %d completed, %d in progress, ",
                    statusCounts[StepStatus.COMPLETED.value],
                    statusCounts[StepStatus.IN_PROGRESS.value]
                )
            )
            planText.append(
                String.format(
                    "%d blocked, %d not started\n\n",
                    statusCounts[StepStatus.BLOCKED.value],
                    statusCounts[StepStatus.NOT_STARTED.value]
                )
            )
            planText.append("Steps:\n")

            for (i in steps.indices) {
                val step = steps[i]
                val status = stepStatuses[i]
                val notes = stepNotes[i]
                val mark = StepStatus.valueOf(status).mark

                planText.append(String.format("%d. %s %s\n", i, mark, step))
                if (notes.isNotEmpty()) {
                    planText.append("   Notes: ").append(notes).append("\n")
                }
            }

            return planText.toString()
        } catch (e: Exception) {
            logger.warn("Error generating plan text from storage: " + e.message)
            return "Error: Unable to retrieve plan with ID $planId"
        }
    }

    internal fun requestFinalizePlan(prompt: String): ChatResponse? {
        return llmService.finalizeChatClient
            .prompt()
//            .advisors(MessageChatMemoryAdvisor(llmService.memory))
//            .advisors { it.param(CHAT_MEMORY_CONVERSATION_ID_KEY, planId)
//                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)}
            .user(prompt)
            .call()
            .chatResponse()
    }

    internal fun finalizeConversation(planID: String): String {
        val planText = currentPlanContent
        try {
            val prompt = FINALIZE_PLAN_PROMPT.format(planText)

            val response = requestFinalizePlan(prompt)

            return """
## Plan Summary:

${response?.result?.output?.text}

"""
        } catch (e: Exception) {
            logger.warn("Failed to finalize plan with LLM | {}", e.message)
            return "Plan completed. Failed to generating summary."
        } finally {
            finishedPlans.add(planID)
            currentStepIndex = -1
        }
    }

    private fun getCurrentPlanContent(planId: String): String {
        try {
            val args = mapOf("command" to "get", "plan_id" to planId)
            val result = planningTool.run(args)
            return result.message
        } catch (e: Exception) {
            logger.warn("Failed to get a plan | " + e.message)
            return generatePlanTextFromStorage()
        }
    }
}
