package ee.smit.agent.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgentRequest(
    @NotNull(message = "Küsimus ei tohi puududa")
    @NotBlank(message = "Küsimus ei tohi olla tühi")
    @Size(max = 2000, message = "Küsimus ei tohi ületada 2000 tähemärki")
    String question,
    String sessionId
) {}
