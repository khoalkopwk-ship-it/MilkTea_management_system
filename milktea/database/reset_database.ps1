# ============================================================================
# MilkTea Management System - KỊCH BẢN TỰ ĐỘNG RESET & KHỞI TẠO CSDL TỪ ĐẦU
# ============================================================================
param(
    [string]$Server = "localhost",
    [string]$Port = "1433",
    [string]$User = "sa",
    [string]$Password = "123456",
    [switch]$IntegratedSecurity = $false
)

$ErrorActionPreference = "Stop"

function Execute-SqlScriptFile {
    param(
        [System.Data.SqlClient.SqlConnection]$Connection,
        [string]$FilePath,
        [string]$Description
    )

    Write-Host ">>> [$Description] Thực thi: $FilePath" -ForegroundColor Cyan
    if (-not (Test-Path $FilePath)) {
        throw "Không tìm thấy tệp: $FilePath"
    }

    $rawContent = [System.IO.File]::ReadAllText($FilePath, [System.Text.Encoding]::UTF8)

    # Tách batch theo lệnh GO trên dòng riêng
    $batches = [System.Text.RegularExpressions.Regex]::Split($rawContent, "^\s*GO\s*$", [System.Text.RegularExpressions.RegexOptions]::Multiline -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)

    foreach ($batch in $batches) {
        $trimmed = $batch.Trim()
        if ($trimmed.Length -gt 0) {
            $cmd = $Connection.CreateCommand()
            $cmd.CommandText = $trimmed
            $cmd.CommandTimeout = 180
            try {
                $null = $cmd.ExecuteNonQuery()
            } catch {
                Write-Host "LỖI KHI CHẠY LỆNH:`n$trimmed" -ForegroundColor Red
                throw $_
            }
        }
    }
    Write-Host "    [OK] Đã hoàn thành $Description`n" -ForegroundColor Green
}

# 1. Chuỗi kết nối tới master
if ($IntegratedSecurity) {
    $masterConnStr = "Server=$Server,$Port;Database=master;Integrated Security=True;Encrypt=False;TrustServerCertificate=True;"
} else {
    $masterConnStr = "Server=$Server,$Port;Database=master;User Id=$User;Password=$Password;Encrypt=False;TrustServerCertificate=True;"
}

Write-Host "`n============================================================================" -ForegroundColor Yellow
Write-Host " BẮT ĐẦU RESET VÀ KHỞI TẠO TOÀN DIỆN CSDL MILKTEA" -ForegroundColor Yellow
Write-Host " Server: $Server, Port: $Port" -ForegroundColor Yellow
Write-Host "============================================================================`n" -ForegroundColor Yellow

$masterConn = New-Object System.Data.SqlClient.SqlConnection($masterConnStr)
$masterConn.Open()

try {
    $dbDir = Split-Path -Parent $MyInvocation.MyCommand.Path

    # Bước 1: Xóa DB cũ
    $dropFile = Join-Path $dbDir "0_DropOldDatabase.sql"
    Execute-SqlScriptFile -Connection $masterConn -FilePath $dropFile -Description "Bước 1/4: Xóa sạch CSDL cũ milktea"

    # Bước 2: Tạo Schema nền tảng
    $schemaFile = Join-Path $dbDir "1_schema.sql"
    Execute-SqlScriptFile -Connection $masterConn -FilePath $schemaFile -Description "Bước 2/4: Khởi tạo database mới & 30 bảng nền tảng"

} finally {
    $masterConn.Close()
}

# 2. Chuỗi kết nối tới milktea database vừa tạo
if ($IntegratedSecurity) {
    $milkteaConnStr = "Server=$Server,$Port;Database=milktea;Integrated Security=True;Encrypt=False;TrustServerCertificate=True;"
} else {
    $milkteaConnStr = "Server=$Server,$Port;Database=milktea;User Id=$User;Password=$Password;Encrypt=False;TrustServerCertificate=True;"
}

$milkteaConn = New-Object System.Data.SqlClient.SqlConnection($milkteaConnStr)
$milkteaConn.Open()

    # Bước 3: Nạp bộ dữ liệu mẫu chuẩn chuỗi đa chi nhánh kết hợp
    $seedFile = Join-Path $dbDir "sample_data.sql"
    Execute-SqlScriptFile -Connection $milkteaConn -FilePath $seedFile -Description "Bước 3/4: Nạp dữ liệu mẫu Multi-Branch Full Seed kết hợp (50 món, công thức, tồn kho, voucher...)"

    # Bước 4: Kiểm tra và đối soát tính toàn vẹn
    $verifyFile = Join-Path $dbDir "validation\verify_demo_integrity.sql"
    if (Test-Path $verifyFile) {
        Execute-SqlScriptFile -Connection $milkteaConn -FilePath $verifyFile -Description "Bước 4/4: Kiểm tra đối soát tính toàn vẹn CSDL (Verification)"
    }

    Write-Host "============================================================================" -ForegroundColor Green
    Write-Host " HOÀN TẤT KHỞI TẠO CƠ SỞ DỮ LIỆU MILKTEA MỚI 100% THÀNH CÔNG!" -ForegroundColor Green
    Write-Host " Bạn có thể khởi động backend ngay bằng lệnh: mvn spring-boot:run" -ForegroundColor Green
    Write-Host "============================================================================`n" -ForegroundColor Green

} finally {
    $milkteaConn.Close()
}
