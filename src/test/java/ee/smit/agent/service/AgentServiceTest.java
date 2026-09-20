package ee.smit.agent.service;

import ee.smit.agent.model.AgentRequest;
import ee.smit.agent.model.AgentResponse;
import ee.smit.agent.model.ConfidenceLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

class AgentServiceTest {

    private SecurityGuardrailService guardrailService;
    private KnowledgeBaseService knowledgeBaseService;
    private ChatClient chatClient;
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
        agentService = new AgentService(guardrailService, knowledgeBaseService, chatClient);
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
