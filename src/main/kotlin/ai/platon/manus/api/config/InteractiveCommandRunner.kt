package ai.platon.manus.api.config

import ai.platon.manus.agent.plan.PlanningFlow
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
@ConditionalOnProperty(prefix = "manus.interactive", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class InteractiveCommandRunner(
    private val planningFlow: PlanningFlow
) : CommandLineRunner {
    @Throws(Exception::class)
    override fun run(vararg args: String) {
        while (true) {
            println("Tell me what you want to do (1. type ':end' to finalize your input 2. type 'exit' to quit): ")
            print(">>> ")
            val input = generateSequence(::readLine)
                .takeWhile { it != ":end" && it != ":exit" && it != "exit" }
                .joinToString("\n")

            if (input.lowercase() in listOf("exit", ":exit")) {
                println("Bye.")
                break
            }

            val planID = "plan_" + System.currentTimeMillis()
            planningFlow.newPlan(planID)
            val result = planningFlow.execute(input)

            println("Plan : ${planningFlow.conversationId}")
            println("Result: \n$result")
        }
    }
}
