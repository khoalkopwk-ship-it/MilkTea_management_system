-- ============================================================================
-- V4__central_warehouse.sql
-- Khởi tạo Kho Hàng Tổng (KHOTONG) và đồng bộ tồn kho nguyên liệu toàn chuỗi
-- ============================================================================

SET NOCOUNT ON;

-- 1. KHỞI TẠO CHI NHÁNH KHO HÀNG TỔNG NẾU CHƯA CÓ
IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'KHOTONG')
BEGIN
    INSERT INTO dbo.ChiNhanh (MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai, ThoiGianTao)
    VALUES ('KHOTONG', N'Kho Hàng Tổng (Toàn Chuỗi)', N'Tổng Kho Trung Tâm MilkTea Chain, TP.HCM', '19001234', N'HOAT_DONG', SYSUTCDATETIME());
    PRINT N'Đã khởi tạo Chi nhánh Kho Hàng Tổng (KHOTONG)';
END
GO

-- 2. ĐỒNG BỘ TỒN KHO NGUYÊN LIỆU THÔ CHO KHO HÀNG TỔNG
DECLARE @khoTongId BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'KHOTONG');

IF @khoTongId IS NOT NULL
BEGIN
    -- Chèn tồn kho vị trí KHO cho tất cả nguyên liệu thô (THO) nếu chưa có
    INSERT INTO dbo.TonNguyenLieu (MaCN, MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
    SELECT 
        @khoTongId, 
        m.MaNL, 
        'KHO', 
        1000.000, 
        50.000, 
        COALESCE(
            (SELECT TOP 1 GiaVonDonVi FROM dbo.TonNguyenLieu WHERE MaNL = m.MaNL AND GiaVonDonVi IS NOT NULL AND GiaVonDonVi > 0),
            20000.000000
        ), 
        0
    FROM dbo.NguyenLieu m
    WHERE m.Loai = 'THO'
      AND NOT EXISTS (
          SELECT 1 FROM dbo.TonNguyenLieu t 
          WHERE t.MaCN = @khoTongId AND t.MaNL = m.MaNL AND t.ViTri = 'KHO'
      );

    PRINT N'Đã đồng bộ nguyên liệu thô vào Kho Hàng Tổng (KHOTONG)';
END
GO
