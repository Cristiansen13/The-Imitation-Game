import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import {
  initAuth,
  login as apiLogin,
  logout as apiLogout,
  register as apiRegister,
  isAuthenticated,
  getUserInfo,
  getToken,
} from '../services/auth';

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
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  getToken: () => string | undefined;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshUser = () => {
    const info = getUserInfo();
    setUser(
      info
        ? { id: info.id, username: info.username, email: info.email, name: info.name, roles: info.roles }
        : null,
    );
  };

  useEffect(() => {
    initAuth()
      .catch((err) => console.error('Failed to initialize auth', err))
      .finally(() => {
        refreshUser();
        setIsLoading(false);
      });
  }, []);

  const handleLogin = async (username: string, password: string) => {
    await apiLogin(username, password);
    refreshUser();
  };

  const handleRegister = async (username: string, email: string, password: string) => {
    await apiRegister(username, email, password);
    await apiLogin(username, password);
    refreshUser();
  };

  const handleLogout = () => {
    apiLogout();
    setUser(null);
  };

  const value: AuthContextType = {
    user,
    isLoading,
    isLoggedIn: isAuthenticated(),
    login: handleLogin,
    register: handleRegister,
    logout: handleLogout,
    getToken,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
