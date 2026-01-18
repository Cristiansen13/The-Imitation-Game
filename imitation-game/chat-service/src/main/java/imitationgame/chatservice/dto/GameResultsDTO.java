package imitationgame.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameResultsDTO {
    private String roomId;
    private String winnerId;
    private String winCondition;
    private String aiPlayerId;
    private String aiUsername;
    private List<PlayerResultDTO> players;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerResultDTO {
        private String oderId;
        private String username;
        private Integer votesReceived;
        private String votedFor;
        private Boolean isAI;
        private String status;
    }
}
