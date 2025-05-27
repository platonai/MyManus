package ai.platon.manus.api

import ai.platon.manus.agent.plan.PlanningFlow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import kotlin.system.exitProcess

@SpringBootApplication(scanBasePackages = ["ai.platon.manus.api"])
class SimpleAgentTaskRunner : ApplicationRunner {
    @Autowired
    lateinit var planningFlow: PlanningFlow

    override fun run(args: ApplicationArguments) {
        val args1 = args.sourceArgs
        if (args1.isEmpty()) {
            return
        }

        planningFlow.newPlan("plan_" + System.currentTimeMillis())
        val results = planningFlow.execute(args1[0])

        if (results.isEmpty()) {
            println("No result found.")
        } else {
            println("Result: ")
            results.forEach {
                println(it)
            }
        }

        exitProcess(0)
    }
}
