package imitationgame.chatservice.service;

import imitationgame.chatservice.dto.GameEvent;
import imitationgame.chatservice.model.GameRoom;
import imitationgame.chatservice.model.RoomPlayer;
import imitationgame.chatservice.model.UserProfile;
import imitationgame.chatservice.repository.GameRoomRepository;
import imitationgame.chatservice.repository.RoomPlayerRepository;
import imitationgame.chatservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
        player.setStatus(RoomPlayer.PlayerStatus.ALIVE);
        player.setAI(false);
        player.setVotesReceived(0);
        player.setJoinedAt(Instant.now());

        room.getPlayers().add(player);
        gameRoomRepository.save(room);

        // Broadcast player joined event
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

        // Randomly assign one player as the AI
        List<RoomPlayer> players = room.getPlayers();
        int aiIndex = new Random().nextInt(players.size());
        RoomPlayer aiPlayer = players.get(aiIndex);
        aiPlayer.setAI(true);
        room.setAiPlayerId(aiPlayer.getOderId());

        // Update room status
        room.setStatus(GameRoom.GameStatus.IN_PROGRESS);
        room.setCurrentRound(1);
        room.setStartedAt(Instant.now());

        gameRoomRepository.save(room);

        // Broadcast GAME_STARTED event to room topic with AI player ID
        // Each client will check if their oderId matches aiPlayerId to determine if they are AI
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
     * Process voting results and eliminate player with most votes
     */
    @Transactional
    public void processVotingResults(String roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

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

            // Check win conditions
            checkWinConditions(room);
        } else {
            // No one eliminated, continue to next round
            advanceRound(room);
        }
    }

    /**
     * Check if game has ended
     */
    private void checkWinConditions(GameRoom room) {
        List<RoomPlayer> alivePlayers = room.getPlayers().stream()
                .filter(p -> p.getStatus() == RoomPlayer.PlayerStatus.ALIVE)
                .collect(Collectors.toList());

        RoomPlayer aiPlayer = room.getPlayers().stream()
                .filter(RoomPlayer::isAI)
                .findFirst()
                .orElse(null);

        boolean aiAlive = aiPlayer != null && aiPlayer.getStatus() == RoomPlayer.PlayerStatus.ALIVE;
        int humanCount = (int) alivePlayers.stream().filter(p -> !p.isAI()).count();

        String winnerId = null;
        String winCondition = null;

        if (!aiAlive) {
            // Humans win - AI was eliminated
            winnerId = "HUMANS";
            winCondition = "AI_ELIMINATED";
            updateHumanWins(room);
        } else if (humanCount <= 1) {
            // AI wins - only AI (and possibly one human) left
            winnerId = aiPlayer.getOderId();
            winCondition = "AI_SURVIVED";
            updateAIWin(aiPlayer.getOderId());
        } else if (room.getCurrentRound() >= room.getMaxRounds()) {
            // Max rounds reached - AI wins by survival
            winnerId = aiPlayer.getOderId();
            winCondition = "ROUNDS_EXHAUSTED";
            updateAIWin(aiPlayer.getOderId());
        }

        if (winnerId != null) {
            endGame(room, winnerId, winCondition);
        } else {
            advanceRound(room);
        }
    }

    /**
     * Advance to next round
     */
    private void advanceRound(GameRoom room) {
        room.setCurrentRound(room.getCurrentRound() + 1);
        room.setStatus(GameRoom.GameStatus.IN_PROGRESS);

        // Reset votes
        for (RoomPlayer player : room.getPlayers()) {
            player.setVotesReceived(0);
            player.setVotedFor(null);
        }

        gameRoomRepository.save(room);

        broadcastToRoom(room.getId(), GameEvent.roundStarted(room.getId(), room.getCurrentRound()));
    }

    /**
     * End the game
     */
    private void endGame(GameRoom room, String winnerId, String winCondition) {
        room.setStatus(GameRoom.GameStatus.FINISHED);
        room.setEndedAt(Instant.now());

        gameRoomRepository.save(room);

        // Update games played for all participants
        for (RoomPlayer player : room.getPlayers()) {
            updateGamesPlayed(player.getOderId());
        }

        // Reveal AI and broadcast game ended
        String aiUsername = room.getPlayers().stream()
                .filter(RoomPlayer::isAI)
                .map(RoomPlayer::getUsername)
                .findFirst()
                .orElse("Unknown");

        broadcastToRoom(room.getId(), GameEvent.gameEnded(room.getId(), winnerId, winCondition, 
                room.getAiPlayerId(), aiUsername));
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
            return;
        }

        if (room.getStatus() == GameRoom.GameStatus.WAITING) {
            // Remove player from waiting room
            room.getPlayers().remove(player);
            roomPlayerRepository.delete(player);
            
            if (room.getPlayers().isEmpty()) {
                gameRoomRepository.delete(room);
            } else {
                gameRoomRepository.save(room);
                broadcastToRoom(roomId, GameEvent.playerLeft(roomId, oderId, player.getUsername()));
            }
        } else {
            // Mark as eliminated if game in progress
            player.setStatus(RoomPlayer.PlayerStatus.ELIMINATED);
            gameRoomRepository.save(room);
            
            broadcastToRoom(roomId, GameEvent.playerLeft(roomId, oderId, player.getUsername()));
            
            // Check win conditions
            checkWinConditions(room);
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
        messagingTemplate.convertAndSend("/topic/room/" + roomId, event);
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
}
