package imitationgame.reportingservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profile")
public class UserProfile {
    @Id
    private String oderId;
    private String username;
    private String email;
    private Instant createdAt;
    
    // Game statistics
    private int gamesPlayed = 0;
    private int gamesWonAsHuman = 0;
    private int gamesWonAsAI = 0;
    private int correctAIIdentifications = 0;

    public UserProfile() {
        this.oderId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public String getOderId() { return oderId; }
    public void setOderId(String oderId) { this.oderId = oderId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public int getGamesWonAsHuman() { return gamesWonAsHuman; }
    public void setGamesWonAsHuman(int gamesWonAsHuman) { this.gamesWonAsHuman = gamesWonAsHuman; }

    public int getGamesWonAsAI() { return gamesWonAsAI; }
    public void setGamesWonAsAI(int gamesWonAsAI) { this.gamesWonAsAI = gamesWonAsAI; }

    public int getCorrectAIIdentifications() { return correctAIIdentifications; }
    public void setCorrectAIIdentifications(int correctAIIdentifications) { this.correctAIIdentifications = correctAIIdentifications; }

    // Calculated stats
    public double getWinRate() {
        return gamesPlayed > 0 ? (double)(gamesWonAsHuman + gamesWonAsAI) / gamesPlayed * 100 : 0;
    }

    public double getDetectRate() {
        return gamesPlayed > 0 ? (double)correctAIIdentifications / gamesPlayed * 100 : 0;
    }
}
