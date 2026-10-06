-- =========================================================
-- 001-foundation.sql
-- Nền database dùng chung cho hệ thống MilkTea
-- SQL Server
-- =========================================================

CREATE TABLE branches (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name NVARCHAR(150) NOT NULL,
    address NVARCHAR(255) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

INSERT INTO branches (code, name, address, active)
VALUES
('CN1', N'Chi nhánh chính', N'Chi nhánh demo', 1);