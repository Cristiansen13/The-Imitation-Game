package imitationgame.aibotservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import imitationgame.aibotservice.config.BotConfig;
import imitationgame.aibotservice.model.BotCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages bot instances for multiple rooms
 * Monitors available rooms and automatically joins when needed
 * Uses a pool of bot accounts to support concurrent rooms
 */
@Slf4j
@Service
public class RoomBotManager {
    
    private final BotConfig config;
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final WebClient webClient;
    
    private final Map<String, BotPlayer> activeBots = new ConcurrentHashMap<>();
    private final Set<String> joiningRooms = ConcurrentHashMap.newKeySet();
    private final List<BotCredentials> botCredentialsPool = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    public RoomBotManager(BotConfig config, OllamaService ollamaService, ObjectMapper objectMapper, AuthService authService) {
        this.config = config;
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
        this.authService = authService;
        this.webClient = WebClient.builder()
                .baseUrl(config.getChatServiceUrl())
                .build();
        
        // Initialize bot credentials pool
        initializeBotPool();
    }
    
    private void initializeBotPool() {
        if (config.getBotPool() == null || config.getBotPool().isEmpty()) {
            log.error("No bot credentials configured in application.yml");
            return;
        }
        
        for (String botEntry : config.getBotPool()) {
            String[] parts = botEntry.split(":");
            if (parts.length >= 2) {
                String username = parts[0];
                String password = parts[1];
                botCredentialsPool.add(new BotCredentials(username, password));
                log.info("Added bot to pool: {}", username);
            }
        }
        
        log.info("Initialized bot pool with {} bots", botCredentialsPool.size());
    }
    
    private synchronized BotCredentials acquireBotCredentials() {
        return botCredentialsPool.stream()
                .filter(bot -> !bot.isInUse())
                .findFirst()
                .map(bot -> {
                    bot.setInUse(true);
                    log.info("Acquired bot: {}", bot.getUsername());
                    return bot;
                })
                .orElse(null);
    }
    
    private synchronized void releaseBotCredentials(String username) {
        botCredentialsPool.stream()
                .filter(bot -> bot.getUsername().equals(username))
                .findFirst()
                .ifPresent(bot -> {
                    bot.setInUse(false);
                    log.info("Released bot: {}", username);
                });
    }
    
