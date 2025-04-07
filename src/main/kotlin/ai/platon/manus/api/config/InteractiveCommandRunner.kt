package ai.platon.manus.api.config

import ai.platon.manus.agent.plan.PlanningFlow
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
@ConditionalOnProperty(prefix = "manus.interactive", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class InteractiveCommandRunner(private val planningFlow: PlanningFlow) : CommandLineRunner {
    @Throws(Exception::class)
    override fun run(vararg args: String) {
        val scanner = Scanner(System.`in`)
        while (true) {
            println("Tell me what you want to do (or type 'exit' to quit): ")
            print(">>> ")
            val query = scanner.nextLine()

            if ("exit".equals(query, ignoreCase = true)) {
                println("Bye.")
                break
            }

            planningFlow.setActivePlanId("plan_" + System.currentTimeMillis())
            val result = planningFlow.execute(query)

            println("Plan : ${planningFlow.conversationId}")
            println("Result: \n$result")
        }

        scanner.close()
    }
}
