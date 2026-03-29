package imitationgame.chatservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_players")
public class RoomPlayer {

    @Id
    private String id = UUID.randomUUID().toString();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    @JsonIgnore
    private GameRoom room;
    
    private String oderId; // user ID from auth-service
    private String username;
    
    @Enumerated(EnumType.STRING)
    private PlayerStatus status = PlayerStatus.ALIVE;
    
    private boolean isAI = false;
    private int votesReceived = 0;
    private String votedFor; // User ID this player voted for
    
    private Instant joinedAt = Instant.now();
    private Instant eliminatedAt;

    public enum PlayerStatus {
        ALIVE,
        ELIMINATED,
        DISCONNECTED
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GameRoom getRoom() {
        return room;
    }

    public void setRoom(GameRoom room) {
        this.room = room;
    }

    public String getUserId() {
        return oderId;
    }

    public void setUserId(String oderId) {
        this.oderId = oderId;
    }

    public String getOderId() {
        return oderId;
    }

    public void setOderId(String oderId) {
        this.oderId = oderId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public boolean isAI() {
        return isAI;
    }

    public void setAI(boolean AI) {
        isAI = AI;
    }

    public int getVotesReceived() {
        return votesReceived;
    }

    public void setVotesReceived(int votesReceived) {
        this.votesReceived = votesReceived;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getEliminatedAt() {
        return eliminatedAt;
    }

    public void setEliminatedAt(Instant eliminatedAt) {
        this.eliminatedAt = eliminatedAt;
    }
    
    public String getRoomId() {
        return room != null ? room.getId() : null;
    }
}
