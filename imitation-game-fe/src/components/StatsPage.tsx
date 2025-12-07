import { motion } from 'motion/react';
import { ArrowLeft, TrendingUp, Users, Brain, Target, Award, Calendar } from 'lucide-react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

interface StatsPageProps {
  onBack: () => void;
}

export function StatsPage({ onBack }: StatsPageProps) {
  const performanceData = [
    { date: 'Lun', rate: 45 },
    { date: 'Mar', rate: 52 },
    { date: 'Mie', rate: 61 },
    { date: 'Joi', rate: 58 },
    { date: 'Vin', rate: 68 },
    { date: 'Sâm', rate: 72 },
    { date: 'Dum', rate: 68 },
  ];

  const roleData = [
    { name: 'Ca Jucător', value: 35, color: '#06b6d4' },
    { name: 'Ca AI', value: 12, color: '#a855f7' },
  ];

  const topPlayers = [
    { rank: 1, username: 'MasterDetective', winRate: 84.2, games: 156 },
    { rank: 2, username: 'AIHunter_Pro', winRate: 81.5, games: 203 },
    { rank: 3, username: 'LogicMaster', winRate: 79.8, games: 142 },
    { rank: 4, username: 'You', winRate: 68.5, games: 47 },
    { rank: 5, username: 'CyberSleuth', winRate: 67.3, games: 89 },
  ];

  const globalStats = [
    { label: 'Jucători activi azi', value: '2,847', icon: Users, color: 'cyan' },
    { label: 'Meciuri jucate azi', value: '1,234', icon: Target, color: 'purple' },
    { label: 'Rată detectare AI (globală)', value: '62.4%', icon: Brain, color: 'cyan' },
    { label: 'Rată câștig AI (globală)', value: '37.6%', icon: Award, color: 'purple' },
  ];

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
          {globalStats.map((stat, idx) => (
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

        <div className="grid lg:grid-cols-2 gap-6">
          {/* Performance over time */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-6 shadow-xl"
          >
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-cyan-500/10 rounded-lg border border-cyan-500/30">
                <TrendingUp className="w-5 h-5 text-cyan-400" />
              </div>
              <div>
                <h2 className="text-white">Evoluția Performanței</h2>
                <p className="text-slate-400">Rata ta de detectare (ultimele 7 zile)</p>
              </div>
            </div>
            
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={performanceData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                  <XAxis dataKey="date" stroke="#94a3b8" />
                  <YAxis stroke="#94a3b8" />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1e293b',
                      border: '1px solid #334155',
                      borderRadius: '8px',
                    }}
                  />
                  <Line
                    type="monotone"
                    dataKey="rate"
                    stroke="url(#colorGradient)"
                    strokeWidth={3}
                    dot={{ fill: '#06b6d4', r: 4 }}
                  />
                  <defs>
                    <linearGradient id="colorGradient" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stopColor="#06b6d4" />
                      <stop offset="100%" stopColor="#a855f7" />
                    </linearGradient>
                  </defs>
                </LineChart>
              </ResponsiveContainer>
            </div>
          </motion.div>

          {/* Win distribution */}
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
              <p className="text-slate-400">Clasament pe baza ratei de detectare</p>
            </div>
          </div>

          <div className="space-y-2">
            {topPlayers.map((player, idx) => (
              <motion.div
                key={idx}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.5 + idx * 0.05 }}
                className={`flex items-center gap-4 p-4 rounded-xl border transition-all ${
                  player.username === 'You'
                    ? 'bg-cyan-500/10 border-cyan-500/30 shadow-lg shadow-cyan-500/10'
                    : 'bg-slate-800/50 border-slate-700 hover:border-slate-600'
                }`}
              >
                {/* Rank badge */}
                <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                  player.rank === 1 ? 'bg-yellow-500/20 border-2 border-yellow-500/50' :
                  player.rank === 2 ? 'bg-slate-400/20 border-2 border-slate-400/50' :
                  player.rank === 3 ? 'bg-orange-700/20 border-2 border-orange-700/50' :
                  'bg-slate-700/50 border border-slate-600/50'
                }`}>
                  <span className={`${
                    player.rank === 1 ? 'text-yellow-400' :
                    player.rank === 2 ? 'text-slate-300' :
                    player.rank === 3 ? 'text-orange-400' :
                    'text-slate-500'
                  }`}>
                    #{player.rank}
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
                  <p className={`${player.username === 'You' ? 'text-cyan-400' : 'text-white'}`}>
                    {player.username}
                  </p>
                  <p className="text-slate-500">{player.games} meciuri</p>
                </div>

                {/* Win rate */}
                <div className="text-right">
                  <p className="text-white">{player.winRate}%</p>
                  <p className="text-slate-500">Win rate</p>
                </div>

                {/* Badge for top 3 */}
                {player.rank <= 3 && (
                  <div className={`p-2 rounded-lg ${
                    player.rank === 1 ? 'bg-yellow-500/10' :
                    player.rank === 2 ? 'bg-slate-400/10' :
                    'bg-orange-700/10'
                  }`}>
                    <Award className={`w-5 h-5 ${
                      player.rank === 1 ? 'text-yellow-400' :
                      player.rank === 2 ? 'text-slate-300' :
                      'text-orange-400'
                    }`} />
                  </div>
                )}
              </motion.div>
            ))}
          </div>
        </motion.div>

        {/* Session statistics */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-6 shadow-xl"
        >
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 bg-purple-500/10 rounded-lg border border-purple-500/30">
              <Calendar className="w-5 h-5 text-purple-400" />
            </div>
            <div>
              <h2 className="text-white">Activitate Azi</h2>
              <p className="text-slate-400">Statistici sesiune curentă</p>
            </div>
          </div>

          <div className="grid md:grid-cols-4 gap-4">
            <div className="p-4 bg-slate-800/50 rounded-xl border border-slate-700">
              <p className="text-slate-400 mb-2">Ore jucate</p>
              <p className="text-white">2h 34m</p>
            </div>
            <div className="p-4 bg-slate-800/50 rounded-xl border border-slate-700">
              <p className="text-slate-400 mb-2">Meciuri finalizate</p>
              <p className="text-white">8</p>
            </div>
            <div className="p-4 bg-slate-800/50 rounded-xl border border-slate-700">
              <p className="text-slate-400 mb-2">Cel mai lung meci</p>
              <p className="text-white">18 minute</p>
            </div>
            <div className="p-4 bg-slate-800/50 rounded-xl border border-slate-700">
              <p className="text-slate-400 mb-2">XP câștigat</p>
              <p className="text-cyan-400">+487</p>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
