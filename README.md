# The Imitation Game - Distributed AI Detection Platform

A high-performance, scalable web platform where players identify an AI bot hidden among 7 participants in real-time chat rooms. Built with **microservices architecture**, **Docker Swarm**, and **distributed systems** best practices.

## Project Overview

**The Imitation Game** is a competitive multiplayer game where:
- 7 players join a chat room for multiple 2-minute rounds
- One participant is secretly an AI bot
- Players vote each round to eliminate who they think is the AI
- Eliminated players observe the game until completion
- Win by identifying the AI (humans) or hiding in plain sight (AI)

The platform demonstrates **concurrent processing**, **real-time communication**, **database replication**, and **distributed rate-limiting**.

---

## Architecture

### Services (9 Microservices)

| Service | Purpose | Technology | Replicas |
|---------|---------|-----------|----------|
| **chat-service** | Core game logic & WebSocket communication | Java Spring Boot | **3** |
| **ai-bot-service** | AI bot responses via Ollama LLM | Java Spring Boot | 1 |
| **rate-limiter** | Distributed DoS protection | Java Spring Boot + Redis | 1 |
| **reporting-service** | Game statistics & leaderboards | Java Spring Boot | 1 |
| **frontend** | React SPA user interface | React/Vite/TypeScript | 1 |
| **auth-server** | Centralized authentication (SSO) | Keycloak OAuth2 | 1 |
| **db-master** | Primary write database | PostgreSQL | 1 |
| **read-replica-db** | Read-only reporting database | PostgreSQL (streaming replication) | 1 |
| **redis** | Caching, rate-limiting, pub/sub | Redis | 1 |

### Network Architecture

The system implements a layered architecture with service isolation:

```
┌─────────────────────────────────────────────────────┐
│                    Frontend (React)                  │
└────────────────────┬────────────────────────────────┘
                     │ HTTP/WebSocket
                     ↓
┌─────────────────────────────────────────────────────┐
│          chat-service (3 replicas)                   │
│  - Game logic, WebSocket, authentication            │
└────┬──────────────┬──────────────┬──────────────────┘
     │              │              │
     ├→ db-master ──┐              ├→ ai-bot-service
     │              │              │
     │         (streaming           ├→ rate-limiter + Redis
     │         replication)    │
     │              │              └→ auth-server (Keycloak)
     │              ↓
     └─→ read-replica-db ←─ reporting-service
          (Read-only)
```

---

## Quick Start

### Prerequisites
- Docker & Docker Swarm (`docker swarm init`)
- Docker Compose (included with Docker Desktop)
- Minimum 8GB RAM recommended

### Deploy the Stack

```bash
cd c:\Users\User\Poli\SCD\Proiect\imitation-game\docker

docker stack deploy -c stack.yml imitation-game
```

Wait ~30 seconds for services to start:

```bash
docker service ls
```

### Access the Application

| Component | URL |
|-----------|-----|
| **Frontend** | http://localhost:3000 |
| **Keycloak Admin** | http://localhost:8080 → realm: `imitation-game` |
| **PostgreSQL** | localhost:5432 (user: `user`, password: `password`) |
| **Redis** | localhost:6379 |

---

## Core Features

### Authentication & Authorization
- **Keycloak SSO** with OAuth2 protocol
- JWT token-based validation
- Role-based access control (Player, Admin)
- Automatic user profile creation from SSO metadata

### Real-Time Game Logic
- **WebSocket** connections for instant communication
- 2-minute round timers with automatic transitions
- Vote counting with pessimistic locking (prevents race conditions)
- Automatic AI elimination logic
- Game state synchronization across 3 replicas

### Database Replication
- **PostgreSQL streaming replication** (master → read-replica)
- Write operations on master only
- Reporting queries offloaded to read-replica
- Ensures data consistency and high availability

### Distributed Rate Limiting
- **Redis-based** rate limiter (5 requests/second per user)
- Protects against DoS attacks
- Works across all 3 chat-service replicas
- Distributed counter synchronization

### Player Statistics
- Games played tracking
- Win/loss ratio
- AI detection accuracy
- XP system (50 XP wins, 30 XP correct votes)
- Leaderboard by votes received

---

## Game Flow

```
1. Player Joins Lobby
   ↓
2. Room Created (7 players max)
   ├─ 1 player assigned as AI (from pool)
   ├─ 6 players are humans
   └─ Game status: WAITING
   ↓
3. Game Starts → IN_PROGRESS
   ├─ Timer: 2 minutes per round
   ├─ Chat messages broadcasted via WebSocket
   └─ AI responds using Ollama LLM
   ↓
4. Voting Phase → VOTING
   ├─ Each player votes for suspected AI
   ├─ Votes counted with locking
   └─ Player with most votes eliminated
   ↓
5. Win Condition Check
   ├─ Humans win if AI eliminated
   ├─ AI wins if majority eliminated
   └─ Max 5 rounds then AI wins
   ↓
6. Game Ends → FINISHED
   ├─ Results fetched from API
   ├─ XP awarded
   └─ Statistics updated
```

