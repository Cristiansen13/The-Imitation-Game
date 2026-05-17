[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$ManagerVmName = 'imitation-game-manager',

    [Parameter(Mandatory = $false)]
    [string]$Worker1VmName = 'imitation-game-worker-1',

    [Parameter(Mandatory = $false)]
    [string]$Worker2VmName = 'imitation-game-worker-2',

    [Parameter(Mandatory = $false)]
    [int]$MemoryMB = 4096,

    [Parameter(Mandatory = $false)]
    [int]$CpuCount = 2,

    [Parameter(Mandatory = $false)]
    [int]$BootWaitSeconds = 300,

    [Parameter(Mandatory = $false)]
    [string]$DockerVersion = 'latest'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$multipassExe = 'C:\Program Files\Multipass\bin\multipass.exe'

function Write-Step {
    param([string]$Message)
    Write-Host "[swarm-multipass] $Message"
}

function Invoke-MultipassShell {
    param(
        [string]$VmName,
        [string]$Command
    )

    $result = & multipass exec $VmName -- powershell -Command $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed on '$VmName': $Command"
    }
    return $result
}

function Test-MultipassAvailable {
    if (-not (Test-Path $multipassExe)) {
        throw "Multipass not found at $multipassExe"
    }
}

Test-MultipassAvailable
Write-Step 'Multipass available'

$vmNames = @($ManagerVmName, $Worker1VmName, $Worker2VmName)

# Clean up any existing VMs
Write-Step 'Cleaning up any existing VMs'
foreach ($vm in $vmNames) {
    $existing = & $multipassExe list --format csv 2>$null | Select-String $vm
    if ($existing) {
        Write-Step "Removing existing VM '$vm'"
        & $multipassExe delete $vm --purge 2>$null
    }
}

Write-Step 'Waiting for Multipass mount cleanup'
Start-Sleep -Seconds 2

# Launch VMs
Write-Step "Launching manager VM '$ManagerVmName'"
& $multipassExe launch --name $ManagerVmName --cpus $CpuCount --memory ${MemoryMB}M --disk 20G 2>&1 | Out-Null

Write-Step "Launching worker VM '$Worker1VmName'"
& $multipassExe launch --name $Worker1VmName --cpus $CpuCount --memory ${MemoryMB}M --disk 20G 2>&1 | Out-Null

Write-Step "Launching worker VM '$Worker2VmName'"
& $multipassExe launch --name $Worker2VmName --cpus $CpuCount --memory ${MemoryMB}M --disk 20G 2>&1 | Out-Null

Write-Step 'All VMs launched, waiting for boot'
Start-Sleep -Seconds 10

# Get IPs
Write-Step 'Retrieving VM IP addresses'
$managerInfo = & $multipassExe info $ManagerVmName --format json | ConvertFrom-Json
$managerIp = $managerInfo.info.$ManagerVmName.ipv4[0]

$worker1Info = & $multipassExe info $Worker1VmName --format json | ConvertFrom-Json
$worker1Ip = $worker1Info.info.$Worker1VmName.ipv4[0]

$worker2Info = & $multipassExe info $Worker2VmName --format json | ConvertFrom-Json
$worker2Ip = $worker2Info.info.$Worker2VmName.ipv4[0]

Write-Host "[swarm-multipass] Manager IP: $managerIp"
Write-Host "[swarm-multipass] Worker 1 IP: $worker1Ip"
Write-Host "[swarm-multipass] Worker 2 IP: $worker2Ip"

# Install Docker on each VM
Write-Step 'Installing Docker on manager'
& $multipassExe exec $ManagerVmName -- bash -c 'sudo apt-get update -qq && sudo apt-get install -y docker.io 2>&1 | tail -5' 2>&1 | Out-Null

Write-Step 'Installing Docker on worker 1'
& $multipassExe exec $Worker1VmName -- bash -c 'sudo apt-get update -qq && sudo apt-get install -y docker.io 2>&1 | tail -5' 2>&1 | Out-Null

Write-Step 'Installing Docker on worker 2'
& $multipassExe exec $Worker2VmName -- bash -c 'sudo apt-get update -qq && sudo apt-get install -y docker.io 2>&1 | tail -5' 2>&1 | Out-Null

Write-Step 'Waiting for Docker daemon startup'
Start-Sleep -Seconds 5

# Add ubuntu user to docker group on each VM
Write-Step 'Adding ubuntu to docker group on manager'
& $multipassExe exec $ManagerVmName -- sudo usermod -aG docker ubuntu 2>&1 | Out-Null

Write-Step 'Adding ubuntu to docker group on worker 1'
& $multipassExe exec $Worker1VmName -- sudo usermod -aG docker ubuntu 2>&1 | Out-Null

Write-Step 'Adding ubuntu to docker group on worker 2'
& $multipassExe exec $Worker2VmName -- sudo usermod -aG docker ubuntu 2>&1 | Out-Null

Start-Sleep -Seconds 2

# Initialize Swarm on manager
Write-Step 'Initializing Docker Swarm on manager'
& $multipassExe exec $ManagerVmName -- docker swarm init --advertise-addr $managerIp 2>&1 | Out-Null

# Get worker join token
Write-Step 'Retrieving worker join token'
$workerToken = & $multipassExe exec $ManagerVmName -- docker swarm join-token -q worker
$workerToken = $workerToken.Trim()

Write-Step 'Joining worker 1 to swarm'
& $multipassExe exec $Worker1VmName -- docker swarm join --token $workerToken "${managerIp}:2377" 2>&1 | Out-Null

Write-Step 'Joining worker 2 to swarm'
& $multipassExe exec $Worker2VmName -- docker swarm join --token $workerToken "${managerIp}:2377" 2>&1 | Out-Null

Write-Step 'Listing swarm nodes'
& $multipassExe exec $ManagerVmName -- docker node ls

Write-Host "`n[swarm-multipass] Cluster ready!"
Write-Host "[swarm-multipass] Manager SSH: ssh ubuntu@$managerIp"
Write-Host "[swarm-multipass] Worker 1 SSH: ssh ubuntu@$worker1Ip"
Write-Host "[swarm-multipass] Worker 2 SSH: ssh ubuntu@$worker2Ip"
