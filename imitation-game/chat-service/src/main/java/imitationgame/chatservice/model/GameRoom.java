package imitationgame.chatservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "game_rooms")
public class GameRoom {

    @Id
    private String id = UUID.randomUUID().toString();
    
    private String name;
    
    @Enumerated(EnumType.STRING)
    private GameStatus status = GameStatus.WAITING;
    
    private int currentRound = 0;
    private int maxRounds = 5;
    private int roundDurationSeconds = 120;
    
    private Instant roundStartTime;
    private Instant createdAt = Instant.now();
    private Instant startedAt;
    private Instant endedAt;
    
    private String winnerId;
    private String winCondition;
    
    private String aiPlayerId; // The player assigned as AI
    
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RoomPlayer> players = new ArrayList<>();

    public enum GameStatus {
        WAITING,      // Waiting for players
        IN_PROGRESS,  // Game is running
        VOTING,       // Voting phase
        FINISHED      // Game ended
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    public int getRoundDurationSeconds() {
        return roundDurationSeconds;
    }

    public void setRoundDurationSeconds(int roundDurationSeconds) {
        this.roundDurationSeconds = roundDurationSeconds;
    }

    public Instant getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(Instant roundStartTime) {
        this.roundStartTime = roundStartTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
    
    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public String getWinCondition() {
        return winCondition;
    }

    public void setWinCondition(String winCondition) {
        this.winCondition = winCondition;
    }

    public String getAiPlayerId() {
        return aiPlayerId;
    }

    public void setAiPlayerId(String aiPlayerId) {
        this.aiPlayerId = aiPlayerId;
    }

    public List<RoomPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<RoomPlayer> players) {
        this.players = players;
    }
    
    public void addPlayer(RoomPlayer player) {
        players.add(player);
        player.setRoom(this);
    }
    
    public void removePlayer(RoomPlayer player) {
        players.remove(player);
        player.setRoom(null);
    }
    
    public int getAlivePlayerCount() {
        return (int) players.stream().filter(p -> p.getStatus() == RoomPlayer.PlayerStatus.ALIVE).count();
    }
    
    public boolean isFull() {
        return players.size() >= 7;
    }
}
