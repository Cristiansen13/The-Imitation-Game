package imitationgame.chatservice.controller;

import imitationgame.chatservice.model.GameRoom;
import imitationgame.chatservice.model.MessageLog;
import imitationgame.chatservice.model.RoomPlayer;
import imitationgame.chatservice.repository.GameRoomRepository;
import imitationgame.chatservice.repository.MessageLogRepository;
import imitationgame.chatservice.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class RoomControllerTest {

    private GameRoomRepository roomRepo;
    private MessageLogRepository msgRepo;
    private GameService gameService;
    private RoomController controller;

    @BeforeEach
    void setUp() {
        roomRepo = Mockito.mock(GameRoomRepository.class);
        msgRepo = Mockito.mock(MessageLogRepository.class);
        gameService = Mockito.mock(GameService.class);
        controller = new RoomController(roomRepo, msgRepo, gameService);
    }

    private Jwt jwtToken() {
        return Jwt.withTokenValue("t")
                .header("alg", "none")
                .subject("user-1")
                .claim("preferred_username", "mihai")
                .build();
    }

    @Test
    void getAvailableRooms_ok() {
        Mockito.when(gameService.getAvailableRooms()).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getAvailableRooms().getStatusCode());
    }

    @Test
    void createRoom_ok() {
        Mockito.when(gameService.createRoom("user-1", "mihai")).thenReturn(new GameRoom());
        assertEquals(HttpStatus.OK, controller.createRoom(jwtToken()).getStatusCode());
    }

    @Test
    void findAndJoin_ok() {
        Mockito.when(gameService.findOrCreateRoom("user-1", "mihai")).thenReturn(new GameRoom());
        assertEquals(HttpStatus.OK, controller.findAndJoinRoom(jwtToken()).getStatusCode());
    }

    @Test
    void joinRoomById_ok() {
        Mockito.when(gameService.joinRoom("r1", "user-1", "mihai")).thenReturn(new GameRoom());
        assertEquals(HttpStatus.OK, controller.joinRoom("r1", jwtToken()).getStatusCode());
    }

    @Test
    void leaveRoom_ok() {
        assertEquals(HttpStatus.OK, controller.leaveRoom("r1", jwtToken()).getStatusCode());
    }

    @Test
    void startGame_ok() {
        Mockito.when(gameService.startGame("r1")).thenReturn(new GameRoom());
        assertEquals(HttpStatus.OK, controller.startGame("r1").getStatusCode());
    }

    @Test
    void vote_ok() {
        assertEquals(HttpStatus.OK, controller.castVote("r1", "u2", jwtToken()).getStatusCode());
    }

    @Test
    void startVoting_ok() {
        assertEquals(HttpStatus.OK, controller.startVoting("r1").getStatusCode());
    }

    @Test
    void endVoting_ok() {
        assertEquals(HttpStatus.OK, controller.endVoting("r1").getStatusCode());
    }

    @Test
    void getRoom_ok() {
        Mockito.when(gameService.getRoom("r1")).thenReturn(new GameRoom());
        assertEquals(HttpStatus.OK, controller.getRoom("r1").getStatusCode());
    }

    @Test
    void updateRoom_ok() {
        GameRoom room = new GameRoom();
        room.setId("r1");
        room.setStatus(GameRoom.GameStatus.WAITING);
        Mockito.when(roomRepo.findById("r1")).thenReturn(Optional.of(room));
        Mockito.when(roomRepo.save(any(GameRoom.class))).thenReturn(room);
        assertEquals(HttpStatus.OK, controller.updateRoom("r1", Map.of("name", "New")).getStatusCode());
    }

    @Test
    void deleteRoom_noContent() {
        GameRoom room = new GameRoom();
        room.setStatus(GameRoom.GameStatus.WAITING);
        Mockito.when(roomRepo.findById("r1")).thenReturn(Optional.of(room));
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteRoom("r1").getStatusCode());
    }

    @Test
    void updateMessage_ok() {
        MessageLog msg = new MessageLog();
        msg.setId("m1");
        msg.setRoomId("r1");
        msg.setUserId("user-1");
        Mockito.when(msgRepo.findById("m1")).thenReturn(Optional.of(msg));
        Mockito.when(msgRepo.save(any(MessageLog.class))).thenReturn(msg);
        assertEquals(HttpStatus.OK, controller.updateMessage("r1", "m1", Map.of("message", "edited"), jwtToken()).getStatusCode());
    }

    @Test
    void deleteMessage_noContent() {
        MessageLog msg = new MessageLog();
        msg.setId("m1");
        msg.setRoomId("r1");
        msg.setUserId("user-1");
        Mockito.when(msgRepo.findById("m1")).thenReturn(Optional.of(msg));
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteMessage("r1", "m1", jwtToken()).getStatusCode());
    }

    @Test
    void getRoomPlayerIds_ok() {
        GameRoom room = new GameRoom();
        RoomPlayer p = new RoomPlayer();
        p.setOderId("user-2");
        p.setAI(false);
        room.setPlayers(List.of(p));
        Mockito.when(roomRepo.findById("r1")).thenReturn(Optional.of(room));

        assertEquals(HttpStatus.OK, controller.getRoomPlayerIds("r1").getStatusCode());
    }

    @Test
    void getResults_ok() {
        Mockito.when(gameService.getGameResults("r1")).thenReturn(new imitationgame.chatservice.dto.GameResultsDTO());
        assertEquals(HttpStatus.OK, controller.getGameResults("r1").getStatusCode());
    }
}
