param(
    [string]$ScriptPath = "$PSScriptRoot\trips-popular.js",
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiPath = "/api/v1/trips/popular?limit=6",
    [string]$Duration = "30s",
    [int]$Vus = 50,
    [int]$ThinkTimeMs = 0,
    [string]$K6Path = "k6"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command $K6Path -ErrorAction SilentlyContinue)) {
    throw "Cannot find k6. Install it or pass -K6Path with the full executable path."
}

if (-not (Test-Path -LiteralPath $ScriptPath)) {
    throw "Cannot find k6 script at: $ScriptPath"
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outDir = Join-Path $repoRoot "target\k6\$timestamp"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$summaryFile = Join-Path $outDir "summary.json"
$stdoutFile = Join-Path $outDir "stdout.txt"

Write-Host "k6 script: $ScriptPath"
Write-Host "Base URL: $BaseUrl"
Write-Host "API path: $ApiPath"
Write-Host "VUs: $Vus"
Write-Host "Duration: $Duration"
Write-Host "Think time ms: $ThinkTimeMs"
Write-Host ""

& $K6Path run `
    --summary-export $summaryFile `
    -e BASE_URL=$BaseUrl `
    -e API_PATH=$ApiPath `
    -e VUS=$Vus `
    -e DURATION=$Duration `
    -e THINK_TIME_MS=$ThinkTimeMs `
    $ScriptPath | Tee-Object -FilePath $stdoutFile

Write-Host ""
Write-Host "Saved stdout: $stdoutFile"
Write-Host "Saved summary: $summaryFile"
