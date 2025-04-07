package ai.platon.manus.api.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Service

@Service
class LlmService(
    private final val chatModel: ChatModel,
    toolCallbackProvider: ToolCallbackProvider
) {
    final val memory = InMemoryChatMemory()
    private final val planningMemory: ChatMemory = InMemoryChatMemory()
    private final val finalizeMemory = InMemoryChatMemory()

    // Planning chat client
    // TODO: use a reasoning model
    val planningChatClient = ChatClient.builder(chatModel).defaultSystem(PLANNING_SYSTEM_PROMPT)
        .defaultAdvisors(MessageChatMemoryAdvisor(planningMemory)).defaultAdvisors(SimpleLoggerAdvisor())
        .defaultTools(toolCallbackProvider).build()

    val chatClient = ChatClient.builder(chatModel).defaultSystem(MANUS_SYSTEM_PROMPT)
        .defaultAdvisors(MessageChatMemoryAdvisor(memory)).defaultAdvisors(SimpleLoggerAdvisor())
        .defaultTools(toolCallbackProvider)
        .defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build()).build()

    val finalizeChatClient = ChatClient.builder(chatModel).defaultSystem(FINALIZE_SYSTEM_PROMPT)
        .defaultAdvisors(MessageChatMemoryAdvisor(finalizeMemory)).defaultAdvisors(SimpleLoggerAdvisor()).build()
}
