# Build and load Docker images into Multipass Swarm
# This script builds all service images locally, exports them, copies to VMs, and loads them

param(
    [string]$WorkspacePath = "c:\Users\User\Poli\SCD\Proiect",
    [array]$VMs = @("imitation-game-manager", "imitation-game-worker-1", "imitation-game-worker-2")
)

function Write-Step {
    param([string]$Message)
    Write-Host "[build-images] $Message" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "[build-images] OK $Message" -ForegroundColor Green
}

function Write-ErrorMsg {
    param([string]$Message)
    Write-Host "[build-images] FAIL $Message" -ForegroundColor Red
}

# Image definitions: @{Name="image:tag"; Context="relative/path/to/dockerfile/dir"}
$Images = @(
    @{Name = "ai-bot-service:1.0"; Context = "$WorkspacePath\imitation-game\ai-bot-service"}
    @{Name = "auth-service:1.0"; Context = "$WorkspacePath\imitation-game\auth-service"}
    @{Name = "chat-service:1.4"; Context = "$WorkspacePath\imitation-game\chat-service"}
    @{Name = "reporting-service:1.0"; Context = "$WorkspacePath\imitation-game\reporting-service"}
    @{Name = "rate-limiter:1.0"; Context = "$WorkspacePath\imitation-game\rate-limiter"}
    @{Name = "imitation-game-fe:1.4"; Context = "$WorkspacePath\imitation-game-fe"}
)

$ExportDir = "$WorkspacePath\docker-exports"
$MultipassPath = "C:\Program Files\Multipass\bin\multipass.exe"

# Ensure Docker is available
Write-Step "Checking Docker availability..."
try {
    $dockerVersion = docker version --format '{{.Server.Version}}' 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorMsg "Docker is not running or not available"
        exit 1
    }
    Write-Success "Docker available: $dockerVersion"
} catch {
    Write-ErrorMsg "Docker check failed: $_"
    exit 1
}

# Create export directory
Write-Step "Creating export directory: $ExportDir"
if (Test-Path $ExportDir) {
    Remove-Item $ExportDir -Recurse -Force
}
New-Item -ItemType Directory -Path $ExportDir | Out-Null

# Build all images
Write-Step "Building service Docker images..."
foreach ($image in $Images) {
    $imageName = $image.Name
    $contextPath = $image.Context
    
    if (-not (Test-Path $contextPath)) {
        Write-ErrorMsg "Context path not found: $contextPath"
        continue
    }
    
    Write-Step "  Building: $imageName"
    $buildCmd = "docker build -t $imageName -f `"$contextPath\Dockerfile`" `"$contextPath`""
    Invoke-Expression $buildCmd
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Built $imageName"
    } else {
        Write-ErrorMsg "Failed to build $imageName"
        exit 1
    }
}

# Export images as tar files
Write-Step "Exporting images to tar files..."
foreach ($image in $Images) {
    $imageName = $image.Name
    $fileName = $imageName -replace ':', '-' -replace '/', '-'
    $tarFile = "$ExportDir\$fileName.tar"
    
    Write-Step "  Exporting: $imageName"
    docker save -o $tarFile $imageName
    
    if ($LASTEXITCODE -eq 0) {
        $fileSize = (Get-Item $tarFile).Length / 1MB
        $sizeRounded = [Math]::Round($fileSize, 2)
        Write-Success "Exported $imageName - Size: $sizeRounded MB"
    } else {
        Write-ErrorMsg "Failed to export $imageName"
        exit 1
    }
}

# Copy images to VMs and load them
Write-Step "Copying images to Multipass VMs and loading..."
foreach ($vm in $VMs) {
    Write-Step "Processing VM: $vm"
    
    # Create remote directory
    & $MultipassPath exec $vm -- mkdir -p /home/ubuntu/docker-exports
    
    # Copy all tar files
    foreach ($tarFile in Get-ChildItem $ExportDir -Filter "*.tar") {
        Write-Step "  Copying: $($tarFile.Name)"
        & $MultipassPath transfer $tarFile.FullName "${vm}:/home/ubuntu/docker-exports/"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Transferred $($tarFile.Name)"
        } else {
            Write-ErrorMsg "Failed to transfer $($tarFile.Name)"
            exit 1
        }
    }
    
    # Load all images
    Write-Step "  Loading images on $vm"
    $loadCmd = 'for tarfile in /home/ubuntu/docker-exports/*.tar; do docker load -i "$tarfile"; done'
    & $MultipassPath exec $vm -- bash -c $loadCmd
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Loaded all images on $vm"
    } else {
        Write-ErrorMsg "Failed to load images on $vm"
        exit 1
    }
}

# Verify images are available on all VMs
Write-Step "Verifying images on all VMs..."
foreach ($vm in $VMs) {
    Write-Step "Checking images on $vm..."
    $output = & $MultipassPath exec $vm -- docker images --format "table {{.Repository}}:{{.Tag}}"
    
    $loadedImages = $output | Where-Object { $_ -match "(ai-bot-service|auth-service|chat-service|reporting-service|rate-limiter|imitation-game-fe)" }
    Write-Success "Found $($loadedImages.Count) application images on $vm"
}

# Redeploy the stack
Write-Step "Redeploying Docker Stack..."
& $MultipassPath exec imitation-game-manager -- docker stack deploy -c /home/ubuntu/docker/stack.yml imitation-game

if ($LASTEXITCODE -eq 0) {
    Write-Success "Stack redeployed successfully"
} else {
    Write-ErrorMsg "Failed to redeploy stack"
    exit 1
}

# Wait for services to stabilize
$waitSeconds = 60
Write-Step "Waiting $waitSeconds seconds for services to start..."
Start-Sleep -Seconds $waitSeconds

# Show final status
Write-Step "Final service status:"
& $MultipassPath exec imitation-game-manager -- docker service ls

Write-Success "All operations completed!"
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "- Monitor service deployment: docker service ps imitation-game-SERVICE"
Write-Host "- Access Kong: http://172.23.175.197 (or check multipass list for current IP)"
Write-Host "- Access Portainer: http://172.23.175.197:9000"
Write-Host "- Check logs: docker service logs imitation-game-SERVICE"
