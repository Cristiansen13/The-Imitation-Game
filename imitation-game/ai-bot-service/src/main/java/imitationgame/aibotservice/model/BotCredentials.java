package imitationgame.aibotservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BotCredentials {
    private String username;
    private String password;
    private boolean inUse;
    
    public BotCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        this.inUse = false;
    }
}
