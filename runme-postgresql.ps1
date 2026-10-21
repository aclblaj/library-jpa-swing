$env:HIBERNATE_DB_URL = 'jdbc:postgresql://192.168.56.101:5432/library_db?sslmode=disable'
$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
Set-Location -LiteralPath $projectRoot

$javaHome = 'C:\Software\Java\jdk-17'
$javaExe = Join-Path $javaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
    throw "JDK 17 not found at '$javaHome'. Update `$javaHome in runme-postgresql.ps1."
}

$dbUrl = $env:HIBERNATE_DB_URL
if ([string]::IsNullOrWhiteSpace($dbUrl)) {
    $dbUrl = Read-Host 'Local test PostgreSQL JDBC URL (example: jdbc:postgresql://localhost:5432/library_db)'
}
# if ($dbUrl -notmatch '(?i)^jdbc:postgresql://(localhost|127\.0\.0\.1|\[::1\])(?::\d+)?/') {
#     throw 'This script only allows a loopback PostgreSQL URL. Restore an isolated test database locally; do not connect to production.'
# }

$confirmation = Read-Host 'Type TEST to confirm this is an isolated PostgreSQL test database'
if ($confirmation -cne 'TEST') {
    throw 'Startup cancelled. No database connection was made.'
}

$username = $env:HIBERNATE_DB_USERNAME
if ([string]::IsNullOrWhiteSpace($username)) {
    $username = Read-Host 'PostgreSQL username'
}
if ([string]::IsNullOrWhiteSpace($username)) {
    throw 'A PostgreSQL username is required.'
}

$password = $env:HIBERNATE_DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($password)) {
    $securePassword = Read-Host 'PostgreSQL password' -AsSecureString
    $password = ([System.Net.NetworkCredential]::new('', $securePassword)).Password
}
if ([string]::IsNullOrWhiteSpace($password)) {
    throw 'A PostgreSQL password is required.'
}

$environmentNames = @(
    'JAVA_HOME',
    'Path',
    'HIBERNATE_DB_URL',
    'HIBERNATE_DB_USERNAME',
    'HIBERNATE_DB_PASSWORD'
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [System.Environment]::GetEnvironmentVariable(
        $name,
        [System.EnvironmentVariableTarget]::Process
    )
}

$exitCode = 0
try {
    [System.Environment]::SetEnvironmentVariable('JAVA_HOME', $javaHome, [System.EnvironmentVariableTarget]::Process)
    [System.Environment]::SetEnvironmentVariable('Path', "$javaHome\bin;$env:Path", [System.EnvironmentVariableTarget]::Process)
    [System.Environment]::SetEnvironmentVariable('HIBERNATE_DB_URL', $dbUrl, [System.EnvironmentVariableTarget]::Process)
    [System.Environment]::SetEnvironmentVariable('HIBERNATE_DB_USERNAME', $username, [System.EnvironmentVariableTarget]::Process)
    [System.Environment]::SetEnvironmentVariable('HIBERNATE_DB_PASSWORD', $password, [System.EnvironmentVariableTarget]::Process)

    Write-Host "Starting against local PostgreSQL test database: $dbUrl"
    $mavenRepo = Join-Path $env:TEMP 'hibernateBiblioteca-m2'
    & mvn "-Dmaven.repo.local=$mavenRepo" clean package exec:java
    $exitCode = $LASTEXITCODE
}
finally {
    foreach ($name in $environmentNames) {
        [System.Environment]::SetEnvironmentVariable(
            $name,
            $previousEnvironment[$name],
            [System.EnvironmentVariableTarget]::Process
        )
    }
    $password = $null
    $securePassword = $null
}

if ($exitCode -ne 0) {
    exit $exitCode
}
