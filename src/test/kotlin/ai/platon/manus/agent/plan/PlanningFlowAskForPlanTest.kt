package ai.platon.manus.agent.plan

import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull

class PlanningFlowAskForPlanTest : PlanningFlowTestBase() {

    @Test
    fun `ask for a plan with a valid input in Chinese`() {
        val result = planningFlow.askForAnInitialPlan(GOAL_TRAVEL_PLAN)
        assertNotNull(result)
        val json = prettyPulsarObjectMapper().writeValueAsString(result)
        println(json)
        assertContains(json, "metadata")
        assertContains(json, "model")
        assertContains(json, "usage")
        assertContains(json, "results")
        assertContains(json, "Progress: .+ steps completed".toRegex())

        val plan = planningFlow.currentPlanContent
        println(plan)
        assertContains(plan, "Progress: .+ steps completed".toRegex())
        assertContains(plan, "Status: 0 completed, 0 in progress, 0 blocked, .+ not started".toRegex())
    }

    @Test
    fun `ask for a plan with a valid input in English`() {
        val result = planningFlow.askForAnInitialPlan(GOAL_SEARCH_WEB)
        assertNotNull(result)

        val plan = planningFlow.currentPlanContent
        println(plan)
        assertContains(plan, "Progress: .+ steps completed".toRegex())
        assertContains(plan, "Status: 0 completed, 0 in progress, 0 blocked, .+ not started".toRegex())
    }

    @Test
    fun `ask for a plan to write code`() {
        val result = planningFlow.askForAnInitialPlan(GOAL_PYTHON_CODE)
        assertNotNull(result)

        val plan = planningFlow.currentPlanContent
        println(plan)
        assertContains(plan, "Progress: .+ steps completed".toRegex())
        assertContains(plan, "Status: 0 completed, 0 in progress, 0 blocked, .+ not started".toRegex())
    }
}
