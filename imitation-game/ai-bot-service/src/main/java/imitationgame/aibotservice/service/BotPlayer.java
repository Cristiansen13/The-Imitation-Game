package imitationgame.aibotservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import imitationgame.aibotservice.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

/**
 * Represents a bot player in a specific room
 * Maintains room-specific chat history and generates responses using Ollama
 */
@Slf4j
public class BotPlayer extends WebSocketClient {
    
    private final String roomId;
    private final String accessToken;
    private final String botUsername;
    private final BotConfig config;
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;
    private final Runnable onDisconnectCallback;
    
    private final List<String> chatHistory = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();
    
    private int messageCount = 0;
    private boolean isVotingPhase = false;
    private volatile boolean isConnected = false;
    private volatile boolean responseScheduled = false;
    
    public BotPlayer(String roomId, String accessToken, String botUsername, 
                     BotConfig config, OllamaService ollamaService, ObjectMapper objectMapper,
                     Runnable onDisconnectCallback) throws Exception {
        super(new URI(config.getChatWsUrl()));
        this.roomId = roomId;
        this.accessToken = accessToken;
        this.botUsername = botUsername;
        this.config = config;
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
        this.onDisconnectCallback = onDisconnectCallback;
        
        // Add Authorization header with token
        this.addHeader("Authorization", "Bearer " + accessToken);
        this.addHeader("Origin", config.getChatServiceUrl());
    }
    
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Bot WebSocket connected for room {}", roomId);
        
