$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
Set-Location -LiteralPath $projectRoot

$javaHome = 'C:\Software\Java\jdk-17'
$javaExe = Join-Path $javaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
    throw "JDK 17 not found at '$javaHome'. Update `$javaHome in runme-sqlite.ps1."
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

$fixture = Join-Path $projectRoot 'sql\library.sqlite'
if (-not (Test-Path -LiteralPath $fixture -PathType Leaf)) {
    throw "SQLite fixture not found: '$fixture'. Create it with: py -3 scripts/create_sqlite_fixture.py"
}

$dbPath = (Resolve-Path -LiteralPath $fixture).Path
$dbUrl = "jdbc:sqlite:$dbPath"
[System.Environment]::SetEnvironmentVariable('HIBERNATE_DB_URL', $dbUrl, [System.EnvironmentVariableTarget]::Process)
Write-Host "Starting with local SQLite fixture: $dbPath"

$mavenRepo = Join-Path $env:TEMP 'hibernateBiblioteca-m2'
& mvn "-Dmaven.repo.local=$mavenRepo" -Psqlite-test clean package exec:java
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
