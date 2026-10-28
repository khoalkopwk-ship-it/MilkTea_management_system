# ============================================================================
# KIEM THU TOAN DIEN HE THONG MILKTEA (API, PERMISSIONS, IDEMPOTENCY, FLOWS)
# Ho tro: Java 21, Spring Boot MVC Monolith, SQL Server runtime
# ============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$results = @()

function Record-TestResult {
    param(
        [string]$TestId,
        [string]$Category,
        [string]$Description,
        [string]$Method,
        [string]$Path,
        [int]$ExpectedStatus,
        [int]$ActualStatus,
        [bool]$Success,
        [string]$Details = ""
    )
    $obj = [PSCustomObject]@{
        TestId = $TestId
        Category = $Category
        Description = $Description
        Method = $Method
        Path = $Path
        ExpectedStatus = $ExpectedStatus
        ActualStatus = $ActualStatus
        Status = if ($Success) { "PASSED" } else { "FAILED" }
        Details = $Details
        Timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    }
    $script:results += $obj
    $color = if ($Success) { "Green" } else { "Red" }
    Write-Host "[$($obj.Status)] $TestId - $Method $Path : $Description (Status: $ActualStatus)" -ForegroundColor $color
}

function Invoke-HttpRequest {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null,
        [string]$TableToken = $null,
        [string]$CsrfToken = $null,
        [string]$IdempotencyKey = $null,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session = $null
    )

    $uri = "$BaseUrl$Path"
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    if ($TableToken) {
        $headers["X-Table-Session-Token"] = $TableToken
    }
    if ($Session) {
        foreach ($c in $Session.Cookies.GetCookies((New-Object System.Uri($BaseUrl)))) {
            if ($c.Name -eq "XSRF-TOKEN") {
                $headers["X-CSRF-TOKEN"] = $c.Value
            }
        }
    }
    if ($CsrfToken -and -not $headers.ContainsKey("X-CSRF-TOKEN")) {
        $headers["X-CSRF-TOKEN"] = $CsrfToken
    }
    if ($IdempotencyKey) {
        $headers["X-Idempotency-Key"] = $IdempotencyKey
    }

    $bodyStr = $null
    if ($Body) {
        if ($Body -is [string]) {
            $bodyStr = $Body
        } else {
            $bodyStr = $Body | ConvertTo-Json -Depth 10 -Compress
        }
    }

    try {
        $params = @{
            Uri = $uri
            Method = $Method
            Headers = $headers
            UseBasicParsing = $true
            ErrorAction = "Stop"
        }
        if ($Session) { $params["WebSession"] = $Session }
        if ($Method -ne "GET") {
            $params["Body"] = if ($bodyStr) { $bodyStr } else { "{}" }
            $params["ContentType"] = "application/json; charset=utf-8"
        }
        $resp = Invoke-WebRequest @params
        return @{
            StatusCode = [int]$resp.StatusCode
            Content = $resp.Content
            Headers = $resp.Headers
        }
    } catch [System.Net.WebException] {
        $resp = $_.Exception.Response
        if ($resp) {
            $stream = $resp.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $content = $reader.ReadToEnd()
            return @{
                StatusCode = [int]$resp.StatusCode
                Content = $content
                Headers = $resp.Headers
            }
        }
        return @{
            StatusCode = 0
            Content = $_.Exception.Message
            Headers = @{}
        }
    } catch {
        return @{
            StatusCode = 0
            Content = $_.Exception.Message
            Headers = @{}
        }
    }
}

function Get-CsrfTokenForSession {
    param([Microsoft.PowerShell.Commands.WebRequestSession]$Session)
    $nullResp = Invoke-HttpRequest -Method "GET" -Path "/menu" -Session $Session
    $csrf = $null
    $uriObj = New-Object System.Uri($BaseUrl)
    foreach ($cookie in $Session.Cookies.GetCookies($uriObj)) {
        if ($cookie.Name -eq "XSRF-TOKEN") {
            $csrf = $cookie.Value
        }
    }
    return $csrf
}

Write-Host "`n============================================================================" -ForegroundColor Yellow
Write-Host " KHOI DONG KIEM THU TOAN DIEN BE MAT HE THONG MILKTEA (API & E2E FLOWS)" -ForegroundColor Yellow
Write-Host " Target: $BaseUrl" -ForegroundColor Yellow
Write-Host "============================================================================`n" -ForegroundColor Yellow

