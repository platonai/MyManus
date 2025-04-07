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
            请报告中国GDP排名前十的城市，需包含以下信息：

            - 名义GDP
            - 人口数量
            - 人均GDP
            - 主要产业

            其他任务：

            - 对结果进行分析说明
            - 绘制柱状图并保存为png格式

        """.trimIndent()
        planningFlow.execute(goal)

        val plan = planningFlow.currentPlanContent
        println(plan)
        assertContains(plan, "Status: .+ completed, 0 in progress, 0 blocked, 0 not started".toRegex())
    }
}
