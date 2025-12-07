package imitationgame.chatservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Rate limiter client that calls the rate-limiter microservice.
 * Uses Redis-backed distributed rate limiting for scalability.
 */
@Service
public class MessageRateLimiter {
    
    private static final Logger log = LoggerFactory.getLogger(MessageRateLimiter.class);
    
    private final WebClient webClient;
    private final String rateLimiterUrl;
    
    public MessageRateLimiter(@Value("${rate-limiter.url:http://localhost:8082}") String rateLimiterUrl) {
        this.rateLimiterUrl = rateLimiterUrl;
        this.webClient = WebClient.builder()
                .baseUrl(rateLimiterUrl)
                .build();
        log.info("MessageRateLimiter initialized with URL: {}", rateLimiterUrl);
    }
    
    /**
     * Check if a user is allowed to send a message by calling the rate-limiter service.
     * @param userId The user ID
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String userId) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/rate-limit/internal/check/{userId}", userId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        // 429 Too Many Requests means rate limited
                        return clientResponse.bodyToMono(String.class)
                                .map(body -> new RateLimitedException("Rate limited: " + body));
                    })
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(2));
            
            if (response != null && response.containsKey("allowed")) {
                boolean allowed = (Boolean) response.get("allowed");
                if (!allowed) {
                    log.info("User {} is rate limited. Remaining: {}, Reset in: {}s", 
                            userId, response.get("remaining"), response.get("resetInSeconds"));
                }
                return allowed;
            }
            
            // If we can't determine, allow the message (fail open)
            log.warn("Could not determine rate limit status for user {}, allowing message", userId);
            return true;
            
        } catch (RateLimitedException e) {
            log.info("User {} is rate limited", userId);
            return false;
        } catch (Exception e) {
            // If rate-limiter service is down, fail open (allow messages)
            log.error("Error checking rate limit for user {}: {}. Allowing message.", userId, e.getMessage());
            return true;
        }
    }
    
    /**
     * Get remaining messages for a user
     */
    public int getRemaining(String userId) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/rate-limit/internal/check/{userId}", userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(2));
            
            if (response != null && response.containsKey("remaining")) {
                return ((Number) response.get("remaining")).intValue();
            }
            return -1;
        } catch (Exception e) {
            log.error("Error getting remaining rate limit for user {}: {}", userId, e.getMessage());
            return -1;
        }
    }
    
    private static class RateLimitedException extends RuntimeException {
        public RateLimitedException(String message) {
            super(message);
        }
    }
}
