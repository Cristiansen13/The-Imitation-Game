package imitationgame.chatservice.controller;

import imitationgame.chatservice.dto.GameResultsDTO;
import imitationgame.chatservice.dto.RoomResponse;
import imitationgame.chatservice.model.GameRoom;
import imitationgame.chatservice.model.MessageLog;
import imitationgame.chatservice.model.RoomPlayer;
import imitationgame.chatservice.repository.GameRoomRepository;
import imitationgame.chatservice.repository.MessageLogRepository;
import imitationgame.chatservice.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    private final GameRoomRepository roomRepo;
    private final MessageLogRepository msgRepo;
    private final GameService gameService;

    public RoomController(GameRoomRepository roomRepo, MessageLogRepository msgRepo, GameService gameService) {
        this.roomRepo = roomRepo;
        this.msgRepo = msgRepo;
        this.gameService = gameService;
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAvailableRooms() {
        List<GameRoom> rooms = gameService.getAvailableRooms();
        List<RoomResponse> response = rooms.stream()
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<RoomResponse> createRoom(@AuthenticationPrincipal Jwt jwt) {
        String oderId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        
        GameRoom room = gameService.createRoom(oderId, username);
        return ResponseEntity.ok(toRoomResponse(room));
    }

    @PostMapping("/join")
    public ResponseEntity<RoomResponse> findAndJoinRoom(@AuthenticationPrincipal Jwt jwt) {
        String oderId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        
        GameRoom room = gameService.findOrCreateRoom(oderId, username);
        return ResponseEntity.ok(toRoomResponse(room));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<RoomResponse> joinRoom(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String oderId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        
        try {
            GameRoom room = gameService.joinRoom(id, oderId, username);
            return ResponseEntity.ok(toRoomResponse(room));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String oderId = jwt.getSubject();
        try {
            gameService.leaveRoom(id, oderId);
        } catch (IllegalArgumentException e) {
            // Leave is idempotent from client perspective.
            log.debug("Ignoring leave for missing room {} by player {}", id, oderId);
        } catch (Exception e) {
            // Avoid surfacing intermittent backend cleanup errors as 500 to clients.
            log.warn("Leave room failed for room {} and player {}: {}", id, oderId, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<RoomResponse> startGame(@PathVariable String id) {
        try {
            GameRoom room = gameService.startGame(id);
            return ResponseEntity.ok(toRoomResponse(room));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<Void> castVote(@PathVariable String id, 
                                          @RequestParam String targetId,
                                          @AuthenticationPrincipal Jwt jwt) {
        String oderId = jwt.getSubject();
        try {
            gameService.castVote(id, oderId, targetId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/voting/start")
    public ResponseEntity<Void> startVoting(@PathVariable String id) {
        try {
            gameService.startVoting(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/voting/end")
    public ResponseEntity<Void> endVoting(@PathVariable String id) {
        try {
            gameService.endVoting(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String id) {
        try {
            GameRoom room = gameService.getRoom(id);
            return ResponseEntity.ok(toRoomResponse(room));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageLog>> getMessages(@PathVariable String id) {
        var msgs = msgRepo.findAll().stream().filter(m -> id.equals(m.getRoomId())).toList();
        return ResponseEntity.ok(msgs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates) {
        GameRoom room = roomRepo.findById(id).orElse(null);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        if (room.getStatus() != GameRoom.GameStatus.WAITING) {
            return ResponseEntity.badRequest().build();
        }

        Object name = updates.get("name");
        if (name instanceof String s && !s.isBlank()) {
            room.setName(s.trim());
        }

        Object maxRounds = updates.get("maxRounds");
        if (maxRounds instanceof Number n) {
            int value = n.intValue();
            if (value >= 1 && value <= 20) {
                room.setMaxRounds(value);
            }
        }

        room = roomRepo.save(room);
        return ResponseEntity.ok(toRoomResponse(room));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String id) {
        GameRoom room = roomRepo.findById(id).orElse(null);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        if (room.getStatus() == GameRoom.GameStatus.IN_PROGRESS || room.getStatus() == GameRoom.GameStatus.VOTING) {
            return ResponseEntity.badRequest().build();
        }
        roomRepo.delete(room);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{roomId}/messages/{messageId}")
    public ResponseEntity<MessageLog> updateMessage(
            @PathVariable String roomId,
            @PathVariable String messageId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        MessageLog message = msgRepo.findById(messageId).orElse(null);
        if (message == null || !roomId.equals(message.getRoomId())) {
            return ResponseEntity.notFound().build();
        }

        String requester = jwt.getSubject();
        if (!requester.equals(message.getUserId())) {
            return ResponseEntity.status(403).build();
        }

        String updated = body.get("message");
        if (updated == null || updated.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        message.setMessage(updated.trim());
        MessageLog saved = msgRepo.save(message);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{roomId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String roomId,
            @PathVariable String messageId,
            @AuthenticationPrincipal Jwt jwt) {
        MessageLog message = msgRepo.findById(messageId).orElse(null);
        if (message == null || !roomId.equals(message.getRoomId())) {
            return ResponseEntity.notFound().build();
        }

        String requester = jwt.getSubject();
        if (!requester.equals(message.getUserId())) {
            return ResponseEntity.status(403).build();
        }

        msgRepo.delete(message);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get player IDs in a room (for leaderboard)
     */
    @GetMapping("/{id}/players/ids")
    public ResponseEntity<List<String>> getRoomPlayerIds(@PathVariable String id) {
        GameRoom room = roomRepo.findById(id).orElse(null);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<String> playerIds = room.getPlayers().stream()
                .filter(p -> !p.isAI()) // Exclude AI
                .map(RoomPlayer::getOderId)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(playerIds);
    }

    /**
     * Get game results for a finished game
     */
    @GetMapping("/{id}/results")
    public ResponseEntity<GameResultsDTO> getGameResults(@PathVariable String id) {
        try {
            GameResultsDTO results = gameService.getGameResults(id);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private RoomResponse toRoomResponse(GameRoom room) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setName(room.getName());
        response.setStatus(room.getStatus().name());
        response.setCurrentRound(room.getCurrentRound());
        response.setMaxRounds(room.getMaxRounds());
        response.setPlayerCount(room.getPlayers() != null ? room.getPlayers().size() : 0);
        response.setCreatedAt(room.getCreatedAt() != null ? room.getCreatedAt().toString() : null);
        response.setRoundStartTime(room.getRoundStartTime() != null ? room.getRoundStartTime().toString() : null);
        response.setCurrentTimeMillis(System.currentTimeMillis());
        response.setRoundDurationSeconds(room.getRoundDurationSeconds());
        response.setVotingDurationSeconds(60); // 60 seconds for voting
        
        if (room.getPlayers() != null) {
            response.setPlayers(room.getPlayers().stream()
                    .map(this::toPlayerResponse)
                    .collect(Collectors.toList()));
        }
        
        return response;
    }

    private RoomResponse.PlayerInfo toPlayerResponse(RoomPlayer player) {
        RoomResponse.PlayerInfo info = new RoomResponse.PlayerInfo();
        info.setOderId(player.getOderId());
        info.setUsername(player.getUsername());
        info.setStatus(player.getStatus().name());
        return info;
    }
}
