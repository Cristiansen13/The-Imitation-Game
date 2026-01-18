package imitationgame.reportingservice.dto;

import java.time.Instant;

public class GameSummary {
    
    private String id;
    private String name;
    private int rounds;
    private Instant startedAt;
    private Instant endedAt;
    private String winnerId;
    private String winCondition;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getRounds() { return rounds; }
    public void setRounds(int rounds) { this.rounds = rounds; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    
    public String getWinCondition() { return winCondition; }
    public void setWinCondition(String winCondition) { this.winCondition = winCondition; }
}
