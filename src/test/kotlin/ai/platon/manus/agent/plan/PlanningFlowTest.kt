package ai.platon.manus.agent.plan

import ai.platon.manus.MyTestApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertContains

@SpringBootTest(classes = [MyTestApplication::class])
@EnabledIfEnvironmentVariable(named = "integration", matches = "true")
class PlanningFlowTest {

    @Autowired
    lateinit var planningFlow: PlanningFlow

    @Test
    fun `Plan and execute writing code`() {
        planningFlow.execute("write python code to print the Fibonacci sequence")

        val plan = planningFlow.currentPlanContent
        println(plan)
        assertContains(plan, "Status: .+ completed, 0 in progress, 0 blocked, 0 not started".toRegex())
    }

    @Test
    fun `Plan and execute file saving`() {
        planningFlow.execute(GOAL_SAVE_FILE)

        val plan = planningFlow.currentPlanContent
        println(plan)
        assertContains(plan, "Status: .+ completed, 0 in progress, 0 blocked, 0 not started".toRegex())
    }

    @Test
    fun `Plan and execute - reporting top-n cities by GDP in China`() {
        val goal = """
**Task: Report China's top 10 cities by GDP including:**

- Nominal GDP
- Population
- GDP per capita
- Major industries

**Additional requirements:**
- Provide analysis of the results
- Create and save a bar chart in PNG format

        """.trimIndent()
        planningFlow.execute(goal)

        val plan = planningFlow.currentPlanContent
        println(plan)
        assertContains(plan, "Status: .+ completed, 0 in progress, 0 blocked, 0 not started".toRegex())
    }
}
