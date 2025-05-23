package ai.platon.manus.examples

import ai.platon.manus.api.SimpleAgentTaskRunner
import org.springframework.boot.runApplication

fun main() {
    val task = """
        
        Please create a SaaS service landing page using React and Tailwind CSS. The website should include:

        1. Service Introduction section highlighting natural language browser control for data collection
        2. Usage Instructions section with API examples in curl, Python, and Node.js
        3. Customer Cases section
        4. Pricing section with 4 tiers:
           - Free tier
           - Basic tier
           - Standard tier
           - Enterprise tier
        5. Service Details section covering:
           - Natural language browser control
           - Distributed collection
           - Automatic webpage parsing
           - Precise content extraction
           - AI multimedia understanding
        6. Customer Testimonials section
        7. FAQ section

        Please use modern, professional design with responsive layout.

    """

    runApplication<SimpleAgentTaskRunner>(task)
}
