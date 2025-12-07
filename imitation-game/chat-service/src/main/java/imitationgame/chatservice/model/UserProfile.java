package imitationgame.chatservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    private String id; // Keycloak UUID
    private String username;
    private String email;
    private String role; // PLAYER, ADMIN
    
    // Game statistics
    private int gamesPlayed = 0;
    private int gamesWonAsHuman = 0;
    private int gamesWonAsAI = 0;
    private int timesEliminatedFirst = 0;
    private int correctAIIdentifications = 0;
    private int totalVotesCast = 0;
    
    private Instant createdAt = Instant.now();
    private Instant lastLoginAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getGamesWonAsHuman() {
        return gamesWonAsHuman;
    }

    public void setGamesWonAsHuman(int gamesWonAsHuman) {
        this.gamesWonAsHuman = gamesWonAsHuman;
    }

    public int getGamesWonAsAI() {
        return gamesWonAsAI;
    }

    public void setGamesWonAsAI(int gamesWonAsAI) {
        this.gamesWonAsAI = gamesWonAsAI;
    }

    public int getTimesEliminatedFirst() {
        return timesEliminatedFirst;
    }

    public void setTimesEliminatedFirst(int timesEliminatedFirst) {
        this.timesEliminatedFirst = timesEliminatedFirst;
    }

    public int getCorrectAIIdentifications() {
        return correctAIIdentifications;
    }

    public void setCorrectAIIdentifications(int correctAIIdentifications) {
        this.correctAIIdentifications = correctAIIdentifications;
    }

    public int getTotalVotesCast() {
        return totalVotesCast;
    }

    public void setTotalVotesCast(int totalVotesCast) {
        this.totalVotesCast = totalVotesCast;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public double getDetectRate() {
        if (totalVotesCast == 0) return 0.0;
        return (double) correctAIIdentifications / totalVotesCast * 100;
    }
    
    public int getTotalWins() {
        return gamesWonAsHuman + gamesWonAsAI;
    }
}
