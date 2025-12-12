package imitationgame.aibotservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import imitationgame.aibotservice.config.BotConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OllamaService {
    
    private final WebClient webClient;
    private final BotConfig config;
    private final ObjectMapper objectMapper;
    
    public OllamaService(BotConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(config.getOllamaUrl())
                .build();
    }
    
    /**
     * Generate a response using Ollama with conversation history
     */
    public String generateResponse(List<String> chatHistory, boolean shouldInitiate) {
        try {
            String context = buildContext(chatHistory, shouldInitiate);
            
            OllamaRequest request = new OllamaRequest();
            request.setModel(config.getModel());
            request.setPrompt(context);
            request.setStream(false);
            request.setOptions(Map.of(
                "temperature", 0.9,  // More randomness for human-like responses
                "top_p", 0.9,
                "max_tokens", 100    // Keep responses short
            ));
            
            OllamaResponse response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .block();
            
            if (response != null && response.getResponse() != null) {
                String generated = response.getResponse().trim();
                log.info("Generated response: {}", generated);
                return cleanupResponse(generated);
            }
            
            return getFallbackResponse(shouldInitiate);
            
        } catch (Exception e) {
            log.error("Error generating response from Ollama: {}", e.getMessage());
            return getFallbackResponse(shouldInitiate);
        }
    }
    
    private String buildContext(List<String> chatHistory, boolean shouldInitiate) {
        StringBuilder context = new StringBuilder();
        context.append(config.getSystemPrompt()).append("\n\n");
        
        if (shouldInitiate) {
            context.append("Initiate a new conversation topic or ask a question to engage other players.\n\n");
        } else {
            context.append("Respond naturally to the recent messages:\n\n");
        }
        
        // Add recent chat history (last 10 messages)
        int startIdx = Math.max(0, chatHistory.size() - 10);
        for (int i = startIdx; i < chatHistory.size(); i++) {
            context.append(chatHistory.get(i)).append("\n");
        }
        
        context.append("\nYour response:");
        return context.toString();
    }
    
    private String cleanupResponse(String response) {
        // Remove any meta commentary or excessive formatting
        response = response.replaceAll("(?i)^(as an? (ai|bot|player)|i (would|will) say:?|my response:?)", "").trim();
        response = response.replaceAll("\"", "");  // Remove quotes
        
        // Truncate if too long
        if (response.length() > 200) {
            int lastPeriod = response.substring(0, 200).lastIndexOf('.');
            if (lastPeriod > 100) {
                response = response.substring(0, lastPeriod + 1);
            } else {
                response = response.substring(0, 200) + "...";
            }
        }
        
        return response;
    }
    
    private String getFallbackResponse(boolean shouldInitiate) {
        if (shouldInitiate) {
            String[] questions = {
                "So what do you all do for fun?",
                "Anyone here play games often?",
                "What's everyone's favorite game?",
                "How's everyone doing today?",
                "Anyone else new to this game?"
            };
            return questions[(int) (Math.random() * questions.length)];
        } else {
            String[] responses = {
                "Interesting point!",
                "I see what you mean",
                "That makes sense",
                "Yeah, I agree",
                "Hmm, not sure about that"
            };
            return responses[(int) (Math.random() * responses.length)];
        }
    }
    
    @Data
    private static class OllamaRequest {
        private String model;
        private String prompt;
        private boolean stream;
        private Map<String, Object> options;
    }
    
    @Data
    private static class OllamaResponse {
        private String response;
        private String model;
        private boolean done;
    }
}
