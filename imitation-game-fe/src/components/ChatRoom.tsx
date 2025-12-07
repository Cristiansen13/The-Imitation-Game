import { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Send, Clock, Users, AlertCircle, X, LogOut, Vote, ChevronUp } from 'lucide-react';
import { wsService, roomApi, ChatMessage } from '../services';

interface Player {
  id: string;
  username: string;
  avatar: string;
  status: 'alive' | 'eliminated';
  votes: number;
}

interface Message {
  id: string;
  oderId: string;
  username: string;
  text: string;
  timestamp: string;
}

interface ChatRoomProps {
  roomId: string | null;
  oderId: string;
  username: string;
  isAI: boolean;
  onPlayerEliminated: (player: Player) => void;
  onGameEnd: (results: any) => void;
  onLeave: () => void;
}

// Simple client-side rate limiter
const MESSAGE_LIMIT = 5;
const WINDOW_MS = 10000; // 10 seconds
let messageTimestamps: number[] = [];

function canSendMessage(): boolean {
  const now = Date.now();
  // Remove old timestamps
  messageTimestamps = messageTimestamps.filter(ts => now - ts < WINDOW_MS);
  return messageTimestamps.length < MESSAGE_LIMIT;
}

function recordMessage(): void {
  messageTimestamps.push(Date.now());
}

