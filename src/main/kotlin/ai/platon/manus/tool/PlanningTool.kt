package ai.platon.manus.tool

import ai.platon.manus.agent.plan.StepStatus
import ai.platon.manus.common.AnyNumberConvertor
import ai.platon.manus.tool.support.ToolExecuteResult
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.function.FunctionToolCallback
import org.springframework.ai.tool.metadata.ToolMetadata
import java.time.OffsetDateTime

class PlanningTool : AbstractTool() {
    private val logger = LoggerFactory.getLogger(PlanningTool::class.java)
    private var _plans: MutableMap<String, MutableMap<String, Any>> = HashMap()

    val plans: Map<String, MutableMap<String, Any>> get() = _plans
    var currentPlanId: String? = null

    fun hasPlan(id: String) = plans.containsKey(id)

    override fun run(args: Map<String, Any?>): ToolExecuteResult {
        return try {
            performPlanningTask(args)
        } catch (e: Exception) {
            ToolExecuteResult(e.message ?: "Failed to do with a plan", true)
        }
    }

    private fun performPlanningTask(args: Map<String, Any?>): ToolExecuteResult {
        logger.info("PlanningTool arguments: $args")

        val command = args["command"] as? String ?: throw IllegalArgumentException("Command is required")
        val planId = args["plan_id"] as? String
        val title = args["title"] as? String
        val steps = args["steps"] as? List<String>
        val stepIndex = AnyNumberConvertor(args["step_index"]).toIntOrNull()
        val stepStatus = args["step_status"] as? String
        val stepNotes = args["step_notes"] as? String

        return when (command) {
            "create" -> {
                requireNotNull(planId) { "Plan ID is required | create" }
                requireNotNull(title) { "Title is required | create" }
                requireNotNull(steps) { "Steps are required | create" }
                createPlan(planId, title, steps)
            }

            "update" -> {
                requireNotNull(planId) { "Plan ID is required | update" }
                updatePlan(planId, title, steps)
            }

            "list" -> listPlans()
            "get" -> getPlan(planId)
            "set_active" -> {
                requireNotNull(planId) { "Plan ID is required | set_active" }
                setActivePlan(planId)
            }

            "mark_step" -> {
                requireNotNull(stepIndex) { "Step index is required | mark_step" }
                requireNotNull(stepStatus) { "Step status is required | mark_step" }
                markStep(planId, stepIndex, stepStatus, stepNotes)
            }

            "delete" -> {
                requireNotNull(planId) { "Plan ID is required | delete" }
                deletePlan(planId)
            }

            else -> throw IllegalArgumentException(
                "Unknown command: $command. Available commands: [create, update, list, get, set_active, mark_step, delete]"
            )
        }
    }

    fun createPlan(planId: String, title: String, steps: List<String>): ToolExecuteResult {
        require(planId.isNotBlank()) { "Plan id is required | create" }
        require(!plans.containsKey(planId)) { "Plan already exists | $planId | create" }
        require(title.isNotBlank()) { "Title is required | create" }
        require(steps.isNotEmpty()) { "`Steps` should be a string list | create" }

        val plan = _plans.computeIfAbsent(planId) { mutableMapOf() }
        plan["plan_id"] = planId
        plan["title"] = title
        plan["steps"] = steps
        plan["step_statuses"] = MutableList(steps.size) { "not_started" }
        plan["step_notes"] = MutableList(steps.size) { "" }

        this.currentPlanId = planId // Set as active plan

        return ToolExecuteResult(
            """
                Plan created successfully | $planId
                
                ${formatPlan(plan)}
                
                """.trimIndent()
        )
    }

