package ai.platon.manus.examples.todo

import ai.platon.manus.api.SimpleAgentTaskRunner
import org.springframework.boot.runApplication

fun main() {
    val task = """
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
    runApplication<SimpleAgentTaskRunner>(task)
}
