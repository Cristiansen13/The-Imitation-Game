import { motion } from 'motion/react';
import { Brain, UserPlus, Shield } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { register } from '../services/auth';

export function LoginPage() {
  const { login } = useAuth();

  const handleKeycloakLogin = () => {
    login();
  };

  const handleRegister = () => {
    register();
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
      {/* Animated background */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute inset-0 opacity-20">
          {[...Array(20)].map((_, i) => (
            <motion.div
              key={i}
              className="absolute text-cyan-500/30 font-mono"
              style={{
                left: `${Math.random() * 100}%`,
                top: `${Math.random() * 100}%`,
              }}
              animate={{
                y: [0, -100],
                opacity: [0, 0.5, 0],
              }}
              transition={{
                duration: 3 + Math.random() * 2,
                repeat: Infinity,
                delay: Math.random() * 2,
              }}
            >
              {Math.random() > 0.5 ? '01010101' : '11001010'}
            </motion.div>
          ))}
        </div>
        
        {/* Grid pattern */}
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b_1px,transparent_1px),linear-gradient(to_bottom,#1e293b_1px,transparent_1px)] bg-[size:4rem_4rem] opacity-20" />
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="relative z-10 w-full max-w-6xl grid md:grid-cols-2 gap-8 items-center"
      >
        {/* Left side - Branding and info */}
        <div className="space-y-6">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="space-y-4"
          >
            <div className="flex items-center gap-3">
              <div className="p-3 bg-gradient-to-br from-cyan-500 to-purple-600 rounded-xl shadow-lg shadow-cyan-500/50">
                <Brain className="w-8 h-8 text-white" />
              </div>
              <h1 className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-purple-500">
                The Imitation Game
              </h1>
            </div>
            
            <p className="text-slate-400">
              A social deduction game of psychology and analysis
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.4 }}
            className="space-y-4"
          >
            <h2 className="text-cyan-400">How does it work?</h2>
            
            <div className="space-y-3">
              <div className="flex gap-3 items-start">
                <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center flex-shrink-0">
                  <span className="text-cyan-400">1</span>
                </div>
                <p className="text-slate-400">
                  Join a chat room with 7 participants
                </p>
              </div>
              
              <div className="flex gap-3 items-start">
                <div className="w-8 h-8 rounded-lg bg-purple-500/10 border border-purple-500/30 flex items-center justify-center flex-shrink-0">
                  <span className="text-purple-400">2</span>
                </div>
                <p className="text-slate-400">
                  One participant is controlled by AI
                </p>
              </div>
              
              <div className="flex gap-3 items-start">
                <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center flex-shrink-0">
                  <span className="text-cyan-400">3</span>
                </div>
                <p className="text-slate-400">
                  Chat and analyze the behavior of others
                </p>
              </div>
              
              <div className="flex gap-3 items-start">
                <div className="w-8 h-8 rounded-lg bg-purple-500/10 border border-purple-500/30 flex items-center justify-center flex-shrink-0">
                  <span className="text-purple-400">4</span>
                </div>
                <p className="text-slate-400">
                  Vote and eliminate who you think is the AI
                </p>
              </div>
            </div>
          </motion.div>
        </div>

        {/* Right side - Login form */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.3 }}
          className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-8 shadow-2xl"
        >
          <div className="space-y-6">
            <div className="text-center space-y-2">
              <h2 className="text-white">Welcome</h2>
              <p className="text-slate-400">Sign in or create an account to play</p>
            </div>

            <div className="space-y-4">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleKeycloakLogin}
                className="w-full bg-gradient-to-r from-cyan-500 to-purple-600 text-white rounded-xl py-3 shadow-lg shadow-cyan-500/30 hover:shadow-cyan-500/50 transition-all flex items-center justify-center gap-2"
              >
                <Shield className="w-5 h-5" />
                Sign In
              </motion.button>
              
              <div className="relative">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-slate-700"></div>
                </div>
                <div className="relative flex justify-center text-sm">
                  <span className="px-2 bg-slate-900/50 text-slate-500">or</span>
                </div>
              </div>
              
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleRegister}
                className="w-full border-2 border-cyan-500/50 bg-slate-800/30 text-cyan-400 rounded-xl py-3 hover:border-cyan-500 hover:bg-cyan-500/10 transition-all flex items-center justify-center gap-2"
              >
                <UserPlus className="w-5 h-5" />
                Create Account
              </motion.button>
            </div>

            <div className="text-center">
              <p className="text-slate-500">
                Authentication via{' '}
                <span className="text-cyan-400">Keycloak SSO</span>
              </p>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </div>
  );
}