    fun updatePlan(planId: String, title: String?, steps: List<String>?): ToolExecuteResult {
        require(planId.isNotBlank()) { "Plan id is required | update" }
        val plan = plans[planId] ?: throw IllegalArgumentException("No plan found to update | $planId")

        if (!title.isNullOrEmpty()) {
            plan["title"] = title
        }

        if (steps != null) {
            val oldSteps = plan["steps"] as List<String>
            val oldStatuses = plan["step_statuses"] as List<String>?
            val oldNotes = plan["step_notes"] as List<String>?

            val newStatuses: MutableList<String> = ArrayList()
            val newNotes: MutableList<String> = ArrayList()

            for (i in steps.indices) {
                val step = steps[i]
                if (i < oldSteps.size && step == oldSteps[i]) {
                    newStatuses.add(oldStatuses!![i])
                    newNotes.add(oldNotes!![i])
                } else {
                    newStatuses.add("not_started")
                    newNotes.add("")
                }
            }

            plan["steps"] = steps
            plan["step_statuses"] = newStatuses
            plan["step_notes"] = newNotes
        }

        return ToolExecuteResult(
            """
                Plan updated successfully: $planId
                
                ${formatPlan(plan)}
                """.trimIndent()
        )
    }

    fun listPlans(): ToolExecuteResult {
        if (plans.isEmpty()) {
            return ToolExecuteResult("No plans available")
        }

        val output = StringBuilder("Available plans:\n")
        for (planId in plans.keys) {
            val plan: Map<String, Any> = plans[planId] ?: continue
            val currentMarker = if (planId == currentPlanId) " (active)" else ""
            val completed = (plan["step_statuses"] as List<String>).count { "completed" == it }
            val total = (plan["steps"] as List<String?>).size
            val progress = "$completed/$total steps completed"
            output.append("""• $planId$currentMarker: ${plan["title"]} - $progress""")
        }

        return ToolExecuteResult(output.toString())
    }

    fun getPlan(planId: String?): ToolExecuteResult {
        var planId1 = planId
        if (planId1.isNullOrEmpty()) {
            requireNotNull(currentPlanId) { "No active plan. Provide a plan_id or set an active plan" }
            planId1 = currentPlanId
        }

        val plan: Map<String, Any> = requireNotNull(plans[planId1]) { "No plan found with ID: $planId1" }
        return ToolExecuteResult(formatPlan(plan))
    }

    fun setActivePlan(planId: String): ToolExecuteResult {
        require(planId.isNotBlank()) { "Parameter `plan_id` is required | set_active" }

        val plan = requireNotNull(plans[planId]) { "No plan found | $planId" }
        currentPlanId = planId

        return ToolExecuteResult(
            """
                Plan '$planId' is now the active plan.

                ${formatPlan(plan)}
                """.trimIndent()
        )
    }

    fun markStep(planId: String?, stepIndex: Int, stepStatus: String, stepNotes: String?): ToolExecuteResult {
        val realPlanId = if (!planId.isNullOrEmpty()) planId else
            requireNotNull(currentPlanId) { "No active plan found. Provide a plan_id or set an active plan." }

        val plan: Map<String, Any> = requireNotNull(plans[realPlanId]) { "No plan found | mark_step | #$realPlanId" }
        val steps = requireNotNull(plan["steps"] as? List<String>) { "No steps found in plan | $realPlanId" }

        require(stepIndex in steps.indices) { "Illegal step #$stepIndex, should be in [0, ${steps.size - 1})" }

        val allowedSteps = listOf("not_started", "in_progress", "completed", "blocked")
        require(allowedSteps.contains(stepStatus)) { "Illegal step_status $stepStatus, should be in: $allowedSteps" }

        val stepStatuses = requireNotNull(plan["step_statuses"] as? MutableList<String>) {
            "No step_statuses found in plan | $realPlanId"
        }
        val stepNotesList = requireNotNull(plan["step_notes"] as? MutableList<String>) {
            "No step_notes found in plan | $realPlanId"
        }

        stepStatuses[stepIndex] = stepStatus
        stepNotes?.let { stepNotesList[stepIndex] = it }

        val result = """Step $stepIndex updated in plan '$realPlanId'.

${formatPlan(plan)}"""

        logger.info(result)

        return ToolExecuteResult(result)
    }

    fun deletePlan(planId: String): ToolExecuteResult {
        require(planId.isNotBlank()) { "Requires `plan_id` | delete" }

        _plans.remove(planId) ?: throw IllegalArgumentException("No such plan to delete | $planId")

        if (planId == currentPlanId) {
            currentPlanId = null
        }

        return ToolExecuteResult("Plan has been deleted | $planId")
    }

