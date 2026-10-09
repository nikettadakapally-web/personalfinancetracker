param(
    [switch]$InstallStartupTask,
    [switch]$Stop
)

$ErrorActionPreference = 'Stop'
$backend = Join-Path $PSScriptRoot 'backend\personalfinancetracker'
$jar = Join-Path $backend 'target\personalfinancetracker-0.0.1-SNAPSHOT.jar'
$dataDirectory = Join-Path $backend 'data'
$stateDirectory = Join-Path $env:LOCALAPPDATA 'FinanceDashboard'
$pidFile = Join-Path $stateDirectory 'server.pid'
$stdoutLog = Join-Path $stateDirectory 'server.log'
$stderrLog = Join-Path $stateDirectory 'server-error.log'
$port = 8081
$baseUrl = "http://127.0.0.1:$port"

function Get-FinanceDashboardLanAddresses {
    Get-NetIPAddress -AddressFamily IPv4 -ErrorAction Stop |
        Where-Object {
            $_.AddressState -eq 'Preferred' -and
            $_.IPAddress -notlike '127.*' -and
            $_.IPAddress -notlike '169.254.*' -and
            $_.InterfaceAlias -notmatch 'Loopback|vEthernet|VirtualBox|VMware'
        } |
        Select-Object -ExpandProperty IPAddress -Unique
}

function Get-FinanceDashboardJavaPath {
    if ($env:JAVA_HOME) {
        $javaFromHome = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $javaFromHome) {
            return $javaFromHome
        }
    }
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) {
        return $javaCommand.Source
    }
    return $null
}

function Write-FinanceDashboardAddresses {
    Write-Output ''
    Write-Output 'Wealth & Expense Intelligence is running.'
    Write-Output "This PC: $baseUrl"
    $addresses = @(Get-FinanceDashboardLanAddresses)
    if ($addresses.Count -eq 0) {
        Write-Warning 'No Wi-Fi/LAN IPv4 address was found. Connect this PC to your local network and rerun this script.'
    } else {
        Write-Output 'Open one of these links on a phone connected to the same Wi-Fi/LAN:'
        foreach ($address in $addresses) {
            Write-Output "  http://${address}:$port"
        }
    }
    Write-Output 'Keep this PC awake and allow TCP port 8081 through Windows Firewall on Private networks if prompted.'
}

if ($InstallStartupTask) {
    $powerShell = Join-Path $PSHOME 'powershell.exe'
    $action = New-ScheduledTaskAction -Execute $powerShell -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    $trigger = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
    $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit ([TimeSpan]::Zero)
    Register-ScheduledTask -TaskName 'Finance Dashboard Local Server' -Action $action -Trigger $trigger -Settings $settings -Description 'Starts the local finance dashboard when this Windows account signs in.' -Force | Out-Null
    Write-Output 'The finance dashboard will start automatically the next time you sign in to Windows.'
    exit 0
}

if ($Stop) {
    if (-not (Test-Path $pidFile)) {
        Write-Output 'No finance dashboard process ID was recorded.'
        exit 0
    }

    $recordedPid = [int](Get-Content $pidFile -Raw)
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $recordedPid"
    if ($process -and $process.CommandLine -and $process.CommandLine.Contains($jar)) {
        Stop-Process -Id $recordedPid
        Write-Output 'The finance dashboard has been stopped.'
    } else {
        Write-Output 'The recorded finance dashboard process is not running.'
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    exit 0
}

$null = New-Item -ItemType Directory -Force -Path $dataDirectory
$null = New-Item -ItemType Directory -Force -Path $stateDirectory

try {
    $health = Invoke-RestMethod -Uri "$baseUrl/health" -TimeoutSec 2
    if ($health.status -eq 'UP' -and $health.application -eq 'Wealth & Expense Intelligence') {
        Write-FinanceDashboardAddresses
        exit 0
    }
} catch {
    $health = $null
}

$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    throw "Port $port is already in use by another application. Stop it or configure SERVER_PORT before starting the finance dashboard."
}

if (-not (Test-Path $jar)) {
    $java = Get-FinanceDashboardJavaPath
    if (-not $java) {
        throw 'Java 17 is required. Install a Java 17 JDK and make java.exe available on PATH.'
    }
    Push-Location $backend
    try {
        & .\mvnw.cmd -B -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw "Finance dashboard build failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $jar)) {
    throw "The finance dashboard application JAR was not created: $jar"
}

$javaPath = Get-FinanceDashboardJavaPath
if (-not $javaPath) {
    throw 'Java 17 is required. Install a Java 17 JDK and make java.exe available on PATH.'
}

$process = Start-Process -FilePath $javaPath `
    -ArgumentList @('-XX:MaxRAMPercentage=75.0', '-jar', "`"$jar`"") `
    -WorkingDirectory $backend -WindowStyle Hidden -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog -PassThru
Set-Content -Path $pidFile -Value $process.Id

$ready = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    Start-Sleep -Seconds 1
    if ($process.HasExited) {
        throw "The finance dashboard exited during startup. Check $stdoutLog and $stderrLog for details."
    }
    try {
        $health = Invoke-RestMethod -Uri "$baseUrl/health" -TimeoutSec 2
        if ($health.status -eq 'UP') {
            $ready = $true
            break
        }
    } catch {
        # Continue waiting while Spring Boot initializes.
    }
}

if (-not $ready) {
    Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    throw "The finance dashboard did not become healthy within 60 seconds. Check $stdoutLog and $stderrLog."
}

Write-FinanceDashboardAddresses
