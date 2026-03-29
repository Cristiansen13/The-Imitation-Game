import { useState } from 'react';
import { motion } from 'motion/react';
import { Brain, UserPlus, Shield, LogIn } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

type Mode = 'login' | 'register';

export function LoginPage() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      if (mode === 'login') {
        await login(username, password);
      } else {
        await register(username, email, password);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
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

        {/* Right side - Login / Register form */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.3 }}
          className="bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl p-8 shadow-2xl"
        >
          <div className="space-y-6">
            <div className="text-center space-y-2">
              <h2 className="text-white">{mode === 'login' ? 'Sign In' : 'Create Account'}</h2>
              <p className="text-slate-400">
                {mode === 'login' ? 'Enter your credentials to play' : 'Register a new account'}
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-1">Username</label>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  autoComplete="username"
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500 transition-colors"
                  placeholder="your_username"
                />
              </div>

              {mode === 'register' && (
                <div>
                  <label className="block text-sm text-slate-400 mb-1">Email</label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoComplete="email"
                    className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500 transition-colors"
                    placeholder="you@example.com"
                  />
                </div>
              )}

              <div>
                <label className="block text-sm text-slate-400 mb-1">Password</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500 transition-colors"
                  placeholder="••••••••"
                />
              </div>

              {error && (
                <p className="text-red-400 text-sm text-center">{error}</p>
              )}

              <motion.button
                type="submit"
                disabled={loading}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="w-full bg-gradient-to-r from-cyan-500 to-purple-600 text-white rounded-xl py-3 shadow-lg shadow-cyan-500/30 hover:shadow-cyan-500/50 transition-all flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {mode === 'login'
                  ? <><Shield className="w-5 h-5" />{loading ? 'Signing in…' : 'Sign In'}</>
                  : <><LogIn className="w-5 h-5" />{loading ? 'Creating account…' : 'Create Account'}</>
                }
              </motion.button>
            </form>

            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-slate-700" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-slate-900/50 text-slate-500">or</span>
              </div>
            </div>

            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(null); }}
              className="w-full border-2 border-cyan-500/50 bg-slate-800/30 text-cyan-400 rounded-xl py-3 hover:border-cyan-500 hover:bg-cyan-500/10 transition-all flex items-center justify-center gap-2"
            >
              <UserPlus className="w-5 h-5" />
              {mode === 'login' ? 'New here? Create an account' : 'Already have an account? Sign in'}
            </motion.button>
          </div>
        </motion.div>
      </motion.div>
    </div>
  );
}