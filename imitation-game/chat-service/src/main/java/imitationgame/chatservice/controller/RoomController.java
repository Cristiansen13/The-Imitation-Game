package imitationgame.chatservice.controller;

import imitationgame.chatservice.dto.RoomResponse;
import imitationgame.chatservice.model.GameRoom;
import imitationgame.chatservice.model.MessageLog;
import imitationgame.chatservice.model.RoomPlayer;
import imitationgame.chatservice.repository.GameRoomRepository;
import imitationgame.chatservice.repository.MessageLogRepository;
import imitationgame.chatservice.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rooms")
public class RoomController {

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
        gameService.leaveRoom(id, oderId);
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

    private RoomResponse toRoomResponse(GameRoom room) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setName(room.getName());
        response.setStatus(room.getStatus().name());
        response.setCurrentRound(room.getCurrentRound());
        response.setMaxRounds(room.getMaxRounds());
        response.setPlayerCount(room.getPlayers() != null ? room.getPlayers().size() : 0);
        response.setCreatedAt(room.getCreatedAt() != null ? room.getCreatedAt().toString() : null);
        
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
