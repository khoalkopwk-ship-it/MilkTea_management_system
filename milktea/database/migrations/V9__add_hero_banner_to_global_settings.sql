-- =======================================================================
-- MIGRATION V9: Bổ sung Hero Banner URL và PublicId cho CauHinhChung
-- =======================================================================

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.CauHinhChung') AND name = 'HeroBannerUrl'
)
BEGIN
    ALTER TABLE dbo.CauHinhChung ADD HeroBannerUrl NVARCHAR(1000) NULL;
    PRINT N'Đã thêm cột HeroBannerUrl vào bảng CauHinhChung';
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.CauHinhChung') AND name = 'HeroBannerPublicId'
)
BEGIN
    ALTER TABLE dbo.CauHinhChung ADD HeroBannerPublicId NVARCHAR(255) NULL;
    PRINT N'Đã thêm cột HeroBannerPublicId vào bảng CauHinhChung';
END
GO
