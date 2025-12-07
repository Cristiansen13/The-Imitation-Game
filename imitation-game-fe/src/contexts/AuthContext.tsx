import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { initKeycloak, login, logout, isAuthenticated, getUserInfo, getToken } from '../services/auth';

interface User {
  id: string;
  username: string;
  email: string;
  name: string;
  roles: string[];
}

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isLoggedIn: boolean;
  login: () => void;
  logout: () => void;
  getToken: () => string | undefined;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const init = async () => {
      try {
        const authenticated = await initKeycloak();
        if (authenticated) {
          const userInfo = getUserInfo();
          if (userInfo) {
            setUser({
              id: userInfo.id || '',
              username: userInfo.username || '',
              email: userInfo.email || '',
              name: userInfo.name || userInfo.username || '',
              roles: userInfo.roles || [],
            });
          }
        }
      } catch (error) {
        console.error('Failed to initialize auth', error);
      } finally {
        setIsLoading(false);
      }
    };

    init();
  }, []);

  const handleLogout = () => {
    setUser(null);
    logout();
  };

  const value: AuthContextType = {
    user,
    isLoading,
    isLoggedIn: isAuthenticated(),
    login,
    logout: handleLogout,
    getToken,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
