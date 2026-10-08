USE [milktea];
GO

-- Update all accounts with valid BCrypt hash for 'Password@123'
UPDATE dbo.TaiKhoan
SET MatKhauBam = '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq';
GO

SELECT MaTK, Email, LEN(MatKhauBam) AS LenHash, MatKhauBam
FROM dbo.TaiKhoan;
GO
