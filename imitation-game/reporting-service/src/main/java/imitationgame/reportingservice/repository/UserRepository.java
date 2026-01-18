package imitationgame.reportingservice.repository;

import imitationgame.reportingservice.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserProfile, String> {
    
    Optional<UserProfile> findByUsername(String username);
    
    @Query("SELECT u FROM UserProfile u ORDER BY (u.gamesWonAsHuman + u.gamesWonAsAI) DESC")
    List<UserProfile> findTopPlayersByWins();
    
    @Query("SELECT u FROM UserProfile u WHERE u.gamesPlayed > 0 ORDER BY (u.gamesWonAsHuman + u.gamesWonAsAI) * 1.0 / u.gamesPlayed DESC")
    List<UserProfile> findTopPlayersByWinRate();
    
    @Query("SELECT u FROM UserProfile u WHERE u.gamesPlayed > 0 ORDER BY u.correctAIIdentifications * 1.0 / u.gamesPlayed DESC")
    List<UserProfile> findTopPlayersByDetectRate();
    
    @Query("SELECT u FROM UserProfile u ORDER BY u.gamesPlayed DESC")
    List<UserProfile> findMostActivePlayers();
    
    @Query("SELECT u FROM UserProfile u ORDER BY u.experiencePoints DESC")
    List<UserProfile> findTopPlayersByXP();
    
    @Query("SELECT COALESCE(SUM(u.gamesPlayed), 0) FROM UserProfile u")
    Long getTotalGameParticipations();
}
