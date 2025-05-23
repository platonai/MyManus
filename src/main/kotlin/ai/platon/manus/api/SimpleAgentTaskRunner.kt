package ai.platon.manus.api

import ai.platon.manus.agent.plan.PlanningFlow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["ai.platon.manus.api"])
class SimpleAgentTaskRunner : ApplicationRunner {
    @Autowired
    lateinit var planningFlow: PlanningFlow

    override fun run(args: ApplicationArguments) {
        val args = args.sourceArgs
        if (args.isEmpty()) {
            return
        }

        planningFlow.newPlan("plan_" + System.currentTimeMillis())
        planningFlow.execute(args[0])
    }
}