# ----------------------------------------------------------------------------
# 1. AUTHENTICATION MATRIX & LOGIN TEST (MOI ACTOR CO SESSION RIENG)
# ----------------------------------------------------------------------------
$tokens = @{}
$sessions = @{}
$csrfs = @{}

$accounts = @(
    @{ Role="ADMIN";   Email="admin@milktea.vn";    Pass="Password@123" },
    @{ Role="MGR_CN1"; Email="manager1@milktea.vn"; Pass="Password@123" },
    @{ Role="MGR_CN2"; Email="manager2@milktea.vn"; Pass="Password@123" },
    @{ Role="CSH_CN1"; Email="cashier@milktea.vn";  Pass="Password@123" },
    @{ Role="CSH_CN2"; Email="cashier2@milktea.vn"; Pass="Password@123" },
    @{ Role="KIT_CN1"; Email="kitchen@milktea.vn";  Pass="Password@123" },
    @{ Role="KIT_CN2"; Email="kitchen2@milktea.vn"; Pass="Password@123" },
    @{ Role="CUST";    Email="customer@milktea.vn"; Pass="Password@123" }
)

foreach ($acc in $accounts) {
    $userSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $csrf = Get-CsrfTokenForSession -Session $userSession
    $sessions[$acc.Role] = $userSession
    $csrfs[$acc.Role] = $csrf

    $loginBody = @{ email = $acc.Email; password = $acc.Pass }
    $res = Invoke-HttpRequest -Method "POST" -Path "/api/v1/auth/login" -Body $loginBody -CsrfToken $csrf -Session $userSession
    $ok = ($res.StatusCode -eq 200)
    $token = $null
    if ($ok) {
        $json = $res.Content | ConvertFrom-Json
        $token = $json.data.token
        $tokens[$acc.Role] = $token
        # Cap nhat CSRF sau khi dang nhap thanh cong (session fixation defense)
        $csrfs[$acc.Role] = Get-CsrfTokenForSession -Session $userSession
    }
    Record-TestResult -TestId "AUTH-LOGIN-$($acc.Role)" -Category "Authentication" -Description "Dang nhap tai khoan $($acc.Email)" -Method "POST" -Path "/api/v1/auth/login" -ExpectedStatus 200 -ActualStatus $res.StatusCode -Success $ok
}

# Negative Auth: Sai mat khau
$negSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$negCsrf = Get-CsrfTokenForSession -Session $negSession
$badLogin = @{ email = "admin@milktea.vn"; password = "WrongPassword999" }
$resBad = Invoke-HttpRequest -Method "POST" -Path "/api/v1/auth/login" -Body $badLogin -CsrfToken $negCsrf -Session $negSession
Record-TestResult -TestId "AUTH-LOGIN-BAD" -Category "Authentication" -Description "Dang nhap sai mat khau nem 401" -Method "POST" -Path "/api/v1/auth/login" -ExpectedStatus 401 -ActualStatus $resBad.StatusCode -Success ($resBad.StatusCode -eq 401)

# ----------------------------------------------------------------------------
# 2. CATALOG & SELLABILITY (V4-14, V4-15)
# ----------------------------------------------------------------------------
$publicSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$publicCsrf = Get-CsrfTokenForSession -Session $publicSession

$resCat = Invoke-HttpRequest -Method "GET" -Path "/api/v1/catalog/categories" -Session $publicSession
Record-TestResult -TestId "CAT-01" -Category "Catalog" -Description "Lay danh muc san pham active" -Method "GET" -Path "/api/v1/catalog/categories" -ExpectedStatus 200 -ActualStatus $resCat.StatusCode -Success ($resCat.StatusCode -eq 200)

$resProd = Invoke-HttpRequest -Method "GET" -Path "/api/v1/catalog/products" -Session $publicSession
Record-TestResult -TestId "CAT-02" -Category "Catalog" -Description "Lay danh sach mon toan chuoi" -Method "GET" -Path "/api/v1/catalog/products" -ExpectedStatus 200 -ActualStatus $resProd.StatusCode -Success ($resProd.StatusCode -eq 200)

$resProdCn1 = Invoke-HttpRequest -Method "GET" -Path "/api/v1/catalog/products?branchId=1" -Session $publicSession
Record-TestResult -TestId "CAT-03" -Category "Catalog" -Description "Lay mon loc theo CN01" -Method "GET" -Path "/api/v1/catalog/products?branchId=1" -ExpectedStatus 200 -ActualStatus $resProdCn1.StatusCode -Success ($resProdCn1.StatusCode -eq 200)

