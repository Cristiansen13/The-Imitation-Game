package imitationgame.chatservice.controller;

import imitationgame.chatservice.dto.GameEvent;
import imitationgame.chatservice.dto.MessageDTO;
import imitationgame.chatservice.dto.VoteDTO;
import imitationgame.chatservice.model.GameRoom;
import imitationgame.chatservice.model.MessageLog;
import imitationgame.chatservice.repository.GameRoomRepository;
import imitationgame.chatservice.repository.MessageLogRepository;
import imitationgame.chatservice.service.GameService;
import imitationgame.chatservice.service.MessageRateLimiter;
import imitationgame.chatservice.service.RedisMessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Controller
public class ChatController {
    
    private final MessageLogRepository msgRepo;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRateLimiter rateLimiter;
    private final GameRoomRepository gameRoomRepository;
    private final RedisMessagePublisher redisPublisher;
    
    @Autowired
    public ChatController(MessageLogRepository msgRepo, GameService gameService, 
                          SimpMessagingTemplate messagingTemplate,
                          MessageRateLimiter rateLimiter,
                          GameRoomRepository gameRoomRepository,
                          RedisMessagePublisher redisPublisher) { 
        this.msgRepo = msgRepo; 
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.rateLimiter = rateLimiter;
        this.gameRoomRepository = gameRoomRepository;
        this.redisPublisher = redisPublisher;
    }

    @MessageMapping("/chat/{roomId}")
    public void send(@DestinationVariable String roomId, MessageDTO dto, Principal principal) {
        String userId = dto.getUserId();
        
        // Check if room is finished
        Optional<GameRoom> roomOpt = gameRoomRepository.findById(roomId);
        if (roomOpt.isPresent() && roomOpt.get().getStatus() == GameRoom.GameStatus.FINISHED) {
            log.warn("Attempted to send message to FINISHED room {}", roomId);
            return; // Don't accept messages for finished games
        }
        
        // Check rate limit
        if (!rateLimiter.isAllowed(userId)) {
            // Return a rate limit error message instead
            MessageDTO errorDto = new MessageDTO();
            errorDto.setUserId("system");
            errorDto.setUsername("System");
            errorDto.setMessage("⚠️ Slow down! You're sending messages too fast. Please wait a few seconds.");
            errorDto.setTimestamp(Instant.now().toString());
            
            // Send error only to the user who is rate limited
            messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/errors",
                errorDto
            );
            return;
        }
        
        // Save message to database
        MessageLog m = new MessageLog();
        m.setRoomId(roomId);
        m.setUserId(userId);
        m.setUsername(dto.getUsername());
        m.setMessage(dto.getMessage());
        m.setTimestamp(Instant.now());
        msgRepo.save(m);
        
        // Set timestamp on DTO for return
        dto.setTimestamp(m.getTimestamp().toString());
        
        // Publish to Redis for cross-replica synchronization
        redisPublisher.publish("/topic/messages/" + roomId, dto);
    }

    @MessageMapping("/game/{roomId}/vote")
    public void handleVote(@DestinationVariable String roomId, VoteDTO vote, Principal principal) {
        try {
            String oderId = principal != null ? principal.getName() : vote.getVoterId();
            gameService.castVote(roomId, oderId, vote.getTargetId());
        } catch (Exception e) {
            // Send error back to user
            messagingTemplate.convertAndSendToUser(
                    principal != null ? principal.getName() : vote.getVoterId(),
                    "/queue/errors",
                    GameEvent.error(roomId, e.getMessage())
            );
        }
    }

    @MessageMapping("/game/{roomId}/start")
    public void handleGameStart(@DestinationVariable String roomId, Principal principal) {
        try {
            gameService.startGame(roomId);
        } catch (Exception e) {
            if (principal != null) {
                messagingTemplate.convertAndSendToUser(
                        principal.getName(),
                        "/queue/errors",
                        GameEvent.error(roomId, e.getMessage())
                );
            }
        }
    }

    @MessageMapping("/game/{roomId}/voting/start")
    public void handleVotingStart(@DestinationVariable String roomId, Principal principal) {
        try {
            gameService.startVoting(roomId);
        } catch (Exception e) {
            if (principal != null) {
                messagingTemplate.convertAndSendToUser(
                        principal.getName(),
                        "/queue/errors",
                        GameEvent.error(roomId, e.getMessage())
                );
            }
        }
    }
}
