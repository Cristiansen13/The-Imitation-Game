package imitationgame.chatservice.listener;

import imitationgame.chatservice.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens to WebSocket connection and disconnection events
 * Handles automatic player removal when clients disconnect
 */
@Slf4j
@Component
public class WebSocketEventListener {
    
    private final GameService gameService;
    
    // Track user sessions: sessionId -> oderId mapping
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    public WebSocketEventListener(GameService gameService) {
        this.gameService = gameService;
    }
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // Try to get user from principal
        Principal principal = headerAccessor.getUser();
        if (principal != null && principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth = 
                (org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal;
            
            if (auth.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) auth.getPrincipal();
                String oderId = jwt.getSubject();
                sessionUserMap.put(sessionId, oderId);
                log.info("WebSocket connected - Session: {}, User: {}", sessionId, oderId);
            }
        }
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        String oderId = sessionUserMap.remove(sessionId);
        
        if (oderId != null) {
            log.info("WebSocket disconnected - Session: {}, User: {}", sessionId, oderId);
            
            // Try to remove player from their room
            try {
                gameService.handlePlayerDisconnect(oderId);
            } catch (Exception e) {
                log.error("Error handling player disconnect for user {}: {}", oderId, e.getMessage());
            }
        } else {
            log.warn("WebSocket disconnected - Session: {}, but no user mapping found", sessionId);
        }
    }
}