$resSearch = Invoke-HttpRequest -Method "GET" -Path "/api/v1/catalog/search?q=tra" -Session $publicSession
Record-TestResult -TestId "CAT-04" -Category "Catalog" -Description "Tim kiem mon voi tu khoa 'tra'" -Method "GET" -Path "/api/v1/catalog/search?q=tra" -ExpectedStatus 200 -ActualStatus $resSearch.StatusCode -Success ($resSearch.StatusCode -eq 200)

$resTop = Invoke-HttpRequest -Method "GET" -Path "/api/v1/catalog/top-selling" -Session $publicSession
Record-TestResult -TestId "CAT-05" -Category "Catalog" -Description "Lay top mon ban chay (max 20)" -Method "GET" -Path "/api/v1/catalog/top-selling" -ExpectedStatus 200 -ActualStatus $resTop.StatusCode -Success ($resTop.StatusCode -eq 200)

$resNewest = Invoke-HttpRequest -Method "GET" -Path "/api/v1/catalog/newest" -Session $publicSession
Record-TestResult -TestId "CAT-06" -Category "Catalog" -Description "Lay mon moi nhat (createdAt + id tie-break)" -Method "GET" -Path "/api/v1/catalog/newest" -ExpectedStatus 200 -ActualStatus $resNewest.StatusCode -Success ($resNewest.StatusCode -eq 200)

# ----------------------------------------------------------------------------
# 3. BRANCHES & SCOPING (V4-02, V4-11)
# ----------------------------------------------------------------------------
$resBranches = Invoke-HttpRequest -Method "GET" -Path "/api/v1/branches" -Session $publicSession
Record-TestResult -TestId "BRANCH-01" -Category "Branch" -Description "Lay danh sach tat ca chi nhanh" -Method "GET" -Path "/api/v1/branches" -ExpectedStatus 200 -ActualStatus $resBranches.StatusCode -Success ($resBranches.StatusCode -eq 200)

# ----------------------------------------------------------------------------
# 4. TABLE QR & CONTEXT LIFECYCLE (V4-01, V4-04)
# ----------------------------------------------------------------------------
$guestSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$guestCsrf = Get-CsrfTokenForSession -Session $guestSession

# 4.1. Khong co QR code -> 400 QR_INVALID
$resNoQr = Invoke-HttpRequest -Method "GET" -Path "/api/v1/tables/context" -Session $guestSession
Record-TestResult -TestId "V4-01-NO-QR" -Category "Table QR" -Description "Truy cap context khong co ma QR bi tu choi 400" -Method "GET" -Path "/api/v1/tables/context" -ExpectedStatus 400 -ActualStatus $resNoQr.StatusCode -Success ($resNoQr.StatusCode -eq 400)

# 4.2. QR gia mao / khong ton tai -> 404 TABLE_NOT_FOUND
$resFakeQr = Invoke-HttpRequest -Method "GET" -Path "/api/v1/tables/context?qr=FAKE_QR_9999" -Session $guestSession
Record-TestResult -TestId "V4-01-FAKE-QR" -Category "Table QR" -Description "Truy cap QR khong ton tai bi tu choi 404" -Method "GET" -Path "/api/v1/tables/context?qr=FAKE_QR_9999" -ExpectedStatus 404 -ActualStatus $resFakeQr.StatusCode -Success ($resFakeQr.StatusCode -eq 404)

# 4.3. Quet QR hop le cua Ban TRONG o CN01 -> Nhan capability tableToken
$resTablesCn1 = Invoke-HttpRequest -Method "GET" -Path "/api/v1/cashier/tables?branchId=1" -Token $tokens["CSH_CN1"] -Session $sessions["CSH_CN1"]
$tablesList = ($resTablesCn1.Content | ConvertFrom-Json).data
$freeTable = $tablesList | Where-Object { $_.status -eq "TRONG" } | Select-Object -First 1
$targetQr = if ($freeTable) { $freeTable.qrCode } else { "QR_CN01_BAN07" }
$resTable5 = Invoke-HttpRequest -Method "GET" -Path "/api/v1/tables/context?qr=$targetQr" -Session $guestSession
$table5Json = $resTable5.Content | ConvertFrom-Json
$tableToken5 = $table5Json.data.tableToken
$tableId5 = $table5Json.data.tableId
$hasTableToken = ($resTable5.StatusCode -eq 200 -and $tableToken5 -ne $null -and $tableToken5.Length -gt 20)
Record-TestResult -TestId "V4-01-VALID-QR" -Category "Table QR" -Description "Quet QR hop le cap dung capability tableToken va context ban ($targetQr)" -Method "GET" -Path "/api/v1/tables/context?qr=$targetQr" -ExpectedStatus 200 -ActualStatus $resTable5.StatusCode -Success $hasTableToken -Details "TableId: $tableId5"

