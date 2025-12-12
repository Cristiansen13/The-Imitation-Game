@echo off
echo Building AI Bot Service...

cd /d "%~dp0"

REM Build with Maven
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo Maven build failed!
    exit /b %ERRORLEVEL%
)

REM Build Docker image
docker build -t ai-bot-service:1.0 .

if %ERRORLEVEL% NEQ 0 (
    echo Docker build failed!
    exit /b %ERRORLEVEL%
)

echo AI Bot Service built successfully!
