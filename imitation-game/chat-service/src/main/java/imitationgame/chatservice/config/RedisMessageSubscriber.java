package imitationgame.chatservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import imitationgame.chatservice.dto.RedisWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisMessageSubscriber implements MessageListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String messageBody = new String(message.getBody());
            log.debug("Received Redis message: {}", messageBody);
            
            RedisWebSocketMessage wsMessage = objectMapper.readValue(messageBody, RedisWebSocketMessage.class);
            
            // Broadcast to local WebSocket clients
            messagingTemplate.convertAndSend(wsMessage.getDestination(), wsMessage.getPayload());
            
        } catch (Exception e) {
            log.error("Error processing Redis message", e);
        }
    }
}