# ----------------------------------------------------------------------------
# 5. ORDER CREATION, PERMISSIONS & IDEMPOTENCY (V4-01, V4-12)
# ----------------------------------------------------------------------------
# 5.1. Anonymous khach tu gui don TABLE ma KHONG co tableToken -> Bi chan 403
$badTableOrder = @{
    tableId = $tableId5
    expectedSessionId = $null
    items = @(@{ productId = 1; quantity = 1 })
}
$resNoTokenOrder = Invoke-HttpRequest -Method "POST" -Path "/api/v1/orders/table" -Body $badTableOrder -CsrfToken $guestCsrf -Session $guestSession
Record-TestResult -TestId "V4-01-ORDER-NO-TOKEN" -Category "Order" -Description "Tao don TABLE thieu tableToken bi chan 403" -Method "POST" -Path "/api/v1/orders/table" -ExpectedStatus 403 -ActualStatus $resNoTokenOrder.StatusCode -Success ($resNoTokenOrder.StatusCode -eq 403)

# 5.2. Khach dung token cua Ban 06 de dat cho Ban 07 -> Bi chan 403
$badTableOrderWrong = @{
    tableId = 13
    tableToken = $tableToken5
    expectedSessionId = $null
    items = @(@{ productId = 1; quantity = 1 })
}
$resWrongTableOrder = Invoke-HttpRequest -Method "POST" -Path "/api/v1/orders/table" -Body $badTableOrderWrong -CsrfToken $guestCsrf -Session $guestSession
Record-TestResult -TestId "V4-01-ORDER-WRONG-TABLE" -Category "Order" -Description "Dung token Ban 06 dat cho Ban 07 bi chan 403" -Method "POST" -Path "/api/v1/orders/table" -ExpectedStatus 403 -ActualStatus $resWrongTableOrder.StatusCode -Success ($resWrongTableOrder.StatusCode -eq 403)

# 5.3. Tao don TABLE hop le voi dung tableToken -> Mo phien, tao don, tra guestTableToken
$idemKey1 = "E2E-TABLE-ORDER-" + (Get-Random)
$validTableOrder = @{
    tableId = $tableId5
    tableToken = $tableToken5
    expectedSessionId = $null
    note = "E2E Automated Table Order Ban 06"
    items = @(@{ productId = 1; quantity = 1 })
}
$resValidTableOrder = Invoke-HttpRequest -Method "POST" -Path "/api/v1/orders/table" -Body $validTableOrder -TableToken $tableToken5 -IdempotencyKey $idemKey1 -CsrfToken $guestCsrf -Session $guestSession
$tableOrderOk = ($resValidTableOrder.StatusCode -eq 200 -or $resValidTableOrder.StatusCode -eq 201)
$tableOrderJson = $null
$createdTableOrderId = $null
$openedSessionId = $null
$guestSessionToken = $null
if ($tableOrderOk) {
    $tableOrderJson = $resValidTableOrder.Content | ConvertFrom-Json
    $createdTableOrderId = $tableOrderJson.data.orderId
    $openedSessionId = $tableOrderJson.data.sessionId
    $guestSessionToken = $tableOrderJson.data.guestTableToken
}
Record-TestResult -TestId "FLOW02-TABLE-ORDER" -Category "Order" -Description "Don dau ban TABLE mo phien thanh cong, cap session token" -Method "POST" -Path "/api/v1/orders/table" -ExpectedStatus 201 -ActualStatus $resValidTableOrder.StatusCode -Success $tableOrderOk -Details "OrderId: $createdTableOrderId, SessionId: $openedSessionId"

