package ai.platon.manus.agent.plan

import ai.platon.manus.MyTestApplication
import ai.platon.manus.agent.BrowserAgent
import ai.platon.manus.agent.PythonAgent
import ai.platon.manus.api.service.LlmService
import ai.platon.manus.common.MyContext
import org.junit.jupiter.api.BeforeEach
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

const val GOAL_TRAVEL_PLAN =
    "给我一个沿中国国界线附近，自驾游周游全国的计划，要求罗列每一个停靠点，并给出每个停靠点的详细介绍。"
const val GOAL_SEARCH_WEB = "find out the most used 100 emojis"
const val GOAL_PYTHON_CODE = "write python code to print the Fibonacci sequence"
const val GOAL_SAVE_FILE = "create a random file with random content and save it"

@SpringBootTest(classes = [MyTestApplication::class])
class PlanningFlowTestBase {

    @Autowired
    lateinit var llmService: LlmService

    @Autowired
    lateinit var toolCallingManager: ToolCallingManager

    lateinit var planningFlow: PlanningFlow

    @BeforeEach
    fun setUp() {
        val agent1 = BrowserAgent(llmService, toolCallingManager)
        val agent2 = PythonAgent(llmService, toolCallingManager, MyContext.AGENT_WORKING_DIR)

        planningFlow = PlanningFlow(llmService, agent1, agent2)
    }
}
