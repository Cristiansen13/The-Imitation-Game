package imitationgame.aibotservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthService {

    private final WebClient webClient;
    private final String tokenUri;
    private final ConcurrentHashMap<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    public AuthService(
            @Value("${auth.url:http://auth-service:8000}") String authUrl
    ) {
        this.webClient = WebClient.builder().build();
        this.tokenUri = authUrl + "/auth/login";
        log.info("Auth Service initialized with URL: {}", tokenUri);
    }

    /**
     * Get a valid access token for a bot user.
     * Tokens are cached and refreshed automatically when expired.
     */
    public String getAccessToken(String username, String password) {
        TokenInfo cached = tokenCache.get(username);
        
        // Check if we have a valid cached token
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached token for {}", username);
            return cached.accessToken;
        }
        
        // Request a new token
        try {
            log.debug("Requesting access token for {} from {}", username, tokenUri);
            JsonNode response = webClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(java.util.Map.of("username", username, "password", password))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                             clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Auth service returned " + clientResponse.statusCode() + ": " + body)))
                    .bodyToMono(JsonNode.class)
                    .block();
            
            if (response != null && response.has("access_token")) {
                String accessToken = response.get("access_token").asText();
                int expiresIn = response.has("expires_in") ? response.get("expires_in").asInt() : 300;
                
                // Cache with 30 seconds buffer before expiry
                long expiryTime = System.currentTimeMillis() + ((expiresIn - 30) * 1000L);
                tokenCache.put(username, new TokenInfo(accessToken, expiryTime));
                
                log.info("Obtained access token for {}, expires in {}s, token preview: {}...{}", 
                         username, expiresIn, 
                         accessToken.substring(0, Math.min(20, accessToken.length())),
                         accessToken.substring(Math.max(0, accessToken.length() - 20)));
                return accessToken;
            } else {
                log.error("Failed to obtain access token for {}: no token in response", username);
                return null;
            }
        } catch (Exception e) {
            log.error("Error obtaining access token for {}: {} - {}", username, e.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Clear cached token for a user (useful when token is invalid)
     */
    public void invalidateToken(String username) {
        tokenCache.remove(username);
        log.debug("Invalidated cached token for {}", username);
    }

    private static class TokenInfo {
        final String accessToken;
        final long expiryTime;

        TokenInfo(String accessToken, long expiryTime) {
            this.accessToken = accessToken;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiryTime;
        }
    }
}
