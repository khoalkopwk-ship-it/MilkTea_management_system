-- ============================================================================
-- V5__schema_alignment.sql
-- Đồng bộ schema phục vụ 26 nhóm sửa lỗi:
-- 1. Thêm cột thời gian tạo món TaoLuc vào MonUong (F11 - 4 bộ sưu tập nổi bật)
-- 2. Thêm các trường snapshot người nhận, địa chỉ giao và hình thức nhận vào DonHang (F13 - Đặt online)
-- 3. Bảo đảm MaCN trên KetCaKho và MeSoChe (F04, F05 - Kho đa chi nhánh)
-- ============================================================================

SET NOCOUNT ON;

-- 1. Bổ sung TaoLuc vào dbo.MonUong
IF COL_LENGTH('dbo.MonUong', 'TaoLuc') IS NULL
BEGIN
    ALTER TABLE dbo.MonUong ADD TaoLuc DATETIME2 NULL CONSTRAINT DF_MonUong_TaoLuc DEFAULT SYSUTCDATETIME();
    EXEC('UPDATE dbo.MonUong SET TaoLuc = SYSUTCDATETIME() WHERE TaoLuc IS NULL');
    PRINT N'Đã thêm cột TaoLuc vào bảng MonUong';
END
GO

-- 2. Bổ sung các trường thông tin nhận hàng vào dbo.DonHang
IF COL_LENGTH('dbo.DonHang', 'NguoiNhan') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD NguoiNhan NVARCHAR(100) NULL;
    PRINT N'Đã thêm cột NguoiNhan vào bảng DonHang';
END
GO

IF COL_LENGTH('dbo.DonHang', 'SoDienThoaiNhan') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD SoDienThoaiNhan VARCHAR(20) NULL;
    PRINT N'Đã thêm cột SoDienThoaiNhan vào bảng DonHang';
END
GO

IF COL_LENGTH('dbo.DonHang', 'DiaChiGiao') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD DiaChiGiao NVARCHAR(500) NULL;
    PRINT N'Đã thêm cột DiaChiGiao vào bảng DonHang';
END
GO

IF COL_LENGTH('dbo.DonHang', 'HinhThucNhan') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD HinhThucNhan VARCHAR(20) NULL CONSTRAINT DF_DonHang_HinhThucNhan DEFAULT 'PICKUP';
    EXEC('UPDATE dbo.DonHang SET HinhThucNhan = ''PICKUP'' WHERE HinhThucNhan IS NULL');
    PRINT N'Đã thêm cột HinhThucNhan vào bảng DonHang';
END
GO

-- 3. Bảo đảm MaCN trên KetCaKho
IF COL_LENGTH('dbo.KetCaKho', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.KetCaKho ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.KetCaKho SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.KetCaKho ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.KetCaKho ADD CONSTRAINT FK_KetCaKho_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'Đã thêm cột MaCN vào bảng KetCaKho';
END
GO

-- 4. Tạo chỉ mục tối ưu truy vấn kho đa chi nhánh nếu chưa tồn tại
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_LichSuKho_MaCN_TaoLuc' AND object_id = OBJECT_ID('dbo.LichSuKho'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_LichSuKho_MaCN_TaoLuc ON dbo.LichSuKho (MaCN, TaoLuc DESC);
    PRINT N'Đã tạo chỉ mục IX_LichSuKho_MaCN_TaoLuc';
END
GO
