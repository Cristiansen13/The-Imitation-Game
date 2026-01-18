import { motion } from 'motion/react';
import { Trophy, TrendingUp, Brain, Target, Play, Home } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useState, useEffect } from 'react';
import { roomApi, GameResults } from '../services/api';

interface ResultsScreenProps {
  roomId: string;
  onPlayAgain: () => void;
  onBackToDashboard: () => void;
}

export function ResultsScreen({ roomId, onPlayAgain, onBackToDashboard }: ResultsScreenProps) {
  const { user } = useAuth();
  const [results, setResults] = useState<GameResults | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    const fetchResults = async () => {
      try {
        setLoading(true);
        const data = await roomApi.getResults(roomId);
        if (data) {
          console.log('[ResultsScreen] Fetched results from API:', data);
          setResults(data);
        }
      } catch (err) {
        console.error('[ResultsScreen] Failed to fetch results:', err);
        setError('Failed to load game results');
      } finally {
        setLoading(false);
      }
    };
    
    fetchResults();
  }, [roomId]);
  
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-cyan-500 mx-auto mb-4"></div>
          <p className="text-slate-400">Loading results...</p>
        </div>
      </div>
    );
  }
  
  if (error || !results) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6">
        <div className="text-center">
          <p className="text-red-400 mb-4">{error || 'No results available'}</p>
          <button
            onClick={onBackToDashboard}
            className="px-6 py-3 bg-slate-800 border border-slate-700 rounded-xl text-white hover:border-cyan-500/50 transition-all"
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }
  
  // Debug logging
  console.log('[ResultsScreen] Received results:', results);
  console.log('[ResultsScreen] User:', user);
  
  // Check if humans won (winnerId is "HUMANS" when humans win)
  const isVictory = results.winnerId === 'HUMANS';
  
  // Get win condition message
  const getWinConditionMessage = () => {
    switch (results.winCondition) {
      case 'AI_ELIMINATED':
        return 'The players successfully identified the AI!';
      case 'AI_SURVIVED':
        return 'The AI eliminated enough players to win!';
      case 'ROUNDS_EXHAUSTED':
        return 'The AI managed to hide until the end!';
      default:
        return isVictory ? 'Humans win!' : 'AI wins!';
    }
  };
  
  // Get current user's player data
  const currentPlayer = results.players?.find(p => p.oderId === user?.id);
  const votesReceivedByUser = currentPlayer?.votesReceived || 0;
  
  console.log('[ResultsScreen] Current player:', currentPlayer);
  console.log('[ResultsScreen] User ID:', user?.id);
  console.log('[ResultsScreen] All players:', results.players);
  
  // Check if user voted for the AI correctly
  const votedCorrectly = currentPlayer && !currentPlayer.isAI && currentPlayer.votedFor === results.aiPlayerId;
  
  // Count how many humans voted correctly for the AI
  const humansWhoVoted = results.players?.filter(p => !p.isAI && p.votedFor) || [];
  const correctVotes = humansWhoVoted.filter(p => p.votedFor === results.aiPlayerId).length;
  const totalVotes = humansWhoVoted.length;
  
  console.log('[ResultsScreen] Voted correctly:', votedCorrectly);
  console.log('[ResultsScreen] Correct votes:', correctVotes, 'Total votes:', totalVotes);
  
  // Calculate XP earned
  // 50 XP for winning, 30 XP for correctly voting for AI
  let xpEarned = 0;
  if (currentPlayer) {
    // Win XP
    if (isVictory && !currentPlayer.isAI) {
      xpEarned += 50;
    } else if (!isVictory && currentPlayer.isAI) {
      xpEarned += 50;
    }
    // Correct AI vote XP (only for humans who voted for the AI)
    if (votedCorrectly) {
      xpEarned += 30;
    }
  }
  
  console.log('[ResultsScreen] XP earned:', xpEarned);
  
  // Sort players by votes received (descending) for leaderboard, excluding AI
  const sortedPlayers = [...(results.players || [])]
    .filter(p => !p.isAI)
    .sort((a, b) => b.votesReceived - a.votesReceived);

  return (
    <div className="min-h-screen p-6">
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Main result banner */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className={`bg-gradient-to-r ${
            isVictory 
              ? 'from-cyan-500 to-purple-600' 
              : 'from-red-500 to-orange-600'
          } rounded-2xl p-12 shadow-2xl relative overflow-hidden`}
        >
          {/* Animated background */}
          <div className="absolute inset-0 opacity-20">
            {[...Array(10)].map((_, i) => (
              <motion.div
                key={i}
                className="absolute w-2 h-2 bg-white rounded-full"
                style={{
                  left: `${Math.random() * 100}%`,
                  top: `${Math.random() * 100}%`,
                }}
                animate={{
                  y: [0, -20, 0],
                  opacity: [0, 1, 0],
                }}
                transition={{
                  duration: 2 + Math.random() * 2,
                  repeat: Infinity,
                  delay: Math.random() * 2,
                }}
              />
            ))}
          </div>

          <div className="relative z-10 text-center space-y-6">
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring', delay: 0.2 }}
            >
              <Trophy className="w-20 h-20 mx-auto text-white" />
            </motion.div>

            <div>
              <h1 className="text-white mb-2">
                {isVictory ? 'Victory!' : 'Defeat'}
              </h1>
              <p className="text-white/90">
                {getWinConditionMessage()}
              </p>
            </div>

            {/* AI reveal */}
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.4 }}
              className="inline-block bg-white/20 backdrop-blur-sm border-2 border-white/30 rounded-2xl p-6"
            >
              <p className="text-white/80 mb-3">The AI was:</p>
              <div className="flex items-center justify-center gap-4">
                <img
                  src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${results.aiUsername}`}
                  alt={results.aiUsername}
                  className="w-16 h-16 rounded-full border-4 border-white/50"
                />
                <div className="text-left">
                  <p className="text-white font-bold">{results.aiUsername}</p>
                  <p className="text-white/70 text-sm">AI Controlled</p>
                </div>
              </div>
            </motion.div>
          </div>
        </motion.div>

        <div className="grid lg:grid-cols-2 gap-6">
          {/* Match statistics */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 }}
            className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-6 shadow-xl"
          >
            <h2 className="text-white mb-6">Match Statistics</h2>
            
            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 bg-slate-800/50 rounded-xl">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-cyan-500/10 rounded-lg border border-cyan-500/30">
                    <Target className="w-5 h-5 text-cyan-400" />
                  </div>
                  <span className="text-slate-300">Game result</span>
                </div>
                <span className="text-white">{results.winCondition.replace('_', ' ')}</span>
              </div>

              <div className="flex items-center justify-between p-4 bg-slate-800/50 rounded-xl">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-purple-500/10 rounded-lg border border-purple-500/30">
                    <Brain className="w-5 h-5 text-purple-400" />
                  </div>
                  <span className="text-slate-300">Votes received (you)</span>
                </div>
                <span className="text-white">{votesReceivedByUser}</span>
              </div>

              <div className="flex items-center justify-between p-4 bg-slate-800/50 rounded-xl">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-cyan-500/10 rounded-lg border border-cyan-500/30">
                    <TrendingUp className="w-5 h-5 text-cyan-400" />
                  </div>
                  <span className="text-slate-300">Correct votes</span>
                </div>
                <span className="text-white">{correctVotes}</span>
              </div>

              <div className="p-4 bg-gradient-to-r from-cyan-500/10 to-purple-500/10 border border-cyan-500/30 rounded-xl">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-slate-300">XP earned</span>
                  <span className="text-cyan-400">+{xpEarned}</span>
                </div>
                <div className="w-full bg-slate-800 rounded-full h-2">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: '100%' }}
                    transition={{ duration: 1, delay: 0.5 }}
                    className="bg-gradient-to-r from-cyan-500 to-purple-600 h-2 rounded-full"
                  />
                </div>
              </div>
            </div>
          </motion.div>

          {/* Leaderboard */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.4 }}
            className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-6 shadow-xl"
          >
            <h2 className="text-white mb-6">Room Leaderboard</h2>
            
            <div className="space-y-2">
              {sortedPlayers.map((player, idx) => {
                const isCurrentUser = player.oderId === user?.id;
                
                // Calculate XP for this player
                let playerXP = 0;
                // Win XP (50 XP)
                if (isVictory && !player.isAI) {
                  playerXP += 50;
                }
                // Correct AI vote XP (30 XP)
                if (!player.isAI && player.votedFor === results.aiPlayerId) {
                  playerXP += 30;
                }
                
                return (
                  <motion.div
                    key={player.oderId}
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.5 + idx * 0.1 }}
                    className={`flex items-center gap-4 p-4 rounded-xl border ${
                      isCurrentUser
                        ? 'bg-cyan-500/10 border-cyan-500/30'
                        : 'bg-slate-800/50 border-slate-700'
                    }`}
                  >
                    <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                      idx === 0 ? 'bg-yellow-500/20 border border-yellow-500/50' :
                      idx === 1 ? 'bg-slate-400/20 border border-slate-400/50' :
                      idx === 2 ? 'bg-orange-700/20 border border-orange-700/50' :
                      'bg-slate-700/50 border border-slate-600/50'
                    }`}>
                      <span className={`${
                        idx === 0 ? 'text-yellow-400' :
                        idx === 1 ? 'text-slate-300' :
                        idx === 2 ? 'text-orange-400' :
                        'text-slate-500'
                      }`}>
                        {idx + 1}
                      </span>
                    </div>
                    
                    <img
                      src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${player.username}`}
                      alt={player.username}
                      className="w-10 h-10 rounded-full border-2 border-slate-700"
                    />
                    
                    <div className="flex-1">
                      <p className="text-white">
                        {player.username}
                        {isCurrentUser && <span className="ml-2 text-cyan-400 text-sm">(You)</span>}
                        {player.isAI && <span className="ml-2 text-red-400 text-sm">(AI)</span>}
                      </p>
                      <p className="text-slate-500">
                        {player.votesReceived} votes received • {player.status.toLowerCase()}
                      </p>
                    </div>

                    <div className="text-right">
                      <p className="text-cyan-400">+{playerXP} XP</p>
                    </div>
                  </motion.div>
                );
              })}
            </div>
          </motion.div>
        </div>

        {/* Action buttons */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
          className="flex flex-col sm:flex-row gap-4"
        >
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={onPlayAgain}
            className="flex-1 flex items-center justify-center gap-3 bg-gradient-to-r from-cyan-500 to-purple-600 rounded-xl p-4 text-white shadow-lg shadow-cyan-500/30 hover:shadow-cyan-500/50 transition-all"
          >
            <Play className="w-5 h-5" />
            <span>Play Again</span>
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={onBackToDashboard}
            className="flex-1 flex items-center justify-center gap-3 bg-slate-800 border border-slate-700 rounded-xl p-4 text-white hover:border-cyan-500/50 transition-all"
          >
            <Home className="w-5 h-5" />
            <span>Back to Dashboard</span>
          </motion.button>
        </motion.div>
      </div>
    </div>
  );
}