param(
    [string]$Url = "http://localhost:8080/api/v1/hello",
    [string]$Rate = "20/s",
    [string]$Duration = "60s",
    [string]$VegetaPath = "$env:USERPROFILE\Desktop\vegeta\vegeta.exe",
    [string]$PrometheusAddr = "0.0.0.0:8880",
    [int]$Connections = 0,
    [int]$MaxConnections = 0,
    [int]$Workers = 0,
    [int]$MaxWorkers = 0
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $VegetaPath)) {
    throw "Cannot find vegeta.exe at: $VegetaPath"
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outDir = Join-Path $repoRoot "target\vegeta\$timestamp"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$targetFile = Join-Path $outDir "targets.txt"
$resultFile = Join-Path $outDir "results.bin"
$reportFile = Join-Path $outDir "report.txt"
$plotFile = Join-Path $outDir "plot.html"

"GET $Url" | Set-Content -Encoding ASCII -Path $targetFile

$ratePerSecond = 0
if ($Rate -match '^([0-9]+(?:\.[0-9]+)?)/s$') {
    $ratePerSecond = [double]$Matches[1]
}

if ($Connections -le 0) {
    if ($ratePerSecond -gt 0) {
        $Connections = [Math]::Min(500, [Math]::Max(100, [Math]::Ceiling($ratePerSecond / 5)))
    } else {
        $Connections = 200
    }
}
if ($MaxConnections -le 0) {
    $MaxConnections = $Connections
}
if ($Workers -le 0) {
    $Workers = $Connections
}
if ($MaxWorkers -le 0) {
    $MaxWorkers = $Workers
}

Write-Host "Vegeta target: $Url"
Write-Host "Rate: $Rate"
Write-Host "Duration: $Duration"
Write-Host "Connections: $Connections"
Write-Host "Max connections: $MaxConnections"
Write-Host "Workers: $Workers"
Write-Host "Max workers: $MaxWorkers"
Write-Host "Prometheus exporter: http://localhost:8880/metrics"
Write-Host "Grafana dashboard: http://localhost:3000/d/vegeta-load-test/vegeta-load-test?orgId=1&from=now-15m&to=now&refresh=5s"
Write-Host ""
Write-Host "Open the Grafana dashboard now, then wait for samples to appear."
Write-Host ""

& $VegetaPath attack `
    -name "vegeta" `
    -rate $Rate `
    -duration $Duration `
    -connections $Connections `
    -max-connections $MaxConnections `
    -workers $Workers `
    -max-workers $MaxWorkers `
    -targets $targetFile `
    -prometheus-addr $PrometheusAddr `
    -output $resultFile

& $VegetaPath report $resultFile | Tee-Object -FilePath $reportFile
& $VegetaPath plot -title "Vegeta Load Test - $Url" -output $plotFile $resultFile

Write-Host ""
Write-Host "Saved result: $resultFile"
Write-Host "Saved report: $reportFile"
Write-Host "Saved plot: $plotFile"
Start-Process $plotFile
