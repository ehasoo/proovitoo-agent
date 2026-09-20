package ee.smit.agent.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    @Test
    void testRateLimiterAllowAndExceed() {
        RateLimiterService rateLimiter = new RateLimiterService(3, 0); // 3 tokens, 0 refill rate
        String client = "test-client-1";

        assertTrue(rateLimiter.tryAcquire(client));
        assertTrue(rateLimiter.tryAcquire(client));
        assertTrue(rateLimiter.tryAcquire(client));
        assertFalse(rateLimiter.tryAcquire(client), "Should be blocked after exceeding capacity");
    }

    @Test
    void testRateLimiterIsolationBetweenClients() {
        RateLimiterService rateLimiter = new RateLimiterService(1, 0);
        String clientA = "client-A";
        String clientB = "client-B";

        assertTrue(rateLimiter.tryAcquire(clientA));
        assertFalse(rateLimiter.tryAcquire(clientA));

        // Client B should still have its own quota
        assertTrue(rateLimiter.tryAcquire(clientB));
        assertFalse(rateLimiter.tryAcquire(clientB));
    }

    @Test
    void testReset() {
        RateLimiterService rateLimiter = new RateLimiterService(1, 0);
        String client = "client-reset";

        assertTrue(rateLimiter.tryAcquire(client));
        assertFalse(rateLimiter.tryAcquire(client));

        rateLimiter.reset(client);
        assertTrue(rateLimiter.tryAcquire(client));
    }
}