    /**
     * Check for rooms that need a bot every 3 seconds (reduced from 5 for faster response)
     */
    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
    public void monitorRooms() {
        try {
            // Get an available bot to use its credentials for API access
            BotCredentials botCreds = botCredentialsPool.stream().findFirst().orElse(null);
            if (botCreds == null) {
                log.warn("No bot credentials available for monitoring");
                return;
            }
            
            // Get access token
            String token = authService.getAccessToken(botCreds.getUsername(), botCreds.getPassword());
            if (token == null) {
                log.error("Failed to obtain access token for monitoring");
                return;
            }
            
            log.debug("Using token for room monitoring: {}...{}", 
                     token.substring(0, Math.min(20, token.length())),
                     token.substring(Math.max(0, token.length() - 20)));
            
            // Get list of waiting rooms
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rooms = (List<Map<String, Object>>) (List<?>) webClient.get()
                    .uri("/rooms")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                             clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Room API returned {}: {}", clientResponse.statusCode(), body))
                                .then(Mono.error(new RuntimeException("Room API error: " + clientResponse.statusCode()))))
                    .bodyToFlux(Map.class)
                    .collectList()
                    .block();
            
            if (rooms == null) return;
            
            for (Map<String, Object> room : rooms) {
                String roomId = (String) room.get("id");
                String status = (String) room.get("status");
                List<Map<String, Object>> players = (List<Map<String, Object>>) room.get("players");
                
                // Check if room needs a bot (has 2+ human players, status is WAITING, bot not already present)
                // Wait for at least 2 players to ensure they have time to establish WebSocket connections
                if ("WAITING".equals(status) && players != null && players.size() >= 2 && 
                    !activeBots.containsKey(roomId) && !joiningRooms.contains(roomId) && !hasBot(players)) {
                    
                    log.info("Room {} has {} players and needs a bot. Joining immediately...", roomId, players.size());
                    
                    // Mark room as being joined to prevent race condition
                    joiningRooms.add(roomId);
                    
                    try {
                        // FIXED: Join immediately instead of scheduling with delay
                        // The delay was causing race conditions where players could join/leave in the meantime
                        // Players should already have WebSocket connections established by now since they
                        // joined via REST API first, then subscribed to WebSocket
                        joinRoomAsBot(roomId);
                    } catch (Exception e) {
                        log.error("Failed to join room {} as bot: {}", roomId, e.getMessage(), e);
                        joiningRooms.remove(roomId); // Clean up on failure
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error monitoring rooms: {}", e.getMessage());
        }
    }
    
    private boolean hasBot(List<Map<String, Object>> players) {
        // Check if any bot from our pool is already in the room
        return players.stream()
                .anyMatch(p -> {
                    String username = (String) p.get("username");
                    return botCredentialsPool.stream()
                            .anyMatch(bot -> bot.getUsername().equals(username));
                });
    }
    
    private void joinRoomAsBot(String roomId) {
        BotCredentials botCreds = null;
        try {
            // Acquire an available bot from the pool
            botCreds = acquireBotCredentials();
            if (botCreds == null) {
                log.warn("No available bots in pool. All {} bots are in use.", botCredentialsPool.size());
                return;
            }
            
            // Get access token for this bot
            String token = authService.getAccessToken(botCreds.getUsername(), botCreds.getPassword());
            if (token == null) {
                log.error("Failed to get access token for bot: {}", botCreds.getUsername());
                releaseBotCredentials(botCreds.getUsername());
                return;
            }
            
            // Join the room via REST API
            Map<String, Object> joinResponse = webClient.post()
                    .uri("/rooms/" + roomId + "/join")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                             clientResponse -> clientResponse.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Join room API returned {}: {}", clientResponse.statusCode(), body))
                                .then(Mono.error(new RuntimeException("Join room API error: " + clientResponse.statusCode()))))
                    .bodyToMono(Map.class)
                    .block();
            
            if (joinResponse == null) {
                log.error("Failed to join room {} with bot {}", roomId, botCreds.getUsername());
                releaseBotCredentials(botCreds.getUsername());
                joiningRooms.remove(roomId); // Clean up if join failed
                return;
            }
            
            log.info("Bot {} joined room {} via REST API, connecting WebSocket...", botCreds.getUsername(), roomId);
            
            // Small delay to ensure PLAYER_JOINED event is processed by other clients
            // This gives time for the broadcast to be received before bot starts interacting
            Thread.sleep(500);
            
            // Create and connect bot player
            final BotCredentials finalBotCreds = botCreds;
            BotPlayer botPlayer = new BotPlayer(
                    roomId, 
                    token,  // Pass the access token 
                    botCreds.getUsername(), 
                    config, 
                    ollamaService, 
                    objectMapper,
                    () -> releaseBotCredentials(finalBotCreds.getUsername())  // Release when disconnected
            );
            
            botPlayer.connect();
            activeBots.put(roomId, botPlayer);
            joiningRooms.remove(roomId); // Remove from joining set now that bot is active
            
            log.info("Bot {} successfully joined room {} and connected to WebSocket", botCreds.getUsername(), roomId);
            
        } catch (InterruptedException e) {
            log.warn("Bot join interrupted for room {}", roomId);
            Thread.currentThread().interrupt();
            if (botCreds != null) {
                releaseBotCredentials(botCreds.getUsername());
            }
            joiningRooms.remove(roomId); // Clean up on interrupt
        } catch (Exception e) {
            log.error("Error joining room {} as bot: {}", roomId, e.getMessage());
            if (botCreds != null) {
                releaseBotCredentials(botCreds.getUsername());
            }
            joiningRooms.remove(roomId); // Clean up on error
        }
    }
    
    /**
     * Clean up inactive bots and check for empty/deleted rooms
     */
    @Scheduled(fixedDelay = 10000)  // Check every 10 seconds
    public void cleanupInactiveBots() {
        activeBots.entrySet().removeIf(entry -> {
            String roomId = entry.getKey();
            BotPlayer bot = entry.getValue();
            
            // Check if WebSocket connection is closed
            if (!bot.isOpen()) {
                log.info("Removing inactive bot from room {} (connection closed)", roomId);
                bot.disconnect();
                return true;
            }
            
            // Check if the room still exists and has human players
            try {
                // Get an available bot credential to use for API access
                BotCredentials botCreds = botCredentialsPool.stream().findFirst().orElse(null);
                if (botCreds == null) {
                    return false;
                }
                
                String token = authService.getAccessToken(botCreds.getUsername(), botCreds.getPassword());
                if (token == null) {
                    return false;
                }
                
                // Try to get the room info
                Map<String, Object> room = webClient.get()
                        .uri("/rooms/" + roomId)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError(), 
                                 clientResponse -> {
                                     log.info("Room {} no longer exists or is not accessible", roomId);
                                     return Mono.empty();
                                 })
                        .bodyToMono(Map.class)
                        .onErrorReturn(null)
                        .block();
                
                if (room == null) {
                    // Room doesn't exist anymore, disconnect bot
                    log.info("Removing bot from deleted room {}", roomId);
                    bot.disconnect();
                    return true;
                }
                
                // Check if room only has bots (no human players)
                List<Map<String, Object>> players = (List<Map<String, Object>>) room.get("players");
                if (players != null) {
                    long humanCount = players.stream()
                            .filter(p -> {
                                String username = (String) p.get("username");
                                return !botCredentialsPool.stream()
                                        .anyMatch(b -> b.getUsername().equals(username));
                            })
                            .count();
                    
                    if (humanCount == 0) {
                        // Only bots left, disconnect
                        log.info("Removing bot from room {} (no human players left)", roomId);
                        bot.disconnect();
                        return true;
                    }
                }
                
            } catch (Exception e) {
                log.debug("Error checking room status for {}: {}", roomId, e.getMessage());
            }
            
            return false;
        });
    }
}
