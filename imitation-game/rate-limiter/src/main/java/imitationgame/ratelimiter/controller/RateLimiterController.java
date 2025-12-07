package imitationgame.ratelimiter.controller;

import imitationgame.ratelimiter.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/rate-limit")
public class RateLimiterController {

    @Autowired
    private RateLimiterService rateLimiterService;

    /**
     * Check if a request is allowed (for internal service use)
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkRateLimit(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        RateLimiterService.RateLimitResult result = rateLimiterService.checkRateLimit(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("allowed", result.isAllowed());
        response.put("remaining", result.getRemaining());
        response.put("resetInSeconds", result.getResetInSeconds());
        response.put("userId", userId);
        
        if (result.isAllowed()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
    }

    /**
     * Check rate limit for a specific user (admin endpoint)
     */
    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Object>> checkRateLimitForUser(@PathVariable String userId) {
        RateLimiterService.RateLimitResult result = rateLimiterService.checkRateLimit(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("allowed", result.isAllowed());
        response.put("remaining", result.getRemaining());
        response.put("resetInSeconds", result.getResetInSeconds());
        response.put("userId", userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get current rate limit status without incrementing
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        int currentCount = rateLimiterService.getRequestCount(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("currentCount", currentCount);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Reset rate limit for a user (admin endpoint)
     */
    @PostMapping("/reset/{userId}")
    public ResponseEntity<Void> resetRateLimit(@PathVariable String userId) {
        rateLimiterService.resetRateLimit(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "rate-limiter");
        return ResponseEntity.ok(response);
    }

    /**
     * Internal endpoint for service-to-service rate limit checks.
     * Does not require authentication - should only be accessible within the internal network.
     */
    @GetMapping("/internal/check/{userId}")
    public ResponseEntity<Map<String, Object>> internalCheckRateLimit(@PathVariable String userId) {
        RateLimiterService.RateLimitResult result = rateLimiterService.checkRateLimit(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("allowed", result.isAllowed());
        response.put("remaining", result.getRemaining());
        response.put("resetInSeconds", result.getResetInSeconds());
        response.put("userId", userId);
        
        if (result.isAllowed()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
    }
}
