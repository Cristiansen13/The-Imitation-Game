package imitationgame.chatservice.dto;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameEvent {
    
    public enum EventType {
        PLAYER_JOINED,
        PLAYER_LEFT,
        GAME_STARTED,
        ROUND_STARTED,
        VOTING_STARTED,
        VOTE_CAST,
        PLAYER_ELIMINATED,
        GAME_ENDED,
        MESSAGE,
        ERROR
    }

    private EventType type;
    private String roomId;
    private Instant timestamp;
    private Map<String, Object> data;

    public GameEvent() {
        this.timestamp = Instant.now();
        this.data = new HashMap<>();
    }

    public GameEvent(EventType type, String roomId) {
        this();
        this.type = type;
        this.roomId = roomId;
    }

    // Factory methods for different event types

    public static GameEvent playerJoined(String roomId, String oderId, String username, int playerCount) {
        GameEvent event = new GameEvent(EventType.PLAYER_JOINED, roomId);
        event.data.put("oderId", oderId);
        event.data.put("username", username);
        event.data.put("playerCount", playerCount);
        return event;
    }

    public static GameEvent playerLeft(String roomId, String oderId, String username) {
        GameEvent event = new GameEvent(EventType.PLAYER_LEFT, roomId);
        event.data.put("oderId", oderId);
        event.data.put("username", username);
        return event;
    }

    public static GameEvent gameStarted(String roomId, int currentRound, int maxRounds, boolean isAI) {
        GameEvent event = new GameEvent(EventType.GAME_STARTED, roomId);
        event.data.put("currentRound", currentRound);
        event.data.put("maxRounds", maxRounds);
        event.data.put("isAI", isAI);
        return event;
    }

    public static GameEvent gameStartedWithAiId(String roomId, int currentRound, int maxRounds, String aiPlayerId) {
        GameEvent event = new GameEvent(EventType.GAME_STARTED, roomId);
        event.data.put("currentRound", currentRound);
        event.data.put("maxRounds", maxRounds);
        event.data.put("aiPlayerId", aiPlayerId);
        return event;
    }

    public static GameEvent roundStarted(String roomId, int roundNumber) {
        GameEvent event = new GameEvent(EventType.ROUND_STARTED, roomId);
        event.data.put("roundNumber", roundNumber);
        return event;
    }

    public static GameEvent votingStarted(String roomId, List<String> eligibleVoters) {
        GameEvent event = new GameEvent(EventType.VOTING_STARTED, roomId);
        event.data.put("eligibleVoters", eligibleVoters);
        return event;
    }

    public static GameEvent voteCast(String roomId, String voterId, int totalVotes) {
        GameEvent event = new GameEvent(EventType.VOTE_CAST, roomId);
        event.data.put("voterId", voterId);
        event.data.put("totalVotes", totalVotes);
        return event;
    }

    public static GameEvent playerEliminated(String roomId, String oderId, String username, 
                                              boolean wasAI, int votesReceived) {
        GameEvent event = new GameEvent(EventType.PLAYER_ELIMINATED, roomId);
        event.data.put("oderId", oderId);
        event.data.put("username", username);
        event.data.put("wasAI", wasAI);
        event.data.put("votesReceived", votesReceived);
        return event;
    }

    public static GameEvent gameEnded(String roomId, String winnerId, String winCondition, 
                                       String aiPlayerId, String aiUsername) {
        GameEvent event = new GameEvent(EventType.GAME_ENDED, roomId);
        event.data.put("winnerId", winnerId);
        event.data.put("winCondition", winCondition);
        event.data.put("aiPlayerId", aiPlayerId);
        event.data.put("aiUsername", aiUsername);
        return event;
    }

    public static GameEvent message(String roomId, String senderId, String senderUsername, String content) {
        GameEvent event = new GameEvent(EventType.MESSAGE, roomId);
        event.data.put("senderId", senderId);
        event.data.put("senderUsername", senderUsername);
        event.data.put("content", content);
        return event;
    }

    public static GameEvent error(String roomId, String message) {
        GameEvent event = new GameEvent(EventType.ERROR, roomId);
        event.data.put("message", message);
        return event;
    }

    // Getters and setters

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
