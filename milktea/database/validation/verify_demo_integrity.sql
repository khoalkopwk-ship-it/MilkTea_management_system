-- ============================================================================
-- SCRIPT KIỂM TRA TÍNH TOÀN VẸN DỮ LIỆU DEMO CHUỖI MILKTEA MULTI-BRANCH
-- File: database/validation/verify_demo_integrity.sql
-- Tuân thủ V4-06, V4-09, V4-10: Đúng schema thực tế, fail-fast THROW khi vi phạm
-- ============================================================================

SET NOCOUNT ON;

PRINT N'====================================================================';
PRINT N'  KIỂM TRA TÍNH TOÀN VẸN DỮ LIỆU CHUỖI ĐA CHI NHÁNH (DEMO INTEGRITY)';
PRINT N'====================================================================';

DECLARE @FailCount INT = 0;

-- 1. Kiểm tra Chi Nhánh
PRINT N'1. Kiểm tra danh mục Chi Nhánh:';
DECLARE @BranchCount INT = (SELECT COUNT(*) FROM dbo.ChiNhanh WHERE MaDinhDanh IN ('KHOTONG', 'CN01', 'CN02') AND TrangThai = 'HOAT_DONG');
IF @BranchCount = 3
    PRINT N'   [PASS] Có đủ 3 chi nhánh cốt lõi (KHOTONG, CN01, CN02) đang hoạt động.';
ELSE
BEGIN
    PRINT N'   [FAIL] Thiếu chi nhánh cốt lõi hoặc trạng thái không hợp lệ. Số lượng: ' + CAST(@BranchCount AS NVARCHAR(10));
    SET @FailCount = @FailCount + 1;
END

-- 2. Kiểm tra Tài Khoản và Phân Công Nhân Sự
PRINT N'2. Kiểm tra Tài khoản & Phân công Nhân sự chi nhánh:';
DECLARE @MissingStaff INT = 0;
SELECT @MissingStaff = COUNT(*)
FROM dbo.TaiKhoan tk
WHERE tk.Email IN ('admin@milktea.vn', 'manager1@milktea.vn', 'manager2@milktea.vn', 
                   'cashier@milktea.vn', 'cashier2@milktea.vn', 'kitchen@milktea.vn', 'kitchen2@milktea.vn')
  AND tk.HoatDong = 0;

IF @MissingStaff = 0
    PRINT N'   [PASS] Tất cả tài khoản vận hành cốt lõi đều active.';
ELSE
BEGIN
    PRINT N'   [FAIL] Có ' + CAST(@MissingStaff AS NVARCHAR(10)) + N' tài khoản vận hành bị vô hiệu.';
    SET @FailCount = @FailCount + 1;
END

-- Kiểm tra phân công chi nhánh của nhân sự
DECLARE @OrphanStaff INT = (
    SELECT COUNT(*) 
    FROM dbo.TaiKhoan tk
    LEFT JOIN dbo.NhanVienChiNhanh nv ON tk.MaTK = nv.MaTK
    WHERE tk.VaiTro IN ('MANAGER', 'CASHIER', 'KITCHEN')
      AND (nv.MaCN IS NULL OR nv.TrangThai <> 'HOAT_DONG')
);

IF @OrphanStaff = 0
    PRINT N'   [PASS] 100% nhân viên (MANAGER, CASHIER, KITCHEN) có phân công chi nhánh hoạt động.';
ELSE
BEGIN
    PRINT N'   [FAIL] Có ' + CAST(@OrphanStaff AS NVARCHAR(10)) + N' nhân viên thiếu phân công chi nhánh active!';
    SET @FailCount = @FailCount + 1;
END

-- 3. Kiểm tra Bàn Ăn, Mã QR & Bất biến 1 phiên OPEN / bàn
PRINT N'3. Kiểm tra Bàn ăn, Mã QR & Phiên bàn:';
DECLARE @DuplicateQR INT = (
    SELECT COUNT(*) FROM (
        SELECT MaQR FROM dbo.Ban GROUP BY MaQR HAVING COUNT(*) > 1
    ) t
);

IF @DuplicateQR = 0
    PRINT N'   [PASS] Toàn bộ mã QR bàn là DUY NHẤT toàn hệ thống.';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện trùng lặp mã QR bàn: ' + CAST(@DuplicateQR AS NVARCHAR(10));
    SET @FailCount = @FailCount + 1;
END