        // Send STOMP CONNECT frame (token already in query parameter)
        try {
            String connectFrame = "CONNECT\n" +
                    "accept-version:1.1,1.2\n" +
                    "heart-beat:10000,10000\n\n\0";
            send(connectFrame);
            
            log.info("Bot sent CONNECT frame");
            
        } catch (Exception e) {
            log.error("Error sending CONNECT frame: {}", e.getMessage());
        }
    }
    
    private void onStompConnected() {
        log.info("Bot STOMP connected to room {}", roomId);
        isConnected = true;
        
        // Subscribe to room topic for game events
        try {
            String subscribeFrame = "SUBSCRIBE\n" +
                    "id:sub-room-" + botUsername + "\n" +
                    "destination:/topic/room/" + roomId + "\n\n\0";
            send(subscribeFrame);
            
            log.info("Bot subscribed to room topic");
            
            // Subscribe to chat messages topic
            String subscribeChatFrame = "SUBSCRIBE\n" +
                    "id:sub-chat-" + botUsername + "\n" +
                    "destination:/topic/messages/" + roomId + "\n\n\0";
            send(subscribeChatFrame);
            
            log.info("Bot subscribed to chat messages topic");
            
            // Schedule periodic message checks
            scheduleNextAction();
            
        } catch (Exception e) {
            log.error("Error subscribing to room: {}", e.getMessage());
        }
    }
    
    @Override
    public void onMessage(String message) {
        try {
            // Handle STOMP CONNECTED frame
            if (message.startsWith("CONNECTED")) {
                log.info("Bot received CONNECTED frame");
                onStompConnected();
                return;
            }
            
            // Parse STOMP MESSAGE frames
            if (message.startsWith("MESSAGE")) {
                String[] lines = message.split("\n");
                String body = lines[lines.length - 1].replace("\0", "");
                
                JsonNode event = objectMapper.readTree(body);
                
                // Check if it's a game event (has 'type' field) or a chat message
                if (event.has("type")) {
                    String eventType = event.get("type").asText();
                    log.info("Bot received event: {} in room {}", eventType, roomId);
                    
                    switch (eventType) {
                        case "VOTING_STARTED":
                            handleVotingStarted();
                            break;
                        case "ROUND_STARTED":
                            handleRoundStarted();
                            break;
                        case "GAME_ENDED":
                            handleGameEnded();
                            break;
                    }
                } else if (event.has("message")) {
                    // This is a chat message from /topic/messages/{roomId}
                    handleChatMessage(event);
                }
            }
        } catch (Exception e) {
            log.error("Error processing message in room {}: {}", roomId, e.getMessage());
        }
    }
    
    private void handleChatMessage(JsonNode message) {
        try {
            String username = message.get("username").asText();
            String messageText = message.get("message").asText();
            
            log.info("Bot {} received message from {}: {}", botUsername, username, messageText);
            
            // Don't process our own messages
            if (username.equals(botUsername)) {
                log.info("Bot {} ignoring own message", botUsername);
                return;
            }
            
            // Add to chat history
            chatHistory.add(username + ": " + messageText);
            log.info("Added message to history: {} - {}", username, messageText);
            
            // Decide whether to respond (only if not already scheduled)
            if (shouldRespond() && !isVotingPhase && !responseScheduled) {
                scheduleResponse(false);
            }
            
        } catch (Exception e) {
            log.error("Error handling chat message: {}", e.getMessage());
        }
    }
    
    private void handleVotingStarted() {
        log.info("Voting started in room {}", roomId);
        isVotingPhase = true;
        
        // Cast a random vote after a delay
        scheduler.schedule(this::castVote, 
                random.nextInt(5000) + 2000, 
                TimeUnit.MILLISECONDS);
    }
    
    private void handleRoundStarted() {
        log.info("New round started in room {}", roomId);
        isVotingPhase = false;
        messageCount = 0;
        
        // Maybe initiate conversation at round start
        if (random.nextDouble() < config.getInitiateQuestionProbability()) {
            scheduleResponse(true);
        }
    }
    
    private void handleGameEnded() {
        log.info("Game ended in room {}", roomId);
        disconnect();
    }
    
    private void scheduleNextAction() {
        if (!isConnected || isVotingPhase) {
            return;
        }
        
        // Randomly decide to initiate conversation
        if (messageCount < config.getMaxMessagesPerRound() && 
            random.nextDouble() < config.getInitiateQuestionProbability()) {
            
            int delay = random.nextInt(config.getResponseDelayMaxMs() - config.getResponseDelayMinMs()) 
                    + config.getResponseDelayMinMs();
            
            scheduler.schedule(() -> {
                if (isConnected && !isVotingPhase) {
                    sendMessage(true);
                    // Don't recursively schedule - bot should respond to incoming messages, not loop
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
        // Don't schedule another check - bot should be message-driven, not poll-driven
    }
    
    private void scheduleResponse(boolean shouldInitiate) {
        responseScheduled = true;
        int delay = random.nextInt(config.getResponseDelayMaxMs() - config.getResponseDelayMinMs()) 
                + config.getResponseDelayMinMs();
        
        scheduler.schedule(() -> {
            sendMessage(shouldInitiate);
            responseScheduled = false;  // Allow scheduling next response
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    private boolean shouldRespond() {
        // Respond with 60% probability if not at message limit
        return messageCount < config.getMaxMessagesPerRound() && random.nextDouble() < 0.6;
    }
    
    private void sendMessage(boolean shouldInitiate) {
        if (!isConnected || isVotingPhase) {
            return;
        }
        
        try {
            String response = ollamaService.generateResponse(chatHistory, shouldInitiate);
            
            // Send via STOMP to the correct chat endpoint
            String messageFrame = "SEND\n" +
                    "destination:/app/chat/" + roomId + "\n" +
                    "content-type:application/json\n\n" +
                    "{\"message\":\"" + escapeJson(response) + "\",\"userId\":\"" + botUsername + "\",\"username\":\"" + botUsername + "\"}\n\0";
            
            send(messageFrame);
            
            // Don't add our own messages to chat history - bot shouldn't respond to itself
            // chatHistory.add(botUsername + ": " + response);
            messageCount++;
            
            log.info("Bot sent message in room {}: {}", roomId, response);
            
        } catch (Exception e) {
            log.error("Error sending message in room {}: {}", roomId, e.getMessage());
        }
    }
    
    private void castVote() {
        // TODO: Implement voting logic - randomly vote for a player (not self)
        log.info("Bot would cast vote in room {} (not yet implemented)", roomId);
    }
    
    private String escapeJson(String str) {
        return str.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    public void disconnect() {
        isConnected = false;
        scheduler.shutdownNow();
        close();
        log.info("Bot {} disconnected from room {}", botUsername, roomId);
        
        // Release bot credentials back to pool
        if (onDisconnectCallback != null) {
            onDisconnectCallback.run();
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("Bot {} connection closed for room {}: {} - {}", botUsername, roomId, code, reason);
        isConnected = false;
        
        // Release bot credentials back to pool
        if (onDisconnectCallback != null) {
            onDisconnectCallback.run();
        }
    }
    
    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error in room {}: {}", roomId, ex.getMessage());
    }
}
