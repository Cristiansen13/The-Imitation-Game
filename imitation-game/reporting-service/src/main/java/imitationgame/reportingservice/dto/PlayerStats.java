package imitationgame.reportingservice.dto;

public class PlayerStats {
    
    private String id;
    private String username;
    private int gamesPlayed;
    private int gamesWonAsHuman;
    private int gamesWonAsAI;
    private int totalWins;
    private int correctAIIdentifications;
    private double winRate;
    private double detectRate;
    private int experiencePoints;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public int getGamesWonAsHuman() { return gamesWonAsHuman; }
    public void setGamesWonAsHuman(int gamesWonAsHuman) { this.gamesWonAsHuman = gamesWonAsHuman; }

    public int getGamesWonAsAI() { return gamesWonAsAI; }
    public void setGamesWonAsAI(int gamesWonAsAI) { this.gamesWonAsAI = gamesWonAsAI; }

    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }

    public int getCorrectAIIdentifications() { return correctAIIdentifications; }
    public void setCorrectAIIdentifications(int correctAIIdentifications) { this.correctAIIdentifications = correctAIIdentifications; }

    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }

    public double getDetectRate() { return detectRate; }
    public void setDetectRate(double detectRate) { this.detectRate = detectRate; }
    
    public int getExperiencePoints() { return experiencePoints; }
    public void setExperiencePoints(int experiencePoints) { this.experiencePoints = experiencePoints; }
}
