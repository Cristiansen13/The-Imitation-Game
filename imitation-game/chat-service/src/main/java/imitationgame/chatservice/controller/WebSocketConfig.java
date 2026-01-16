package imitationgame.chatservice.controller;

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

import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final JwtDecoder jwtDecoder;
    
    public WebSocketConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
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
                            System.err.println("JWT validation failed: " + e.getMessage());
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
            if (request instanceof ServletServerHttpRequest servletRequest) {
                String token = servletRequest.getServletRequest().getParameter("access_token");
                
                if (token != null && !token.isEmpty()) {
                    try {
                        Jwt jwt = jwtDecoder.decode(token);
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(jwt, null, java.util.Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        attributes.put("jwt", jwt);
                        return true;
                    } catch (Exception e) {
                        // Token validation failed
                        return false;
                    }
                }
            }
            // Allow connections without token (for backwards compatibility with frontend)
            return true;
        }
        
        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            // Nothing to do after handshake
        }
    }
}
