-- =======================================================================
-- MIGRATION V8: Bổ sung Hotline & Giờ hoạt động cho Cấu hình chung và Chi nhánh
-- =======================================================================

-- 1. Bổ sung Hotline và GioHoatDong vào bảng CauHinhChung
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.CauHinhChung') AND name = 'Hotline'
)
BEGIN
    ALTER TABLE dbo.CauHinhChung ADD Hotline NVARCHAR(30) NULL;
    PRINT N'Đã thêm cột Hotline vào bảng CauHinhChung';
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.CauHinhChung') AND name = 'GioHoatDong'
)
BEGIN
    ALTER TABLE dbo.CauHinhChung ADD GioHoatDong NVARCHAR(120) NULL;
    PRINT N'Đã thêm cột GioHoatDong vào bảng CauHinhChung';
END
GO

-- 2. Bổ sung GioMoCua vào bảng ChiNhanh
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.ChiNhanh') AND name = 'GioMoCua'
)
BEGIN
    ALTER TABLE dbo.ChiNhanh ADD GioMoCua NVARCHAR(120) NULL;
    PRINT N'Đã thêm cột GioMoCua vào bảng ChiNhanh';
END
GO

-- 3. Backfill dữ liệu mặc định
UPDATE dbo.CauHinhChung
SET Hotline = COALESCE(Hotline, N'0345121523'),
    GioHoatDong = COALESCE(GioHoatDong, N'08:00 – 22:00 (Hằng ngày)')
WHERE MaCauHinh = 1;

UPDATE dbo.ChiNhanh
SET GioMoCua = COALESCE(GioMoCua, N'08:00 - 22:00')
WHERE GioMoCua IS NULL;
GO
