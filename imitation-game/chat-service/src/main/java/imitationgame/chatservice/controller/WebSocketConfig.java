package imitationgame.chatservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final JwtDecoder jwtDecoder;
    
    public WebSocketConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use external message broker (Redis) for multi-instance support
        // This allows WebSocket messages to be synchronized across all replicas
        config.enableSimpleBroker("/topic", "/queue")
              .setTaskScheduler(null); // Will be auto-configured with Redis
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    String altHeader = accessor.getFirstNativeHeader("X-Authorization");
                    
                    String token = null;
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7);
                    } else if (altHeader != null) {
                        token = altHeader;
                    }
                    
                    if (token != null) {
                        try {
                            Jwt jwt = jwtDecoder.decode(token);
                            UsernamePasswordAuthenticationToken auth = 
                                new UsernamePasswordAuthenticationToken(jwt, null, java.util.Collections.emptyList());
                            accessor.setUser(auth);
                        } catch (Exception e) {
                            log.debug("JWT validation failed on CONNECT frame: {}", e.getMessage());
                        }
                    }
                }
                
                return message;
            }
        });
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS endpoint for browser clients - now with JWT authentication
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new JwtHandshakeInterceptor(jwtDecoder))
                .withSockJS();
        
        // Plain WebSocket endpoint for Java clients (bots)
        registry.addEndpoint("/ws-plain")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new JwtHandshakeInterceptor(jwtDecoder));
    }
    
    /**
     * Handshake interceptor to authenticate WebSocket connections using JWT from query parameter
     */
    private static class JwtHandshakeInterceptor implements HandshakeInterceptor {
        private final JwtDecoder jwtDecoder;
        
        public JwtHandshakeInterceptor(JwtDecoder jwtDecoder) {
            this.jwtDecoder = jwtDecoder;
        }
        
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            String token = null;
            
            // Try to extract token from Authorization header
            List<String> authHeaders = request.getHeaders().get("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                    log.debug("[WebSocketConfig] Token found in Authorization header");
                }
            }
            
            // If not found in header, try query parameter (for backwards compatibility)
            if (token == null || token.isEmpty()) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    token = servletRequest.getServletRequest().getParameter("access_token");
                    if (token != null) {
                        log.debug("[WebSocketConfig] Token found in query parameter");
                    }
                }
            }
            
            // If still not found, try URI query string
            if (token == null || token.isEmpty()) {
                String query = request.getURI().getQuery();
                if (query != null && query.contains("access_token=")) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("access_token=")) {
                            token = param.substring("access_token=".length());
                            log.debug("[WebSocketConfig] Token found in URI query string");
                            break;
                        }
                    }
                }
            }
            
            log.debug("[WebSocketConfig] Handshake interceptor called. Token present: {}", token != null);
            
            if (token != null && !token.isEmpty()) {
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(jwt, null, java.util.Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    attributes.put("jwt", jwt);
                    log.debug("[WebSocketConfig] Token validated successfully for user: {}", jwt.getSubject());
                    return true;
                } catch (Exception e) {
                    // Token validation failed
                    log.debug("[WebSocketConfig] Token validation failed: {}", e.getMessage());
                    return false;
                }
            }
            // Allow connections without token (for backwards compatibility with frontend)
            log.debug("[WebSocketConfig] Allowing connection without token");
            return true;
        }
        
        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            // Nothing to do after handshake
        }
    }
}
