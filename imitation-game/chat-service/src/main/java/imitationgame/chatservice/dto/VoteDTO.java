package imitationgame.chatservice.dto;

public class VoteDTO {
    
    private String voterId;
    private String targetId;

    public VoteDTO() {}

    public VoteDTO(String voterId, String targetId) {
        this.voterId = voterId;
        this.targetId = targetId;
    }

    public String getVoterId() {
        return voterId;
    }

    public void setVoterId(String voterId) {
        this.voterId = voterId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
}
