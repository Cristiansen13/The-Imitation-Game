package imitationgame.chatservice.dto;

import java.util.List;

public class RoomResponse {
    
    private String id;
    private String name;
    private String status;
    private int currentRound;
    private int maxRounds;
    private int playerCount;
    private List<PlayerInfo> players;
    private String createdAt;
    private int lobbyDurationSeconds = 60;
    private String roundStartTime;
    private long currentTimeMillis;
    private int roundDurationSeconds = 120;
    private int votingDurationSeconds = 60;

    public static class PlayerInfo {
        private String oderId;
        private String username;
        private String status;

        public String getOderId() {
            return oderId;
        }

        public void setOderId(String oderId) {
            this.oderId = oderId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfo> players) {
        this.players = players;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getLobbyDurationSeconds() {
        return lobbyDurationSeconds;
    }

    public void setLobbyDurationSeconds(int lobbyDurationSeconds) {
        this.lobbyDurationSeconds = lobbyDurationSeconds;
    }

    public String getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(String roundStartTime) {
        this.roundStartTime = roundStartTime;
    }

    public long getCurrentTimeMillis() {
        return currentTimeMillis;
    }

    public void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }

    public int getRoundDurationSeconds() {
        return roundDurationSeconds;
    }

    public void setRoundDurationSeconds(int roundDurationSeconds) {
        this.roundDurationSeconds = roundDurationSeconds;
    }

    public int getVotingDurationSeconds() {
        return votingDurationSeconds;
    }

    public void setVotingDurationSeconds(int votingDurationSeconds) {
        this.votingDurationSeconds = votingDurationSeconds;
    }
}
