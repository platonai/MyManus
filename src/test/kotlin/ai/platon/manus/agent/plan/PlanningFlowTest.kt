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

    @Test
    fun `Code Agent - write a mine game in Python`() {
        val goal = """
使用python写一个扫雷游戏，要求如下：

- 扫雷游戏是一个二维数组，每个元素可以是数字或空格，数字表示该位置周围有数字个地雷，空格表示该位置没有地雷。
- 玩家通过点击某个位置，如果该位置是数字，则显示数字，如果该位置是空格，则显示一个问号，表示该位置有地雷。
- 玩家点击一个位置，如果该位置有地雷，则游戏结束，玩家输；如果该位置没有地雷，则显示数字，如果数字为0，则将该位置的周围8个位置都显示数字，如果数字不为0，

        """.trimIndent()
        planningFlow.execute(goal)

        val plan = planningFlow.currentPlanContent
        println(plan)
    }
}
