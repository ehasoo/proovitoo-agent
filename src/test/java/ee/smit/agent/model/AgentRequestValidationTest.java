package ee.smit.agent.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRequest() {
        AgentRequest request = new AgentRequest("Kuidas taotleda ligipääsu?", "session-123");
        Set<ConstraintViolation<AgentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testEmptyQuestion() {
        AgentRequest request = new AgentRequest("", "session-123");
        Set<ConstraintViolation<AgentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testBlankQuestion() {
        AgentRequest request = new AgentRequest("   ", "session-123");
        Set<ConstraintViolation<AgentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testNullQuestion() {
        AgentRequest request = new AgentRequest(null, "session-123");
        Set<ConstraintViolation<AgentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testOversizedQuestion() {
        String longQuestion = "a".repeat(2001);
        AgentRequest request = new AgentRequest(longQuestion, "session-123");
        Set<ConstraintViolation<AgentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
