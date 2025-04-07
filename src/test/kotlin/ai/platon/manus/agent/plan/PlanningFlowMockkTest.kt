package ai.platon.manus.agent.plan

import ai.platon.manus.MyTestApplication
import ai.platon.manus.agent.MyAgent
import ai.platon.manus.api.service.LlmService
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.ai.util.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
@SpringBootTest(classes = [MyTestApplication::class])
class PlanningFlowMockkTest {

    @Autowired
    private lateinit var llmService: LlmService

    @MockK
    private lateinit var agent1: MyAgent

    @MockK
    private lateinit var agent2: MyAgent

    private lateinit var planningFlow: PlanningFlow

    @BeforeEach
    fun setUp() {
        every { agent1.name } returns "PLANNING_AGENT"
        every { agent1.description } returns "I can do the planning"
        every { agent2.name } returns "BROWSER_AGENT"
        every { agent2.description } returns "I can search the WWW for information"

        planningFlow = PlanningFlow(llmService, agent1, agent2)
    }

    @Test
    fun `test execute with empty input`() {
        val result = planningFlow.execute("")
        println(result)
        assertTrue { result.isNotBlank() }
    }

    @Test
    fun `test execute with valid input`() {
        every { agent1.run(any()) } returns "Step 1 executed"
        every { agent2.run(any()) } returns "Step 2 executed"

        val result = planningFlow.execute("Test request")
        assertNotNull(result)
    }

    @Test
    fun `test request initial plan`() {
        // clear tool call list, just temporary
        planningFlow.tools.clear()
        val result = planningFlow.askForAnInitialPlan("How to create an AI agent that can create AI agents?")
        println(result)
        assertNotNull(result)
        assertTrue { result.result.toString().contains("BROWSER_AGENT") }
        assertTrue { result.result.toString().contains("PLANNING_AGENT") }
    }

    @Test
    fun `ensure valid tool calls`() {
        val toolCalls = planningFlow.tools
        toolCalls.forEach {
            val name = it.toolDefinition.name()
            val description = it.toolDefinition.description()
            val inputSchema = it.toolDefinition.inputSchema()

            println("------------TOOL CALL-------------------")
            println("*** $name ***\n$description")
            println(it.toolDefinition.inputSchema())

            val obj = pulsarObjectMapper().readTree(inputSchema)
            assertTrue { obj.isObject }
            assertTrue { obj.has("type") }
            assertTrue { obj.has("properties") }
        }
    }

    @Test
    fun `test convert toolInput to json`() {
        val toolCall = planningFlow.tools[0]
        // val toolInput = toolCall.toolDefinition.inputSchema()
        var toolInput = """{"a": "b"}"""
        var toolInputType = Map::class.java
        var json = JsonParser.fromJson(toolInput, toolInputType)
        assertTrue { json is Map }

        toolInput = """
            {
              "command" : "create",
              "plan_id" : "plan_1743829916123",
              "title" : "Create an AI Agent that can Create AI Agents",
              "steps" : [ "[PLANNING_AGENT] Define the requirements and scope for the AI agent that can create other AI agents.", "[BROWSER_AGENT] Research existing frameworks, tools, and methodologies for creating AI agents.", "[PLANNING_AGENT] Analyze the research findings and identify the most suitable approach for the task.", "[BROWSER_AGENT] Gather detailed documentation and tutorials on the selected framework or methodology.", "[PLANNING_AGENT] Design the architecture of the AI agent, including its core components and functionalities.", "[BROWSER_AGENT] Search for open-source implementations or examples of similar AI agents for reference.", "[PLANNING_AGENT] Develop a step-by-step implementation plan for building the AI agent.", "[BROWSER_AGENT] Collect additional resources (e.g., datasets, APIs) needed for the implementation.", "[PLANNING_AGENT] Review and refine the plan based on the gathered resources and constraints.", "[PLANNING_AGENT] Assign tasks and coordinate the execution of the plan to build the AI agent." ]
            }
        """.trimIndent()

        toolInputType = Map::class.java
        json = JsonParser.fromJson(toolInput, toolInputType)
        assertTrue { json is Map }
        assertTrue { json.containsKey("steps") }
    }

    @Test
    fun `test request initial plan with tool calls`() {
        // clear tool call list, just temporary
        val toolCall = planningFlow.tools[0]
        val result = planningFlow.askForAnInitialPlan("Create an AI agent that can create AI agents")
        println(result)
        assertNotNull(result)
        assertContains(result.result.toString(), agent1.name)
        assertContains(result.result.toString(), agent2.name)
    }

    @Test
    fun `test get executor with valid step type`() {
        every { agent1.name } returns "AGENT1"
        every { agent2.name } returns "AGENT2"

        val executor = planningFlow.chooseBestAgent("AGENT1")
        assertEquals(agent1, executor)
    }

    @Test
    fun `test get executor with invalid step type`() {
        every { agent1.name } returns "AGENT1"
        every { agent2.name } returns "AGENT2"

        val executor = planningFlow.chooseBestAgent("INVALID")
        assertEquals(agent1, executor) // Assuming agent1 is the default
    }

    @Test
    fun `test execute step successfully`() {
        every { agent1.run(any()) } returns "Step executed successfully"

        val result = planningFlow.executeStep(agent1, mapOf("text" to "Step 1"))
        assertEquals("Step executed successfully", result)
    }

    @Test
    fun `test execute step with exception`() {
        every { agent1.run(any()) } throws RuntimeException("Execution failed")

        val result = planningFlow.executeStep(agent1, mapOf("text" to "Step 1"))
        assertEquals("""Failed to execute step #-1 🫨 | Execution failed""", result)
    }
}
