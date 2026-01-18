import { motion } from 'motion/react';
import { Play, Trophy, Target, Zap, User, BarChart3, Settings, LogOut, Brain } from 'lucide-react';
import { GameScreen, UserData } from '../App';
import { useState, useEffect } from 'react';
import { reportingApi, type PlayerStats } from '../services/api';

interface DashboardProps {
  userData: UserData;
  onStartGame: () => void;
  onNavigate: (screen: GameScreen) => void;
}

export function Dashboard({ userData, onStartGame, onNavigate }: DashboardProps) {
  const [playerStats, setPlayerStats] = useState<PlayerStats | null>(null);
  const [isLoadingStats, setIsLoadingStats] = useState(true);
  const [recentGames, setRecentGames] = useState<any[]>([]);

  useEffect(() => {
    const loadStats = async () => {
      try {
        setIsLoadingStats(true);
        const [stats, games] = await Promise.all([
          reportingApi.getMyStats(),
          reportingApi.getRecentGames(5)
        ]);
        if (stats) setPlayerStats(stats);
        if (games) setRecentGames(games);
      } catch (error) {
        console.error('Failed to load player stats:', error);
      } finally {
        setIsLoadingStats(false);
      }
    };
    
    loadStats();
  }, []);

  const displayStats = {
    gamesPlayed: playerStats?.gamesPlayed ?? userData.gamesPlayed,
    detectRate: playerStats ? playerStats.detectRate.toFixed(1) : userData.detectRate,
    aiWins: playerStats?.totalWins ?? 0,
    winRate: playerStats ? playerStats.winRate.toFixed(1) : userData.kdRatio,
  };
  return (
    <div className="min-h-screen p-6">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between"
        >
          <div className="flex items-center gap-3">
            <div className="p-2 bg-gradient-to-br from-cyan-500 to-purple-600 rounded-lg shadow-lg shadow-cyan-500/30">
              <Brain className="w-6 h-6 text-white" />
            </div>
            <h1 className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-purple-500">
              The Imitation Game
            </h1>
          </div>

          {/* Navigation */}
          <div className="flex items-center gap-3">
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => onNavigate('stats')}
              className="p-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-300 hover:text-cyan-400 hover:border-cyan-500/50 transition-all"
            >
              <BarChart3 className="w-5 h-5" />
            </motion.button>
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="p-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-300 hover:text-cyan-400 hover:border-cyan-500/50 transition-all"
            >
              <Settings className="w-5 h-5" />
            </motion.button>
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => onNavigate('login')}
              className="p-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-300 hover:text-red-400 hover:border-red-500/50 transition-all"
            >
              <LogOut className="w-5 h-5" />
            </motion.button>
          </div>
        </motion.div>

        <div className="grid lg:grid-cols-3 gap-6">
          {/* Left sidebar - Profile */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
            className="lg:col-span-1 space-y-6"
          >
            {/* Profile card */}
            <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-6 shadow-xl">
              <div className="space-y-4">
                <div className="flex flex-col items-center gap-4">
                  <div className="relative">
                    <img
                      src={userData.avatar}
                      alt={userData.username}
                      className="w-24 h-24 rounded-full border-4 border-cyan-500/30 shadow-lg shadow-cyan-500/50"
                    />
                    <div className="absolute -bottom-2 -right-2 px-3 py-1 bg-gradient-to-r from-cyan-500 to-purple-600 rounded-full shadow-lg">
                      <span className="text-white">{userData.rank}</span>
                    </div>
                  </div>
                  <div className="text-center">
                    <h2 className="text-white">{userData.username}</h2>
                    <p className="text-slate-400">Detective</p>
                  </div>
                </div>

                <div className="pt-4 border-t border-slate-800 space-y-3">
                  <button className="w-full flex items-center gap-3 p-3 rounded-lg bg-slate-800/50 border border-slate-700 text-slate-300 hover:border-cyan-500/50 hover:text-cyan-400 transition-all">
                    <User className="w-5 h-5" />
                    <span>Profile</span>
                  </button>
                  <button 
                    onClick={() => onNavigate('stats')}
                    className="w-full flex items-center gap-3 p-3 rounded-lg bg-slate-800/50 border border-slate-700 text-slate-300 hover:border-cyan-500/50 hover:text-cyan-400 transition-all"
                  >
                    <BarChart3 className="w-5 h-5" />
                    <span>Statistics</span>
                  </button>
                </div>
              </div>
            </div>
          </motion.div>

          {/* Main content */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="lg:col-span-2 space-y-6"
          >
            {/* Start game button */}
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={onStartGame}
              className="w-full bg-gradient-to-r from-cyan-500 to-purple-600 rounded-2xl p-8 shadow-2xl shadow-cyan-500/30 hover:shadow-cyan-500/50 transition-all group"
            >
              <div className="flex items-center justify-between">
                <div className="text-left">
                  <h2 className="text-white mb-2">Start a new match</h2>
                  <p className="text-cyan-100">Enter the lobby and find opponents</p>
                </div>
                <div className="p-4 bg-white/20 rounded-full group-hover:bg-white/30 transition-all">
                  <Play className="w-8 h-8 text-white" />
                </div>
              </div>
            </motion.button>

            {/* Stats grid */}
            <div className="grid md:grid-cols-2 gap-4">
              <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-xl p-6 shadow-xl">
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-cyan-500/10 rounded-xl border border-cyan-500/30">
                    <Trophy className="w-6 h-6 text-cyan-400" />
                  </div>
                  <div>
                    <p className="text-slate-400">Games Played</p>
                    <p className="text-white">{isLoadingStats ? '...' : displayStats.gamesPlayed}</p>
                  </div>
                </div>
              </div>

              <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-xl p-6 shadow-xl">
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-purple-500/10 rounded-xl border border-purple-500/30">
                    <Target className="w-6 h-6 text-purple-400" />
                  </div>
                  <div>
                    <p className="text-slate-400">Detection Rate</p>
                    <p className="text-white">{isLoadingStats ? '...' : `${displayStats.detectRate}%`}</p>
                  </div>
                </div>
              </div>

              <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-xl p-6 shadow-xl">
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-cyan-500/10 rounded-xl border border-cyan-500/30">
                    <Brain className="w-6 h-6 text-cyan-400" />
                  </div>
                  <div>
                    <p className="text-slate-400">Total Wins</p>
                    <p className="text-white">{isLoadingStats ? '...' : displayStats.aiWins}</p>
                  </div>
                </div>
              </div>

              <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-xl p-6 shadow-xl">
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-purple-500/10 rounded-xl border border-purple-500/30">
                    <Zap className="w-6 h-6 text-purple-400" />
                  </div>
                  <div>
                    <p className="text-slate-400">Win Rate</p>
                    <p className="text-white">{isLoadingStats ? '...' : `${displayStats.winRate}%`}</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Recent matches */}
            <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-6 shadow-xl">
              <h3 className="text-white mb-4">Recent Games</h3>
              <div className="space-y-3">
                {isLoadingStats ? (
                  <div className="text-center text-slate-400 py-8">Loading...</div>
                ) : recentGames.length === 0 ? (
                  <div className="text-center text-slate-400 py-8">No recent games</div>
                ) : (
                  recentGames.map((game, idx) => {
                    const duration = game.endedAt && game.startedAt 
                      ? Math.round((new Date(game.endedAt).getTime() - new Date(game.startedAt).getTime()) / 60000)
                      : 0;
                    const timeAgo = game.endedAt 
                      ? Math.round((Date.now() - new Date(game.endedAt).getTime()) / 3600000)
                      : 0;
                    
                    // Determine if player won this game
                    const isHumanWin = game.winnerId === 'HUMANS';
                    const isAIWin = game.winnerId && game.winnerId !== 'HUMANS';
                    const playerWon = isHumanWin; // Player is human, so they win if humans won
                    const verdict = playerWon ? 'Victory' : 'Defeat';
                    const verdictColor = playerWon ? 'text-green-400' : 'text-red-400';
                    
                    return (
                      <div
                        key={idx}
                        className="flex items-center justify-between p-4 bg-slate-800/50 rounded-lg border border-slate-700"
                      >
                        <div className="flex items-center gap-4">
                          <div className={`w-2 h-2 rounded-full ${playerWon ? 'bg-green-400' : 'bg-red-400'}`} />
                          <div>
                            <p className="text-white">{game.name || 'Game Room'}</p>
                            <p className="text-slate-400">{game.rounds} rounds · {duration}m · <span className={verdictColor}>{verdict}</span></p>
                          </div>
                        </div>
                        <span className="text-slate-500">{timeAgo}h ago</span>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}