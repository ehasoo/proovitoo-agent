package ee.smit.agent.service;

import ee.smit.agent.model.AgentRequest;
import ee.smit.agent.model.AgentResponse;
import ee.smit.agent.model.ConfidenceLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

class AgentServiceTest {

    private SecurityGuardrailService guardrailService;
    private KnowledgeBaseService knowledgeBaseService;
    private ChatClient chatClient;
    private ChatMemory chatMemory;
    private AgentService agentService;

    @BeforeEach
    void setUp() {
        RateLimiterService rateLimiterService = new RateLimiterService(100, 100);
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        ChatClient mockGuardrailClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        
        when(chatClientBuilder.build()).thenReturn(mockGuardrailClient);
        when(mockGuardrailClient.prompt().user(anyString()).call().content()).thenReturn("SAFE");
        
        guardrailService = new SecurityGuardrailService(rateLimiterService, chatClientBuilder);
        knowledgeBaseService = new KnowledgeBaseService();
        knowledgeBaseService.init();
        chatClient = mock(ChatClient.class);
        chatMemory = new InMemoryChatMemory();
        agentService = new AgentService(guardrailService, knowledgeBaseService, chatClient, chatMemory);
    }

    @Test
    void testProcessAndValidateResponseWithValidCitation() {
        String rawAnswer = "GitLabi ligipääsu taotlemiseks logi sisse teenuste portaali. [allikas: gitlab-access.md]";
        AgentResponse response = agentService.processAndValidateResponse(rawAnswer, "Kuidas taotleda ligipääsu GitLabile?");

        assertFalse(response.refused());
        assertEquals(ConfidenceLevel.HIGH, response.confidence());
        assertFalse(response.sources().isEmpty());
        assertEquals("gitlab-access.md", response.sources().get(0).file());
        assertTrue(response.answer().contains("[allikas: gitlab-access.md]"));
    }

    @Test
    void testProcessAndValidateMultiTurnResponseInheritsSourcesFromChatMemory() {
        String sessionId = "sess-followup-1";
        chatMemory.add(sessionId, List.of(
            new UserMessage("Kuidas taotleda ligipääsu GitLabile?"),
            new AssistantMessage("GitLabi ligipääsu taotlemiseks ava teenuste portaal. [allikas: gitlab-access.md]")
        ));

        String rawAnswer = "Konto loomine ja ligipääsu andmine võtab aega tavaliselt kuni 48 tundi (2 tööpäeva).";
        AgentResponse response = agentService.processAndValidateResponse(rawAnswer, "Kui kaua see võtab aega?", sessionId);

        assertFalse(response.refused(), "Follow-up question should not be refused when conversation has prior source");
        assertEquals(ConfidenceLevel.HIGH, response.confidence());
        assertFalse(response.sources().isEmpty(), "Sources should be inherited from previous turns");
        assertEquals("gitlab-access.md", response.sources().get(0).file());
        assertTrue(response.answer().contains("[allikas: gitlab-access.md]"), "Citation should be appended if missing in follow-up");
        assertTrue(response.answer().contains("48") || response.answer().contains("tööpäeva"));
    }

    @Test
    void testProcessAndValidateRefusalResponse() {
        String rawAnswer = "See küsimus ei kuulu IT-teenuste teadmusbaasi ulatuse alla.";
        AgentResponse response = agentService.processAndValidateResponse(rawAnswer, "Mis on Eesti pealinn?");

        assertTrue(response.refused());
        assertEquals(ConfidenceLevel.LOW, response.confidence());
        assertTrue(response.sources().isEmpty());
        assertNotNull(response.refusalReason());
    }

    @Test
    void testProcessAndValidateResponseWithNoMatchingSource() {
        // POST-01: If no matching sources exist in the knowledge base, response is refused
        String rawAnswer = "Marsi serverile ligipääsuks ava teleport portaal.";
        AgentResponse response = agentService.processAndValidateResponse(rawAnswer, "Kuidas taotleda ligipääsu Marsi serverile?");

        assertTrue(response.refused());
        assertEquals(ConfidenceLevel.LOW, response.confidence());
        assertTrue(response.sources().isEmpty());
        assertNotNull(response.refusalReason());
    }

    @Test
    void testOversizedRequestThrowsIllegalArgumentException() {
        AgentRequest oversizedRequest = new AgentRequest("a".repeat(3001), "sess-1");
        assertThrows(IllegalArgumentException.class, () -> agentService.ask(oversizedRequest));
    }
}
