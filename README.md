# The Imitation Game

Distributed multiplayer game platform where 7 players chat in real time and try to detect the hidden AI participant.

## Overview

The project is implemented with microservices, Docker Swarm orchestration, PostgreSQL replication, Redis-based rate limiting, and a React frontend.

Main gameplay flow:
- players join a room
- game starts when enough players are present
- chat round runs in real time
- voting phase eliminates a player
- game ends when humans eliminate AI or AI survives

## Runtime Architecture

Architecture diagram files:
- [documentation/architecture-diagram.svg](documentation/architecture-diagram.svg)
- [documentation/architecture-diagram.drawio](documentation/architecture-diagram.drawio)

Services in stack:
- `frontend` (React/Vite, port 3000 -> nginx 80)
- `kong` (API gateway, port 80)
- `auth-service` (FastAPI, JWT auth)
- `chat-service` (Spring Boot, 3 replicas)
- `reporting-service` (Spring Boot)
- `rate-limiter` (Spring Boot)
- `ai-bot-service` (Spring Boot worker/integration)
- `ollama` (LLM runtime)
- `db-master` + `read-replica-db` + `auth-db` (PostgreSQL)
- `redis`
- `prometheus`, `grafana`, `portainer`

## API Surface (Implemented)

### Auth Service (`/auth`)
- `GET /health`
- `GET /jwks`
- `POST /register`
- `POST /login`
- `POST /refresh`
- `GET /userinfo`
- `PUT /logout`
- `DELETE /delete-account`

### Chat Service (`/rooms`, `/profile`, WebSocket)
- `GET /rooms`
- `POST /rooms/create`
- `POST /rooms/join`
- `POST /rooms/{id}/join`
- `POST /rooms/{id}/leave`
- `POST /rooms/{id}/start`
- `POST /rooms/{id}/vote?targetId=...`
- `POST /rooms/{id}/voting/start`
- `POST /rooms/{id}/voting/end`
- `GET /rooms/{id}`
- `GET /rooms/{id}/messages`
- `PUT /rooms/{id}`
- `DELETE /rooms/{id}`
- `PUT /rooms/{roomId}/messages/{messageId}`
- `DELETE /rooms/{roomId}/messages/{messageId}`
- `GET /rooms/{id}/players/ids`
- `GET /rooms/{id}/results`
- `GET /profile/me`
- `PATCH /profile/me`
- `DELETE /profile/me`
- WebSocket/STOMP:
  - send: `/app/chat/{roomId}`
  - subscribe: `/topic/messages/{roomId}`, `/topic/game/{roomId}`

### Reporting Service (`/reports`)
- `GET /leaderboard`
- `GET /me/stats`
- `GET /player/{oderId}/stats`
- `GET /player/username/{username}/stats`
- `GET /global`
- `GET /games/recent`
- `GET /room/{roomId}/leaderboard`
- `GET /health`
- `PUT /settings`
- `DELETE /cache`

### Rate Limiter (`/rate-limit`)
- `GET /check`
- `GET /check/{userId}`
- `GET /status`
- `POST /reset/{userId}`
- `GET /internal/check/{userId}`
- `GET /health`

## Testing

Controller/API unit tests are added for REST services:
- `imitation-game/chat-service/src/test/java/imitationgame/chatservice/controller/RoomControllerTest.java`
- `imitation-game/chat-service/src/test/java/imitationgame/chatservice/controller/ProfileControllerTest.java`
- `imitation-game/reporting-service/src/test/java/imitationgame/reportingservice/controller/ReportingControllerTest.java`
- `imitation-game/rate-limiter/src/test/java/imitationgame/ratelimiter/controller/RateLimiterControllerTest.java`
- `imitation-game/auth-service/tests/test_auth_api.py`

Run Java tests:
```powershell
cd imitation-game/chat-service
.\mvnw.cmd clean test

cd ..\reporting-service
.\mvnw.cmd clean test

cd ..\rate-limiter
.\mvnw.cmd clean test
```

Run auth-service tests:
```powershell
cd imitation-game/auth-service
pytest
```

## Manual API Testing (Postman)

Import collection:
- [documentation/postman/The-Imitation-Game.postman_collection.json](documentation/postman/The-Imitation-Game.postman_collection.json)

Collection variables:
- `gatewayBase` (default `http://localhost`)
- `rateLimiterBase` (default `http://localhost:8082`)
- `token`, `refreshToken`, `roomId`, `messageId`, `userId`, `username`

## Deploy (Docker Swarm)

```powershell
cd imitation-game/docker
docker stack deploy -c stack.yml imitation-game
docker service ls
```

## Project Structure

```text
.
├── .github/
│   └── workflows/
│       └── ci-cd.yml
├── documentation/
│   ├── architecture-diagram.drawio
│   ├── architecture-diagram.svg
│   ├── idp_m1.md
│   ├── postman/
│   │   └── The-Imitation-Game.postman_collection.json
│   └── archive/
│       ├── Milestone1.md
│       └── diagram.mmd
├── imitation-game/
│   ├── ai-bot-service/
│   ├── auth-service/
│   ├── chat-service/
│   ├── docker/
│   ├── rate-limiter/
│   └── reporting-service/
├── imitation-game-fe/
└── README.md
```

Folder responsibilities:
- `documentation/`: project docs, architecture diagrams, Postman assets
- `imitation-game/`: backend services + deployment stack
- `imitation-game-fe/`: frontend application
- root `README.md`: entry point and operational guide
