import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getToken } from './auth';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

export interface ChatMessage {
  oderId: string;
  userId?: string;  // Backend might send this instead
  username: string;
  message: string;
  timestamp: string;
}

export interface GameEvent {
  type: 'PLAYER_JOINED' | 'PLAYER_LEFT' | 'GAME_STARTED' | 'ROUND_STARTED' | 'VOTING_STARTED' | 'PLAYER_ELIMINATED' | 'GAME_ENDED';
  data: any;
}

type MessageCallback = (message: ChatMessage) => void;
type EventCallback = (event: GameEvent) => void;

class WebSocketService {
  private client: Client | null = null;
  private messageCallbacks: Map<string, MessageCallback[]> = new Map();
  private eventCallbacks: Map<string, EventCallback[]> = new Map();
  private messageSubscriptions: Map<string, StompSubscription> = new Map();
  private eventSubscriptions: Map<string, StompSubscription> = new Map();
  private connected = false;

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      const token = getToken();
      
      this.client = new Client({
        webSocketFactory: () => new SockJS(WS_URL),
        connectHeaders: token ? { 
          'Authorization': `Bearer ${token}`,
          'X-Authorization': token  // Fallback header
        } : {},
        debug: (str) => {
          console.log('STOMP: ' + str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      this.client.onConnect = () => {
        console.log('WebSocket connected');
        this.connected = true;
        resolve();
      };

      this.client.onStompError = (frame) => {
        console.error('STOMP error', frame);
        console.error('Error details:', frame.headers, frame.body);
        reject(new Error(frame.headers['message']));
      };

      this.client.onDisconnect = () => {
        console.log('WebSocket disconnected');
        this.connected = false;
      };

      this.client.activate();
    });
  }

  disconnect(): void {
    if (this.client) {
      // Unsubscribe all subscriptions
      this.messageSubscriptions.forEach(sub => sub.unsubscribe());
      this.eventSubscriptions.forEach(sub => sub.unsubscribe());
      this.messageSubscriptions.clear();
      this.eventSubscriptions.clear();
      
      this.client.deactivate();
      this.client = null;
      this.connected = false;
      this.messageCallbacks.clear();
      this.eventCallbacks.clear();
    }
  }

  isConnected(): boolean {
    return this.connected;
  }

  // Subscribe to chat messages in a room
  subscribeToRoom(roomId: string, onMessage: MessageCallback): () => void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return () => {};
    }

    const topic = `/topic/messages/${roomId}`;
    
    // Store callback
    if (!this.messageCallbacks.has(roomId)) {
      this.messageCallbacks.set(roomId, []);
    }
    this.messageCallbacks.get(roomId)!.push(onMessage);

    // Only create one subscription per topic - reuse if exists
    if (!this.messageSubscriptions.has(roomId)) {
      console.log(`[WebSocket] Creating new subscription for ${topic}`);
      const subscription = this.client.subscribe(topic, (message: IMessage) => {
        console.log(`[WebSocket] Message received on ${topic}:`, message.body);
        const chatMessage: ChatMessage = JSON.parse(message.body);
        this.messageCallbacks.get(roomId)?.forEach(cb => cb(chatMessage));
      });
      this.messageSubscriptions.set(roomId, subscription);
    } else {
      console.log(`[WebSocket] Reusing existing subscription for ${topic}`);
    }

    // Return unsubscribe function
    return () => {
      const callbacks = this.messageCallbacks.get(roomId);
      if (callbacks) {
        const index = callbacks.indexOf(onMessage);
        if (index > -1) {
          callbacks.splice(index, 1);
        }
        // Only unsubscribe from STOMP when no more callbacks
        if (callbacks.length === 0) {
          const sub = this.messageSubscriptions.get(roomId);
          if (sub) {
            console.log(`[WebSocket] Unsubscribing from ${topic}`);
            sub.unsubscribe();
            this.messageSubscriptions.delete(roomId);
          }
        }
      }
    };
  }

  // Subscribe to game events in a room
  subscribeToGameEvents(roomId: string, onEvent: EventCallback): () => void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return () => {};
    }

    const topic = `/topic/room/${roomId}`;
    
    // Store callback
    if (!this.eventCallbacks.has(roomId)) {
      this.eventCallbacks.set(roomId, []);
    }
    this.eventCallbacks.get(roomId)!.push(onEvent);

    // Only create one subscription per topic - reuse if exists
    if (!this.eventSubscriptions.has(roomId)) {
      console.log(`[WebSocket] Creating new subscription for ${topic}`);
      const subscription = this.client.subscribe(topic, (message: IMessage) => {
        const event: GameEvent = JSON.parse(message.body);
        console.log('Room event received:', event);
        this.eventCallbacks.get(roomId)?.forEach(cb => cb(event));
      });
      this.eventSubscriptions.set(roomId, subscription);
    } else {
      console.log(`[WebSocket] Reusing existing subscription for ${topic}`);
    }

    // Return unsubscribe function
    return () => {
      const callbacks = this.eventCallbacks.get(roomId);
      if (callbacks) {
        const index = callbacks.indexOf(onEvent);
        if (index > -1) {
          callbacks.splice(index, 1);
        }
        // Only unsubscribe from STOMP when no more callbacks
        if (callbacks.length === 0) {
          const sub = this.eventSubscriptions.get(roomId);
          if (sub) {
            console.log(`[WebSocket] Unsubscribing from ${topic}`);
            sub.unsubscribe();
            this.eventSubscriptions.delete(roomId);
          }
        }
      }
    };
  }

  // Subscribe to user-specific game events (for AI role assignment)
  subscribeToUserQueue(userId: string, onEvent: EventCallback): () => void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return () => {};
    }

    const queue = `/user/${userId}/queue/game`;
    console.log('Subscribing to user queue:', queue);
    
    const subscription = this.client.subscribe(queue, (message: IMessage) => {
      const event: GameEvent = JSON.parse(message.body);
      console.log('User queue event received:', event);
      onEvent(event);
    });

    return () => {
      subscription.unsubscribe();
    };
  }

  // Send a chat message
  sendMessage(roomId: string, userId: string, username: string, message: string): void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return;
    }

    console.log(`[WebSocket] Sending message to /app/chat/${roomId}`, { userId, username, message });

    this.client.publish({
      destination: `/app/chat/${roomId}`,
      body: JSON.stringify({
        userId,  // Backend expects userId
        username,
        message,
      }),
    });
  }

  // Send a vote
  sendVote(roomId: string, oderId: string, targetUserId: string): void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return;
    }

    this.client.publish({
      destination: `/app/vote/${roomId}`,
      body: JSON.stringify({
        oderId,
        targetUserId,
      }),
    });
  }
}

// Singleton instance
export const wsService = new WebSocketService();
export default wsService;
