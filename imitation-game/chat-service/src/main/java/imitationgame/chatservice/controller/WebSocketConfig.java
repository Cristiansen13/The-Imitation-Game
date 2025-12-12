package imitationgame.chatservice.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
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
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS endpoint for browser clients
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
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
