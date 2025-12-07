package imitationgame.reportingservice.repository;

import imitationgame.reportingservice.model.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface GameRoomRepository extends JpaRepository<GameRoom, String> {
    
    List<GameRoom> findByStatus(GameRoom.GameStatus status);
    
    @Query("SELECT COUNT(g) FROM GameRoom g WHERE g.status = 'FINISHED'")
    long countFinishedGames();
    
    @Query("SELECT COUNT(g) FROM GameRoom g WHERE g.status IN ('WAITING', 'IN_PROGRESS', 'VOTING')")
    long countActiveGames();
    
    @Query("SELECT g FROM GameRoom g WHERE g.endedAt >= :since ORDER BY g.endedAt DESC")
    List<GameRoom> findRecentlyFinishedGames(Instant since);
    
    @Query("SELECT g FROM GameRoom g WHERE g.status = 'FINISHED' ORDER BY g.endedAt DESC")
    List<GameRoom> findAllFinishedGamesOrderByEndedAt();
}