# 5.4. IDEMPOTENCY REPLAY: Gui lai cung idempotencyKey + cung payload -> Tra dung don cu
$resReplay = Invoke-HttpRequest -Method "POST" -Path "/api/v1/orders/table" -Body $validTableOrder -TableToken $tableToken5 -IdempotencyKey $idemKey1 -CsrfToken $guestCsrf -Session $guestSession
$replayOk = $false
if ($resReplay.StatusCode -eq 200 -or $resReplay.StatusCode -eq 201) {
    $repJson = $resReplay.Content | ConvertFrom-Json
    $replayOk = ($repJson.data.orderId -eq $createdTableOrderId)
}
Record-TestResult -TestId "V4-12-IDEMPOTENCY-REPLAY" -Category "Idempotency" -Description "Replay Idempotency tra ve dung don cu" -Method "POST" -Path "/api/v1/orders/table" -ExpectedStatus 200 -ActualStatus $resReplay.StatusCode -Success $replayOk -Details "Replayed OrderId: $($repJson.data.orderId)"

# 5.5. IDEMPOTENCY CONFLICT: Gui cung idempotencyKey nhung khac payload -> Nem 409 CONFLICT
$conflictOrder = @{
    tableId = $tableId5
    tableToken = $tableToken5
    expectedSessionId = $openedSessionId
    note = "DIFFERENT PAYLOAD CONFLICT"
    items = @(@{ productId = 2; quantity = 3 })
}
$resConflict = Invoke-HttpRequest -Method "POST" -Path "/api/v1/orders/table" -Body $conflictOrder -TableToken $tableToken5 -IdempotencyKey $idemKey1 -CsrfToken $guestCsrf -Session $guestSession
Record-TestResult -TestId "V4-12-IDEMPOTENCY-CONFLICT" -Category "Idempotency" -Description "Trung Key khac Payload nem 409 Conflict" -Method "POST" -Path "/api/v1/orders/table" -ExpectedStatus 409 -ActualStatus $resConflict.StatusCode -Success ($resConflict.StatusCode -eq 409)

# ----------------------------------------------------------------------------
# 6. COUNTER ORDER & STAFF ISOLATION (V4-02, FLOW05)
# ----------------------------------------------------------------------------
# 6.1. Customer tu goi API tao don quay COUNTER -> Bi chan 403
$counterReq = @{
    branchId = 1
    note = "Counter order test"
    items = @(@{ productId = 1; quantity = 1 })
}
$resCustCounter = Invoke-HttpRequest -Method "POST" -Path "/api/v1/orders/counter" -Body $counterReq -Token $tokens["CUST"] -CsrfToken $csrfs["CUST"] -Session $sessions["CUST"]
Record-TestResult -TestId "V4-02-CUST-COUNTER-DENIED" -Category "Permissions" -Description "Customer khong the tao don COUNTER (403)" -Method "POST" -Path "/api/v1/orders/counter" -ExpectedStatus 403 -ActualStatus $resCustCounter.StatusCode -Success ($resCustCounter.StatusCode -eq 403)

# 6.2. Cashier CN01 tao don COUNTER -> Gan dung chi nhanh phan cong (CN01)
$counterCashierReq = @{
    branchId = 2 # Client co tinh gui CN02
    note = "Thu ngan CN01 lap don mang ve"
    items = @(@{ productId = 1; quantity = 2 })
}
$resCashierCounter = Invoke-HttpRequest -Method "POST" -Path "/api/v1/orders/counter" -Body $counterCashierReq -Token $tokens["CSH_CN1"] -CsrfToken $csrfs["CSH_CN1"] -Session $sessions["CSH_CN1"]
$cOk = ($resCashierCounter.StatusCode -eq 200 -or $resCashierCounter.StatusCode -eq 201)
$counterOrderId = $null
$counterOrderItemId = $null
if ($cOk) {
    $cJson = $resCashierCounter.Content | ConvertFrom-Json
    $counterOrderId = $cJson.data.orderId
    if ($cJson.data.items -and $cJson.data.items.Count -gt 0) {
        $counterOrderItemId = $cJson.data.items[0].id
    }
    # Kiem tra server ep dung branchId=1
    $cOk = ($cJson.data.branchId -eq 1)
}
Record-TestResult -TestId "FLOW05-COUNTER-ORDER" -Category "Order" -Description "Thu ngan CN01 tao don COUNTER ghi de dung chi nhanh 1" -Method "POST" -Path "/api/v1/orders/counter" -ExpectedStatus 201 -ActualStatus $resCashierCounter.StatusCode -Success $cOk -Details "OrderId: $counterOrderId, BranchId: $($cJson.data.branchId)"

