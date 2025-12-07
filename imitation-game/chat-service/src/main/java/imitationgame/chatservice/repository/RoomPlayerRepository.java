package imitationgame.chatservice.repository;

import imitationgame.chatservice.model.RoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, String> {
    
    List<RoomPlayer> findByRoomId(String roomId);
    
    Optional<RoomPlayer> findByRoomIdAndOderId(String roomId, String oderId);
    
    @Query("SELECT rp FROM RoomPlayer rp WHERE rp.room.id = :roomId AND rp.status = 'ALIVE'")
    List<RoomPlayer> findAlivePlayersByRoomId(@Param("roomId") String roomId);
    
    @Query("SELECT COUNT(rp) FROM RoomPlayer rp WHERE rp.room.id = :roomId")
    int countByRoomId(@Param("roomId") String roomId);
    
    boolean existsByRoomIdAndOderId(String roomId, String oderId);
}
