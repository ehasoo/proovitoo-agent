package ee.smit.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class AiConfig {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    @Bean
    public ChatMemory chatMemory() {
        return new ChatMemory() {
            private final InMemoryChatMemory delegate = new InMemoryChatMemory();
            private final Map<String, Boolean> lruKeys = Collections.synchronizedMap(
                new LinkedHashMap<String, Boolean>(1000, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                        if (size() > 1000) {
                            delegate.clear(eldest.getKey());
                            return true;
                        }
                        return false;
                    }
                }
            );

            @Override
            public void add(String conversationId, java.util.List<org.springframework.ai.chat.messages.Message> messages) {
                lruKeys.put(conversationId, true);
                delegate.add(conversationId, messages);
            }

            @Override
            public java.util.List<org.springframework.ai.chat.messages.Message> get(String conversationId, int lastN) {
                lruKeys.put(conversationId, true);
                return delegate.get(conversationId, lastN);
            }

            @Override
            public void clear(String conversationId) {
                lruKeys.remove(conversationId);
                delegate.clear(conversationId);
            }
        };
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
            .defaultSystem(systemPromptResource)
            .defaultFunctions("searchKnowledgeBase", "listTopics", "getTopicContent")
            .defaultAdvisors(new PromptChatMemoryAdvisor(chatMemory))
            .build();
    }
}