# 6.3. Cashier CN01 xac nhan don (CONFIRM: CHO_THANH_TOAN -> CHO_CHE_BIEN)
$resConfirm = Invoke-HttpRequest -Method "POST" -Path "/api/v1/cashier/orders/$counterOrderId/confirm" -Token $tokens["CSH_CN1"] -CsrfToken $csrfs["CSH_CN1"] -Session $sessions["CSH_CN1"]
Record-TestResult -TestId "FLOW05-CASHIER-CONFIRM" -Category "Order" -Description "Thu ngan xac nhan don COUNTER de bep bat dau nau" -Method "POST" -Path "/api/v1/cashier/orders/$counterOrderId/confirm" -ExpectedStatus 200 -ActualStatus $resConfirm.StatusCode -Success ($resConfirm.StatusCode -eq 200)

# ----------------------------------------------------------------------------
# 7. KITCHEN KDS LIFECYCLE (FLOW05, V4-13)
# ----------------------------------------------------------------------------
# 7.1. Kitchen CN02 co can thiep don CN01 -> Bi chan 403
$resKitchenWrong = Invoke-HttpRequest -Method "POST" -Path "/api/v1/kitchen/orders/$counterOrderId/complete" -Token $tokens["KIT_CN2"] -CsrfToken $csrfs["KIT_CN2"] -Session $sessions["KIT_CN2"]
Record-TestResult -TestId "V4-02-KITCHEN-CROSS-BRANCH" -Category "Permissions" -Description "Bep CN02 khong duoc hoan thanh don CN01 (403)" -Method "POST" -Path "/api/v1/kitchen/orders/$counterOrderId/complete" -ExpectedStatus 403 -ActualStatus $resKitchenWrong.StatusCode -Success ($resKitchenWrong.StatusCode -eq 403)

# 7.2. Kitchen CN01 bat dau pha che (START)
$resKitchenStart = Invoke-HttpRequest -Method "POST" -Path "/api/v1/kitchen/orders/$counterOrderId/start" -Token $tokens["KIT_CN1"] -CsrfToken $csrfs["KIT_CN1"] -Session $sessions["KIT_CN1"]
Record-TestResult -TestId "FLOW05-KITCHEN-START" -Category "Kitchen" -Description "Bep CN01 bat dau pha che don $counterOrderId" -Method "POST" -Path "/api/v1/kitchen/orders/$counterOrderId/start" -ExpectedStatus 200 -ActualStatus $resKitchenStart.StatusCode -Success ($resKitchenStart.StatusCode -eq 200)

# 7.3. Kitchen CN01 hoan thanh pha che (COMPLETE -> Tru kho thuc te)
$resKitchenComplete = Invoke-HttpRequest -Method "POST" -Path "/api/v1/kitchen/orders/$counterOrderId/complete" -Token $tokens["KIT_CN1"] -CsrfToken $csrfs["KIT_CN1"] -Session $sessions["KIT_CN1"]
Record-TestResult -TestId "FLOW05-KITCHEN-COMPLETE" -Category "Kitchen" -Description "Bep CN01 hoan thanh pha che don $counterOrderId" -Method "POST" -Path "/api/v1/kitchen/orders/$counterOrderId/complete" -ExpectedStatus 200 -ActualStatus $resKitchenComplete.StatusCode -Success ($resKitchenComplete.StatusCode -eq 200)

# ----------------------------------------------------------------------------
# 8. PAYMENT & BILLING (FLOW05, V4-09)
# ----------------------------------------------------------------------------
# Thu tien mat cho don COUNTER
$payReq = @{
    method = "CASH"
    reference = "POS-CASH-AUTO-001"
}
# Lay invoiceId cua counterOrder
$resOrderInfo = Invoke-HttpRequest -Method "GET" -Path "/api/v1/orders/$counterOrderId" -Token $tokens["CSH_CN1"] -Session $sessions["CSH_CN1"]
$orderInfoJson = $resOrderInfo.Content | ConvertFrom-Json
$invoiceId = $orderInfoJson.data.invoiceId

$resPay = Invoke-HttpRequest -Method "POST" -Path "/api/v1/cashier/invoices/$invoiceId/payments" -Body $payReq -Token $tokens["CSH_CN1"] -CsrfToken $csrfs["CSH_CN1"] -Session $sessions["CSH_CN1"]
$payOk = ($resPay.StatusCode -eq 200 -or $resPay.StatusCode -eq 201)
Record-TestResult -TestId "FLOW05-PAYMENT-CASH" -Category "Payment" -Description "Thu ngan ghi thu tien mat thanh cong hoa don $invoiceId" -Method "POST" -Path "/api/v1/cashier/invoices/$invoiceId/payments" -ExpectedStatus 201 -ActualStatus $resPay.StatusCode -Success $payOk -Details $resPay.Content

