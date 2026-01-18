package imitationgame.reportingservice.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "room_players")
public class RoomPlayer {

    @Id
    private String id;
    
    @Column(name = "room_id")
    private String roomId;
    
    @Column(name = "oder_id")
    private String oderId;
    
    private String username;
    
    @Column(name = "is_ai")
    private boolean isAI = false;
    
    @Column(name = "joined_at")
    private Instant joinedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getOderId() { return oderId; }
    public void setOderId(String oderId) { this.oderId = oderId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isAI() { return isAI; }
    public void setAI(boolean AI) { isAI = AI; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
}
