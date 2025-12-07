import { motion } from 'motion/react';
import { X, Users } from 'lucide-react';

interface Player {
  id: number;
  username: string;
  avatar: string;
  status: 'alive' | 'eliminated';
}

interface EliminationScreenProps {
  player: Player;
}

export function EliminationScreen({ player }: EliminationScreenProps) {
  return (
    <div className="min-h-screen flex items-center justify-center p-6">
      <motion.div
        initial={{ opacity: 0, scale: 0.8 }}
        animate={{ opacity: 1, scale: 1 }}
        className="max-w-2xl w-full"
      >
        {/* Main elimination card */}
        <div className="bg-slate-900/50 backdrop-blur-xl border border-red-500/50 rounded-2xl p-12 shadow-2xl shadow-red-500/20">
          <div className="text-center space-y-8">
            {/* Animated X icon */}
            <motion.div
              initial={{ scale: 0, rotate: -180 }}
              animate={{ scale: 1, rotate: 0 }}
              transition={{ type: 'spring', duration: 0.8 }}
              className="relative mx-auto w-32 h-32"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-red-500 to-red-700 rounded-full animate-pulse" />
              <div className="relative w-full h-full bg-slate-900 rounded-full border-4 border-red-500 flex items-center justify-center">
                <X className="w-16 h-16 text-red-400" />
              </div>
            </motion.div>

            {/* Player eliminated */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="space-y-4"
            >
              <h1 className="text-white">Player Eliminated</h1>
              
              <div className="flex items-center justify-center gap-4">
                <div className="relative">
                  <img
                    src={player.avatar}
                    alt={player.username}
                    className="w-20 h-20 rounded-full border-4 border-red-500/50 grayscale"
                  />
                  <div className="absolute inset-0 bg-red-500/30 rounded-full" />
                </div>
                <div className="text-left">
                  <p className="text-red-400">{player.username}</p>
                  <p className="text-slate-500">has been eliminated from the game</p>
                </div>
              </div>
            </motion.div>

            {/* Message */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.6 }}
              className="p-6 bg-slate-800/50 border border-slate-700 rounded-xl"
            >
              <p className="text-slate-300">
                The majority of players voted to eliminate this suspect.
                <br />
                <span className="text-slate-500">The game continues with remaining players...</span>
              </p>
            </motion.div>

            {/* Loading indicator */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.9 }}
              className="flex items-center justify-center gap-2"
            >
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
              <span className="text-slate-400">Preparing new round...</span>
            </motion.div>
          </div>
        </div>

        {/* Remaining players hint */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.2 }}
          className="mt-6 p-4 bg-slate-900/30 backdrop-blur-xl border border-slate-800 rounded-xl"
        >
          <div className="flex items-center justify-center gap-2 text-slate-400">
            <Users className="w-5 h-5" />
            <span>Continue investigation with remaining players</span>
          </div>
        </motion.div>
      </motion.div>
    </div>
  );
}
