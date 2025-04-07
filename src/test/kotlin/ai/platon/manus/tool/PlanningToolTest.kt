package ai.platon.manus.tool

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class PlanningToolTest {

    private lateinit var planningTool: PlanningTool

    @BeforeEach
    fun setUp() {
        planningTool = PlanningTool()
    }

    @Test
    fun testCreatePlan() {
        val planId = "plan1"
        val title = "Test Plan"
        val steps = listOf("Step 1", "Step 2")

        val result = planningTool.createPlan(planId, title, steps)

        assertNotNull(result)
        assertTrue(result.message.contains("Plan created successfully"))
        assertTrue(planningTool.plans.containsKey(planId))
        assertEquals(planId, planningTool.currentPlanId)
    }

    @Test
    fun testCreatePlanWithExistingId() {
        val planId = "plan1"
        val title = "Test Plan"
        val steps = listOf("Step 1", "Step 2")

        planningTool.createPlan(planId, title, steps)

        assertThrows(RuntimeException::class.java) {
            planningTool.createPlan(planId, "Another Plan", listOf("Step 1"))
        }
    }

    @Test
    fun testUpdatePlan() {
        val planId = "plan1"
        val title = "Test Plan"
        val steps = listOf("Step 1", "Step 2")

        planningTool.createPlan(planId, title, steps)

        val newTitle = "Updated Plan"
        val newSteps = listOf("Step 1", "Step 2", "Step 3")

        val result = planningTool.updatePlan(planId, newTitle, newSteps)

        assertNotNull(result)
        assertTrue(result.message.contains("Plan updated successfully"))
        assertEquals(newTitle, planningTool.plans[planId]?.get("title"))
        assertEquals(newSteps, planningTool.plans[planId]?.get("steps"))
    }

    @Test
    fun testListPlans() {
        val planId1 = "plan1"
        val title1 = "Test Plan 1"
        val steps1 = listOf("Step 1", "Step 2")

        val planId2 = "plan2"
        val title2 = "Test Plan 2"
        val steps2 = listOf("Step 1", "Step 2", "Step 3")

        planningTool.createPlan(planId1, title1, steps1)
        planningTool.createPlan(planId2, title2, steps2)

        val result = planningTool.listPlans()

        assertNotNull(result)
        assertTrue(result.message.contains("Available plans"))
        assertTrue(result.message.contains(planId1))
        assertTrue(result.message.contains(planId2))
    }

    @Test
    fun testGetPlan() {
        val planId = "plan1"
        val title = "Test Plan"
        val steps = listOf("Step 1", "Step 2")

        planningTool.createPlan(planId, title, steps)

        val result = planningTool.getPlan(planId)

        assertNotNull(result)
        assertTrue(result.message) { result.message.contains("""#💡MY PLAN: $title💡""") }
        assertTrue(result.message) { result.message.contains("## Status Summary") }
        assertTrue(result.message) { result.message.contains("## Steps Summary") }
    }

    @Test
    fun testSetActivePlan() {
        val planId = "plan1"
        val title = "Test Plan"
        val steps = listOf("Step 1", "Step 2")

        planningTool.createPlan(planId, title, steps)

        val result = planningTool.setActivePlan(planId)

        assertNotNull(result)
        assertTrue(result.message.contains("Plan '$planId' is now the active plan"))
        assertEquals(planId, planningTool.currentPlanId)
    }

    @Test
    fun testMarkStep() {
        val planId = "plan1"
        val title = "Test Plan"
        val steps = listOf("Step 1", "Step 2")

        planningTool.createPlan(planId, title, steps)

        val result = planningTool.markStep(planId, 0, "in_progress", "Working on it")

        assertNotNull(result)
        assertTrue(result.message.contains("Step 0 updated in plan '$planId'"))
        val plan = planningTool.plans[planId]
        assertNotNull(plan)
        requireNotNull(plan)

        assertContains(plan["step_statuses"]?.toString() ?: "", "in_progress")
        assertContains(plan["step_notes"]?.toString() ?: "", "Working on it")
    }

    @Test
    fun testDeletePlan() {
        val planId = "plan1"
        val title = "Test Plan"
        val steps = listOf("Step 1", "Step 2")

        planningTool.createPlan(planId, title, steps)

        val result = planningTool.deletePlan(planId)

        assertNotNull(result)
        assertTrue(result.message.contains("Plan has been deleted | $planId"))
        assertFalse(planningTool.plans.containsKey(planId))
        assertNull(planningTool.currentPlanId)
    }
}
