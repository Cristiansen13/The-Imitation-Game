import { useEffect, useState, useCallback } from 'react';
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
  onGameStart: (roomId: string, isAI: boolean) => void;
  onLeave?: () => void;
}

export function Lobby({ onGameStart, onLeave }: LobbyProps) {
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
  const LOBBY_DURATION = 30; // seconds - countdown duration once 3+ players

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
      // Try to join an available room or create a new one
      const room = await roomApi.joinAny();
      
      setRoomId(room.id);
      
      // Set initial players from room data
      if (room.players && room.players.length > 0) {
        setPlayers(room.players.map((p: { oderId: string; username: string; status: string }) => ({
          id: p.oderId,
          username: p.username,
          avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${p.username}`,
          status: p.status.toLowerCase() as Player['status'],
        })));
      } else {
        // Add self if no players list
        setPlayers([{
          id: user?.oderId || 'self',
          username: user?.username || 'You',
          avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${user?.username || 'You'}`,
          status: 'connected',
        }]);
      }
      
      // Connect to WebSocket
      await wsService.connect();
      
      // Track if we've already started the game transition
      let gameStarted = false;
      
      // Subscribe to game events on room topic
      wsService.subscribeToGameEvents(room.id, (event) => {
        console.log('Game event received:', event);
        switch (event.type) {
          case 'PLAYER_JOINED':
            setPlayers(prev => {
              // Don't add if already exists (check by id)
              if (prev.some(p => p.id === event.data.oderId)) return prev;
              return [...prev, {
                id: event.data.oderId,
                username: event.data.username,
                avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${event.data.username}`,
                status: 'connected',
              }];
            });
            break;
          case 'PLAYER_LEFT':
            setPlayers(prev => prev.filter(p => p.id !== event.data.oderId));
            break;
          case 'GAME_STARTED':
            if (gameStarted) return; // Prevent duplicate handling
            gameStarted = true;
            
            // Check if current user is the AI by comparing aiPlayerId
            const aiPlayerId = event.data.aiPlayerId;
            const isAI = aiPlayerId === user?.oderId;
            console.log('GAME_STARTED - aiPlayerId:', aiPlayerId, 'myId:', user?.oderId, 'isAI:', isAI);
            
            setAssignedRole(isAI ? 'AI' : 'Player');
            setTimeout(() => {
              onGameStart(room.id, isAI);
            }, 2000);
            break;
        }
      });
      
      setIsLoading(false);
      
    } catch (err) {
      console.error('Failed to create/join room:', err);
      setError('Failed to connect to game server. Please try again.');
      setIsLoading(false);
    }
  }, [onGameStart, user]);

  // Check if game can start (minimum 3 players) and manage timer
  useEffect(() => {
    const hasEnoughPlayers = players.length >= 3;
    setCanStartGame(hasEnoughPlayers);
    
    if (hasEnoughPlayers && !timerStartedAt) {
      // Start the countdown timer when we reach 3+ players
      setTimerStartedAt(new Date());
      setTimeLeft(LOBBY_DURATION);
    } else if (!hasEnoughPlayers && timerStartedAt) {
      // Reset timer if players drop below 3
      setTimerStartedAt(null);
      setTimeLeft(LOBBY_DURATION);
    }
  }, [players, timerStartedAt]);

  const handleStartGame = useCallback(async () => {
    if (!roomId || !canStartGame || isStartingGame || assignedRole) {
      console.log('[Lobby] handleStartGame blocked:', { roomId, canStartGame, isStartingGame, assignedRole });
      return;
    }
    
    console.log('[Lobby] Starting game for room:', roomId);
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
  }, [roomId, canStartGame, isStartingGame, assignedRole]);

  const handleLeaveRoom = useCallback(async () => {
    if (roomId) {
      try {
        await roomApi.leave(roomId);
      } catch (err) {
        console.error('Failed to leave room:', err);
      }
    }
    wsService.disconnect();
    if (onLeave) {
      onLeave();
    }
  }, [roomId, onLeave]);

  useEffect(() => {
    createOrJoinRoom();
    
    return () => {
      wsService.disconnect();
    };
  }, [createOrJoinRoom]);

  // Timer effect - only runs when 3+ players (timerStartedAt is set)
  useEffect(() => {
    if (!timerStartedAt || isLoading || assignedRole || isStartingGame) return;
    
    const timer = setInterval(() => {
      const remaining = calculateTimeLeft(timerStartedAt);
      setTimeLeft(remaining);
      
      if (remaining === 0 && canStartGame && roomId && !isStartingGame) {
        clearInterval(timer);
        // All players try to start - the first one succeeds, others get 400 which is fine
        // The GAME_STARTED event will transition everyone
        console.log('[Lobby] Timer reached 0, attempting to start game');
        handleStartGame();
      }
    }, 1000);
    
    return () => clearInterval(timer);
  }, [timerStartedAt, isLoading, canStartGame, roomId, assignedRole, isStartingGame, calculateTimeLeft, handleStartGame]);

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