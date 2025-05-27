package ai.platon.manus.agent

import ai.platon.manus.api.service.LlmService
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage

abstract class ReActAgent(llmService: LlmService) : AbstractAgent(llmService) {

    /**
     * Perform the next step of the agent: think and act.
     * */
    public override fun step(): List<String> {
        val moreAct = think()
        if (!moreAct) {
            return listOf("""💯 Complete! Every thing is done! ✨✨✨""")
        }
        return act()
    }

    /**
     * Add a new message to [messages], so the agent can think with more context.
     *
     * @param messages The messages to add the new message to.
     * @return The newly added message.
     * */
    override fun addThinkPromptTo(messages: MutableList<Message>): Message {
        return UserMessage("")
    }

    /**
     * Think and decide whether to act.
     * */
    protected abstract fun think(): Boolean

    /**
     * Act and return the result.
     * */
    protected abstract fun act(): List<String>
}
