package imitationgame.reportingservice.dto;

public class GlobalStats {
    
    private long totalPlayers;
    private long totalGamesPlayed;
    private long activeGames;
    private int gamesLast24Hours;
    private double averageWinRate;
    private double averageDetectRate;

    public long getTotalPlayers() { return totalPlayers; }
    public void setTotalPlayers(long totalPlayers) { this.totalPlayers = totalPlayers; }

    public long getTotalGamesPlayed() { return totalGamesPlayed; }
    public void setTotalGamesPlayed(long totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }

    public long getActiveGames() { return activeGames; }
    public void setActiveGames(long activeGames) { this.activeGames = activeGames; }

    public int getGamesLast24Hours() { return gamesLast24Hours; }
    public void setGamesLast24Hours(int gamesLast24Hours) { this.gamesLast24Hours = gamesLast24Hours; }

    public double getAverageWinRate() { return averageWinRate; }
    public void setAverageWinRate(double averageWinRate) { this.averageWinRate = averageWinRate; }

    public double getAverageDetectRate() { return averageDetectRate; }
    public void setAverageDetectRate(double averageDetectRate) { this.averageDetectRate = averageDetectRate; }
}
