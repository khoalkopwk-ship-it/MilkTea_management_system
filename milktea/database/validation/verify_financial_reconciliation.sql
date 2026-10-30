-- ============================================================================
-- SCRIPT ĐỐI SOÁT TÀI CHÍNH & DOANH THU CHUỖI MILKTEA MULTI-BRANCH
-- File: database/validation/verify_financial_reconciliation.sql
-- Tuân thủ V4-06 & V4-09: Đúng schema thực tế, tách giảm giá, fail-fast THROW
-- ============================================================================

SET NOCOUNT ON;

PRINT N'====================================================================';
PRINT N'  ĐỐI SOÁT TÀI CHÍNH, HÓA ĐƠN & DOANH THU (FINANCIAL RECONCILIATION)';
PRINT N'====================================================================';

DECLARE @ViolationCount INT = 0;

-- 1. Kiểm tra công thức Hóa đơn: PhaiTra = max(0, TongTien - TienGiam - TienGiamVoucher)
PRINT N'1. Kiểm tra tính toàn vẹn công thức tính tiền hóa đơn:';

SELECT 
    hd.MaHD,
    hd.TongTien,
    hd.TienGiam,
    ISNULL(hd.TienGiamVoucher, 0) AS TienGiamVoucher,
    hd.PhaiTra,
    CASE WHEN (hd.TongTien - hd.TienGiam - ISNULL(hd.TienGiamVoucher, 0)) < 0 THEN 0.00 ELSE (hd.TongTien - hd.TienGiam - ISNULL(hd.TienGiamVoucher, 0)) END AS PhaiTraKyVong,
    ABS(hd.PhaiTra - CASE WHEN (hd.TongTien - hd.TienGiam - ISNULL(hd.TienGiamVoucher, 0)) < 0 THEN 0.00 ELSE (hd.TongTien - hd.TienGiam - ISNULL(hd.TienGiamVoucher, 0)) END) AS DoLech
FROM dbo.HoaDon hd
WHERE ABS(hd.PhaiTra - CASE WHEN (hd.TongTien - hd.TienGiam - ISNULL(hd.TienGiamVoucher, 0)) < 0 THEN 0.00 ELSE (hd.TongTien - hd.TienGiam - ISNULL(hd.TienGiamVoucher, 0)) END) > 0.01;

DECLARE @InvoiceFormulaError INT = (
    SELECT COUNT(*) 
    FROM dbo.HoaDon hd
    WHERE ABS(hd.PhaiTra - CASE WHEN (hd.TongTien - hd.TienGiam - ISNULL(hd.TienGiamVoucher, 0)) < 0 THEN 0.00 ELSE (hd.TongTien - hd.TienGiam - ISNULL(hd.TienGiamVoucher, 0)) END) > 0.01
);

IF @InvoiceFormulaError = 0
    PRINT N'   [PASS] 100% hóa đơn khớp chính xác công thức: Phải trả = Tổng tiền - Giảm giá - Voucher.';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện ' + CAST(@InvoiceFormulaError AS NVARCHAR(10)) + N' hóa đơn bị lệch số tiền!';
    SET @ViolationCount = @ViolationCount + @InvoiceFormulaError;
END;

-- 2. Đối soát thanh toán thực thu với hóa đơn của đơn hoàn thành (HOAN_THANH)
PRINT N'2. Đối soát thanh toán thực thu với hóa đơn đã hoàn thành:';

SELECT 
    hd.MaHD,
    dh.MaDH,
    hd.PhaiTra AS TienPhaiThu,
    ISNULL(SUM(tt.SoTien), 0) AS TongTienDaThu,
    ABS(hd.PhaiTra - ISNULL(SUM(tt.SoTien), 0)) AS ChenhLech
FROM dbo.HoaDon hd
INNER JOIN dbo.DonHang dh ON hd.MaDH = dh.MaDH
LEFT JOIN dbo.ThanhToan tt ON hd.MaHD = tt.MaHD
WHERE dh.TrangThai = 'HOAN_THANH' AND hd.TrangThai = 'HIEU_LUC'
GROUP BY hd.MaHD, dh.MaDH, hd.PhaiTra
HAVING ABS(hd.PhaiTra - ISNULL(SUM(tt.SoTien), 0)) > 0.01;

DECLARE @PaymentMismatchCount INT = (
    SELECT COUNT(*) FROM (
        SELECT hd.MaHD
        FROM dbo.HoaDon hd
        INNER JOIN dbo.DonHang dh ON hd.MaDH = dh.MaDH
        LEFT JOIN dbo.ThanhToan tt ON hd.MaHD = tt.MaHD
        WHERE dh.TrangThai = 'HOAN_THANH' AND hd.TrangThai = 'HIEU_LUC'
        GROUP BY hd.MaHD, hd.PhaiTra
        HAVING ABS(hd.PhaiTra - ISNULL(SUM(tt.SoTien), 0)) > 0.01
    ) t
);

