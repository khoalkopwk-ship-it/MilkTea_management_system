-- ============================================================================
-- SCRIPT ĐỐI SOÁT TÀI CHÍNH & DOANH THU CHUỖI MILKTEA MULTI-BRANCH
-- File: database/validation/verify_financial_reconciliation.sql
-- ============================================================================

USE milktea;
GO

SET NOCOUNT ON;

PRINT N'====================================================================';
PRINT N'  ĐỐI SOÁT TÀI CHÍNH, HÓA ĐƠN & DOANH THU (FINANCIAL RECONCILIATION)';
PRINT N'====================================================================';

-- 1. Kiểm tra công thức Hóa đơn: TongTien = TienHang - GiamGia - GiamGiaVoucher
PRINT N'1. Kiểm tra tính toàn vẹn công thức tính tiền hóa đơn:';
SELECT 
    hd.MaHD,
    hd.TienHang,
    hd.GiamGia,
    ISNULL(hd.GiamGiaVoucher, 0) AS GiamGiaVoucher,
    hd.TongTien,
    (hd.TienHang - hd.GiamGia - ISNULL(hd.GiamGiaVoucher, 0)) AS TongTienKyVong,
    ABS(hd.TongTien - (hd.TienHang - hd.GiamGia - ISNULL(hd.GiamGiaVoucher, 0))) AS DoLech
FROM dbo.HoaDon hd
WHERE ABS(hd.TongTien - (hd.TienHang - hd.GiamGia - ISNULL(hd.GiamGiaVoucher, 0))) > 0.01;

DECLARE @InvoiceFormulaError INT = (
    SELECT COUNT(*) 
    FROM dbo.HoaDon hd
    WHERE ABS(hd.TongTien - (hd.TienHang - hd.GiamGia - ISNULL(hd.GiamGiaVoucher, 0))) > 0.01
);

IF @InvoiceFormulaError = 0
    PRINT N'   [PASS] 100% hóa đơn khớp chính xác công thức: Tổng = Tiền hàng - Giảm giá - Voucher.';
ELSE
    PRINT N'   [FAIL] Phát hiện ' + CAST(@InvoiceFormulaError AS NVARCHAR(10)) + N' hóa đơn bị lệch số tiền!';

-- 2. Đối soát tiền thực thu trong ThanhToan với TongTien hóa đơn
PRINT N'2. Đối soát thanh toán thực thu với hóa đơn đã thanh toán:';
SELECT 
    hd.MaHD,
    hd.TongTien AS TongTienHoaDon,
    ISNULL(SUM(tt.SoTien), 0) AS TongTienDaThu,
    ABS(hd.TongTien - ISNULL(SUM(tt.SoTien), 0)) AS ChenhLech
FROM dbo.HoaDon hd
JOIN dbo.ThanhToan tt ON hd.MaHD = tt.MaHD
WHERE hd.TrangThai = 'DA_THANH_TOAN'
GROUP BY hd.MaHD, hd.TongTien
HAVING ABS(hd.TongTien - ISNULL(SUM(tt.SoTien), 0)) > 0.01;

DECLARE @PaymentMismatchCount INT = (
    SELECT COUNT(*) FROM (
        SELECT hd.MaHD
        FROM dbo.HoaDon hd
        JOIN dbo.ThanhToan tt ON hd.MaHD = tt.MaHD
        WHERE hd.TrangThai = 'DA_THANH_TOAN'
        GROUP BY hd.MaHD, hd.TongTien
        HAVING ABS(hd.TongTien - ISNULL(SUM(tt.SoTien), 0)) > 0.01
    ) t
);

IF @PaymentMismatchCount = 0
    PRINT N'   [PASS] 100% hóa đơn DA_THANH_TOAN khớp tiền thực thu trong bảng ThanhToan.';
ELSE
    PRINT N'   [FAIL] Có ' + CAST(@PaymentMismatchCount AS NVARCHAR(10)) + N' hóa đơn bị lệch giữa khoản thu và tổng tiền!';

-- 3. Tổng hợp doanh thu thực tế theo Chi Nhánh
PRINT N'3. Báo cáo doanh thu thực thu theo từng chi nhánh:';
SELECT 
    cn.TenChiNhanh,
    COUNT(DISTINCT hd.MaHD) AS SoLuongHoaDon,
    ISNULL(SUM(CASE WHEN tt.PhuongThuc = 'CASH' THEN tt.SoTien ELSE 0 END), 0) AS ThuTienMat,
    ISNULL(SUM(CASE WHEN tt.PhuongThuc = 'BANK_TRANSFER' THEN tt.SoTien ELSE 0 END), 0) AS ThuChuyenKhoan,
    ISNULL(SUM(tt.SoTien), 0) AS TongThucThu
FROM dbo.ChiNhanh cn
LEFT JOIN dbo.DonHang dh ON dh.MaCN = cn.MaCN
LEFT JOIN dbo.HoaDon hd ON hd.MaDH = dh.MaDH AND hd.TrangThai = 'DA_THANH_TOAN'
LEFT JOIN dbo.ThanhToan tt ON tt.MaHD = hd.MaHD
GROUP BY cn.TenChiNhanh
ORDER BY TongThucThu DESC;

-- 4. Đối soát số lượt sử dụng Voucher
PRINT N'4. Đối soát lượt sử dụng Voucher (usedCount vs LichSuSuDungVoucher):';
SELECT 
    v.MaVoucher,
    v.TenVoucher,
    v.SoLuongDaDung AS UsedCountInVoucher,
    COUNT(ls.MaLichSu) AS CountInHistory,
    ABS(v.SoLuongDaDung - COUNT(ls.MaLichSu)) AS ChenhLech
FROM dbo.Voucher v
LEFT JOIN dbo.LichSuSuDungVoucher ls ON v.MaVoucher = ls.MaVoucher AND ls.TrangThai = 'DA_DUNG'
GROUP BY v.MaVoucher, v.TenVoucher, v.SoLuongDaDung
HAVING ABS(v.SoLuongDaDung - COUNT(ls.MaLichSu)) > 0;

DECLARE @VoucherCountMismatch INT = (
    SELECT COUNT(*) FROM (
        SELECT v.MaVoucher
        FROM dbo.Voucher v
        LEFT JOIN dbo.LichSuSuDungVoucher ls ON v.MaVoucher = ls.MaVoucher AND ls.TrangThai = 'DA_DUNG'
        GROUP BY v.MaVoucher, v.SoLuongDaDung
        HAVING ABS(v.SoLuongDaDung - COUNT(ls.MaLichSu)) > 0
    ) t
);

IF @VoucherCountMismatch = 0
    PRINT N'   [PASS] 100% voucher có usedCount khớp số bản ghi sử dụng thành công trong lịch sử.';
ELSE
    PRINT N'   [FAIL] Phát hiện ' + CAST(@VoucherCountMismatch AS NVARCHAR(10)) + N' voucher lệch giữa usedCount và lịch sử sử dụng!';

PRINT N'====================================================================';
PRINT N'  HOÀN TẤT ĐỐI SOÁT TÀI CHÍNH';
PRINT N'====================================================================';
GO
