# Build all Docker images for Docker Swarm deployment
# Run this script before deploying to Swarm

Write-Host "Building chat-service..." -ForegroundColor Cyan
Set-Location ..\chat-service
.\mvnw.cmd clean package -DskipTests
docker build -t chat-service:1.0 .

Write-Host "Building rate-limiter..." -ForegroundColor Cyan
Set-Location ..\rate-limiter
.\mvnw.cmd clean package -DskipTests
docker build -t rate-limiter:1.0 .

Write-Host "Building reporting-service..." -ForegroundColor Cyan
Set-Location ..\reporting-service
.\mvnw.cmd clean package -DskipTests
docker build -t reporting-service:1.0 .

Write-Host "Building frontend..." -ForegroundColor Cyan
Set-Location ..\..\imitation-game-fe
npm install
npm run build
docker build -t imitation-game-fe:1.0 .

Write-Host "All images built successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "To deploy to Docker Swarm:" -ForegroundColor Yellow
Write-Host "1. Initialize Swarm (if not done): docker swarm init"
Write-Host "2. Deploy the stack: docker stack deploy -c stack.yml imitation-game"
Write-Host "3. Check services: docker service ls"
Write-Host "4. View logs: docker service logs imitation-game_chat-service"

Set-Location ..\imitation-game\docker
