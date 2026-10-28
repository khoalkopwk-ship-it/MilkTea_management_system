param(
    [string]$Server = "localhost",
    [string]$Port = "1433",
    [string]$Database = "milktea",
    [string]$User = "sa",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"
$connStr = "Server=${Server},${Port};Database=${Database};User Id=${User};Password=${Password};Encrypt=False;TrustServerCertificate=True;"

Write-Host ">>> Connecting to ${Database} on ${Server}:${Port}..." -ForegroundColor Cyan
$conn = New-Object System.Data.SqlClient.SqlConnection($connStr)
$conn.Open()

$conn.add_InfoMessage({
    param($sender, $event)
    Write-Host $event.Message -ForegroundColor Yellow
})

try {
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    $filePath = Join-Path $scriptDir "sample_data.sql"
    Write-Host ">>> Loading: $filePath" -ForegroundColor Cyan
    $rawContent = [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)

    $batches = [System.Text.RegularExpressions.Regex]::Split($rawContent, "^\s*GO\s*$", [System.Text.RegularExpressions.RegexOptions]::Multiline -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)

    $step = 0
    foreach ($batch in $batches) {
        $trimmed = $batch.Trim()
        if ($trimmed.Length -gt 0) {
            $step++
            $cmd = $conn.CreateCommand()
            $cmd.CommandText = $trimmed
            $cmd.CommandTimeout = 300
            try {
                $null = $cmd.ExecuteNonQuery()
            } catch {
                Write-Host "ERROR AT BATCH $step" -ForegroundColor Red
                throw $_
            }
        }
    }
    Write-Host ">>> SUCCESS: Full demo data loaded into milktea database!" -ForegroundColor Green
} finally {
    $conn.Close()
}
