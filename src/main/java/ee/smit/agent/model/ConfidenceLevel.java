package ee.smit.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ConfidenceLevel {
    @JsonProperty("high")
    HIGH,
    @JsonProperty("medium")
    MEDIUM,
    @JsonProperty("low")
    LOW
}