---

## Implementation Details

### Mandatory Requirements Met

1. **Authentication & SSO**: Keycloak OAuth2
2. **Role Management**: User profiles with roles
3. **Database**: PostgreSQL with ORM (JPA/Hibernate)
4. **Docker Delivery**: Full stack deployment
5. **Microservices**: 9 containerized services
6. **Min 5 Components**: 9 services (6 custom)
7. **Docker Swarm**: Stack deployment
8. **DNS Service Discovery**: Environment variables, no hardcoding
9. **Network Security**: Isolated networks per component group
10. **Replication**: chat-service (3 replicas) + DB replication

### **Advanced Features**

| Feature | Implementation |
|---------|---|
| **Database Read Replicas** | PostgreSQL streaming replication + reporting-service |
| **Distributed Rate Limiting** | Redis counter sync across 3 replicas |
| **Real-Time Notifications** | WebSocket events (GAME_STARTED, PLAYER_ELIMINATED, GAME_ENDED) |
| **Asynchronous AI Processing** | Ollama LLM integration with request queuing |
| **Data Consistency** | Pessimistic locking in game state updates |
| **REST API** | Fully RESTful with authentication, game results endpoint |

---

## Project Structure

```
imitation-game/
├── chat-service/              # Main game service (3 replicas)
│   ├── src/main/java/
│   │   ├── controller/        # REST endpoints
│   │   ├── service/           # Game logic
│   │   ├── dto/               # Data transfer objects
│   │   ├── entity/            # JPA entities
│   │   └── config/            # Spring configuration
│   └── Dockerfile
├── ai-bot-service/            # AI bot service
├── rate-limiter/              # Rate limiting service
├── reporting-service/         # Statistics & reporting
├── docker/
│   ├── stack.yml              # Docker Swarm configuration
│   ├── build-images.ps1       # Build script
│   └── keycloak/              # Keycloak configuration
└── imitation-game-fe/         # React frontend
    ├── src/
    │   ├── components/        # React components
    │   ├── contexts/          # Auth context
    │   ├── services/          # API client
    │   └── styles/            # Tailwind CSS
    └── Dockerfile
```

---

## Testing

### Manual Testing
1. Open http://localhost:3000
2. Login with Keycloak credentials
3. Create a new game room
4. Join with another user (open private/incognito window)
5. Play 2+ rounds and verify:
   - Chat messages appear in real-time
   - Votes are counted correctly
   - Winner is determined
   - Statistics are updated
   - XP is awarded

### API Testing
```bash
# Get game results
curl -H "Authorization: Bearer <token>" \
  http://localhost/rooms/{roomId}/results

# Check rate limiting
for i in {1..10}; do
  curl -H "Authorization: Bearer <token>" \
    http://localhost/rooms -w "%{http_code}\n"
done
# Should get 429 (Too Many Requests) after 5th request
```

---

## Scaling Considerations

### Current Scale
- **7 concurrent games** with 3 replicas = ~200 max concurrent players
- **Database**: PostgreSQL handles 1000+ TPS with replication
- **Rate Limiting**: Redis handles 10,000+ requests/sec

### To Scale Further
1. Increase `chat-service` replicas: `docker service scale imitation-game_chat-service=5`
2. Add more PostgreSQL replicas for read-heavy workloads
3. Implement Redis clustering for higher throughput
4. Add load balancer (Nginx/HAProxy)

---

## Security Features

- JWT token validation on all endpoints
- Role-based access control (RBAC)
- Distributed rate limiting (DoS protection)
- Pessimistic locking for game state consistency
- Keycloak SSO for centralized authentication
- Docker networks isolation
- HTTPS ready (configurable)

---

## Database Schema

### Key Tables
- **user_profiles**: User accounts, roles, statistics
- **game_rooms**: Game instances, status, timestamps
- **room_players**: Player participation, votes, roles
- **message_log**: Chat messages history

All tables replicate from master to read-replica via PostgreSQL streaming replication.

---

## Development

### Build Services
```bash
cd imitation-game/chat-service
./mvnw.cmd clean package -DskipTests
docker build -t chat-service:1.0 .
docker service update --image chat-service:1.0 imitation-game_chat-service
```

### Build Frontend
```bash
cd imitation-game-fe
npm run build
docker build -t frontend:1.0 .
docker service update --image frontend:1.0 imitation-game_frontend
```