# ----------------------------------------------------------------------------
# 9. INVENTORY SCOPING & PERMISSIONS (V4-11)
# ----------------------------------------------------------------------------
# 9.1. Cashier CN01 xem ton kho CN01 -> 200 OK
$resStockCn1 = Invoke-HttpRequest -Method "GET" -Path '/api/v1/inventory/stocks?location=KHO&branchId=1' -Token $tokens["CSH_CN1"] -Session $sessions["CSH_CN1"]
Record-TestResult -TestId "V4-11-STOCK-OWN-BRANCH" -Category "Inventory" -Description "Thu ngan CN01 xem ton kho CN01 (200)" -Method "GET" -Path '/api/v1/inventory/stocks?location=KHO&branchId=1' -ExpectedStatus 200 -ActualStatus $resStockCn1.StatusCode -Success ($resStockCn1.StatusCode -eq 200)

# 9.2. Cashier CN01 xem ton kho CN02 -> 403 ACCESS_DENIED
$resStockCross = Invoke-HttpRequest -Method "GET" -Path '/api/v1/inventory/stocks?location=KHO&branchId=2' -Token $tokens["CSH_CN1"] -Session $sessions["CSH_CN1"]
Record-TestResult -TestId "V4-11-STOCK-CROSS-BRANCH" -Category "Inventory" -Description "Thu ngan CN01 truy cap ton kho CN02 bi chan 403" -Method "GET" -Path '/api/v1/inventory/stocks?location=KHO&branchId=2' -ExpectedStatus 403 -ActualStatus $resStockCross.StatusCode -Success ($resStockCross.StatusCode -eq 403)

# 9.3. Shift Settlement: Bep CN01 xem preview ket ca CN01 -> 200 OK
$resPreview = Invoke-HttpRequest -Method "GET" -Path '/api/v1/inventory/shift-settlement/preview?location=BEP&branchId=1' -Token $tokens["KIT_CN1"] -Session $sessions["KIT_CN1"]
Record-TestResult -TestId "V4-11-SETTLEMENT-PREVIEW" -Category "Inventory" -Description "Bep xem bang xem truoc ket ca kho Bep CN01 (200)" -Method "GET" -Path '/api/v1/inventory/shift-settlement/preview?location=BEP&branchId=1' -ExpectedStatus 200 -ActualStatus $resPreview.StatusCode -Success ($resPreview.StatusCode -eq 200)

# 9.4. Cashier CN01 khong co quyen xem ket ca kho -> 403 Forbidden
$resCashierPreview = Invoke-HttpRequest -Method "GET" -Path '/api/v1/inventory/shift-settlement/preview?location=BEP&branchId=1' -Token $tokens["CSH_CN1"] -Session $sessions["CSH_CN1"]
Record-TestResult -TestId "V4-11-CASHIER-PREVIEW-DENIED" -Category "Inventory" -Description "Thu ngan khong co quyen xem ket ca kho bi tu choi 403" -Method "GET" -Path '/api/v1/inventory/shift-settlement/preview?location=BEP&branchId=1' -ExpectedStatus 403 -ActualStatus $resCashierPreview.StatusCode -Success ($resCashierPreview.StatusCode -eq 403)

# ----------------------------------------------------------------------------
# 10. REVIEWS & FAVORITES (FLOW13)
# ----------------------------------------------------------------------------
# 10.1. Danh gia ngan duoi 50 ky tu -> Bi tu choi 400
$revItemId = if ($counterOrderItemId) { $counterOrderItemId } else { 1 }
$shortReview = @{
    orderItemId = $revItemId
    rating = 5
    content = "Ngon qua"
}
$resShortRev = Invoke-HttpRequest -Method "POST" -Path "/api/v1/reviews" -Body $shortReview -Token $tokens["CUST"] -CsrfToken $csrfs["CUST"] -Session $sessions["CUST"]
Record-TestResult -TestId "FLOW13-REVIEW-SHORT" -Category "Review" -Description "Danh gia duoi 50 ky tu bi tu choi 400" -Method "POST" -Path "/api/v1/reviews" -ExpectedStatus 400 -ActualStatus $resShortRev.StatusCode -Success ($resShortRev.StatusCode -eq 400)

