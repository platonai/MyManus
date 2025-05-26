package ai.platon.manus.api.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Service

@Service
class LlmService(
    private final val chatModel: ChatModel,
    toolCallbackProvider: ToolCallbackProvider
) {
    final val conversationMemory: ChatMemory = MessageWindowChatMemory.builder().maxMessages(1000).build()

    final val agentMemory: ChatMemory = MessageWindowChatMemory.builder().maxMessages(1000).build()

    // Planning chat client
    val planningChatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(SimpleLoggerAdvisor())
        .defaultOptions(OpenAiChatOptions.builder().temperature(0.1).build())
        .build();

    val agentClient = ChatClient.builder(chatModel)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(agentMemory).build())
        .defaultAdvisors(SimpleLoggerAdvisor())
        .defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
        .build();

    val finalizeChatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(conversationMemory).build())
        .defaultAdvisors(SimpleLoggerAdvisor())
        .build();
}