IF @PaymentMismatchCount = 0
    PRINT N'   [PASS] 100% hóa đơn của đơn HOAN_THANH khớp đúng tiền thực thu trong bảng ThanhToan.';
ELSE
BEGIN
    PRINT N'   [FAIL] Có ' + CAST(@PaymentMismatchCount AS NVARCHAR(10)) + N' hóa đơn bị lệch giữa khoản thu và tiền phải trả!';
    SET @ViolationCount = @ViolationCount + @PaymentMismatchCount;
END;

-- 3. Tổng hợp doanh thu thực tế theo Chi Nhánh
PRINT N'3. Báo cáo doanh thu thực thu theo từng chi nhánh:';
SELECT 
    cn.TenChiNhanh,
    COUNT(DISTINCT hd.MaHD) AS SoLuongHoaDon,
    ISNULL(SUM(CASE WHEN tt.PhuongThuc = 'CASH' THEN tt.SoTien ELSE 0 END), 0) AS ThuTienMat,
    ISNULL(SUM(CASE WHEN tt.PhuongThuc = 'BANK_TRANSFER' THEN tt.SoTien ELSE 0 END), 0) AS ThuChuyenKhoan,
    ISNULL(SUM(tt.SoTien), 0) AS TongThucThu
FROM dbo.ChiNhanh cn
LEFT JOIN dbo.DonHang dh ON dh.MaCN = cn.MaCN AND dh.TrangThai = 'HOAN_THANH'
LEFT JOIN dbo.HoaDon hd ON hd.MaDH = dh.MaDH AND hd.TrangThai = 'HIEU_LUC'
LEFT JOIN dbo.ThanhToan tt ON tt.MaHD = hd.MaHD
GROUP BY cn.TenChiNhanh
ORDER BY TongThucThu DESC;

-- 4. Đối soát số lượt sử dụng Voucher (SoLuotDaDung vs LichSuSuDungVoucher)
PRINT N'4. Đối soát lượt sử dụng Voucher (SoLuotDaDung vs LichSuSuDungVoucher):';

SELECT 
    v.MaVoucher,
    v.TenVoucher,
    v.SoLuotDaDung AS UsedCountInVoucher,
    COUNT(ls.MaLichSu) AS CountInHistory,
    ABS(v.SoLuotDaDung - COUNT(ls.MaLichSu)) AS ChenhLech
FROM dbo.Voucher v
LEFT JOIN dbo.LichSuSuDungVoucher ls ON v.MaVoucher = ls.MaVoucher AND ls.TrangThai = 'DA_DUNG'
GROUP BY v.MaVoucher, v.TenVoucher, v.SoLuotDaDung
HAVING ABS(v.SoLuotDaDung - COUNT(ls.MaLichSu)) > 0;

DECLARE @VoucherCountMismatch INT = (
    SELECT COUNT(*) FROM (
        SELECT v.MaVoucher
        FROM dbo.Voucher v
        LEFT JOIN dbo.LichSuSuDungVoucher ls ON v.MaVoucher = ls.MaVoucher AND ls.TrangThai = 'DA_DUNG'
        GROUP BY v.MaVoucher, v.SoLuotDaDung
        HAVING ABS(v.SoLuotDaDung - COUNT(ls.MaLichSu)) > 0
    ) t
);

IF @VoucherCountMismatch = 0
    PRINT N'   [PASS] 100% voucher có SoLuotDaDung khớp số bản ghi sử dụng thành công trong lịch sử.';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện ' + CAST(@VoucherCountMismatch AS NVARCHAR(10)) + N' voucher lệch giữa SoLuotDaDung và lịch sử sử dụng!';
    SET @ViolationCount = @ViolationCount + @VoucherCountMismatch;
END;

PRINT N'====================================================================';
PRINT N'  TỔNG KẾT ĐỐI SOÁT TÀI CHÍNH';
PRINT N'====================================================================';

IF @ViolationCount > 0
BEGIN
    DECLARE @ErrMsg NVARCHAR(250) = N'ĐỐI SOÁT TÀI CHÍNH THẤT BẠI: Phát hiện ' + CAST(@ViolationCount AS NVARCHAR(10)) + N' vi phạm dữ liệu!';
    THROW 50000, @ErrMsg, 1;
END;

PRINT N'>>> ĐỐI SOÁT TÀI CHÍNH 100% HOÀN HẢO - KHÔNG CÓ BẤT KỲ LỖI NÀO.';
GO
