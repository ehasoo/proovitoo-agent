package ee.smit.agent.model;

import java.util.List;

public record AgentResponse(
    String answer,
    List<SourceCitation> sources,
    ConfidenceLevel confidence,
    boolean refused,
    String refusalReason
) {
    public static AgentResponse refused(String reason) {
        return new AgentResponse(
            reason,
            List.of(),
            ConfidenceLevel.LOW,
            true,
            reason
        );
    }
}
