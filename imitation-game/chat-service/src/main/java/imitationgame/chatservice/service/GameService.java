package imitationgame.chatservice.service;

import imitationgame.chatservice.dto.GameEvent;
import imitationgame.chatservice.dto.GameResultsDTO;
import imitationgame.chatservice.model.GameRoom;
import imitationgame.chatservice.model.RoomPlayer;
import imitationgame.chatservice.model.UserProfile;
import imitationgame.chatservice.repository.GameRoomRepository;
import imitationgame.chatservice.repository.RoomPlayerRepository;
import imitationgame.chatservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameService {

    private static final int MIN_PLAYERS = 3;
    private static final int MAX_PLAYERS = 7;
    private static final int DEFAULT_ROUNDS = 5;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private RoomPlayerRepository roomPlayerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private RedisMessagePublisher redisPublisher;

    /**
     * Create a new game room
     */
    @Transactional
    public GameRoom createRoom(String creatorId, String creatorUsername) {
        GameRoom room = new GameRoom();
        room.setId(UUID.randomUUID().toString());
        room.setName("Room-" + room.getId().substring(0, 8));
        room.setStatus(GameRoom.GameStatus.WAITING);
        room.setMaxRounds(DEFAULT_ROUNDS);
        room.setCurrentRound(0);
        room.setCreatedAt(Instant.now());
        room.setPlayers(new ArrayList<>());

        room = gameRoomRepository.save(room);
        
        // Add creator as first player
        joinRoom(room.getId(), creatorId, creatorUsername);
        
        return room;
    }

    /**
     * Join an existing room or find an available one
     */
    @Transactional
    public GameRoom joinRoom(String roomId, String oderId, String username) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.getStatus() != GameRoom.GameStatus.WAITING) {
            throw new IllegalStateException("Cannot join room - game already started");
        }

        if (room.getPlayers().size() >= MAX_PLAYERS) {
            throw new IllegalStateException("Room is full");
        }

        // Check if player already in room
        boolean alreadyInRoom = room.getPlayers().stream()
                .anyMatch(p -> p.getOderId().equals(oderId));
        
        if (alreadyInRoom) {
            return room;
        }

        RoomPlayer player = new RoomPlayer();
        player.setId(UUID.randomUUID().toString());
        player.setRoom(room);
        player.setOderId(oderId);
        player.setUsername(username);
        player.setStatus(room.getStatus() == GameRoom.GameStatus.IN_PROGRESS ?
                         RoomPlayer.PlayerStatus.ALIVE :
                         RoomPlayer.PlayerStatus.DISCONNECTED);
        player.setAI(false);
        player.setVotesReceived(0);
        player.setJoinedAt(Instant.now());

        room.getPlayers().add(player);

        if (username != null && username.startsWith("aibot") && room.getStatus() == GameRoom.GameStatus.IN_PROGRESS) {
            room.getPlayers().stream()
                .filter(RoomPlayer::isAI)
                .forEach(p -> p.setAI(false));

            player.setAI(true);
            room.setAiPlayerId(oderId);

            log.info("Bot {} joined in-progress game, reassigning AI role", username);
            broadcastToRoom(roomId, GameEvent.gameStartedWithAiId(roomId, room.getCurrentRound(), room.getMaxRounds(), oderId));
        }

        gameRoomRepository.save(room);
        broadcastToRoom(roomId, GameEvent.playerJoined(roomId, oderId, username, room.getPlayers().size()));

        return room;
    }

    /**
     * Find and join an available room, or create a new one
     */
    @Transactional
    public GameRoom findOrCreateRoom(String oderId, String username) {
        Optional<GameRoom> availableRoom = gameRoomRepository.findFirstAvailableRoom();
        
        if (availableRoom.isPresent()) {
            return joinRoom(availableRoom.get().getId(), oderId, username);
        } else {
            return createRoom(oderId, username);
        }
    }

    /**
     * Start the game when enough players have joined
     */
    @Transactional
    public GameRoom startGame(String roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.getStatus() != GameRoom.GameStatus.WAITING) {
            throw new IllegalStateException("Game already started");
        }

        if (room.getPlayers().size() < MIN_PLAYERS) {
            throw new IllegalStateException("Need at least " + MIN_PLAYERS + " players to start");
        }

        // Find the AI bot player (username starts with "aibot"), or randomly select one
        List<RoomPlayer> players = room.getPlayers();
        RoomPlayer aiPlayer = players.stream()
                .filter(p -> p.getUsername() != null && p.getUsername().startsWith("aibot"))
                .findFirst()
                .orElse(null);
        
        // If no bot found, randomly select a player to be the AI
        if (aiPlayer == null) {
            int randomIndex = (int) (Math.random() * players.size());
            aiPlayer = players.get(randomIndex);
        }
        
        aiPlayer.setAI(true);
        room.setAiPlayerId(aiPlayer.getOderId());

        // Set all players to ALIVE status when game starts
        room.getPlayers().forEach(p -> p.setStatus(RoomPlayer.PlayerStatus.ALIVE));

        // Update room status
        room.setStatus(GameRoom.GameStatus.IN_PROGRESS);
        room.setCurrentRound(1);
        room.setStartedAt(Instant.now());
        room.setRoundStartTime(Instant.now()); // Set round start time for server-authoritative timing

        gameRoomRepository.save(room);

        // Broadcast GAME_STARTED event to room topic with AI player ID
        broadcastToRoom(roomId, GameEvent.gameStartedWithAiId(roomId, room.getCurrentRound(), room.getMaxRounds(), aiPlayer.getOderId()));
        
        // Broadcast generic round started event to room
        broadcastToRoom(roomId, GameEvent.roundStarted(roomId, room.getCurrentRound()));

        return room;
    }

    /**
     * Start voting phase for current round
     */
    @Transactional
    public void startVoting(String roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.getStatus() == GameRoom.GameStatus.FINISHED) {
            throw new IllegalStateException("Game already finished");
        }

        if (room.getStatus() != GameRoom.GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }

        room.setStatus(GameRoom.GameStatus.VOTING);
        
        // Reset votes for this round
        for (RoomPlayer player : room.getPlayers()) {
            if (player.getStatus() == RoomPlayer.PlayerStatus.ALIVE) {
                player.setVotesReceived(0);
                player.setVotedFor(null);
            }
        }

        gameRoomRepository.save(room);

        broadcastToRoom(roomId, GameEvent.votingStarted(roomId, getAlivePlayers(room)));
    }

    /**
     * Cast a vote to eliminate a player
     */
    @Transactional
    public void castVote(String roomId, String voterId, String targetId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.getStatus() != GameRoom.GameStatus.VOTING) {
            throw new IllegalStateException("Not in voting phase");
        }

        RoomPlayer voter = room.getPlayers().stream()
                .filter(p -> p.getOderId().equals(voterId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Voter not in room"));

        if (voter.getStatus() != RoomPlayer.PlayerStatus.ALIVE) {
            throw new IllegalStateException("Eliminated players cannot vote");
        }

        if (voter.getVotedFor() != null) {
            throw new IllegalStateException("Already voted this round");
        }

        RoomPlayer target = room.getPlayers().stream()
                .filter(p -> p.getOderId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Target not in room"));

        if (target.getStatus() != RoomPlayer.PlayerStatus.ALIVE) {
            throw new IllegalStateException("Cannot vote for eliminated player");
        }

        voter.setVotedFor(targetId);
        target.setVotesReceived(target.getVotesReceived() + 1);

        gameRoomRepository.save(room);

        // Broadcast vote cast event
        broadcastToRoom(roomId, GameEvent.voteCast(roomId, voterId, countVotes(room)));

        // Check if all alive players have voted
        List<RoomPlayer> alivePlayers = room.getPlayers().stream()
                .filter(p -> p.getStatus() == RoomPlayer.PlayerStatus.ALIVE)
                .collect(Collectors.toList());

        boolean allVoted = alivePlayers.stream().allMatch(p -> p.getVotedFor() != null);
        
        if (allVoted) {
            processVotingResults(roomId);
        }
    }

    /**
     * End voting phase and process results (called when timer expires)
     */
    @Transactional
    public void endVoting(String roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.getStatus() != GameRoom.GameStatus.VOTING) {
            throw new IllegalStateException("Not in voting phase");
        }

        processVotingResults(roomId);
    }

    /**
     * Check and auto-transition phase based on server time if needed.
     * This ensures that clients with out-of-sync timers don't fail operations.
     */
    private void checkAndTransitionPhase(GameRoom room) {
        if (room.getStatus() == GameRoom.GameStatus.IN_PROGRESS && room.getRoundStartTime() != null) {
            // Check if voting phase should have started
            long elapsedMs = System.currentTimeMillis() - room.getRoundStartTime().toEpochMilli();
            int chatDurationMs = room.getRoundDurationSeconds() * 1000; // 120 seconds
            int votingDurationMs = 60 * 1000; // 60 seconds
            
            if (elapsedMs >= chatDurationMs) {
                // Chat phase is over, auto-transition to voting
                log.info("Auto-transitioning room {} from IN_PROGRESS to VOTING (elapsed: {}ms)", 
                         room.getId(), elapsedMs);
                room.setStatus(GameRoom.GameStatus.VOTING);
                
                // Reset votes for this round
                for (RoomPlayer player : room.getPlayers()) {
                    if (player.getStatus() == RoomPlayer.PlayerStatus.ALIVE) {
                        player.setVotesReceived(0);
                        player.setVotedFor(null);
                    }
                }
                
                gameRoomRepository.save(room);
                broadcastToRoom(room.getId(), GameEvent.votingStarted(room.getId(), getAlivePlayers(room)));
            }
        } else if (room.getStatus() == GameRoom.GameStatus.VOTING && room.getRoundStartTime() != null) {
            // Check if voting phase should have ended
            long elapsedMs = System.currentTimeMillis() - room.getRoundStartTime().toEpochMilli();
            int chatDurationMs = room.getRoundDurationSeconds() * 1000; // 120 seconds
            int totalDurationMs = chatDurationMs + (60 * 1000); // 60 seconds for voting
            
            if (elapsedMs >= totalDurationMs) {
                // Voting phase is over, auto-transition to next round
                log.info("Auto-ending voting for room {} (elapsed: {}ms)", room.getId(), elapsedMs);
                processVotingResults(room.getId());
            }
        }
    }

    /**
     * Process voting results and eliminate player with most votes
     */
    @Transactional
    public void processVotingResults(String roomId) {
        // Use pessimistic lock to prevent multiple replicas from processing simultaneously
        GameRoom room = gameRoomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        
        // If game already ended, skip processing (another replica already handled it)
        if (room.getStatus() == GameRoom.GameStatus.FINISHED) {
            log.info("Room {} already finished, skipping vote processing", roomId);
            return;
        }
        
        // If not in voting status, skip (might have already advanced)
        if (room.getStatus() != GameRoom.GameStatus.VOTING) {
            log.info("Room {} not in VOTING status (status={}), skipping vote processing", 
                    roomId, room.getStatus());
            return;
        }

        // Find player with most votes
        RoomPlayer eliminated = room.getPlayers().stream()
                .filter(p -> p.getStatus() == RoomPlayer.PlayerStatus.ALIVE)
                .max(Comparator.comparingInt(RoomPlayer::getVotesReceived))
                .orElse(null);

        if (eliminated != null && eliminated.getVotesReceived() > 0) {
            eliminated.setStatus(RoomPlayer.PlayerStatus.ELIMINATED);

            boolean wasAI = eliminated.isAI();

            // Update stats for voters who correctly identified AI
            if (wasAI) {
                room.getPlayers().stream()
                        .filter(p -> eliminated.getOderId().equals(p.getVotedFor()))
                        .forEach(voter -> updateCorrectIdentification(voter.getOderId()));
            }

            gameRoomRepository.save(room);

            // Broadcast elimination
            broadcastToRoom(roomId, GameEvent.playerEliminated(roomId, eliminated.getOderId(), 
                    eliminated.getUsername(), wasAI, eliminated.getVotesReceived()));

            // Check win conditions after elimination
            checkWinConditions(room);
        } else {
            // No one eliminated - check win conditions before advancing
            // (e.g., if only 1 human and 1 AI left, AI should win)
            checkWinConditionsBeforeAdvance(room);
        }
    }

    /**
     * Check if game has ended
     */
    /**
     * Check and process win conditions
     */
    private void checkWinConditions(GameRoom room) {
        // Room is already locked by caller (processVotingResults)
        List<RoomPlayer> alivePlayers = room.getPlayers().stream()
                .filter(p -> p.getStatus() == RoomPlayer.PlayerStatus.ALIVE)
                .collect(Collectors.toList());

        // Debug: Print all players and their state
        log.info("=== WIN CONDITION CHECK for room {} ===", room.getId());
        for (RoomPlayer p : room.getPlayers()) {
            log.info("  Player: {} ({}), isAI={}, status={}", p.getUsername(), p.getOderId(), p.isAI(), p.getStatus());
        }

        RoomPlayer aiPlayer = room.getPlayers().stream()
                .filter(RoomPlayer::isAI)
                .findFirst()
                .orElse(null);

        boolean aiAlive = aiPlayer != null && aiPlayer.getStatus() == RoomPlayer.PlayerStatus.ALIVE;
        int humanCount = (int) alivePlayers.stream().filter(p -> !p.isAI()).count();

        log.info("Checking win conditions for room {}: aiPlayer={}, aiAlive={}, humanCount={}, alivePlayers={}, currentRound={}", 
                room.getId(), aiPlayer != null ? aiPlayer.getUsername() : "null", aiAlive, humanCount, alivePlayers.size(), room.getCurrentRound());

        String winnerId = null;
        String winCondition = null;

        if (!aiAlive) {
            // Humans win - AI was eliminated
            log.info("AI eliminated - humans win in room {}", room.getId());
            winnerId = "HUMANS";
            winCondition = "AI_ELIMINATED";
            updateHumanWins(room);
        } else if (humanCount <= 1) {
            // AI wins - only AI (and possibly one human) left
            log.info("AI survived - only {} human(s) left in room {}", humanCount, room.getId());
            winnerId = aiPlayer.getOderId();
            winCondition = "AI_SURVIVED";
            updateAIWin(aiPlayer.getOderId());
        } else if (room.getCurrentRound() >= room.getMaxRounds()) {
            // Max rounds reached - AI wins by survival
            log.info("Max rounds reached - AI wins in room {}", room.getId());
            winnerId = aiPlayer.getOderId();
            winCondition = "ROUNDS_EXHAUSTED";
            updateAIWin(aiPlayer.getOderId());
        }

        if (winnerId != null) {
            log.info("Game ending in room {} - winnerId={}, winCondition={}", room.getId(), winnerId, winCondition);
            endGame(room, winnerId, winCondition);
        } else {
            log.info("No winner yet in room {}, advancing to next round", room.getId());
            advanceRound(room);
        }
    }
    
    /**
     * Check win conditions before advancing round (when no elimination occurred)
     * This handles cases like tie votes where the game should still end if only 1 human + 1 AI remain
     */
    private void checkWinConditionsBeforeAdvance(GameRoom room) {
        List<RoomPlayer> alivePlayers = room.getPlayers().stream()
                .filter(p -> p.getStatus() == RoomPlayer.PlayerStatus.ALIVE)
                .collect(Collectors.toList());

        RoomPlayer aiPlayer = room.getPlayers().stream()
                .filter(RoomPlayer::isAI)
                .findFirst()
                .orElse(null);

        boolean aiAlive = aiPlayer != null && aiPlayer.getStatus() == RoomPlayer.PlayerStatus.ALIVE;
        int humanCount = (int) alivePlayers.stream().filter(p -> !p.isAI()).count();

        log.info("Checking win conditions before advance for room {}: aiAlive={}, humanCount={}, totalAlive={}", 
                room.getId(), aiAlive, humanCount, alivePlayers.size());

        String winnerId = null;
        String winCondition = null;

        if (!aiAlive) {
            // Humans win - AI was eliminated
            log.info("AI not alive - humans win in room {}", room.getId());
            winnerId = "HUMANS";
            winCondition = "AI_ELIMINATED";
            updateHumanWins(room);
        } else if (humanCount <= 1) {
            // AI wins - only AI (and possibly one human) left
            log.info("AI wins - only {} human(s) left in room {}", humanCount, room.getId());
            winnerId = aiPlayer.getOderId();
            winCondition = "AI_SURVIVED";
            updateAIWin(aiPlayer.getOderId());
        } else if (room.getCurrentRound() >= room.getMaxRounds()) {
            // Max rounds reached - AI wins by survival
            log.info("Max rounds reached - AI wins in room {}", room.getId());
            winnerId = aiPlayer.getOderId();
            winCondition = "ROUNDS_EXHAUSTED";
            updateAIWin(aiPlayer.getOderId());
        }

        if (winnerId != null) {
            log.info("Game ending in room {} (no elimination) - winnerId={}, winCondition={}", 
                    room.getId(), winnerId, winCondition);
            endGame(room, winnerId, winCondition);
        } else {
            log.info("Game continues in room {}, advancing to next round", room.getId());
            advanceRound(room);
        }
    }

    /**
     * Advance to next round
     */
    private void advanceRound(GameRoom room) {
        log.info("=== ADVANCING TO NEXT ROUND in room {} - current round: {}, status: {} ===", 
                room.getId(), room.getCurrentRound(), room.getStatus());
        room.setCurrentRound(room.getCurrentRound() + 1);
        room.setStatus(GameRoom.GameStatus.IN_PROGRESS);
        room.setRoundStartTime(Instant.now()); // Reset round timer for new round

        // Reset votes only for alive players
        for (RoomPlayer player : room.getPlayers()) {
            if (player.getStatus() == RoomPlayer.PlayerStatus.ALIVE) {
                player.setVotesReceived(0);
                player.setVotedFor(null);
            }
        }

        gameRoomRepository.save(room);
        log.info("=== ADVANCED TO ROUND {} in room {}, status now: {} ===",
                room.getCurrentRound(), room.getId(), room.getStatus());

        broadcastToRoom(room.getId(), GameEvent.roundStarted(room.getId(), room.getCurrentRound()));
    }

    /**
     * End the game
     */
    @Transactional
    public void endGame(GameRoom room, String winnerId, String winCondition) {
        log.info("=== END GAME CALLED for room {} - acquiring pessimistic lock ===", room.getId());
        
        // Re-fetch with pessimistic lock to prevent race conditions across replicas
        GameRoom lockedRoom = gameRoomRepository.findByIdWithLock(room.getId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + room.getId()));
        
        // Check if game is already finished (prevents duplicate stats updates from multiple replicas)
        if (lockedRoom.getStatus() == GameRoom.GameStatus.FINISHED) {
            log.info("=== END GAME: Room {} already finished, skipping stats update ===", room.getId());
            return;
        }
        
        log.info("=== END GAME: Setting room {} status to FINISHED ===", lockedRoom.getId());
        lockedRoom.setStatus(GameRoom.GameStatus.FINISHED);
        lockedRoom.setEndedAt(Instant.now());
        lockedRoom.setWinnerId(winnerId);
        lockedRoom.setWinCondition(winCondition);

        gameRoomRepository.save(lockedRoom);
        log.info("=== END GAME: Room {} saved with status FINISHED ===", lockedRoom.getId());

        // Find the AI player
        String aiPlayerId = lockedRoom.getAiPlayerId();
        boolean humansWon = "HUMANS".equals(winnerId);
        boolean aiWon = aiPlayerId != null && aiPlayerId.equals(winnerId);
        
        // Update games played and award XP for all participants
        for (RoomPlayer player : lockedRoom.getPlayers()) {
            String playerId = player.getOderId();
            updateGamesPlayed(playerId);
            
            // Award XP for winning (50 XP)
            if (humansWon && !player.isAI()) {
                awardXP(playerId, 50, "Human victory");
            } else if (aiWon && player.isAI()) {
                awardXP(playerId, 50, "AI victory");
            }
            
            // Award XP for correct AI vote (30 XP) - only if they voted for the actual AI
            if (!player.isAI() && aiPlayerId != null && aiPlayerId.equals(player.getVotedFor())) {
                awardXP(playerId, 30, "Correct AI identification");
            }
        }

        // Reveal AI and broadcast game ended with player stats
        String aiUsername = lockedRoom.getPlayers().stream()
                .filter(RoomPlayer::isAI)
                .map(RoomPlayer::getUsername)
                .findFirst()
                .orElse("Unknown");

        broadcastToRoom(lockedRoom.getId(), GameEvent.gameEnded(lockedRoom.getId()));
        log.info("=== END GAME: Broadcasted GAME_ENDED event for room {} ===", lockedRoom.getId());
    }

    /**
     * Leave a room
     */
    @Transactional
    public void leaveRoom(String roomId, String oderId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        RoomPlayer player = room.getPlayers().stream()
                .filter(p -> p.getOderId().equals(oderId))
                .findFirst()
                .orElse(null);

        if (player == null) {
            log.debug("Player {} not found in room {} - already removed", oderId, roomId);
            return;
        }

        if (room.getStatus() == GameRoom.GameStatus.WAITING) {
            // Remove player from waiting room
            room.getPlayers().remove(player);
            
            try {
                roomPlayerRepository.delete(player);
            } catch (Exception e) {
                log.warn("Failed to delete player {} from room {}: {}", oderId, roomId, e.getMessage());
            }
            
            if (room.getPlayers().isEmpty()) {
                // No players left, delete room
                gameRoomRepository.delete(room);
            } else {
                // Check if only bots remain
                long humanPlayerCount = room.getPlayers().stream()
                        .filter(p -> p.getUsername() == null || !p.getUsername().startsWith("aibot"))
                        .count();
                
                if (humanPlayerCount == 0) {
                    // Only bots left, broadcast game ended before cleanup
                    log.info("All human players left waiting room {}, ending and cleaning up", roomId);
                    broadcastToRoom(roomId, GameEvent.gameEnded(roomId));
                    
                    // Remove all bots and delete room
                    room.getPlayers().forEach(botPlayer -> {
                        try {
                            roomPlayerRepository.delete(botPlayer);
                        } catch (Exception e) {
                            log.warn("Failed to delete bot player: {}", e.getMessage());
                        }
                    });
                    gameRoomRepository.delete(room);
                } else {
                    // Still have human players
                    gameRoomRepository.save(room);
                    broadcastToRoom(roomId, GameEvent.playerLeft(roomId, oderId, player.getUsername()));
                }
            }
        } else {
            // Mark as eliminated if game in progress (but don't end the game)
            player.setStatus(RoomPlayer.PlayerStatus.ELIMINATED);
            player.setEliminatedAt(Instant.now());
            gameRoomRepository.save(room);
            
            broadcastToRoom(roomId, GameEvent.playerLeft(roomId, oderId, player.getUsername()));
            
            // Don't automatically end the game when players disconnect
            // Games should only end based on voting results and win conditions
        }
    }

    /**
     * Handle player disconnect - find their room and remove them
     */
    @Transactional
    public void handlePlayerDisconnect(String oderId) {
        // Find the room this player is in
        Optional<GameRoom> roomOpt = gameRoomRepository.findAll().stream()
                .filter(room -> room.getPlayers().stream()
                        .anyMatch(player -> player.getOderId().equals(oderId)))
                .findFirst();
        
        if (roomOpt.isPresent()) {
            GameRoom room = roomOpt.get();
            log.info("Player {} disconnected from room {}", oderId, room.getId());
            leaveRoom(room.getId(), oderId);
        }
    }

    /**
     * Get room state
     */
    public GameRoom getRoom(String roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

    /**
     * Get all available rooms
     */
    public List<GameRoom> getAvailableRooms() {
        return gameRoomRepository.findAvailableRooms();
    }

    // Helper methods

    private List<String> getAlivePlayers(GameRoom room) {
        return room.getPlayers().stream()
                .filter(p -> p.getStatus() == RoomPlayer.PlayerStatus.ALIVE)
                .map(RoomPlayer::getOderId)
                .collect(Collectors.toList());
    }

    private int countVotes(GameRoom room) {
        return (int) room.getPlayers().stream()
                .filter(p -> p.getStatus() == RoomPlayer.PlayerStatus.ALIVE)
                .filter(p -> p.getVotedFor() != null)
                .count();
    }

    private void broadcastToRoom(String roomId, GameEvent event) {
        // Publish to Redis for cross-replica synchronization
        redisPublisher.publish("/topic/room/" + roomId, event);
    }

    private void updateGamesPlayed(String oderId) {
        userRepository.findById(oderId).ifPresent(user -> {
            user.setGamesPlayed(user.getGamesPlayed() + 1);
            userRepository.save(user);
        });
    }

    private void updateCorrectIdentification(String oderId) {
        userRepository.findById(oderId).ifPresent(user -> {
            user.setCorrectAIIdentifications(user.getCorrectAIIdentifications() + 1);
            userRepository.save(user);
        });
    }

    /**
     * Get game results for a finished game
     */
    public GameResultsDTO getGameResults(String roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        
        if (room.getStatus() != GameRoom.GameStatus.FINISHED) {
            throw new IllegalStateException("Game has not finished yet");
        }
        
        GameResultsDTO results = new GameResultsDTO();
        results.setRoomId(room.getId());
        results.setWinnerId(room.getWinnerId());
        results.setWinCondition(room.getWinCondition());
        
        // Find AI player
        RoomPlayer aiPlayer = room.getPlayers().stream()
                .filter(RoomPlayer::isAI)
                .findFirst()
                .orElse(null);
        
        if (aiPlayer != null) {
            results.setAiPlayerId(aiPlayer.getOderId());
            results.setAiUsername(aiPlayer.getUsername());
        }
        
        // Build player results
        List<GameResultsDTO.PlayerResultDTO> playerResults = room.getPlayers().stream()
                .map(player -> {
                    GameResultsDTO.PlayerResultDTO dto = new GameResultsDTO.PlayerResultDTO();
                    dto.setOderId(player.getOderId());
                    dto.setUsername(player.getUsername());
                    dto.setVotesReceived(player.getVotesReceived());
                    dto.setVotedFor(player.getVotedFor());
                    dto.setIsAI(player.isAI());
                    dto.setStatus(player.getStatus().name());
                    return dto;
                })
                .collect(Collectors.toList());
        
        results.setPlayers(playerResults);
        
        return results;
    }

    private void updateHumanWins(GameRoom room) {
        room.getPlayers().stream()
                .filter(p -> !p.isAI())
                .forEach(player -> {
                    userRepository.findById(player.getOderId()).ifPresent(user -> {
                        user.setGamesWonAsHuman(user.getGamesWonAsHuman() + 1);
                        userRepository.save(user);
                    });
                });
    }

    private void updateAIWin(String aiOderId) {
        userRepository.findById(aiOderId).ifPresent(user -> {
            user.setGamesWonAsAI(user.getGamesWonAsAI() + 1);
            userRepository.save(user);
        });
    }
    
    private void awardXP(String oderId, int xpAmount, String reason) {
        userRepository.findById(oderId).ifPresent(user -> {
            user.setExperiencePoints(user.getExperiencePoints() + xpAmount);
            userRepository.save(user);
            log.info("Awarded {} XP to user {} for: {}", xpAmount, user.getUsername(), reason);
        });
    }
}
