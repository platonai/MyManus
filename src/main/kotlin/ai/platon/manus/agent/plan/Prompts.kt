package ai.platon.manus.agent.plan

const val INITIAL_PLAN_PROMPT = """
Create a reasonable plan with clear steps to accomplish the task.

Available Agents Information:
{agents_info}

Task to accomplish:
{query}

You can use the planning tool to help you create the plan, assign {plan_id} as the plan id.

Important: For each step in the plan, start with [AGENT_NAME] where AGENT_NAME is one of the available agents listed above.
For example: "[BROWSER_AGENT] Search for relevant information" or "[REACT_AGENT] Process the search results"

"""

const val FINALIZE_PLAN_PROMPT = """
Based on the execution history and the final plan status:

Plan Status:
%s

Please analyze:
1. What was the original user request?
2. What steps were executed successfully?
3. Were there any challenges or failures?
4. What specific results were achieved?

Provide a clear and concise response addressing:
- Direct answer to the user's original question
- Key accomplishments and findings
- Any relevant data or metrics collected
- Recommendations or next steps (if applicable)

Format your response in a user-friendly way.

"""