# 10.2. Them vao yeu thich
$resFav = Invoke-HttpRequest -Method "POST" -Path "/api/v1/favorites/1" -Token $tokens["CUST"] -CsrfToken $csrfs["CUST"] -Session $sessions["CUST"]
Record-TestResult -TestId "FLOW13-FAVORITE-TOGGLE" -Category "Favorite" -Description "Them mon vao danh sach yeu thich" -Method "POST" -Path "/api/v1/favorites/1" -ExpectedStatus 200 -ActualStatus $resFav.StatusCode -Success ($resFav.StatusCode -eq 200)

# ----------------------------------------------------------------------------
# 11. VOUCHERS (FLOW11)
# ----------------------------------------------------------------------------
$vBody = @{
    voucherCode = "CHAOMUNG"
    branchId = 1
    subtotal = 100000.00
}
$resVoucher = Invoke-HttpRequest -Method "POST" -Path "/api/v1/vouchers/validate" -Body $vBody -Token $tokens["CUST"] -CsrfToken $csrfs["CUST"] -Session $sessions["CUST"]
Record-TestResult -TestId "FLOW11-VOUCHER-VALIDATE" -Category "Voucher" -Description "Xac thuc voucher CHAOMUNG hop le" -Method "POST" -Path "/api/v1/vouchers/validate" -ExpectedStatus 200 -ActualStatus $resVoucher.StatusCode -Success ($resVoucher.StatusCode -eq 200)

# ----------------------------------------------------------------------------
# 12. ADMIN & MANAGER PERMISSIONS (FLOW15)
# ----------------------------------------------------------------------------
# 12.1. Manager CN01 xem dashboard bao cao CN01 -> 200
$resRepCn1 = Invoke-HttpRequest -Method "GET" -Path "/api/v1/admin/reports/dashboard?branchId=1" -Token $tokens["MGR_CN1"] -Session $sessions["MGR_CN1"]
Record-TestResult -TestId "FLOW15-REPORT-OWN-BRANCH" -Category "Reports" -Description "Quan ly xem bao cao chi nhanh minh (200)" -Method "GET" -Path "/api/v1/admin/reports/dashboard?branchId=1" -ExpectedStatus 200 -ActualStatus $resRepCn1.StatusCode -Success ($resRepCn1.StatusCode -eq 200)

# 12.2. Admin xem danh sach nhan su toan he thong -> 200
$resStaff = Invoke-HttpRequest -Method "GET" -Path "/api/v1/admin/accounts" -Token $tokens["ADMIN"] -Session $sessions["ADMIN"]
Record-TestResult -TestId "FLOW15-ADMIN-STAFF-LIST" -Category "Admin" -Description "Admin xem danh sach nhan su toan he thong (200)" -Method "GET" -Path "/api/v1/admin/accounts" -ExpectedStatus 200 -ActualStatus $resStaff.StatusCode -Success ($resStaff.StatusCode -eq 200)

# 12.3. Tu choi dat hang Online (In-store Only Scope) -> 410 GONE
$resOnline = Invoke-HttpRequest -Method "POST" -Path "/api/v1/orders/online" -CsrfToken $publicCsrf -Session $publicSession
Record-TestResult -TestId "V4-ONLINE-REJECT" -Category "Scope" -Description "Tu choi dat hang online tu xa (410 GONE)" -Method "POST" -Path "/api/v1/orders/online" -ExpectedStatus 410 -ActualStatus $resOnline.StatusCode -Success ($resOnline.StatusCode -eq 410)

# ----------------------------------------------------------------------------
# 13. TONG HOP VA XUAT KET QUA
# ----------------------------------------------------------------------------
$totalTests = $results.Count
$passed = ($results | Where-Object { $_.Status -eq "PASSED" }).Count
$failed = ($results | Where-Object { $_.Status -eq "FAILED" }).Count

Write-Host "`n============================================================================" -ForegroundColor Yellow
Write-Host " KET QUA KIEM THU THUC TE TREN MOI TRUONG RUNTIME" -ForegroundColor Yellow
Write-Host " Total test cases: $totalTests" -ForegroundColor Yellow
Write-Host " Passed: $passed" -ForegroundColor Green
Write-Host " Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Red" } else { "Green" })
Write-Host "============================================================================`n" -ForegroundColor Yellow

$resultsJson = $results | ConvertTo-Json -Depth 5
$jsonPath = "H:\UTE\Nam_3\MilkTea_management_system\milktea\docs\api_test_results.json"
[System.IO.File]::WriteAllText($jsonPath, $resultsJson, [System.Text.Encoding]::UTF8)
Write-Host "Da xuat ket qua machine-readable toi: $jsonPath" -ForegroundColor Cyan
