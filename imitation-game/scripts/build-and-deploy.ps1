# =============================================================================
# build-and-deploy.ps1
# Builds all Docker images locally, transfers them to Multipass VMs,
# and deploys the Docker Swarm stack.
# =============================================================================

[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$ManagerVmName = 'imitation-game-manager',

    [Parameter(Mandatory = $false)]
    [string]$Worker1VmName = 'imitation-game-worker-1',

    [Parameter(Mandatory = $false)]
    [string]$Worker2VmName = 'imitation-game-worker-2'
)

$ErrorActionPreference = 'Continue'

$multipassExe = 'C:\Program Files\Multipass\bin\multipass.exe'
$projectRoot  = (Resolve-Path (Join-Path $PSScriptRoot '..\..'))
$dockerDir    = Join-Path $projectRoot 'imitation-game\docker'
$tempDir      = Join-Path $projectRoot 'imitation-game\scripts\_image-tars'

# Image definitions: name, tag, build context relative to project root
$images = @(
    @{ Name = 'ai-bot-service';    Tag = '1.0'; Context = 'imitation-game\ai-bot-service' },
    @{ Name = 'auth-service';      Tag = '1.0'; Context = 'imitation-game\auth-service' },
    @{ Name = 'chat-service';      Tag = '1.4'; Context = 'imitation-game\chat-service' },
    @{ Name = 'reporting-service'; Tag = '1.0'; Context = 'imitation-game\reporting-service' },
    @{ Name = 'rate-limiter';      Tag = '1.0'; Context = 'imitation-game\rate-limiter' },
    @{ Name = 'imitation-game-fe'; Tag = '1.4'; Context = 'imitation-game-fe' }
)

function Write-Step {
    param([string]$Message)
    Write-Host "`n[build-deploy] $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[build-deploy] $Message" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Message)
    Write-Host "[build-deploy] FAIL $Message" -ForegroundColor Red
}

# ---- Preflight checks -------------------------------------------------------
if (-not (Test-Path $multipassExe)) {
    throw "Multipass not found at $multipassExe"
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker CLI not found on PATH"
}

# ---- Step 1: Build all Docker images ----------------------------------------
Write-Step "PHASE 1 — Building Docker images"
$failedBuilds = [System.Collections.ArrayList]::new()

foreach ($img in $images) {
    $fullTag  = "$($img.Name):$($img.Tag)"
    $ctxPath  = Join-Path $projectRoot $img.Context
    Write-Step "Building $fullTag from $ctxPath"

    docker build -t $fullTag $ctxPath 2>&1 | ForEach-Object { Write-Host "  $_" }
    if ($LASTEXITCODE -ne 0) {
        Write-Fail "Failed to build $fullTag"
        [void]$failedBuilds.Add($fullTag)
    } else {
        Write-Ok "OK $fullTag"
    }
}

if ($failedBuilds.Count -gt 0) {
    Write-Fail "The following images failed to build: $($failedBuilds -join ', ')"
    Write-Host "Fix the build errors above and re-run this script." -ForegroundColor Yellow
    exit 1
}
Write-Ok "All images built successfully!"

# ---- Step 2: Save images to tar files ---------------------------------------
Write-Step "PHASE 2 — Saving images to tar archives"
if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

foreach ($img in $images) {
    $fullTag = "$($img.Name):$($img.Tag)"
    $tarFile = Join-Path $tempDir "$($img.Name).tar"
    Write-Step "Saving $fullTag -> $tarFile"
    docker save -o $tarFile $fullTag
    if ($LASTEXITCODE -ne 0) { throw "Failed to save $fullTag" }
    Write-Ok "OK saved $($img.Name).tar"
}

# ---- Step 3: Transfer images to all VMs -------------------------------------
Write-Step "PHASE 3 — Transferring images to Multipass VMs"
$vmNames = @($ManagerVmName, $Worker1VmName, $Worker2VmName)

foreach ($vm in $vmNames) {
    Write-Step "Transferring images to $vm"
    # Create destination directory on VM
    & $multipassExe exec $vm -- mkdir -p /tmp/docker-images

    foreach ($img in $images) {
        $tarFile = Join-Path $tempDir "$($img.Name).tar"
        Write-Host "  Sending $($img.Name).tar to $vm..." -NoNewline
        & $multipassExe transfer $tarFile "${vm}:/tmp/docker-images/$($img.Name).tar"
        if ($LASTEXITCODE -ne 0) { throw "Failed to transfer $($img.Name).tar to $vm" }
        Write-Host " done" -ForegroundColor Green
    }
}