-- Bất biến: Tối đa 1 phiên OPEN trên mỗi bàn
DECLARE @MultipleOpenSessions INT = (
    SELECT COUNT(*) FROM (
        SELECT MaBan FROM dbo.PhienBan WHERE TrangThai = 'OPEN' GROUP BY MaBan HAVING COUNT(*) > 1
    ) t
);

IF @MultipleOpenSessions = 0
    PRINT N'   [PASS] Bất biến: Mỗi bàn có tối đa 1 phiên OPEN tại một thời điểm.';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện bàn có nhiều hơn 1 phiên OPEN: ' + CAST(@MultipleOpenSessions AS NVARCHAR(10));
    SET @FailCount = @FailCount + 1;
END

-- 4. Kiểm tra Thực đơn và Định mức Nguyên liệu
PRINT N'4. Kiểm tra Thực đơn & Định mức Công thức (BOM):';
DECLARE @OrphanRecipes INT = (
    SELECT COUNT(*) 
    FROM dbo.CongThucMon ctm
    LEFT JOIN dbo.NguyenLieu nl ON ctm.MaNL = nl.MaNL
    WHERE nl.MaNL IS NULL
);

IF @OrphanRecipes = 0
    PRINT N'   [PASS] 100% định mức món liên kết đúng nguyên liệu tồn tại.';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện ' + CAST(@OrphanRecipes AS NVARCHAR(10)) + N' công thức trỏ đến nguyên liệu không tồn tại!';
    SET @FailCount = @FailCount + 1;
END

-- 5. Kiểm tra Công thức Sơ chế
PRINT N'5. Kiểm tra Công thức Sơ chế (Mẻ sơ chế Bếp):';
DECLARE @OrphanPreps INT = (
    SELECT COUNT(*)
    FROM dbo.CongThucSoChe ctsc
    LEFT JOIN dbo.NguyenLieu nl ON ctsc.MaNLDauRa = nl.MaNL
    WHERE nl.MaNL IS NULL OR nl.Loai <> 'SOCHE'
);

IF @OrphanPreps = 0
    PRINT N'   [PASS] 100% công thức sơ chế liên kết đúng nguyên liệu thành phẩm SOCHE.';
ELSE
BEGIN
    PRINT N'   [FAIL] Có ' + CAST(@OrphanPreps AS NVARCHAR(10)) + N' công thức sơ chế không khớp thành phẩm SOCHE!';
    SET @FailCount = @FailCount + 1;
END

-- 6. Kiểm tra Voucher & Giới hạn
PRINT N'6. Kiểm tra Voucher Khuyến mãi:';
DECLARE @InvalidVoucher INT = (
    SELECT COUNT(*)
    FROM dbo.Voucher
    WHERE NgayKetThuc < NgayBatDau OR TongSoLuot < SoLuotDaDung
);

IF @InvalidVoucher = 0
    PRINT N'   [PASS] 100% voucher có thời gian hợp lệ và số lượt dùng không vượt hạn mức.';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện ' + CAST(@InvalidVoucher AS NVARCHAR(10)) + N' voucher sai lệch hạn mức/thời gian!';
    SET @FailCount = @FailCount + 1;
END

-- 7. Kiểm tra Bất biến Kết ca kho: TonThucTe = LuongGiuLai + LuongLoaiBo
PRINT N'7. Kiểm tra tính toàn vẹn kết ca kho:';
DECLARE @InvalidSettlementItems INT = (
    SELECT COUNT(*)
    FROM dbo.ChiTietKetCaKho
    WHERE ABS(TonThucTe - (LuongGiuLai + LuongLoaiBo)) > 0.001
);

IF @InvalidSettlementItems = 0
    PRINT N'   [PASS] 100% bản ghi chi tiết kết ca khớp công thức: TonThucTe = LuongGiuLai + LuongLoaiBo.';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện ' + CAST(@InvalidSettlementItems AS NVARCHAR(10)) + N' dòng kết ca sai lệch số học!';
    SET @FailCount = @FailCount + 1;
END

PRINT N'====================================================================';
PRINT N'  TỔNG KẾT TOÀN VẸN DỮ LIỆU DEMO';
PRINT N'====================================================================';

IF @FailCount > 0
BEGIN
    DECLARE @ErrMsg NVARCHAR(250) = N'KIỂM TRA DỮ LIỆU DEMO THẤT BẠI: Có ' + CAST(@FailCount AS NVARCHAR(10)) + N' tiêu chí không đạt!';
    THROW 50000, @ErrMsg, 1;
END;

PRINT N'>>> 100% TIÊU CHÍ TOÀN VẸN DỮ LIỆU DEMO ĐẠT CHUẨN.';
GO
