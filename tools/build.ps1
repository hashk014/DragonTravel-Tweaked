param([string]$RuntimeRoot = (Join-Path $PSScriptRoot '..\.tools'))
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = (Resolve-Path (Join-Path $RuntimeRoot 'jdk25')).Path
Push-Location (Join-Path $PSScriptRoot '..')
try {
    & (Join-Path $RuntimeRoot 'apache-maven-3.9.11\bin\mvn.cmd') -B verify
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
} finally { Pop-Location }
