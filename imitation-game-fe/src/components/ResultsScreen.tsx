import { motion } from 'motion/react';
import { Trophy, TrendingUp, Brain, Target, Play, Home } from 'lucide-react';

interface ResultsScreenProps {
  results: {
    aiPlayer: any;
    winner: 'humans' | 'ai';
    roundsPlayed: number;
  };
  onPlayAgain: () => void;
  onBackToDashboard: () => void;
}

export function ResultsScreen({ results, onPlayAgain, onBackToDashboard }: ResultsScreenProps) {
  const isVictory = results.winner === 'humans';

  const playerStats = [
    { username: 'You', votes: 2, correctVotes: 1, survivedRounds: results.roundsPlayed },
    { username: 'Alex92', votes: 1, correctVotes: 1, survivedRounds: 3 },
    { username: 'Detective_Sarah', votes: 3, correctVotes: 2, survivedRounds: results.roundsPlayed },
    { username: 'CyberHunter', votes: 1, correctVotes: 0, survivedRounds: 2 },
    { username: 'MindReader', votes: 2, correctVotes: 1, survivedRounds: 4 },
  ].sort((a, b) => b.correctVotes - a.correctVotes);

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
                {isVictory 
                  ? 'The players successfully identified the AI!' 
                  : 'The AI managed to hide until the end!'}
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
                  src="https://api.dicebear.com/7.x/avataaars/svg?seed=QuantumBot"
                  alt="QuantumBot"
                  className="w-16 h-16 rounded-full border-4 border-white/50"
                />
                <div className="text-left">
                  <p className="text-white">QuantumBot</p>
                  <p className="text-white/70">AI Controlled</p>
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
                  <span className="text-slate-300">Rounds played</span>
                </div>
                <span className="text-white">{results.roundsPlayed}/5</span>
              </div>

              <div className="flex items-center justify-between p-4 bg-slate-800/50 rounded-xl">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-purple-500/10 rounded-lg border border-purple-500/30">
                    <Brain className="w-5 h-5 text-purple-400" />
                  </div>
                  <span className="text-slate-300">Votes received (you)</span>
                </div>
                <span className="text-white">2</span>
              </div>

              <div className="flex items-center justify-between p-4 bg-slate-800/50 rounded-xl">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-cyan-500/10 rounded-lg border border-cyan-500/30">
                    <TrendingUp className="w-5 h-5 text-cyan-400" />
                  </div>
                  <span className="text-slate-300">Correct votes</span>
                </div>
                <span className="text-white">1/3</span>
              </div>

              <div className="p-4 bg-gradient-to-r from-cyan-500/10 to-purple-500/10 border border-cyan-500/30 rounded-xl">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-slate-300">XP earned</span>
                  <span className="text-cyan-400">+{isVictory ? '125' : '50'}</span>
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
              {playerStats.map((player, idx) => (
                <motion.div
                  key={idx}
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.5 + idx * 0.1 }}
                  className={`flex items-center gap-4 p-4 rounded-xl border ${
                    player.username === 'You'
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
                  
                  <div className="flex-1">
                    <p className="text-white">{player.username}</p>
                    <p className="text-slate-500">
                      {player.correctVotes} correct votes • {player.survivedRounds} rounds
                    </p>
                  </div>

                  <div className="text-right">
                    <p className="text-cyan-400">{player.correctVotes * 50} XP</p>
                  </div>
                </motion.div>
              ))}
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