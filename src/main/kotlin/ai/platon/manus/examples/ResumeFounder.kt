package ai.platon.manus.examples

import ai.platon.manus.agent.plan.PlanningFlow
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["ai.platon.manus.api"])
class ResumeFounder(
    private val planningFlow: PlanningFlow
) {
    private val query = """
在全网查找阿里巴巴战略投资部周端奇先生的经历，并整理为简历。你需要同时在中文和英文互联网搜索。以下是他的名片：

周端奇|瑞琦
Duangi
云智能集团-战略投资部-战略投资
阿里云
奥运会日方云服务台作伙作
投资总监
Cloud Intelligence Group-Strategic InvestmentGroup-Strategicinvestment
Investment Director
手机(M):+86 15120003935
www.aliyun.com
电邮(E): duangi.zdq@alibaba-inc.com北京市朝阳区广善路18号院-阿里巴巴北京朝阳科技园C区
Alibaba Beiiing Chaoyang Science &Technology ParkC,No.18Guanashan Road,chaovana District,Beiiing.China

    """

    @PostConstruct
    fun run() {
        planningFlow.newPlan("plan_" + System.currentTimeMillis())
        planningFlow.execute(query)
    }
}

fun main() {
    // val additionalProfiles = mutableListOf("private")
    System.setProperty("spring.profiles.include", "private")
    runApplication<ResumeFounder>()
}
