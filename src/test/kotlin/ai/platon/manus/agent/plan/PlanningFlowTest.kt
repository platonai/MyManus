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
Please report the **top 10 cities in China by GDP**, including the following details:  

- **Nominal GDP**  
- **Population**  
- **GDP per capita**  
- **Key industries**  

**Additional tasks:**  
- **Analyze and explain the results**  
- **Generate a bar chart and save it as a PNG file**  

---  
### **Key Notes:**  
1. **Data Scope:** Focus on the latest available year (2023 or 2024).  
2. **Analysis:** Highlight trends (e.g., growth drivers, regional disparities).  
3. **Visualization:** Ensure the chart is labeled (city names, GDP values) and formatted clearly.  

        """.trimIndent()
        planningFlow.execute(goal)

        val plan = planningFlow.currentPlanContent
        println(plan)
        assertContains(plan, "Status: .+ completed, 0 in progress, 0 blocked, 0 not started".toRegex())
    }
}
