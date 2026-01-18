package imitationgame.reportingservice.repository;

import imitationgame.reportingservice.model.RoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, String> {
    List<RoomPlayer> findByRoomId(String roomId);
}
