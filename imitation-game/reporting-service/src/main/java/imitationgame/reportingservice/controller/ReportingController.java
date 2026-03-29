package imitationgame.reportingservice.controller;

import imitationgame.reportingservice.dto.*;
import imitationgame.reportingservice.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/reports")
public class ReportingController {

    private final Map<String, Object> reportSettings = new ConcurrentHashMap<>();

    @Autowired
    private ReportingService reportingService;

    /**
     * Get leaderboard
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(reportingService.getLeaderboard(limit));
    }

    /**
     * Get current user's stats
     */
    @GetMapping("/me/stats")
    public ResponseEntity<PlayerStats> getMyStats(@AuthenticationPrincipal Jwt jwt) {
        String oderId = jwt.getSubject();
        PlayerStats stats = reportingService.getPlayerStats(oderId);
        
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get stats for a specific player
     */
    @GetMapping("/player/{oderId}/stats")
    public ResponseEntity<PlayerStats> getPlayerStats(@PathVariable String oderId) {
        PlayerStats stats = reportingService.getPlayerStats(oderId);
        
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get stats by username
     */
    @GetMapping("/player/username/{username}/stats")
    public ResponseEntity<PlayerStats> getPlayerStatsByUsername(@PathVariable String username) {
        PlayerStats stats = reportingService.getPlayerStatsByUsername(username);
        
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get global game statistics
     */
    @GetMapping("/global")
    public ResponseEntity<GlobalStats> getGlobalStats() {
        return ResponseEntity.ok(reportingService.getGlobalStats());
    }

    /**
     * Get recent game history
     */
    @GetMapping("/games/recent")
    public ResponseEntity<List<GameSummary>> getRecentGames(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(reportingService.getRecentGames(limit));
    }
    
    /**
     * Get room leaderboard for players in a specific room
     */
    @GetMapping("/room/{roomId}/leaderboard")
    public ResponseEntity<List<PlayerStats>> getRoomLeaderboard(@PathVariable String roomId) {
        return ResponseEntity.ok(reportingService.getRoomLeaderboard(roomId));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "reporting-service");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        reportSettings.putAll(settings);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Reporting settings updated");
        response.put("settings", reportSettings);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> clearCache() {
        reportSettings.clear();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Reporting cache/settings cleared");
        return ResponseEntity.ok(response);
    }
}
