package imitationgame.ratelimiter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${rate-limiter.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${rate-limiter.window-size-seconds:60}")
    private int windowSizeSeconds;

    private static final String KEY_PREFIX = "rate_limit:";

    /**
     * Check if a request is allowed for a given user
     * Uses sliding window algorithm with Redis
     */
    public boolean isAllowed(String userId) {
        String key = KEY_PREFIX + userId;
        
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        if (currentCount == null) {
            return false;
        }
        
        if (currentCount == 1) {
            // First request - set expiration
            redisTemplate.expire(key, Duration.ofSeconds(windowSizeSeconds));
        }
        
        return currentCount <= requestsPerMinute;
    }

    /**
     * Check if allowed and get remaining requests
     */
    public RateLimitResult checkRateLimit(String userId) {
        String key = KEY_PREFIX + userId;
        
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        if (currentCount == null) {
            return new RateLimitResult(false, 0, 0);
        }
        
        if (currentCount == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSizeSeconds));
        }
        
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        int remaining = Math.max(0, requestsPerMinute - currentCount.intValue());
        
        return new RateLimitResult(
                currentCount <= requestsPerMinute,
                remaining,
                ttl != null ? ttl.intValue() : windowSizeSeconds
        );
    }

    /**
     * Get current request count for a user
     */
    public int getRequestCount(String userId) {
        String key = KEY_PREFIX + userId;
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * Reset rate limit for a user (admin function)
     */
    public void resetRateLimit(String userId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    public static class RateLimitResult {
        private final boolean allowed;
        private final int remaining;
        private final int resetInSeconds;

        public RateLimitResult(boolean allowed, int remaining, int resetInSeconds) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.resetInSeconds = resetInSeconds;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public int getRemaining() {
            return remaining;
        }

        public int getResetInSeconds() {
            return resetInSeconds;
        }
    }
}
