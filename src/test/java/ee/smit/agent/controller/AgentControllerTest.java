package ee.smit.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.smit.agent.model.AgentRequest;
import ee.smit.agent.model.AgentResponse;
import ee.smit.agent.model.ConfidenceLevel;
import ee.smit.agent.model.SourceCitation;
import ee.smit.agent.service.AgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentService agentService;

    @Test
    void testSuccessfulAsk() throws Exception {
        AgentResponse response = new AgentResponse(
            "GitLabi ligipääsu taotlemiseks logi sisse teenuste portaali. [allikas: gitlab-access.md]",
            List.of(new SourceCitation("gitlab-access.md", "GitLab ligipääs", "Taotlusvorm...")),
            ConfidenceLevel.HIGH,
            false,
            null
        );

        when(agentService.ask(any(AgentRequest.class))).thenReturn(response);

        AgentRequest request = new AgentRequest("Kuidas taotleda ligipääsu GitLabile?", "sess-1");

        mockMvc.perform(post("/api/v1/agent/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer").value(response.answer()))
            .andExpect(jsonPath("$.confidence").value("high"))
            .andExpect(jsonPath("$.refused").value(false))
            .andExpect(jsonPath("$.sources[0].file").value("gitlab-access.md"));
    }

    @Test
    void testEmptyQuestionReturns400() throws Exception {
        // API-01: Empty question should return HTTP 400 Bad Request
        AgentRequest request = new AgentRequest("", "sess-1");

        mockMvc.perform(post("/api/v1/agent/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testMissingFieldReturns400() throws Exception {
        // API-02: Missing field should return HTTP 400 Bad Request
        String invalidJson = "{\"sessionId\": \"sess-1\"}";

        mockMvc.perform(post("/api/v1/agent/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/agent/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not valid json}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }
}
