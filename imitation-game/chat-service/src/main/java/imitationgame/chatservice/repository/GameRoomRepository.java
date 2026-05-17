package imitationgame.chatservice.repository;

import imitationgame.chatservice.model.GameRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoom, String> {
    
    @Query("SELECT r FROM GameRoom r WHERE r.status = 'WAITING' AND SIZE(r.players) < 7 ORDER BY r.createdAt ASC")
    List<GameRoom> findAvailableRooms();
    
    // Return the first available room by delegating to the list query to avoid
    // JPA single-result exceptions when multiple rows match. This keeps behaviour
    // deterministic (oldest waiting room) while being safe if more than one
    // matching row exists.
    default Optional<GameRoom> findFirstAvailableRoom() {
        List<GameRoom> list = findAvailableRooms();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
    
    List<GameRoom> findByStatus(GameRoom.GameStatus status);
    
    @Query("SELECT r FROM GameRoom r WHERE r.status IN ('WAITING', 'IN_PROGRESS', 'VOTING')")
    List<GameRoom> findActiveRooms();
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM GameRoom r WHERE r.id = :id")
    Optional<GameRoom> findByIdWithLock(String id);
}