export function ChatRoom({ roomId, oderId, username, isAI, onPlayerEliminated, onGameEnd, onLeave }: ChatRoomProps) {
  const [players, setPlayers] = useState<Player[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [currentRound, setCurrentRound] = useState(1);
  const [maxRounds, setMaxRounds] = useState(5);
  const [timeLeft, setTimeLeft] = useState(120);
  const [selectedVote, setSelectedVote] = useState<string | null>(null);
  const [showVoteModal, setShowVoteModal] = useState(false);
  const [hasVoted, setHasVoted] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const [isVotingPhase, setIsVotingPhase] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [rateLimited, setRateLimited] = useState(false);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const subscriptionsRef = useRef<{ unsubRoom?: () => void; unsubEvents?: () => void }>({});
  const hasInitializedRef = useRef(false);
  const timerTriggeredRef = useRef(false);

  // Load room data and connect to WebSocket
  useEffect(() => {
    // Prevent double initialization
    if (hasInitializedRef.current || !roomId) return;
    
    const initializeRoom = async () => {
      hasInitializedRef.current = true;
      
      try {
        // Fetch current room state
        const room = await roomApi.get(roomId);
        
        if (!room) {
          throw new Error('Failed to fetch room data');
        }
        
        // Set players from room data
        if (room.players && room.players.length > 0) {
          setPlayers(room.players.map((p: { oderId: string; username: string; status: string }) => ({
            id: p.oderId,
            username: p.username,
            avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${p.username}`,
            status: p.status.toLowerCase() as 'alive' | 'eliminated',
            votes: 0,
          })));
        }
        
        setCurrentRound(room.currentRound || 1);
        setMaxRounds(room.maxRounds || 5);
        
        // Connect to WebSocket if not already connected
        if (!wsService.isConnected()) {
          await wsService.connect();
        }
        
        // Subscribe to room messages
        const unsubRoom = wsService.subscribeToRoom(roomId, (chatMessage: ChatMessage) => {
          console.log('[ChatRoom] Message received:', chatMessage);
          const oderId = chatMessage.oderId || chatMessage.userId || 'unknown';
          setMessages(prev => {
            return [...prev, {
              id: `msg-${Date.now()}-${Math.random()}`,
              oderId,
              username: chatMessage.username,
              text: chatMessage.message,
              timestamp: new Date(chatMessage.timestamp).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
            }];
          });
        });
        subscriptionsRef.current.unsubRoom = unsubRoom;
        
        // Subscribe to game events
        const unsubEvents = wsService.subscribeToGameEvents(roomId, (event) => {
          console.log('Game event in ChatRoom:', event);
          switch (event.type) {
            case 'ROUND_STARTED':
              console.log('[ChatRoom] Round started:', event.data.roundNumber);
              setCurrentRound(event.data.roundNumber);
              setTimeLeft(120); // 2 minutes for chat
              setHasVoted(false);
              setIsVotingPhase(false);
              setIsTransitioning(false);
              timerTriggeredRef.current = false; // Reset timer trigger for new round
              break;
            case 'VOTING_STARTED':
              console.log('[ChatRoom] Voting started');
              setIsVotingPhase(true);
              setTimeLeft(60); // 1 minute for voting
              setIsTransitioning(false);
              timerTriggeredRef.current = false; // Reset timer trigger for voting phase
              break;
            case 'PLAYER_ELIMINATED':
              console.log('[ChatRoom] Player eliminated:', event.data);
              setPlayers(prev => {
                const updated = prev.map(p => 
                  p.id === event.data.oderId ? { ...p, status: 'eliminated' as const } : p
                );
                const eliminatedPlayer = updated.find(p => p.id === event.data.oderId);
                if (eliminatedPlayer) {
                  // Call callback after state update
                  setTimeout(() => onPlayerEliminated(eliminatedPlayer), 0);
                }
                return updated;
              });
              break;
            case 'GAME_ENDED':
              console.log('[ChatRoom] Game ended:', event.data);
              onGameEnd({
                winnerId: event.data.winnerId,
                winCondition: event.data.winCondition,
                aiPlayerId: event.data.aiPlayerId,
                aiUsername: event.data.aiUsername,
              });
              break;
            case 'PLAYER_JOINED':
              setPlayers(prev => {
                if (prev.some(p => p.id === event.data.oderId)) return prev;
                return [...prev, {
                  id: event.data.oderId,
                  username: event.data.username,
                  avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${event.data.username}`,
                  status: 'alive',
                  votes: 0,
                }];
              });
              break;
            case 'PLAYER_LEFT':
              setPlayers(prev => prev.filter(p => p.id !== event.data.oderId));
              break;
          }
        });
        subscriptionsRef.current.unsubEvents = unsubEvents;
        
        setIsConnected(true);
        
      } catch (err) {
        console.error('Failed to initialize room:', err);
        setError('Failed to connect to game. Please try rejoining.');
        hasInitializedRef.current = false; // Allow retry
      }
    };
    
    initializeRoom();
    
    return () => {
      // Cleanup subscriptions
      if (subscriptionsRef.current.unsubRoom) {
        subscriptionsRef.current.unsubRoom();
      }
      if (subscriptionsRef.current.unsubEvents) {
        subscriptionsRef.current.unsubEvents();
      }
      hasInitializedRef.current = false;
    };
  }, [roomId]); // Only depend on roomId, not on callbacks or state

  // Timer with phase transition logic
  useEffect(() => {
    if (timeLeft > 0) {
      const timer = setTimeout(() => setTimeLeft(timeLeft - 1), 1000);
      return () => clearTimeout(timer);
    } else if (timeLeft === 0 && !timerTriggeredRef.current && !isTransitioning && roomId) {
      // Timer hit 0 - trigger phase transition
      timerTriggeredRef.current = true;
      setIsTransitioning(true);
      
      if (!isVotingPhase) {
        // Chat phase ended -> Start voting
        console.log('[ChatRoom] Chat phase ended, starting voting...');
        roomApi.startVoting(roomId)
          .then(() => {
            console.log('[ChatRoom] Voting started successfully');
          })
          .catch((err) => {
            console.error('[ChatRoom] Failed to start voting:', err);
            // If voting already started or error, just set the phase locally
            setIsVotingPhase(true);
            setTimeLeft(60);
            timerTriggeredRef.current = false;
          })
          .finally(() => {
            setIsTransitioning(false);
          });
      } else {
        // Voting phase ended -> Process votes (backend handles this when all votes are in)
        // If not all votes cast, we need to force end voting
        console.log('[ChatRoom] Voting phase ended');
        setIsTransitioning(false);
        // The backend will process results when votes are complete
        // For now, just show a message
        setError('Voting time expired! Waiting for results...');
        setTimeout(() => setError(null), 3000);
      }
    }
  }, [timeLeft, isVotingPhase, isTransitioning, roomId]);

  // Scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendMessage = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    if (inputMessage.trim() && roomId && isConnected) {
      // Check client-side rate limit
      if (!canSendMessage()) {
        setRateLimited(true);
        setTimeout(() => setRateLimited(false), 3000);
        return;
      }
      
      // Record the message and send
      recordMessage();
      wsService.sendMessage(roomId, oderId, username, inputMessage);
      setInputMessage('');
    }
  }, [inputMessage, oderId, username, isConnected, roomId]);

  const scrollToTop = () => {
    messagesContainerRef.current?.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleVoteClick = (playerId: string) => {
    if (playerId !== oderId && !hasVoted && isVotingPhase) {
      setSelectedVote(playerId);
      setShowVoteModal(true);
    }
  };

  const confirmVote = async () => {
    if (selectedVote && roomId) {
      try {
        await roomApi.vote(roomId, selectedVote);
        setHasVoted(true);
        setShowVoteModal(false);
      } catch (err) {
        console.error('Failed to submit vote:', err);
        setError('Failed to submit vote. Please try again.');
      }
    }
  };

  const handleStartVoting = async () => {
    if (roomId) {
      try {
        await roomApi.startVoting(roomId);
      } catch (err) {
        console.error('Failed to start voting:', err);
      }
    }
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="h-screen p-4 overflow-hidden">
      <div className="max-w-7xl mx-auto h-full flex flex-col">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-t-2xl p-4 flex items-center justify-between flex-shrink-0"
        >
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2">
              <Users className="w-5 h-5 text-cyan-400" />
              <span className="text-white">
                {players.filter(p => p.status === 'alive').length} alive
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-slate-400">Round</span>
              <span className="text-cyan-400">{currentRound}/{maxRounds}</span>
            </div>
            {/* Phase indicator */}
            <div className={`px-3 py-1 rounded-full text-sm font-medium ${
              isVotingPhase 
                ? 'bg-purple-500/20 text-purple-400 border border-purple-500/30' 
                : 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/30'
            }`}>
              {isVotingPhase ? '🗳️ Voting' : '💬 Chat'}
            </div>
            {roomId && (
              <div className="flex items-center gap-2">
                <span className="text-slate-500 text-sm">Room: {roomId.slice(0, 8)}...</span>
              </div>
            )}
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3">
              <Clock className={`w-5 h-5 ${timeLeft < 30 ? 'text-red-400' : 'text-cyan-400'}`} />
              <motion.span
                key={timeLeft}
                initial={{ scale: timeLeft < 30 ? 1.1 : 1 }}
                animate={{ scale: 1 }}
                className={`font-mono ${timeLeft < 30 ? 'text-red-400' : 'text-white'}`}
              >
                {formatTime(timeLeft)}
              </motion.span>
            </div>
            <button
              onClick={onLeave}
              className="p-2 text-slate-400 hover:text-red-400 transition-colors"
              title="Leave game"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </motion.div>

        {/* Main content - takes remaining space */}
        <div className="flex-1 grid lg:grid-cols-4 gap-0 bg-slate-900/50 backdrop-blur-xl border-x border-slate-800 min-h-0 overflow-hidden">
          {/* Players sidebar */}
          <div className="lg:col-span-1 border-r border-slate-800 p-4 overflow-y-auto">
            <h3 className="text-white mb-4 flex items-center gap-2">
              Players
              {isVotingPhase && !hasVoted && (
                <span className="text-xs text-purple-400">(Click to vote)</span>
              )}
            </h3>
            <div className="space-y-2">
              {players.map((player) => (
                <motion.button
                  key={player.id}
                  onClick={() => handleVoteClick(player.id)}
                  disabled={player.id === oderId || hasVoted || player.status === 'eliminated'}
                  whileHover={isVotingPhase && player.id !== oderId && !hasVoted && player.status === 'alive' ? { scale: 1.02, x: 4 } : {}}
                  className={`w-full p-3 rounded-xl border transition-all text-left ${
                    player.status === 'eliminated'
                      ? 'bg-slate-800/30 border-slate-700 opacity-50'
                      : player.id === oderId
                      ? 'bg-cyan-500/10 border-cyan-500/30'
                      : !isVotingPhase
                      ? 'bg-slate-800/50 border-slate-700 cursor-default'
                      : hasVoted
                      ? 'bg-slate-800/50 border-slate-700 cursor-not-allowed'
                      : 'bg-slate-800/50 border-slate-700 hover:border-purple-500/50 hover:bg-purple-500/10 cursor-pointer'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="relative">
                      <img
                        src={player.avatar}
                        alt={player.username}
                        className="w-10 h-10 rounded-full"
                      />
                      {player.status === 'eliminated' && (
                        <div className="absolute inset-0 bg-red-500/50 rounded-full flex items-center justify-center">
                          <X className="w-6 h-6 text-white" />
                        </div>
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-white truncate">{player.username}</p>
                      <p className="text-slate-500 text-sm">
                        {player.status === 'eliminated' ? 'Eliminated' : player.id === oderId ? 'You' : 'Suspect'}
                      </p>
                    </div>
                  </div>
                </motion.button>
              ))}
            </div>

            {isVotingPhase && !hasVoted && timeLeft > 0 && (
              <div className="mt-6 p-4 bg-purple-500/10 border border-purple-500/30 rounded-xl">
                <div className="flex items-start gap-2">
                  <AlertCircle className="w-5 h-5 text-purple-400 flex-shrink-0 mt-0.5" />
                  <p className="text-purple-300">
                    Vote now! Choose a suspicious player to eliminate.
                  </p>
                </div>
              </div>
            )}
            
            {!isVotingPhase && (
              <div className="mt-6 p-4 bg-cyan-500/10 border border-cyan-500/30 rounded-xl">
                <div className="flex items-start gap-2">
                  <AlertCircle className="w-5 h-5 text-cyan-400 flex-shrink-0 mt-0.5" />
                  <p className="text-cyan-300 text-sm">
                    Chat with others to figure out who the AI is. Voting starts when the timer ends.
                  </p>
                </div>
              </div>
            )}
            
            {hasVoted && (
              <div className="mt-6 p-4 bg-green-500/10 border border-green-500/30 rounded-xl">
                <div className="flex items-start gap-2">
                  <Vote className="w-5 h-5 text-green-400 flex-shrink-0 mt-0.5" />
                  <p className="text-green-300 text-sm">
                    Vote submitted! Waiting for others...
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* Chat area */}
          <div className="lg:col-span-3 flex flex-col min-h-0 relative">
            {/* Scroll to top button */}
            <button
              onClick={scrollToTop}
              className="absolute top-2 right-6 z-10 p-2 bg-slate-800/90 hover:bg-slate-700 border border-slate-600 rounded-full shadow-lg transition-colors"
              title="Scroll to top"
            >
              <ChevronUp className="w-4 h-4 text-slate-300" />
            </button>
            
            {/* Messages - scrollable area with proper containment */}
            <div ref={messagesContainerRef} className="flex-1 overflow-y-auto p-4 space-y-3 min-h-0">
              <AnimatePresence>
                {messages.map((message) => (
                  <motion.div
                    key={message.id}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className={`flex gap-3 ${message.oderId === oderId ? 'flex-row-reverse' : ''}`}
                  >
                    <img
                      src={players.find(p => p.id === message.oderId)?.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${message.username}`}
                      alt={message.username}
                      className="w-8 h-8 rounded-full flex-shrink-0"
                    />
                    <div className={`flex-1 ${message.oderId === oderId ? 'text-right' : ''}`}>
                      <div className={`flex items-center gap-2 mb-1 ${message.oderId === oderId ? 'justify-end' : ''}`}>
                        <span className={`text-sm ${message.oderId === oderId ? 'text-cyan-400' : 'text-slate-300'}`}>
                          {message.username}
                        </span>
                        <span className="text-slate-500 text-xs">{message.timestamp}</span>
                      </div>
                      <div className={`inline-block p-3 rounded-xl max-w-lg ${
                        message.oderId === oderId
                          ? 'bg-gradient-to-r from-cyan-500 to-purple-600 text-white'
                          : 'bg-slate-800 text-slate-200'
                      }`}>
                        {message.text}
                      </div>
                    </div>
                  </motion.div>
                ))}
              </AnimatePresence>
              <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <form onSubmit={handleSendMessage} className="p-4 border-t border-slate-800">
              {rateLimited && (
                <div className="mb-2 p-2 bg-red-500/20 border border-red-500/30 rounded-lg text-red-400 text-sm text-center">
                  ⚠️ Slow down! You can send up to {MESSAGE_LIMIT} messages every {WINDOW_MS / 1000} seconds.
                </div>
              )}
              <div className="flex gap-2">
                <input
                  type="text"
                  value={inputMessage}
                  onChange={(e) => setInputMessage(e.target.value)}
                  placeholder="Write a message..."
                  className={`flex-1 bg-slate-800 border rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:outline-none focus:ring-2 transition-all ${
                    rateLimited 
                      ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500/20' 
                      : 'border-slate-700 focus:border-cyan-500 focus:ring-cyan-500/20'
                  }`}
                  disabled={rateLimited}
                />
                <motion.button
                  whileHover={{ scale: rateLimited ? 1 : 1.05 }}
                  whileTap={{ scale: rateLimited ? 1 : 0.95 }}
                  type="submit"
                  disabled={rateLimited}
                  className={`px-6 py-3 rounded-xl text-white shadow-lg transition-all ${
                    rateLimited
                      ? 'bg-slate-600 cursor-not-allowed shadow-none'
                      : 'bg-gradient-to-r from-cyan-500 to-purple-600 shadow-cyan-500/30 hover:shadow-cyan-500/50'
                  }`}
                >
                  <Send className="w-5 h-5" />
                </motion.button>
              </div>
            </form>
          </div>
        </div>

        {/* Footer */}
        <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-b-2xl p-4 flex-shrink-0">
          {error ? (
            <p className="text-center text-red-400">{error}</p>
          ) : (
            <p className="text-center text-slate-400">
              {isVotingPhase 
                ? 'Vote for the player you think is the AI!' 
                : 'Chat with others to figure out who the AI is'}
            </p>
          )}
        </div>
      </div>

      {/* Vote confirmation modal */}
      <AnimatePresence>
        {showVoteModal && selectedVote && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 z-50"
            onClick={() => setShowVoteModal(false)}
          >
            <motion.div
              initial={{ scale: 0.9, y: 20 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.9, y: 20 }}
              onClick={(e) => e.stopPropagation()}
              className="bg-slate-900 border border-slate-800 rounded-2xl p-8 max-w-md w-full shadow-2xl"
            >
              <div className="text-center space-y-6">
                <div className="w-20 h-20 mx-auto rounded-full bg-gradient-to-br from-cyan-500 to-purple-600 p-1">
                  <img
                    src={players.find(p => p.id === selectedVote)?.avatar}
                    alt=""
                    className="w-full h-full rounded-full border-4 border-slate-900"
                  />
                </div>
                
                <div>
                  <h3 className="text-white mb-2">Confirm Vote</h3>
                  <p className="text-slate-400">
                    Do you want to vote for{' '}
                    <span className="text-cyan-400">
                      {players.find(p => p.id === selectedVote)?.username}
                    </span>
                    {' '}as the AI?
                  </p>
                </div>

                <div className="flex gap-3">
                  <button
                    onClick={() => setShowVoteModal(false)}
                    className="flex-1 px-4 py-3 bg-slate-800 border border-slate-700 rounded-xl text-white hover:border-slate-600 transition-all"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={confirmVote}
                    className="flex-1 px-4 py-3 bg-gradient-to-r from-cyan-500 to-purple-600 rounded-xl text-white shadow-lg shadow-cyan-500/30 hover:shadow-cyan-500/50 transition-all"
                  >
                    Confirm
                  </button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}