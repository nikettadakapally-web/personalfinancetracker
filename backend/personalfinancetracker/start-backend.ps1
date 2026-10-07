$ErrorActionPreference = 'Stop'
$port = 8081
$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1

if ($listener) {
    Write-Output "Finance backend is already listening on port $port."
    exit 0
}

$javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Process')
if (-not $javaHome) {
    $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
}
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
    throw 'Java 17 is required. Set JAVA_HOME to a JDK 17 installation.'
}

$env:JAVA_HOME = $javaHome
$env:Path = "$(Join-Path $javaHome 'bin');$env:Path"
Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd spring-boot:run
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}