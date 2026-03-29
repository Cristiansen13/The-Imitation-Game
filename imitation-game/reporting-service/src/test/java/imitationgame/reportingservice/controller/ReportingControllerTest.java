package imitationgame.reportingservice.controller;

import imitationgame.reportingservice.dto.GlobalStats;
import imitationgame.reportingservice.dto.LeaderboardResponse;
import imitationgame.reportingservice.dto.PlayerStats;
import imitationgame.reportingservice.service.ReportingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportingControllerTest {

    private ReportingService reportingService;
    private ReportingController controller;

    @BeforeEach
    void setUp() {
        reportingService = Mockito.mock(ReportingService.class);
        controller = new ReportingController();
        java.lang.reflect.Field field;
        try {
            field = ReportingController.class.getDeclaredField("reportingService");
            field.setAccessible(true);
            field.set(controller, reportingService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void leaderboard_ok() {
        Mockito.when(reportingService.getLeaderboard(10)).thenReturn(new LeaderboardResponse());
        assertEquals(HttpStatus.OK, controller.getLeaderboard(10).getStatusCode());
    }

    @Test
    void meStats_ok() {
        Mockito.when(reportingService.getPlayerStats("u1")).thenReturn(new PlayerStats());
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1").build();
        assertEquals(HttpStatus.OK, controller.getMyStats(jwt).getStatusCode());
    }

    @Test
    void playerStats_ok() {
        Mockito.when(reportingService.getPlayerStats("u2")).thenReturn(new PlayerStats());
        assertEquals(HttpStatus.OK, controller.getPlayerStats("u2").getStatusCode());
    }

    @Test
    void playerStatsByUsername_ok() {
        Mockito.when(reportingService.getPlayerStatsByUsername("mihai")).thenReturn(new PlayerStats());
        assertEquals(HttpStatus.OK, controller.getPlayerStatsByUsername("mihai").getStatusCode());
    }

    @Test
    void global_ok() {
        Mockito.when(reportingService.getGlobalStats()).thenReturn(new GlobalStats());
        assertEquals(HttpStatus.OK, controller.getGlobalStats().getStatusCode());
    }

    @Test
    void recentGames_ok() {
        Mockito.when(reportingService.getRecentGames(20)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getRecentGames(20).getStatusCode());
    }

    @Test
    void roomLeaderboard_ok() {
        Mockito.when(reportingService.getRoomLeaderboard("r1")).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getRoomLeaderboard("r1").getStatusCode());
    }

    @Test
    void health_ok() {
        assertEquals(HttpStatus.OK, controller.health().getStatusCode());
    }

    @Test
    void updateSettings_ok() {
        assertEquals(HttpStatus.OK, controller.updateSettings(Map.of("leaderboardLimit", 100)).getStatusCode());
    }

    @Test
    void clearCache_ok() {
        assertEquals(HttpStatus.OK, controller.clearCache().getStatusCode());
    }
}
