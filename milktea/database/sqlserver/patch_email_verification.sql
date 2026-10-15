-- ============================================================================
-- Migration Patch: Bổ sung xác thực email và quản lý mã OTP an toàn
-- Tương thích ngược: Giữ nguyên toàn bộ tài khoản hiện có (gán EmailXacThuc = 1)
-- ============================================================================

USE milktea;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- 1. Bổ sung cột EmailXacThuc vào bảng dbo.TaiKhoan nếu chưa có
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.TaiKhoan') AND name = 'EmailXacThuc'
)
BEGIN
    PRINT N'-- Đang thêm cột EmailXacThuc vào dbo.TaiKhoan...';
    ALTER TABLE dbo.TaiKhoan 
    ADD EmailXacThuc BIT NOT NULL CONSTRAINT DF_TaiKhoan_EmailXacThuc DEFAULT 1 WITH VALUES;
    
    EXEC('UPDATE dbo.TaiKhoan SET EmailXacThuc = 1 WHERE EmailXacThuc IS NULL OR EmailXacThuc = 0');
    PRINT N'-- Đã thêm cột EmailXacThuc thành công (toàn bộ tài khoản cũ giữ nguyên quyền đăng nhập).';
END
ELSE
BEGIN
    PRINT N'-- Cột EmailXacThuc đã tồn tại trong dbo.TaiKhoan.';
    EXEC('UPDATE dbo.TaiKhoan SET EmailXacThuc = 1 WHERE EmailXacThuc IS NULL');
END
GO

-- 2. Tạo bảng dbo.XacThucEmail nếu chưa có
IF OBJECT_ID('dbo.XacThucEmail', 'U') IS NULL
BEGIN
    PRINT N'-- Đang tạo bảng dbo.XacThucEmail...';
    CREATE TABLE dbo.XacThucEmail (
        MaOTP BIGINT IDENTITY(1, 1) PRIMARY KEY,
        Email VARCHAR(254) NOT NULL,
        OtpHash VARCHAR(255) NOT NULL,
        Salt VARCHAR(64) NOT NULL,
        MucDich VARCHAR(32) NOT NULL, -- REGISTER_VERIFY, PASSWORD_RESET
        HetHanLuc DATETIME2 NOT NULL,
        SoLanThu INT NOT NULL CONSTRAINT DF_XacThucEmail_SoLanThu DEFAULT 0,
        ToiDaThu INT NOT NULL CONSTRAINT DF_XacThucEmail_ToiDaThu DEFAULT 5,
        DaDungLuc DATETIME2 NULL,
        VoHieuLuc DATETIME2 NULL,
        GuiCuoiLuc DATETIME2 NOT NULL,
        TaoLuc DATETIME2 NOT NULL
    );

    CREATE INDEX IX_XacThucEmail_Email_MucDich ON dbo.XacThucEmail(Email, MucDich);
    PRINT N'-- Đã tạo bảng dbo.XacThucEmail thành công.';
END
ELSE
BEGIN
    PRINT N'-- Bảng dbo.XacThucEmail đã tồn tại.';
END
GO

PRINT N'>>> Patch xác thực email & OTP hoàn tất thành công!';
GO
