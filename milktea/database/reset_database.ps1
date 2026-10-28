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

try {
    $migDir = Join-Path $dbDir "migrations"

    # Bước 3: Chạy lần lượt 5 migration chuỗi
    $v1 = Join-Path $migDir "V1__multi_branch_schema_and_backfill.sql"
    Execute-SqlScriptFile -Connection $milkteaConn -FilePath $v1 -Description "Bước 3.1: Migration V1 (Đa chi nhánh, phân bổ món & nhân sự)"

    $v2 = Join-Path $migDir "V2__customer_experience_favorites_and_reviews.sql"
    Execute-SqlScriptFile -Connection $milkteaConn -FilePath $v2 -Description "Bước 3.2: Migration V2 (Đánh giá, bình luận & món yêu thích)"

    $v3 = Join-Path $migDir "V3__voucher_and_discounts.sql"
    Execute-SqlScriptFile -Connection $milkteaConn -FilePath $v3 -Description "Bước 3.3: Migration V3 (Hệ thống voucher & chiết khấu)"

    $v4 = Join-Path $migDir "V4__central_warehouse.sql"
    Execute-SqlScriptFile -Connection $milkteaConn -FilePath $v4 -Description "Bước 3.4: Migration V4 (Kho Hàng Tổng Trung Tâm KHOTONG)"

    $v5 = Join-Path $migDir "V5__schema_alignment.sql"
    Execute-SqlScriptFile -Connection $milkteaConn -FilePath $v5 -Description "Bước 3.5: Migration V5 (Đồng bộ snapshot đặt hàng, ngày tạo & kết ca kho)"

    $v6 = Join-Path $migDir "V6__in_store_only_cleanup.sql"
    Execute-SqlScriptFile -Connection $milkteaConn -FilePath $v6 -Description "Bước 3.6: Migration V6 (Chuẩn hóa hệ thống phục vụ tại quán TABLE/COUNTER)"

    # Bước 4: Nạp bộ dữ liệu mẫu chuẩn
    $seedFile = Join-Path $dbDir "sample_data.sql"
    Execute-SqlScriptFile -Connection $milkteaConn -FilePath $seedFile -Description "Bước 4/4: Nạp dữ liệu mẫu Multi-Branch Full Seed (70 món, 10 CT sơ chế, tồn kho, voucher...)"

    Write-Host "============================================================================" -ForegroundColor Green
    Write-Host " HOÀN TẤT KHỞI TẠO CƠ SỞ DỮ LIỆU MILKTEA MỚI 100% THÀNH CÔNG!" -ForegroundColor Green
    Write-Host " Bạn có thể khởi động backend ngay bằng lệnh: mvn spring-boot:run" -ForegroundColor Green
    Write-Host "============================================================================`n" -ForegroundColor Green

} finally {
    $milkteaConn.Close()
}
