package imitationgame.reportingservice.dto;

import java.util.List;

public class LeaderboardResponse {
    
    private List<PlayerStats> topByWins;
    private List<PlayerStats> topByWinRate;
    private List<PlayerStats> topByDetectRate;
    private List<PlayerStats> mostActive;

    public List<PlayerStats> getTopByWins() { return topByWins; }
    public void setTopByWins(List<PlayerStats> topByWins) { this.topByWins = topByWins; }

    public List<PlayerStats> getTopByWinRate() { return topByWinRate; }
    public void setTopByWinRate(List<PlayerStats> topByWinRate) { this.topByWinRate = topByWinRate; }

    public List<PlayerStats> getTopByDetectRate() { return topByDetectRate; }
    public void setTopByDetectRate(List<PlayerStats> topByDetectRate) { this.topByDetectRate = topByDetectRate; }

    public List<PlayerStats> getMostActive() { return mostActive; }
    public void setMostActive(List<PlayerStats> mostActive) { this.mostActive = mostActive; }
}
