param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiPath = "/api/v1/benchmark/test-orders/async",
    [string]$Duration = "30s",
    [int]$Vus = 50,
    [int]$TotalUsers = 0,
    [int]$ThinkTimeMs = 0,
    [int]$Stock = 1000,
    [decimal]$Amount = 100000,
    [int]$Quantity = 1,
    [int]$UserBase = 1000,
    [string]$TicketRef = "",
    [string]$K6Path = "k6"
)

$ErrorActionPreference = "Stop"

$scriptPath = Join-Path $PSScriptRoot "benchmark-test-orders.js"

if (-not (Get-Command $K6Path -ErrorAction SilentlyContinue)) {
    throw "Cannot find k6. Install it or pass -K6Path with the full executable path."
}

if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "Cannot find k6 script at: $scriptPath"
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outDir = Join-Path $repoRoot "target\k6\$timestamp-benchmark-orders"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$summaryFile = Join-Path $outDir "summary.json"
$stdoutFile = Join-Path $outDir "stdout.txt"

Write-Host "k6 script: $scriptPath"
Write-Host "Base URL: $BaseUrl"
Write-Host "API path: $ApiPath"
Write-Host "VUs: $Vus"
Write-Host "Duration: $Duration"
Write-Host "Total users: $TotalUsers"
Write-Host "Amount: $Amount"
Write-Host "Stock: $Stock"
Write-Host "Quantity: $Quantity"
Write-Host "User base: $UserBase"
Write-Host "Ticket ref: $TicketRef"
Write-Host ""

$ticketRefEnv = @()
if ($TicketRef -ne "") {
    $ticketRefEnv = @("-e", "TICKET_REF=$TicketRef")
}

& $K6Path run `
    --summary-export $summaryFile `
    -e BASE_URL=$BaseUrl `
    -e API_PATH=$ApiPath `
    -e ENDPOINT=$ApiPath `
    -e VUS=$Vus `
    -e DURATION=$Duration `
    -e TOTAL_USERS=$TotalUsers `
    -e THINK_TIME_MS=$ThinkTimeMs `
    -e STOCK=$Stock `
    -e AMOUNT=$Amount `
    -e QUANTITY=$Quantity `
    -e USER_BASE=$UserBase `
    @ticketRefEnv `
    $scriptPath | Tee-Object -FilePath $stdoutFile

Write-Host ""
Write-Host "Saved stdout: $stdoutFile"
Write-Host "Saved summary: $summaryFile"
