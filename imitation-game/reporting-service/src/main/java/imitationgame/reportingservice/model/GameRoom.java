package imitationgame.reportingservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_rooms")
public class GameRoom {
    
    public enum GameStatus {
        WAITING, IN_PROGRESS, VOTING, FINISHED
    }

    @Id
    private String id;
    private String name;
    
    @Enumerated(EnumType.STRING)
    private GameStatus status = GameStatus.WAITING;
    
    @Column(name = "current_round")
    private int currentRound = 0;
    
    @Column(name = "max_rounds")
    private int maxRounds = 5;
    
    @Column(name = "ai_player_id")
    private String aiPlayerId;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "started_at")
    private Instant startedAt;
    
    @Column(name = "ended_at")
    private Instant endedAt;
    
    @Column(name = "winner_id")
    private String winnerId;
    
    @Column(name = "win_condition")
    private String winCondition;

    public GameRoom() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }

    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }

    public String getAiPlayerId() { return aiPlayerId; }
    public void setAiPlayerId(String aiPlayerId) { this.aiPlayerId = aiPlayerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    
    public String getWinCondition() { return winCondition; }
    public void setWinCondition(String winCondition) { this.winCondition = winCondition; }
}