# ---- Step 4: Load images on all VMs -----------------------------------------
Write-Step "PHASE 4 — Loading images on VMs"
foreach ($vm in $vmNames) {
    Write-Step "Loading images on $vm"
    foreach ($img in $images) {
        $fullTag = "$($img.Name):$($img.Tag)"
        Write-Host "  Loading $fullTag on $vm..." -NoNewline
        & $multipassExe exec $vm -- sudo docker load -i "/tmp/docker-images/$($img.Name).tar" 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Failed to load $fullTag on $vm" }
        Write-Host " done" -ForegroundColor Green
    }

    # Clean up tar files on the VM
    & $multipassExe exec $vm -- rm -rf /tmp/docker-images
}

# ---- Step 5: Deploy/update the stack ----------------------------------------
Write-Step "PHASE 5 — Deploying Docker Swarm stack"

# Transfer stack config and supporting files to manager
Write-Step "Transferring stack configuration to $ManagerVmName"
& $multipassExe exec $ManagerVmName -- mkdir -p /tmp/stack-deploy/kong
& $multipassExe exec $ManagerVmName -- mkdir -p /tmp/stack-deploy/prometheus
& $multipassExe exec $ManagerVmName -- mkdir -p /tmp/stack-deploy/grafana/provisioning/datasources
& $multipassExe exec $ManagerVmName -- mkdir -p /tmp/stack-deploy/grafana/provisioning/dashboards
& $multipassExe exec $ManagerVmName -- mkdir -p /tmp/stack-deploy/grafana/dashboards

& $multipassExe transfer (Join-Path $dockerDir 'stack.yml') "${ManagerVmName}:/tmp/stack-deploy/stack.yml"
& $multipassExe transfer (Join-Path $dockerDir 'kong\kong.yml') "${ManagerVmName}:/tmp/stack-deploy/kong/kong.yml"
& $multipassExe transfer (Join-Path $dockerDir 'prometheus\prometheus.yml') "${ManagerVmName}:/tmp/stack-deploy/prometheus/prometheus.yml"

# Transfer Grafana configs
$grafanaDsPath = Join-Path $dockerDir 'grafana\provisioning\datasources\prometheus.yml'
$grafanaDashProvPath = Join-Path $dockerDir 'grafana\provisioning\dashboards\dashboards.yml'
$grafanaDashJsonPath = Join-Path $dockerDir 'grafana\dashboards\spring-boot.json'

if (Test-Path $grafanaDsPath) {
    & $multipassExe transfer $grafanaDsPath "${ManagerVmName}:/tmp/stack-deploy/grafana/provisioning/datasources/prometheus.yml"
}
if (Test-Path $grafanaDashProvPath) {
    & $multipassExe transfer $grafanaDashProvPath "${ManagerVmName}:/tmp/stack-deploy/grafana/provisioning/dashboards/dashboards.yml"
}
if (Test-Path $grafanaDashJsonPath) {
    & $multipassExe transfer $grafanaDashJsonPath "${ManagerVmName}:/tmp/stack-deploy/grafana/dashboards/spring-boot.json"
}

# Deploy the stack from the manager VM
Write-Step "Running docker stack deploy on $ManagerVmName"
& $multipassExe exec $ManagerVmName -- bash -c 'cd /tmp/stack-deploy; sudo docker stack deploy -c stack.yml imitation-game'
if ($LASTEXITCODE -ne 0) { throw "docker stack deploy failed" }

# Wait a moment and show status
Start-Sleep -Seconds 5
Write-Step "Stack service status:"
& $multipassExe exec $ManagerVmName -- sudo docker service ls

# ---- Step 6: Cleanup local tar files ----------------------------------------
Write-Step "PHASE 6 — Cleaning up local tar files"
if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }

# ---- Done --------------------------------------------------------------------
Write-Host "`n" -NoNewline
Write-Host "============================================================" -ForegroundColor Green
Write-Host "  Deployment complete!                                      " -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Yellow
Write-Host "  Check services:  multipass exec $ManagerVmName -- sudo docker service ls"
Write-Host "  Service logs:    multipass exec $ManagerVmName -- sudo docker service logs imitation-game_chat-service"
Write-Host "  Stack remove:    multipass exec $ManagerVmName -- sudo docker stack rm imitation-game"
Write-Host ""
