import { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { LoginPage } from './components/LoginPage';
import { Dashboard } from './components/Dashboard';
import { Lobby } from './components/Lobby';
import { ChatRoom } from './components/ChatRoom';
import { EliminationScreen } from './components/EliminationScreen';
import { ResultsScreen } from './components/ResultsScreen';
import { StatsPage } from './components/StatsPage';
import { profileApi } from './services';

export type GameScreen = 
  | 'login' 
  | 'dashboard' 
  | 'lobby' 
  | 'chatroom' 
  | 'elimination' 
  | 'results' 
  | 'stats';

export interface UserData {
  id: string;
  username: string;
  email: string;
  avatar: string;
  rank: string;
  gamesPlayed: number;
  detectRate: number;
  aiWins: number;
  kdRatio: number;
}

export interface GameState {
  roomId: string | null;
  currentRound: number;
  status: string;
  isAI: boolean;
}

function AppContent() {
  const { user, isLoading, isLoggedIn, logout } = useAuth();
  const [currentScreen, setCurrentScreen] = useState<GameScreen>('login');
  const [userData, setUserData] = useState<UserData | null>(null);
  const [eliminatedPlayer, setEliminatedPlayer] = useState<any>(null);
  const [gameResults, setGameResults] = useState<any>(null);
  const [gameState, setGameState] = useState<GameState>({ roomId: null, currentRound: 1, status: 'IDLE', isAI: false });

  // Load user profile when authenticated
  useEffect(() => {
    if (isLoggedIn && user) {
      loadUserProfile();
    }
  }, [isLoggedIn, user]);

  // Navigate to dashboard when user is authenticated
  useEffect(() => {
    if (!isLoading) {
      if (isLoggedIn && userData) {
        setCurrentScreen('dashboard');
      } else if (!isLoggedIn) {
        setCurrentScreen('login');
      }
    }
  }, [isLoading, isLoggedIn, userData]);

  const loadUserProfile = async () => {
    try {
      const profile = await profileApi.getMe();
      setUserData({
        id: profile.id,
        username: profile.username,
        email: profile.email,
        avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${profile.username}`,
        rank: profile.role || 'Player',
        gamesPlayed: profile.gamesPlayed || 0,
        detectRate: profile.detectRate || 0,
        aiWins: profile.gamesWon || 0,
        kdRatio: 0,
      });
    } catch (error) {
      console.error('Failed to load user profile', error);
      // Use Keycloak user info as fallback
      if (user) {
        setUserData({
          id: user.id,
          username: user.username,
          email: user.email,
          avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${user.username}`,
          rank: 'Player',
          gamesPlayed: 0,
          detectRate: 0,
          aiWins: 0,
          kdRatio: 0,
        });
      }
    }
  };

  const handleLogin = (username: string) => {
    // This is called when using local login (fallback)
    setUserData({
      id: 'local-' + username,
      username,
      email: '',
      avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${username}`,
      rank: 'Detective',
      gamesPlayed: 0,
      detectRate: 0,
      aiWins: 0,
      kdRatio: 0
    });
    setCurrentScreen('dashboard');
  };

  const handleStartGame = () => {
    setCurrentScreen('lobby');
  };

  const handleGameStart = (roomId: string, isAI: boolean) => {
    setGameState({ roomId, currentRound: 1, status: 'IN_PROGRESS', isAI });
    setCurrentScreen('chatroom');
  };

  const handlePlayerEliminated = (player: any) => {
    setEliminatedPlayer(player);
    setCurrentScreen('elimination');
    setTimeout(() => {
      setCurrentScreen('chatroom');
    }, 3000);
  };

  const handleGameEnd = (results: any) => {
    setGameResults(results);
    setGameState({ roomId: null, currentRound: 1, status: 'IDLE', isAI: false });
    setCurrentScreen('results');
  };

  const handleNavigate = (screen: GameScreen) => {
    if (screen === 'login') {
      logout();
    }
    setCurrentScreen(screen);
  };

  // Show loading screen while initializing auth
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-cyan-500 mx-auto mb-4"></div>
          <p className="text-slate-400">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950">
      {currentScreen === 'login' && (
        <LoginPage onLogin={handleLogin} />
      )}
      {currentScreen === 'dashboard' && userData && (
        <Dashboard 
          userData={userData} 
          onStartGame={handleStartGame}
          onNavigate={handleNavigate}
        />
      )}
      {currentScreen === 'lobby' && (
        <Lobby 
          onGameStart={handleGameStart} 
          onLeave={() => setCurrentScreen('dashboard')}
        />
      )}
      {currentScreen === 'chatroom' && userData && (
        <ChatRoom 
          roomId={gameState.roomId}
          oderId={userData.id}
          username={userData.username}
          isAI={gameState.isAI}
          onPlayerEliminated={handlePlayerEliminated}
          onGameEnd={handleGameEnd}
          onLeave={() => setCurrentScreen('dashboard')}
        />
      )}
      {currentScreen === 'elimination' && eliminatedPlayer && (
        <EliminationScreen player={eliminatedPlayer} />
      )}
      {currentScreen === 'results' && gameResults && (
        <ResultsScreen 
          results={gameResults}
          onPlayAgain={handleStartGame}
          onBackToDashboard={() => setCurrentScreen('dashboard')}
        />
      )}
      {currentScreen === 'stats' && (
        <StatsPage onBack={() => setCurrentScreen('dashboard')} />
      )}
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
