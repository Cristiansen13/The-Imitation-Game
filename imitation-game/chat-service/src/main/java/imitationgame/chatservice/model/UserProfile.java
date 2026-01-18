package imitationgame.chatservice.model;

import jakarta.persistence.Column;
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
    @Column(name = "games_played")
    private int gamesPlayed = 0;
    
    @Column(name = "games_won_as_human")
    private int gamesWonAsHuman = 0;
    
    @Column(name = "games_won_as_ai")
    private int gamesWonAsAI = 0;
    
    @Column(name = "times_eliminated_first")
    private int timesEliminatedFirst = 0;
    
    @Column(name = "correctaiidentifications")
    private int correctAIIdentifications = 0;
    
    @Column(name = "total_votes_cast")
    private int totalVotesCast = 0;
    
    @Column(name = "experience_points")
    private int experiencePoints = 0;
    
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
    
    @Column(name = "last_login_at")
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
    
    public int getExperiencePoints() {
        return experiencePoints;
    }

    public void setExperiencePoints(int experiencePoints) {
        this.experiencePoints = experiencePoints;
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
