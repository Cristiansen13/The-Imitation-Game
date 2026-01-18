import { motion } from 'motion/react';
import { ArrowLeft, TrendingUp, Users, Brain, Target, Award, Calendar } from 'lucide-react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { useState, useEffect } from 'react';
import { reportingApi, type PlayerStats, type GlobalStats } from '../services/api';

interface StatsPageProps {
  onBack: () => void;
}

export function StatsPage({ onBack }: StatsPageProps) {
  const [globalStats, setGlobalStats] = useState<GlobalStats | null>(null);
  const [topPlayers, setTopPlayers] = useState<PlayerStats[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadStats = async () => {
      try {
        setIsLoading(true);
        const [global, leaderboard] = await Promise.all([
          reportingApi.getGlobalStats(),
          reportingApi.getLeaderboard(5)
        ]);
        
        if (global) setGlobalStats(global);
        if (leaderboard) setTopPlayers(leaderboard.topByXP || []);
      } catch (error) {
        console.error('Failed to load statistics:', error);
      } finally {
        setIsLoading(false);
      }
    };
    
    loadStats();
  }, []);

  const performanceData = [
    { date: 'Lun', rate: 45 },
    { date: 'Mar', rate: 52 },
    { date: 'Mie', rate: 61 },
    { date: 'Joi', rate: 58 },
    { date: 'Vin', rate: 68 },
    { date: 'Sâm', rate: 72 },
    { date: 'Dum', rate: 68 },
  ];

  const roleData = globalStats ? [
    { name: 'Ca Jucător', value: Math.round(globalStats.averageWinRate), color: '#06b6d4' },
    { name: 'Ca AI', value: Math.round(100 - globalStats.averageWinRate), color: '#a855f7' },
  ] : [];

  const globalStatsDisplay = globalStats ? [
    { label: 'Total jucători', value: globalStats.totalPlayers.toString(), icon: Users, color: 'cyan' },
    { label: 'Meciuri jucate', value: globalStats.totalGamesPlayed.toString(), icon: Target, color: 'purple' },
    { label: 'Rată detectare AI (globală)', value: `${globalStats.averageDetectRate.toFixed(1)}%`, icon: Brain, color: 'cyan' },
    { label: 'Jocuri active', value: globalStats.activeGames.toString(), icon: Award, color: 'purple' },
  ] : [];

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-cyan-500"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen p-6">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center gap-4"
        >
          <motion.button
            whileHover={{ scale: 1.05, x: -2 }}
            whileTap={{ scale: 0.95 }}
            onClick={onBack}
            className="p-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-300 hover:text-cyan-400 hover:border-cyan-500/50 transition-all"
          >
            <ArrowLeft className="w-5 h-5" />
          </motion.button>
          <h1 className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-purple-500">
            Statistici Globale
          </h1>
        </motion.div>

        {/* Global stats grid */}
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
          {globalStatsDisplay.map((stat, idx) => (
            <motion.div
              key={idx}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.1 }}
              className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-xl p-6 shadow-xl"
            >
              <div className="flex items-start justify-between mb-3">
                <div className={`p-2 rounded-lg border ${
                  stat.color === 'cyan' 
                    ? 'bg-cyan-500/10 border-cyan-500/30' 
                    : 'bg-purple-500/10 border-purple-500/30'
                }`}>
                  <stat.icon className={`w-5 h-5 ${
                    stat.color === 'cyan' ? 'text-cyan-400' : 'text-purple-400'
                  }`} />
                </div>
              </div>
              <p className="text-white mb-1">{stat.value}</p>
              <p className="text-slate-400">{stat.label}</p>
            </motion.div>
          ))}
        </div>

        {/* Win distribution */}
        <div className="grid lg:grid-cols-1 gap-6">
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 }}
            className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-6 shadow-xl"
          >
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-purple-500/10 rounded-lg border border-purple-500/30">
                <Award className="w-5 h-5 text-purple-400" />
              </div>
              <div>
                <h2 className="text-white">Distribuție Victorii</h2>
                <p className="text-slate-400">Câștiguri pe roluri</p>
              </div>
            </div>

            <div className="flex items-center justify-center h-64">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={roleData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={90}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {roleData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1e293b',
                      border: '1px solid #334155',
                      borderRadius: '8px',
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>

            <div className="flex justify-center gap-6 mt-4">
              {roleData.map((item, idx) => (
                <div key={idx} className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }} />
                  <span className="text-slate-300">{item.name}: {item.value}</span>
                </div>
              ))}
            </div>
          </motion.div>
        </div>

        {/* Top players leaderboard */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-6 shadow-xl"
        >
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 bg-cyan-500/10 rounded-lg border border-cyan-500/30">
              <Award className="w-5 h-5 text-cyan-400" />
            </div>
            <div>
              <h2 className="text-white">Top Jucători Globali</h2>
              <p className="text-slate-400">Clasament pe baza punctelor de experiență</p>
            </div>
          </div>

          <div className="space-y-2">
            {topPlayers.map((player, idx) => (
              <motion.div
                key={idx}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.5 + idx * 0.05 }}
                className="flex items-center gap-4 p-4 rounded-xl border transition-all bg-slate-800/50 border-slate-700 hover:border-slate-600"
              >
                {/* Rank badge */}
                <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                  idx + 1 === 1 ? 'bg-yellow-500/20 border-2 border-yellow-500/50' :
                  idx + 1 === 2 ? 'bg-slate-400/20 border-2 border-slate-400/50' :
                  idx + 1 === 3 ? 'bg-orange-700/20 border-2 border-orange-700/50' :
                  'bg-slate-700/50 border border-slate-600/50'
                }`}>
                  <span className={`${
                    idx + 1 === 1 ? 'text-yellow-400' :
                    idx + 1 === 2 ? 'text-slate-300' :
                    idx + 1 === 3 ? 'text-orange-400' :
                    'text-slate-500'
                  }`}>
                    #{idx + 1}
                  </span>
                </div>

                {/* Avatar */}
                <img
                  src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${player.username}`}
                  alt={player.username}
                  className="w-10 h-10 rounded-full border-2 border-slate-700"
                />

                {/* Player info */}
                <div className="flex-1">
                  <p className="text-white">{player.username}</p>
                  <p className="text-slate-500">{player.gamesPlayed} meciuri</p>
                </div>

                {/* XP */}
                <div className="text-right">
                  <p className="text-cyan-400">{player.experiencePoints || 0}</p>
                  <p className="text-slate-500">XP</p>
                </div>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>
    </div>
  );
}
