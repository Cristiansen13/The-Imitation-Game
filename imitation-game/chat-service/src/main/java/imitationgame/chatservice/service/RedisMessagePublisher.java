package imitationgame.chatservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import imitationgame.chatservice.dto.RedisWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RedisMessagePublisher {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ChannelTopic websocketTopic;

    @Autowired
    private ObjectMapper objectMapper;

    public void publish(String destination, Object payload) {
        try {
            RedisWebSocketMessage message = new RedisWebSocketMessage(destination, payload);
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(websocketTopic.getTopic(), json);
            log.debug("Published message to Redis: {}", destination);
        } catch (JsonProcessingException e) {
            log.error("Error publishing message to Redis", e);
        }
    }
}
