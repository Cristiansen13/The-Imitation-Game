import { getToken } from './auth';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const REPORTING_API_URL = import.meta.env.VITE_REPORTING_API_URL || 'http://localhost:8083';

// Generic fetch wrapper with authentication
async function fetchWithAuth<T>(
  endpoint: string,
  options: RequestInit = {},
  baseUrl: string = API_BASE_URL
): Promise<T | null> {
  const token = getToken();
  
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };
  
  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }
  
  const response = await fetch(`${baseUrl}${endpoint}`, {
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

// Reporting API
export const reportingApi = {
  getMyStats: () => fetchWithAuth<PlayerStats>('/reports/me/stats', {}, REPORTING_API_URL),
  getPlayerStats: (oderId: string) => fetchWithAuth<PlayerStats>(`/reports/player/${oderId}/stats`, {}, REPORTING_API_URL),
  getLeaderboard: (limit: number = 10) => fetchWithAuth<LeaderboardResponse>(`/reports/leaderboard?limit=${limit}`, {}, REPORTING_API_URL),
  getGlobalStats: () => fetchWithAuth<GlobalStats>('/reports/global', {}, REPORTING_API_URL),
  getRecentGames: (limit: number = 20) => fetchWithAuth<GameSummary[]>(`/reports/games/recent?limit=${limit}`, {}, REPORTING_API_URL),
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
  
  endVoting: (roomId: string) => fetchWithAuth<void>(`/rooms/${roomId}/voting/end`, { method: 'POST' }),
  
  vote: (roomId: string, targetId: string) => 
    fetchWithAuth<void>(`/rooms/${roomId}/vote?targetId=${encodeURIComponent(targetId)}`, {
      method: 'POST',
    }),
  
  getResults: (roomId: string) => fetchWithAuth<GameResults>(`/rooms/${roomId}/results`),
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

// Reporting Types
export interface PlayerStats {
  id: string;
  username: string;
  gamesPlayed: number;
  gamesWonAsHuman: number;
  gamesWonAsAI: number;
  totalWins: number;
  wins?: number; // For compatibility
  correctAIIdentifications: number;
  winRate: number;
  detectRate: number;
  experiencePoints?: number;
}

export interface LeaderboardResponse {
  topByWins: PlayerStats[];
  topByWinRate: PlayerStats[];
  topByDetectRate: PlayerStats[];
  mostActive: PlayerStats[];
  topByXP: PlayerStats[];
}

export interface GlobalStats {
  totalPlayers: number;
  totalGamesPlayed: number;
  activeGames: number;
  gamesLast24Hours: number;
  averageWinRate: number;
  averageDetectRate: number;
}

export interface GameSummary {
  id: string;
  name: string;
  rounds: number;
  startedAt: string;
  endedAt: string;
  winnerId?: string;
  winCondition?: string;
}

export interface GameResults {
  roomId: string;
  winnerId: string;
  winCondition: string;
  aiPlayerId: string;
  aiUsername: string;
  players: PlayerResult[];
}

export interface PlayerResult {
  oderId: string;
  username: string;
  votesReceived: number;
  votedFor: string | null;
  isAI: boolean;
  status: string;
}

export default {
  profile: profileApi,
  room: roomApi,
  reporting: reportingApi,
};