    private fun formatPlan(plan: Map<String, Any>): String {
        val planTitle = plan["title"] as? String ?: "Unknown Title"
        val planId = plan["plan_id"] as? String ?: "Unknown ID"
        val steps = plan["steps"] as? List<String> ?: emptyList()
        val stepStatuses = plan["step_statuses"] as? List<String> ?: emptyList()
        val stepNotes = plan["step_notes"] as? List<String> ?: emptyList()

        // Check if the sizes of steps, stepStatuses, and stepNotes are consistent
        require(steps.size == stepNotes.size && steps.size == stepNotes.size) {
            "[Steps, stepStatuses, stepNotes] must have the same size"
        }

        val totalSteps = steps.size
        val completed = stepStatuses.count { it == "completed" }
        val inProgress = stepStatuses.count { it == "in_progress" }
        val blocked = stepStatuses.count { it == "blocked" }
        val notStarted = stepStatuses.count { it == "not_started" }

        val progressPercentage = if (totalSteps > 0) {
            String.format("%.1f%%", (1.0 * completed / totalSteps) * 100)
        } else "0%"

        val statusSummary = """
* Progress: $completed/$totalSteps steps completed ($progressPercentage)
* Status: $completed completed, $inProgress in progress, $blocked blocked, $notStarted not started
        """.trimIndent()

        val stepsSummary = buildString {
            for (i in 0..<totalSteps) {
                val step = steps[i]
                val status = StepStatus.fromValue(stepStatuses[i])
                val notes = stepNotes[i]
                append(". ${status.emoji} $step\n")
                if (notes.isNotEmpty()) {
                    append("   Notes: $notes\n")
                }
            }
        }

        val title = "MY PLAN: $planTitle"
        return """
#💡$title💡

* ID: $planId
* Time: ${OffsetDateTime.now()}

## Status Summary
$statusSummary

## Steps Summary
$stepsSummary

        """.trimIndent()
    }

    companion object {
        private val PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "command": {
			            "description": "The command to execute. Available commands: create, update, list, get, set_active, mark_step, delete.",
			            "enum": [
			                "create",
			                "update",
			                "list",
			                "get",
			                "set_active",
			                "mark_step",
			                "delete"
			            ],
			            "type": "string"
			        },
			        "plan_id": {
			            "description": "Unique identifier for the plan. Required for create, update, set_active, and delete commands. Optional for get and mark_step (uses active plan if not specified).",
			            "type": "string"
			        },
			        "title": {
			            "description": "Title for the plan. Required for create command, optional for update command.",
			            "type": "string"
			        },
			        "steps": {
			            "description": "List of plan steps. Required for create command, optional for update command.",
			            "type": "array",
			            "items": {
			                "type": "string"
			            }
			        },
			        "step_index": {
			            "description": "Index of the step to update (0-based). Required for mark_step command.",
			            "type": "integer"
			        },
			        "step_status": {
			            "description": "Status to set for a step. Used with mark_step command.",
			            "enum": ["not_started", "in_progress", "completed", "blocked"],
			            "type": "string"
			        },
			        "step_notes": {
			            "description": "Additional notes for a step. Optional for mark_step command.",
			            "type": "string"
			        }
			    },
			    "required": ["command"],
			    "additionalProperties": false
			}
			
			""".trimIndent()

        private const val name = "planning"

        private val description = """
### Planning Tool

A utility that enables the agent to create, manage, and execute plans for solving complex tasks.  
It supports creating new plans, updating individual steps, and tracking overall progress.

			""".trimIndent()

        val functionToolCallback: FunctionToolCallback<*, *>
            get() = FunctionToolCallback.builder(name, INSTANCE).description(description)
                .inputSchema(PARAMETERS)
                .inputType(Map::class.java)
                .toolMetadata(ToolMetadata.builder().returnDirect(true).build())
                .build()

        val INSTANCE by lazy { PlanningTool() }
    }
}
