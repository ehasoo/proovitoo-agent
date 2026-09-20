package ee.smit.agent.controller;

import ee.smit.agent.model.AgentRequest;
import ee.smit.agent.model.AgentResponse;
import ee.smit.agent.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AgentResponse> ask(@Valid @RequestBody AgentRequest request) {
        AgentResponse response = agentService.ask(request);
        return ResponseEntity.ok(response);
    }
}
