package imitationgame.chatservice.controller;

import imitationgame.chatservice.dto.GameEvent;
import imitationgame.chatservice.dto.MessageDTO;
import imitationgame.chatservice.dto.VoteDTO;
import imitationgame.chatservice.model.MessageLog;
import imitationgame.chatservice.repository.MessageLogRepository;
import imitationgame.chatservice.service.GameService;
import imitationgame.chatservice.service.MessageRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Controller
public class ChatController {
    
    private final MessageLogRepository msgRepo;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRateLimiter rateLimiter;
    
    @Autowired
    public ChatController(MessageLogRepository msgRepo, GameService gameService, 
                          SimpMessagingTemplate messagingTemplate,
                          MessageRateLimiter rateLimiter) { 
        this.msgRepo = msgRepo; 
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.rateLimiter = rateLimiter;
    }

    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/messages/{roomId}")
    public MessageDTO send(@DestinationVariable String roomId, MessageDTO dto, Principal principal) {
        String userId = dto.getUserId();
        
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
            return null; // Don't broadcast
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
        
        return dto;
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
