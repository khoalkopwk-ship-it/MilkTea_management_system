-- ============================================================================
-- SCRIPT KIỂM TRA TÍNH TOÀN VẸN DỮ LIỆU DEMO CHUỖI MILKTEA MULTI-BRANCH
-- File: database/validation/verify_demo_integrity.sql
-- ============================================================================

USE milktea;
GO

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
  AND tk.TrangThai = 0;

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

-- 3. Kiểm tra Bàn Ăn & Mã QR
PRINT N'3. Kiểm tra Bàn ăn & Mã QR phân nhánh:';
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
    LEFT JOIN dbo.NguyenLieu nl ON ctsc.MaNLThanhPham = nl.MaNL
    WHERE nl.MaNL IS NULL OR nl.LoaiNguyenLieu <> 'SO_CHE'
);

IF @OrphanPreps = 0
    PRINT N'   [PASS] 100% công thức sơ chế liên kết đúng nguyên liệu thành phẩm SO_CHE.';
ELSE
BEGIN
    PRINT N'   [FAIL] Có ' + CAST(@OrphanPreps AS NVARCHAR(10)) + N' công thức sơ chế không khớp thành phẩm SO_CHE!';
    SET @FailCount = @FailCount + 1;
END

-- 6. Kiểm tra Voucher & Giới hạn
PRINT N'6. Kiểm tra Voucher Khuyến mãi:';
DECLARE @InvalidVoucher INT = (
    SELECT COUNT(*)
    FROM dbo.Voucher
    WHERE NgayKetThuc < NgayBatDau OR GioiHanSuDung < SoLuongDaDung
);

IF @InvalidVoucher = 0
    PRINT N'   [PASS] Toàn bộ voucher có thời gian và giới hạn sử dụng logic.';
ELSE
BEGIN
    PRINT N'   [FAIL] Có ' + CAST(@InvalidVoucher AS NVARCHAR(10)) + N' voucher sai logic ngày hoặc vượt quota!';
    SET @FailCount = @FailCount + 1;
END

PRINT N'--------------------------------------------------------------------';
IF @FailCount = 0
    PRINT N'>>> KẾT LUẬN: TẤT CẢ CÁC MỤC KIỂM TRA ĐỀU ĐẠT CHUẨN [PASS 100%].';
ELSE
    PRINT N'>>> KẾT LUẬN: CÓ ' + CAST(@FailCount AS NVARCHAR(10)) + N' MỤC CHƯA ĐẠT. VUI LÒNG KIỂM TRA LẠI!';
PRINT N'====================================================================';
GO
