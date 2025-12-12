package imitationgame.aibotservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "bot")
public class BotConfig {
    private String chatServiceUrl;
    private String chatWsUrl;
    private List<String> botPool;
    private String ollamaUrl;
    private String model;
    private int responseDelayMinMs;
    private int responseDelayMaxMs;
    private double initiateQuestionProbability;
    private int maxMessagesPerRound;
    private String systemPrompt;
}
