package imitationgame.chatservice.repository;

import imitationgame.chatservice.model.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoom, String> {
    
    @Query("SELECT r FROM GameRoom r WHERE r.status = 'WAITING' AND SIZE(r.players) < 7 ORDER BY r.createdAt ASC")
    List<GameRoom> findAvailableRooms();
    
    @Query("SELECT r FROM GameRoom r WHERE r.status = 'WAITING' AND SIZE(r.players) < 7 ORDER BY r.createdAt ASC")
    Optional<GameRoom> findFirstAvailableRoom();
    
    List<GameRoom> findByStatus(GameRoom.GameStatus status);
    
    @Query("SELECT r FROM GameRoom r WHERE r.status IN ('WAITING', 'IN_PROGRESS', 'VOTING')")
    List<GameRoom> findActiveRooms();
}

