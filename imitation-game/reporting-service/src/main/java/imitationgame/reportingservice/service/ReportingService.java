package imitationgame.reportingservice.service;

import imitationgame.reportingservice.dto.*;
import imitationgame.reportingservice.model.GameRoom;
import imitationgame.reportingservice.model.UserProfile;
import imitationgame.reportingservice.repository.GameRoomRepository;
import imitationgame.reportingservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    /**
     * Get leaderboard with top players by different metrics
     */
    public LeaderboardResponse getLeaderboard(int limit) {
        LeaderboardResponse response = new LeaderboardResponse();
        
        // Top players by total wins
        response.setTopByWins(userRepository.findTopPlayersByWins()
                .stream()
                .limit(limit)
                .map(this::toPlayerStats)
                .collect(Collectors.toList()));
        
        // Top players by win rate
        response.setTopByWinRate(userRepository.findTopPlayersByWinRate()
                .stream()
                .limit(limit)
                .map(this::toPlayerStats)
                .collect(Collectors.toList()));
        
        // Top players by AI detection rate
        response.setTopByDetectRate(userRepository.findTopPlayersByDetectRate()
                .stream()
                .limit(limit)
                .map(this::toPlayerStats)
                .collect(Collectors.toList()));
        
        // Most active players
        response.setMostActive(userRepository.findMostActivePlayers()
                .stream()
                .limit(limit)
                .map(this::toPlayerStats)
                .collect(Collectors.toList()));
        
        return response;
    }

    /**
     * Get statistics for a specific player
     */
    public PlayerStats getPlayerStats(String oderId) {
        return userRepository.findById(oderId)
                .map(this::toPlayerStats)
                .orElse(null);
    }

    /**
     * Get player stats by username
     */
    public PlayerStats getPlayerStatsByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toPlayerStats)
                .orElse(null);
    }

    /**
     * Get global game statistics
     */
    public GlobalStats getGlobalStats() {
        GlobalStats stats = new GlobalStats();
        
        stats.setTotalPlayers(userRepository.count());
        stats.setTotalGamesPlayed(gameRoomRepository.countFinishedGames());
        stats.setActiveGames(gameRoomRepository.countActiveGames());
        
        // Games in last 24 hours
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        stats.setGamesLast24Hours(gameRoomRepository.findRecentlyFinishedGames(since24h).size());
        
        // Calculate average win rates
        List<UserProfile> allPlayers = userRepository.findAll();
        if (!allPlayers.isEmpty()) {
            double avgWinRate = allPlayers.stream()
                    .filter(p -> p.getGamesPlayed() > 0)
                    .mapToDouble(UserProfile::getWinRate)
                    .average()
                    .orElse(0);
            stats.setAverageWinRate(avgWinRate);
            
            double avgDetectRate = allPlayers.stream()
                    .filter(p -> p.getGamesPlayed() > 0)
                    .mapToDouble(UserProfile::getDetectRate)
                    .average()
                    .orElse(0);
            stats.setAverageDetectRate(avgDetectRate);
        }
        
        return stats;
    }

    /**
     * Get recent game history
     */
    public List<GameSummary> getRecentGames(int limit) {
        return gameRoomRepository.findAllFinishedGamesOrderByEndedAt()
                .stream()
                .limit(limit)
                .map(this::toGameSummary)
                .collect(Collectors.toList());
    }

    private PlayerStats toPlayerStats(UserProfile profile) {
        PlayerStats stats = new PlayerStats();
        stats.setOderId(profile.getOderId());
        stats.setUsername(profile.getUsername());
        stats.setGamesPlayed(profile.getGamesPlayed());
        stats.setGamesWonAsHuman(profile.getGamesWonAsHuman());
        stats.setGamesWonAsAI(profile.getGamesWonAsAI());
        stats.setTotalWins(profile.getGamesWonAsHuman() + profile.getGamesWonAsAI());
        stats.setCorrectAIIdentifications(profile.getCorrectAIIdentifications());
        stats.setWinRate(profile.getWinRate());
        stats.setDetectRate(profile.getDetectRate());
        return stats;
    }

    private GameSummary toGameSummary(GameRoom room) {
        GameSummary summary = new GameSummary();
        summary.setId(room.getId());
        summary.setName(room.getName());
        summary.setRounds(room.getCurrentRound());
        summary.setStartedAt(room.getStartedAt());
        summary.setEndedAt(room.getEndedAt());
        return summary;
    }
}
