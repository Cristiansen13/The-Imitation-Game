import { getToken } from './auth';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Generic fetch wrapper with authentication
async function fetchWithAuth<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T | null> {
  const token = getToken();
  
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };
  
  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }
  
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });
  
  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Unauthorized - please login again');
    }
    throw new Error(`API Error: ${response.status} ${response.statusText}`);
  }
  
  // Handle empty responses
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

// Profile API
export const profileApi = {
  getMe: () => fetchWithAuth<UserProfile>('/profile/me'),
};

// Room API
export const roomApi = {
  create: () => fetchWithAuth<GameRoom>('/rooms/create', { method: 'POST' }),
  
  join: (roomId: string) => fetchWithAuth<GameRoom>(`/rooms/${roomId}/join`, { method: 'POST' }),
  
  joinAny: () => fetchWithAuth<GameRoom>('/rooms/join', { method: 'POST' }),
  
  leave: (roomId: string) => fetchWithAuth<void>(`/rooms/${roomId}/leave`, { method: 'POST' }),
  
  get: (roomId: string) => fetchWithAuth<GameRoom>(`/rooms/${roomId}`),
  
  getMessages: (roomId: string) => fetchWithAuth<MessageLog[]>(`/rooms/${roomId}/messages`),
  
  listAvailable: () => fetchWithAuth<GameRoom[]>('/rooms'),
  
  startGame: (roomId: string) => fetchWithAuth<GameRoom>(`/rooms/${roomId}/start`, { method: 'POST' }),
  
  startVoting: (roomId: string) => fetchWithAuth<void>(`/rooms/${roomId}/voting/start`, { method: 'POST' }),
  
  vote: (roomId: string, targetId: string) => 
    fetchWithAuth<void>(`/rooms/${roomId}/vote?targetId=${encodeURIComponent(targetId)}`, {
      method: 'POST',
    }),
};

// Types
export interface UserProfile {
  id: string;
  username: string;
  email: string;
  role: string;
  gamesPlayed?: number;
  gamesWon?: number;
  detectRate?: number;
}

export interface GameRoom {
  id: string;
  status: 'WAITING' | 'IN_PROGRESS' | 'VOTING' | 'FINISHED';
  players?: RoomPlayer[];
  currentRound?: number;
  maxRounds?: number;
  roundEndTime?: string;
  aiPlayerId?: string;
  createdAt: string;
}

export interface RoomPlayer {
  id: string;
  oderId: string;
  username: string;
  status: 'ALIVE' | 'ELIMINATED';
  isAI?: boolean;
  votes?: number;
}

export interface MessageLog {
  id: string;
  roomId: string;
  oderId: string;
  username?: string;
  message: string;
  timestamp: string;
}

export default {
  profile: profileApi,
  room: roomApi,
};
