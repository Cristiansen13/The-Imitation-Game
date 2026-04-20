import { useEffect, useState, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Users, Clock, Sparkles, RefreshCw, AlertCircle, LogOut } from 'lucide-react';
import { roomApi, wsService, GameRoom } from '../services';
import { useAuth } from '../contexts/AuthContext';

interface Player {
  id: string;
  username: string;
  avatar: string;
  status: 'connected' | 'connecting' | 'alive' | 'eliminated';
}

interface LobbyProps {
  initialRoomId?: string | null;
  onRoomJoined?: (roomId: string) => void;
  onGameStart: (roomId: string, isAI: boolean) => void;
  onLeave?: () => void;
}

export function Lobby({ initialRoomId = null, onRoomJoined, onGameStart, onLeave }: LobbyProps) {
  const { user } = useAuth();
  const [players, setPlayers] = useState<Player[]>([]);
  const [timeLeft, setTimeLeft] = useState(30);
  const [assignedRole, setAssignedRole] = useState<'Player' | 'AI' | null>(null);
  const [roomId, setRoomId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [canStartGame, setCanStartGame] = useState(false);
  const [timerStartedAt, setTimerStartedAt] = useState<Date | null>(null);
  const [isStartingGame, setIsStartingGame] = useState(false);
  const [isRoomLeader, setIsRoomLeader] = useState(false);
  const hasInitialized = useRef(false);
  const LOBBY_DURATION = 30; // seconds - countdown duration once 3+ players

  const getLobbyTimerKey = useCallback((id: string) => `imitation_game_lobby_timer_${id}`, []);

  // Debug: Log version to confirm new code is loaded
  useEffect(() => {
    console.log('[Lobby] Version 2.0 - Room leader only starts game');
  }, []);

  // Calculate time left based on when timer started (when 3+ players joined)
  const calculateTimeLeft = useCallback((startedAt: Date): number => {
    const now = new Date();
    const elapsed = Math.floor((now.getTime() - startedAt.getTime()) / 1000);
    const remaining = LOBBY_DURATION - elapsed;
    return Math.max(0, remaining);
  }, []);

  const createOrJoinRoom = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      console.log('[Lobby] Step 1: Connecting to WebSocket...');
      // Connect to WebSocket FIRST
      if (!wsService.isConnected()) {
        await wsService.connect();
      }
      
      console.log('[Lobby] Step 2: Fetching available rooms to pre-subscribe...');
      // Get list of available rooms to pre-subscribe
      const availableRooms = await roomApi.listAvailable();
      console.log('[Lobby] Available rooms:', availableRooms);
      
      // Track if we've already started the game transition
      let gameStarted = false;
      
      // Pre-subscribe to potential room topics (subscribe to all available rooms)
      const subscriptions: Array<() => void> = [];
      availableRooms.forEach((room: GameRoom) => {
        const unsub = wsService.subscribeToGameEvents(room.id, (event) => {
          console.log('[Lobby] Game event received:', event);
          switch (event.type) {
            case 'PLAYER_JOINED':
              setPlayers(prev => {
                // Don't add if already exists (check by id)
                if (prev.some(p => p.id === event.data.oderId)) {
                  console.log('[Lobby] Player already in list:', event.data.username);
                  return prev;
                }
                console.log('[Lobby] Adding player:', event.data.username);
                const newPlayers = [...prev, {
                  id: event.data.oderId,
                  username: event.data.username,
                  avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${event.data.username}`,
                  status: 'connected',
                }];
                // If this is the first player being added and it's me, I'm the leader
                if (prev.length === 0 && event.data.oderId === user?.id) {
                  console.log('[Lobby] I am the room leader (first player)');
                  setIsRoomLeader(true);
                }
                return newPlayers;
              });
              break;
            case 'PLAYER_LEFT':
              console.log('[Lobby] Player left:', event.data.username);
              setPlayers(prev => prev.filter(p => p.id !== event.data.oderId));
              break;
            case 'GAME_STARTED':
              if (roomId) {
                localStorage.removeItem(getLobbyTimerKey(roomId));
              }
              if (gameStarted) {
                // Game already started, but this might be an update with the correct aiPlayerId
                const aiPlayerId = event.data.aiPlayerId;
                if (aiPlayerId) {
                  console.log('GAME_STARTED update - aiPlayerId:', aiPlayerId, 'myId:', user?.oderId);
                  // Update role if needed
                  const isAI = aiPlayerId === user?.oderId;
                  if (isAI !== (assignedRole === 'AI')) {
                    console.log('Updating role based on late bot join');
                    setAssignedRole(isAI ? 'AI' : 'Player');
                  }
                }
                return;
              }
              gameStarted = true;
              
              // Check if current user is the AI by comparing aiPlayerId
              const aiPlayerId = event.data.aiPlayerId;
              const isAI = aiPlayerId === user?.id;
              console.log('GAME_STARTED - aiPlayerId:', aiPlayerId, 'myId:', user?.id, 'isAI:', isAI);
              
              setAssignedRole(isAI ? 'AI' : 'Player');
              setTimeout(() => {
                onGameStart(room.id, isAI);
              }, 2000);
              break;
          }
        });
        subscriptions.push(unsub);
      });
      
      console.log('[Lobby] Step 3: Joining room...');
      // On refresh, try to reattach to the previously persisted room first.
      let room = null;
      if (initialRoomId) {
        try {
          room = await roomApi.join(initialRoomId);
          console.log('[Lobby] Rejoined persisted room:', initialRoomId);
        } catch (err) {
          console.warn('[Lobby] Failed to rejoin persisted room, falling back to joinAny:', err);
        }
      }

      // Fallback for first-time entry or when old room is no longer joinable.
      if (!room) {
        room = await roomApi.joinAny();
      }
      console.log('[Lobby] Joined room:', room.id);
      
      setRoomId(room.id);
      onRoomJoined?.(room.id);
      
      // Subscribe to the specific room we joined (if it wasn't in the list)
      const alreadySubscribed = availableRooms.some((r: GameRoom) => r.id === room.id);
      if (!alreadySubscribed) {
        console.log('[Lobby] Subscribing to new room:', room.id);
        wsService.subscribeToGameEvents(room.id, (event) => {
          console.log('[Lobby] Game event received:', event);
          switch (event.type) {
            case 'PLAYER_JOINED':
              setPlayers(prev => {
                if (prev.some(p => p.id === event.data.oderId)) {
                  console.log('[Lobby] Player already in list:', event.data.username);
                  return prev;
                }
                console.log('[Lobby] Adding player:', event.data.username);
                return [...prev, {
                  id: event.data.oderId,
                  username: event.data.username,
                  avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${event.data.username}`,
                  status: 'connected',
                }];
              });
              break;
            case 'PLAYER_LEFT':
              console.log('[Lobby] Player left:', event.data.username);
              setPlayers(prev => prev.filter(p => p.id !== event.data.oderId));
              break;
            case 'GAME_STARTED':
              if (gameStarted) {
                // Game already started, but this might be an update with the correct aiPlayerId
                const aiPlayerId = event.data.aiPlayerId;
                if (aiPlayerId) {
                  console.log('GAME_STARTED update - aiPlayerId:', aiPlayerId, 'myId:', user?.id);
                }
                return;
              }
              gameStarted = true;
              
              const aiPlayerId = event.data.aiPlayerId;
              const isAI = aiPlayerId === user?.id;
              console.log('GAME_STARTED - aiPlayerId:', aiPlayerId, 'myId:', user?.id, 'isAI:', isAI);
              
              setAssignedRole(isAI ? 'AI' : 'Player');
              setTimeout(() => {
                onGameStart(room.id, isAI);
              }, 2000);
              break;
          }
        });
      }
      
      console.log('[Lobby] Step 4: Setting initial players...');
      // Set initial players from room data
      if (room.players && room.players.length > 0) {
        const initialPlayers = room.players.map((p: { oderId: string; username: string; status: string }) => ({
          id: p.oderId,
          username: p.username,
          avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${p.username}`,
          // Map backend status to frontend status: DISCONNECTED (waiting in lobby) -> connected
          status: (p.status.toLowerCase() === 'disconnected' ? 'connected' : p.status.toLowerCase()) as Player['status'],
        }));
        console.log('[Lobby] Initial players:', initialPlayers);
        setPlayers(initialPlayers);
        
        // Check if game already started (players have 'alive' status)
        const gameAlreadyStarted = initialPlayers.some(p => p.status === 'alive');
        if (gameAlreadyStarted && room.aiPlayerId) {
          console.log('[Lobby] Game already started on join, transitioning immediately. AI:', room.aiPlayerId);
          const isAI = room.aiPlayerId === user?.id;
          setAssignedRole(isAI ? 'AI' : 'Player');
          setTimeout(() => {
            onGameStart(room.id, isAI);
          }, 500);
          return; // Skip the rest of initialization
        }
        
        // Check if current user is the room leader (first player to join)
        const firstPlayer = initialPlayers[0];
        const amLeader = firstPlayer?.id === user?.id;
        console.log('[Lobby] Am I room leader?', amLeader, 'First player:', firstPlayer?.username);
        setIsRoomLeader(amLeader);
      } else {
        // Add self if no players list
        setPlayers([{
          id: user?.id || 'self',
          username: user?.username || 'You',
          avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${user?.username || 'You'}`,
          status: 'connected',
        }]);
        setIsRoomLeader(true); // First player is the leader
      }
      
      setIsLoading(false);
      
    } catch (err) {
      console.error('Failed to create/join room:', err);
      setError('Failed to connect to game server. Please try again.');
      setIsLoading(false);
    }
  }, [getLobbyTimerKey, initialRoomId, onGameStart, onRoomJoined, roomId, user]);

  // Check if game can start (minimum 3 players) and manage timer
  useEffect(() => {
    const hasEnoughPlayers = players.length >= 3;
    setCanStartGame(hasEnoughPlayers);
    
    if (hasEnoughPlayers && !timerStartedAt) {
      // Restore existing timer on refresh, otherwise start a new countdown.
      if (roomId) {
        const raw = localStorage.getItem(getLobbyTimerKey(roomId));
        if (raw) {
          const restoredAt = new Date(raw);
          const remaining = calculateTimeLeft(restoredAt);
          if (!Number.isNaN(restoredAt.getTime()) && remaining > 0) {
            setTimerStartedAt(restoredAt);
            setTimeLeft(remaining);
            return;
          }
        }
      }

      const startedAt = new Date();
      setTimerStartedAt(startedAt);
      setTimeLeft(LOBBY_DURATION);
      if (roomId) {
        localStorage.setItem(getLobbyTimerKey(roomId), startedAt.toISOString());
      }
    } else if (!hasEnoughPlayers && timerStartedAt) {
      // Reset timer if players drop below 3
      setTimerStartedAt(null);
      setTimeLeft(LOBBY_DURATION);
      if (roomId) {
        localStorage.removeItem(getLobbyTimerKey(roomId));
      }
    }
  }, [players, timerStartedAt, roomId, getLobbyTimerKey, calculateTimeLeft]);

  const handleStartGame = useCallback(async () => {
    if (!roomId || !canStartGame || isStartingGame || assignedRole) {
      console.log('[Lobby] handleStartGame blocked:', { roomId, canStartGame, isStartingGame, assignedRole });
      return;
    }
    
    // Double-check: if any player has status 'alive', game already started
    const gameAlreadyStarted = players.some(p => p.status === 'alive');
    if (gameAlreadyStarted) {
      console.log('[Lobby] Game already started (detected from player status), skipping start request');
      return;
    }
    
    console.log('[Lobby] Room leader starting game for room:', roomId);
    setIsStartingGame(true);
    try {
      const result = await roomApi.startGame(roomId);
      console.log('[Lobby] Start game API success:', result);
      // Success - the GAME_STARTED event will come via WebSocket
    } catch (err: any) {
      // If we get a 400 error, it likely means the game already started
      // (another client beat us to it). Just wait for the WebSocket event.
      console.log('[Lobby] Start game request failed (game may have already started):', err?.message);
      // Don't show error - the GAME_STARTED event should arrive via WebSocket
    }
    // Don't reset isStartingGame - we're waiting for WebSocket event
  }, [roomId, canStartGame, isStartingGame, assignedRole, players]);

  const handleLeaveRoom = useCallback(async () => {
    if (roomId) {
      try {
        await roomApi.leave(roomId);
      } catch (err) {
        console.error('Failed to leave room:', err);
      }
      localStorage.removeItem(getLobbyTimerKey(roomId));
    }
    wsService.disconnect();
    if (onLeave) {
      onLeave();
    }
  }, [roomId, onLeave, getLobbyTimerKey]);

  // Initialize once on mount only
  useEffect(() => {
    if (hasInitialized.current) {
      console.log('[Lobby] Already initialized, skipping');
      return;
    }
    
    hasInitialized.current = true;
    console.log('[Lobby] First initialization');
    createOrJoinRoom();
  }, []); // Empty deps - run only once
  
  // Cleanup on unmount only
  useEffect(() => {
    return () => {
      console.log('[Lobby] Component unmounting, disconnecting WebSocket');
      wsService.disconnect();
    };
  }, []);

  // Timer effect - only runs when 3+ players (timerStartedAt is set)
  useEffect(() => {
    if (!timerStartedAt || isLoading || assignedRole || isStartingGame) return;
    
    const timer = setInterval(() => {
      const remaining = calculateTimeLeft(timerStartedAt);
      setTimeLeft(remaining);
      
      if (remaining === 0 && canStartGame && roomId && !isStartingGame && isRoomLeader) {
        clearInterval(timer);
        // Only the room leader (first player) starts the game
        // Everyone else just waits for the GAME_STARTED WebSocket event
        console.log('[Lobby] Timer reached 0, room leader starting game');
        handleStartGame();
      }
    }, 1000);
    
    return () => clearInterval(timer);
  }, [timerStartedAt, isLoading, canStartGame, roomId, assignedRole, isStartingGame, isRoomLeader, calculateTimeLeft, handleStartGame]);

  const handleRetry = () => {
    setError(null);
    setPlayers([]);
    setTimeLeft(LOBBY_DURATION);
    setTimerStartedAt(null);
    createOrJoinRoom();
  };

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="bg-slate-900/50 backdrop-blur-xl border border-red-500/50 rounded-2xl p-8 shadow-2xl max-w-md w-full text-center"
        >
          <AlertCircle className="w-12 h-12 text-red-400 mx-auto mb-4" />
          <h2 className="text-white text-xl mb-2">Connection Error</h2>
          <p className="text-slate-400 mb-6">{error}</p>
          <button
            onClick={handleRetry}
            className="px-6 py-3 bg-cyan-500 hover:bg-cyan-400 text-white rounded-lg transition-colors flex items-center gap-2 mx-auto"
          >
            <RefreshCw className="w-5 h-5" />
            Try Again
          </button>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-6">
      <div className="w-full max-w-4xl space-y-6">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center space-y-2"
        >
          <h1 className="text-white">Game Lobby</h1>
          <p className="text-slate-400">
            {roomId ? `Room: ${roomId.substring(0, 8)}...` : 'Connecting...'}
          </p>
        </motion.div>

        {/* Main lobby card */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-8 shadow-2xl"
        >
          {/* Timer */}
          <div className="flex items-center justify-center gap-8 mb-8 pb-8 border-b border-slate-800">
            <div className="text-center">
              <div className="flex items-center gap-2 text-slate-400 mb-2">
                <Users className="w-5 h-5" />
                <span>Players</span>
              </div>
              <p className="text-white">{players.length} / 7</p>
            </div>
            
            <div className="w-px h-12 bg-slate-800" />
            
            <div className="text-center">
              <div className="flex items-center gap-2 text-slate-400 mb-2">
                <Clock className="w-5 h-5" />
                <span>{canStartGame ? 'Starting in' : 'Waiting for players'}</span>
              </div>
              {canStartGame ? (
                <motion.p
                  key={timeLeft}
                  initial={{ scale: 1.2, color: '#06b6d4' }}
                  animate={{ scale: 1, color: '#ffffff' }}
                  className="text-white"
                >
                  {timeLeft}s
                </motion.p>
              ) : (
                <p className="text-slate-500">--</p>
              )}
            </div>
          </div>

          {/* Leave Room Button */}
          <div className="flex justify-end mb-4">
            <button
              onClick={handleLeaveRoom}
              className="px-4 py-2 bg-red-500/20 hover:bg-red-500/30 text-red-400 hover:text-red-300 rounded-lg transition-colors flex items-center gap-2 border border-red-500/30"
            >
              <LogOut className="w-4 h-4" />
              Leave Room
            </button>
          </div>

          {/* Players grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            <AnimatePresence>
              {players.map((player, idx) => (
                <motion.div
                  key={player.id}
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: idx * 0.1 }}
                  className={`p-4 rounded-xl border ${
                    player.status === 'connected'
                      ? 'bg-slate-800/50 border-cyan-500/30'
                      : 'bg-slate-800/30 border-slate-700'
                  } transition-all`}
                >
                  <div className="space-y-3">
                    <div className="relative mx-auto w-16 h-16">
                      <img
                        src={player.avatar}
                        alt={player.username}
                        className="w-full h-full rounded-full border-2 border-cyan-500/50"
                      />
                      <div className={`absolute -bottom-1 -right-1 w-4 h-4 rounded-full border-2 border-slate-900 ${
                        player.status === 'connected' ? 'bg-cyan-400' : 'bg-yellow-400'
                      }`} />
                    </div>
                    <div className="text-center">
                      <p className="text-white truncate">{player.username}</p>
                      <p className="text-slate-500">
                        {player.status === 'connected' ? 'Connected' : 'Connecting...'}
                      </p>
                    </div>
                  </div>
                </motion.div>
              ))}
              
              {/* Empty slots */}
              {[...Array(7 - players.length)].map((_, idx) => (
                <motion.div
                  key={`empty-${idx}`}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="p-4 rounded-xl border border-dashed border-slate-700 bg-slate-800/20"
                >
                  <div className="space-y-3">
                    <div className="mx-auto w-16 h-16 rounded-full bg-slate-800 border-2 border-slate-700 flex items-center justify-center">
                      <Users className="w-6 h-6 text-slate-600" />
                    </div>
                    <div className="text-center">
                      <p className="text-slate-600">Waiting...</p>
                    </div>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>

          {/* Role assignment */}
          <AnimatePresence>
            {assignedRole && (
              <motion.div
                initial={{ opacity: 0, y: 20, scale: 0.9 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                className={`p-6 rounded-xl border-2 ${
                  assignedRole === 'AI' 
                    ? 'bg-red-500/10 border-red-500/50' 
                    : 'bg-cyan-500/10 border-cyan-500/50'
                }`}
              >
                <div className="flex items-center justify-center gap-3">
                  <Sparkles className={`w-6 h-6 ${assignedRole === 'AI' ? 'text-red-400' : 'text-cyan-400'}`} />
                  <div className="text-center">
                    <p className="text-white mb-1">Your role in this game:</p>
                    <p className={assignedRole === 'AI' ? 'text-red-400' : 'text-cyan-400'}>
                      {assignedRole === 'AI' 
                        ? 'You are the AI! Blend in and survive!' 
                        : 'You are a human player. Find the AI!'}
                    </p>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Start Game Button */}
          {!assignedRole && canStartGame && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex justify-center"
            >
              <button
                onClick={handleStartGame}
                className="px-8 py-4 bg-cyan-500 hover:bg-cyan-400 text-white rounded-xl text-lg font-semibold transition-colors shadow-lg shadow-cyan-500/25"
              >
                Start Game ({players.length} players)
              </button>
            </motion.div>
          )}

          {/* Loading indicator */}
          {!assignedRole && !canStartGame && (
            <div className="flex flex-col items-center justify-center gap-2">
              <div className="flex gap-1">
                {[0, 1, 2].map((i) => (
                  <motion.div
                    key={i}
                    className="w-2 h-2 bg-cyan-400 rounded-full"
                    animate={{
                      scale: [1, 1.5, 1],
                      opacity: [1, 0.5, 1],
                    }}
                    transition={{
                      duration: 1,
                      repeat: Infinity,
                      delay: i * 0.2,
                    }}
                  />
                ))}
              </div>
              <span className="text-slate-400">Waiting for more players (minimum 3)...</span>
            </div>
          )}
        </motion.div>
      </div>
    </div>
  );
}