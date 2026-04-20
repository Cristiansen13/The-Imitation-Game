package imitationgame.chatservice.listener;

import imitationgame.chatservice.service.GameService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Listens to WebSocket connection and disconnection events
 * Handles automatic player removal when clients disconnect
 */
@Slf4j
@Component
public class WebSocketEventListener {

    private static final long DISCONNECT_GRACE_MS = 20000;
    
    private final GameService gameService;
    
    // Track user sessions: sessionId -> oderId mapping
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    // Track active sessions per user so refresh does not look like a hard disconnect.
    private final Map<String, Set<String>> userSessionsMap = new ConcurrentHashMap<>();
    // Keep pending delayed disconnect tasks so they can be cancelled on quick reconnect.
    private final Map<String, ScheduledFuture<?>> pendingDisconnectTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService disconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    
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
                userSessionsMap.computeIfAbsent(oderId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);

                ScheduledFuture<?> pendingTask = pendingDisconnectTasks.remove(oderId);
                if (pendingTask != null) {
                    pendingTask.cancel(false);
                    log.info("Cancelled pending disconnect for user {} due to quick reconnect", oderId);
                }

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

            Set<String> sessions = userSessionsMap.get(oderId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessionsMap.remove(oderId);
                }
            }

            ScheduledFuture<?> existingTask = pendingDisconnectTasks.remove(oderId);
            if (existingTask != null) {
                existingTask.cancel(false);
            }

            ScheduledFuture<?> delayedTask = disconnectScheduler.schedule(() -> {
                Set<String> activeSessions = userSessionsMap.get(oderId);
                if (activeSessions != null && !activeSessions.isEmpty()) {
                    log.info("Skipping disconnect cleanup for user {} because a session reconnected", oderId);
                    return;
                }

                try {
                    // Do not auto-remove users from rooms on transient socket loss or page refresh.
                    // Room membership changes should only happen via explicit leave/game-end flows.
                    log.info("User {} remained disconnected past grace period; preserving room membership", oderId);
                } catch (Exception e) {
                    log.error("Error handling disconnect grace task for user {}: {}", oderId, e.getMessage());
                } finally {
                    pendingDisconnectTasks.remove(oderId);
                }
            }, DISCONNECT_GRACE_MS, TimeUnit.MILLISECONDS);

            pendingDisconnectTasks.put(oderId, delayedTask);
            
        } else {
            log.warn("WebSocket disconnected - Session: {}, but no user mapping found", sessionId);
        }
    }

    @PreDestroy
    public void shutdownScheduler() {
        disconnectScheduler.shutdownNow();
    }
}
