package ee.smit.agent.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final int DEFAULT_CAPACITY = 20;
    private static final int REFILL_TOKENS_PER_MINUTE = 20;

    private static class Bucket {
        double tokens;
        long lastRefillTimestampMillis;

        Bucket(int initialTokens) {
            this.tokens = initialTokens;
            this.lastRefillTimestampMillis = System.currentTimeMillis();
        }
    }

    private final Map<String, Bucket> clientBuckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final int refillRatePerMinute;

    public RateLimiterService() {
        this(DEFAULT_CAPACITY, REFILL_TOKENS_PER_MINUTE);
    }

    public RateLimiterService(int capacity, int refillRatePerMinute) {
        this.capacity = capacity;
        this.refillRatePerMinute = refillRatePerMinute;
    }

    public boolean tryAcquire(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            clientId = "anonymous";
        }

        long now = System.currentTimeMillis();
        Bucket bucket = clientBuckets.computeIfAbsent(clientId, k -> new Bucket(capacity));

        synchronized (bucket) {
            // Refill tokens based on elapsed time
            double elapsedMinutes = (now - bucket.lastRefillTimestampMillis) / 60000.0;
            bucket.tokens = Math.min(capacity, bucket.tokens + (elapsedMinutes * refillRatePerMinute));
            bucket.lastRefillTimestampMillis = now;

            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return true;
            }

            return false;
        }
    }

    public void reset(String clientId) {
        if (clientId != null) {
            clientBuckets.remove(clientId);
        } else {
            clientBuckets.clear();
        }
    }
}
