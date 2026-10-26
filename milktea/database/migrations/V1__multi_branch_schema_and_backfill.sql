-- ============================================================================
-- V1__multi_branch_schema_and_backfill.sql
-- Di chuyển CSDL sang kiến trúc Chuỗi Đa Chi Nhánh (MilkTea Multi-Branch Chain)
-- Đảm bảo an toàn 100% dữ liệu đang có của quán (Backfill vào Chi nhánh 1).
-- ============================================================================

SET NOCOUNT ON;

-- 1. BẢNG CHI NHÁNH (Branch)
IF OBJECT_ID('dbo.ChiNhanh', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ChiNhanh (
        MaCN BIGINT IDENTITY(1,1) NOT NULL,
        MaDinhDanh VARCHAR(20) NOT NULL,
        TenChiNhanh NVARCHAR(150) NOT NULL,
        DiaChi NVARCHAR(255) NOT NULL,
        SoDienThoai VARCHAR(20) NULL,
        TrangThai NVARCHAR(30) NOT NULL CONSTRAINT DF_ChiNhanh_TrangThai DEFAULT N'HOAT_DONG',
        ThoiGianTao DATETIME2 NOT NULL CONSTRAINT DF_ChiNhanh_ThoiGianTao DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_ChiNhanh PRIMARY KEY (MaCN),
        CONSTRAINT UQ_ChiNhanh_MaDinhDanh UNIQUE (MaDinhDanh)
    );
    PRINT N'Đã tạo bảng ChiNhanh';
END
GO

-- 2. BẢNG PHÂN CÔNG NHÂN VIÊN CHI NHÁNH (StaffBranch)
IF OBJECT_ID('dbo.NhanVienChiNhanh', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.NhanVienChiNhanh (
        MaNVCN BIGINT IDENTITY(1,1) NOT NULL,
        MaTK BIGINT NOT NULL,
        MaCN BIGINT NOT NULL,
        VaiTroChiNhanh NVARCHAR(30) NOT NULL,
        TrangThai NVARCHAR(30) NOT NULL CONSTRAINT DF_NVCN_TrangThai DEFAULT N'HOAT_DONG',
        NgayPhanCong DATETIME2 NOT NULL CONSTRAINT DF_NVCN_NgayPhanCong DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_NhanVienChiNhanh PRIMARY KEY (MaNVCN),
        CONSTRAINT FK_NVCN_TaiKhoan FOREIGN KEY (MaTK) REFERENCES dbo.TaiKhoan(MaTK),
        CONSTRAINT FK_NVCN_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN),
        CONSTRAINT UQ_NVCN_TK_CN UNIQUE (MaTK, MaCN)
    );
    PRINT N'Đã tạo bảng NhanVienChiNhanh';
END
GO

-- 3. BẢNG MÓN ĂN THEO CHI NHÁNH (BranchProduct)
IF OBJECT_ID('dbo.MonUongChiNhanh', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.MonUongChiNhanh (
        MaMonChiNhanh BIGINT IDENTITY(1,1) NOT NULL,
        MaCN BIGINT NOT NULL,
        MaMon BIGINT NOT NULL,
        KinhDoanh BIT NOT NULL CONSTRAINT DF_MonUongChiNhanh_KinhDoanh DEFAULT 1,
        ConPhucVu BIT NOT NULL CONSTRAINT DF_MonUongChiNhanh_ConPhucVu DEFAULT 1,
        CapNhatLuc DATETIME2 NOT NULL CONSTRAINT DF_MonUongChiNhanh_CapNhatLuc DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_MonUongChiNhanh PRIMARY KEY (MaMonChiNhanh),
        CONSTRAINT FK_MonUongChiNhanh_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN),
        CONSTRAINT FK_MonUongChiNhanh_MonUong FOREIGN KEY (MaMon) REFERENCES dbo.MonUong(MaMon),
        CONSTRAINT UQ_MonUongChiNhanh_CN_Mon UNIQUE (MaCN, MaMon)
    );
    PRINT N'Đã tạo bảng MonUongChiNhanh';
END
GO

-- 4. KHỞI TẠO DỮ LIỆU CHI NHÁNH CHUẨN
IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01')
BEGIN
    SET IDENTITY_INSERT dbo.ChiNhanh ON;
    INSERT INTO dbo.ChiNhanh (MaCN, MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai)
    VALUES (1, 'CN01', N'MilkTea Quán 01 - Võ Văn Ngân', N'1 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP.HCM', '02838968641', N'HOAT_DONG');
    SET IDENTITY_INSERT dbo.ChiNhanh OFF;
    PRINT N'Đã khởi tạo Chi nhánh 1 (CN01)';
END

IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02')
BEGIN
    SET IDENTITY_INSERT dbo.ChiNhanh ON;
    INSERT INTO dbo.ChiNhanh (MaCN, MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai)
    VALUES (2, 'CN02', N'MilkTea Quán 02 - D2 Bình Thạnh', N'10 Nguyễn Gia Trí (D2), Phường 25, Bình Thạnh, TP.HCM', '02835123456', N'HOAT_DONG');
    SET IDENTITY_INSERT dbo.ChiNhanh OFF;
    PRINT N'Đã khởi tạo Chi nhánh 2 (CN02)';
END
GO

-- 5. BỔ SUNG CỘT MaCN VÀ RÀNG BUỘC CHO CÁC BẢNG NGHIỆP VỤ

-- 5.1 Bảng Ban
IF COL_LENGTH('dbo.Ban', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.Ban ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.Ban SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.Ban ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.Ban ADD CONSTRAINT FK_Ban_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'Đã thêm MaCN vào bảng Ban và liên kết FK';
END

-- Đổi ràng buộc tên bàn duy nhất toàn cục thành duy nhất THEO CHI NHÁNH
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_Ban_TenBan' AND object_id = OBJECT_ID('dbo.Ban'))
BEGIN
    ALTER TABLE dbo.Ban DROP CONSTRAINT UQ_Ban_TenBan;
    PRINT N'Đã hủy ràng buộc UQ_Ban_TenBan cũ';
END

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_Ban_ChiNhanh_Ten' AND object_id = OBJECT_ID('dbo.Ban'))
BEGIN
    ALTER TABLE dbo.Ban ADD CONSTRAINT UQ_Ban_ChiNhanh_Ten UNIQUE (MaCN, TenBan);
    PRINT N'Đã thêm ràng buộc UQ_Ban_ChiNhanh_Ten (MaCN, TenBan)';
END
GO

-- 5.2 Bảng DonHang
IF COL_LENGTH('dbo.DonHang', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.DonHang SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.DonHang ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.DonHang ADD CONSTRAINT FK_DonHang_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'Đã thêm MaCN vào bảng DonHang và liên kết FK';
END
GO

-- 5.3 Bảng TonNguyenLieu (Tách kho theo Chi nhánh)
IF COL_LENGTH('dbo.TonNguyenLieu', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.TonNguyenLieu ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.TonNguyenLieu SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.TonNguyenLieu ALTER COLUMN MaCN BIGINT NOT NULL;

    IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'PK_TonNguyenLieu' AND parent_object_id = OBJECT_ID('dbo.TonNguyenLieu'))
    BEGIN
        ALTER TABLE dbo.TonNguyenLieu DROP CONSTRAINT PK_TonNguyenLieu;
    END

    ALTER TABLE dbo.TonNguyenLieu ADD CONSTRAINT PK_TonNguyenLieu PRIMARY KEY (MaCN, MaNL, ViTri);
    ALTER TABLE dbo.TonNguyenLieu ADD CONSTRAINT FK_TonNguyenLieu_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'Đã chuyển PK TonNguyenLieu thành (MaCN, MaNL, ViTri)';
END
GO

-- 5.4 Bảng PhieuNhap
IF COL_LENGTH('dbo.PhieuNhap', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.PhieuNhap ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.PhieuNhap SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.PhieuNhap ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.PhieuNhap ADD CONSTRAINT FK_PhieuNhap_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'Đã thêm MaCN vào bảng PhieuNhap';
END
GO

-- 5.5 Bảng MeSoChe
IF COL_LENGTH('dbo.MeSoChe', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.MeSoChe ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.MeSoChe SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.MeSoChe ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.MeSoChe ADD CONSTRAINT FK_MeSoChe_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'Đã thêm MaCN vào bảng MeSoChe';
END
GO

-- 5.6 Bảng KetCaKho
IF COL_LENGTH('dbo.KetCaKho', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.KetCaKho ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.KetCaKho SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.KetCaKho ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.KetCaKho ADD CONSTRAINT FK_KetCaKho_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'Đã thêm MaCN vào bảng KetCaKho';
END
GO

-- 5.7 Bảng LichSuKho
IF COL_LENGTH('dbo.LichSuKho', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.LichSuKho ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.LichSuKho SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.LichSuKho ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.LichSuKho ADD CONSTRAINT FK_LichSuKho_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'Đã thêm MaCN vào bảng LichSuKho';
END
GO

-- 6. BACKFILL MÓN ĂN CHO CÁC CHI NHÁNH (MonUongChiNhanh)
INSERT INTO dbo.MonUongChiNhanh (MaCN, MaMon, KinhDoanh, ConPhucVu)
SELECT 1, m.MaMon, 1, 1
FROM dbo.MonUong m
WHERE NOT EXISTS (SELECT 1 FROM dbo.MonUongChiNhanh mucn WHERE mucn.MaCN = 1 AND mucn.MaMon = m.MaMon);

INSERT INTO dbo.MonUongChiNhanh (MaCN, MaMon, KinhDoanh, ConPhucVu)
SELECT 2, m.MaMon, 1, 1
FROM dbo.MonUong m
WHERE NOT EXISTS (SELECT 1 FROM dbo.MonUongChiNhanh mucn WHERE mucn.MaCN = 2 AND mucn.MaMon = m.MaMon);
PRINT N'Đã đồng bộ MonUongChiNhanh cho Chi nhánh 1 và Chi nhánh 2';
GO

-- 7. KHỞI TẠO TÀI KHOẢN QUẢN LÝ (MANAGER) NẾU CHƯA CÓ
-- Mật khẩu mặc định: 123456
IF NOT EXISTS (SELECT 1 FROM dbo.TaiKhoan WHERE Email = 'manager1@milktea.vn')
BEGIN
    INSERT INTO dbo.TaiKhoan (Email, MatKhauBam, HoTen, DienThoai, VaiTro, HoatDong, EmailXacThuc, TokenVersion, TaoLuc)
    VALUES ('manager1@milktea.vn', '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq', N'Quản lý Cơ sở 01', '0901111111', 'MANAGER', 1, 1, 0, SYSUTCDATETIME());
    PRINT N'Đã tạo tài khoản manager1@milktea.vn';
END

IF NOT EXISTS (SELECT 1 FROM dbo.TaiKhoan WHERE Email = 'manager2@milktea.vn')
BEGIN
    INSERT INTO dbo.TaiKhoan (Email, MatKhauBam, HoTen, DienThoai, VaiTro, HoatDong, EmailXacThuc, TokenVersion, TaoLuc)
    VALUES ('manager2@milktea.vn', '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq', N'Quản lý Cơ sở 02', '0902222222', 'MANAGER', 1, 1, 0, SYSUTCDATETIME());
    PRINT N'Đã tạo tài khoản manager2@milktea.vn';
END
GO

-- 8. PHÂN CÔNG NHÂN VIÊN VÀO CHI NHÁNH (NhanVienChiNhanh)
-- Chi nhánh 1:
-- Admin (1, 5), Cashier (2), Kitchen (4), Manager 1
INSERT INTO dbo.NhanVienChiNhanh (MaTK, MaCN, VaiTroChiNhanh, TrangThai)
SELECT tk.MaTK, 1, tk.VaiTro, N'HOAT_DONG'
FROM dbo.TaiKhoan tk
WHERE tk.Email IN ('admin@milktea.vn', 'cashier@milktea.vn', 'kitchen@milktea.vn', 'admin2@milktea.vn', 'manager1@milktea.vn')
  AND NOT EXISTS (SELECT 1 FROM dbo.NhanVienChiNhanh nv WHERE nv.MaTK = tk.MaTK AND nv.MaCN = 1);

-- Chi nhánh 2:
-- Admin (1), Cashier 2 (6), Kitchen 2 (9), Manager 2
INSERT INTO dbo.NhanVienChiNhanh (MaTK, MaCN, VaiTroChiNhanh, TrangThai)
SELECT tk.MaTK, 2, tk.VaiTro, N'HOAT_DONG'
FROM dbo.TaiKhoan tk
WHERE tk.Email IN ('admin@milktea.vn', 'cashier2@milktea.vn', 'kitchen2@milktea.vn', 'manager2@milktea.vn')
  AND NOT EXISTS (SELECT 1 FROM dbo.NhanVienChiNhanh nv WHERE nv.MaTK = tk.MaTK AND nv.MaCN = 2);

PRINT N'Đã phân công nhân sự vào Chi nhánh 1 và Chi nhánh 2';
GO

-- 9. KHỞI TẠO BÀN VÀ TỒN KHO CHO CHI NHÁNH 2 (CN02) ĐỂ VẬN HÀNH ĐỘC LẬP
-- Tạo 6 bàn cho CN02
IF NOT EXISTS (SELECT 1 FROM dbo.Ban WHERE MaCN = 2)
BEGIN
    INSERT INTO dbo.Ban (TenBan, MaQR, TrangThai, HoatDong, MaCN, Version)
    VALUES 
    (N'Bàn 01', 'CN02_TABLE_01_TOKEN_ABC123', 'TRONG', 1, 2, 0),
    (N'Bàn 02', 'CN02_TABLE_02_TOKEN_DEF456', 'TRONG', 1, 2, 0),
    (N'Bàn 03', 'CN02_TABLE_03_TOKEN_GHI789', 'TRONG', 1, 2, 0),
    (N'Bàn 04', 'CN02_TABLE_04_TOKEN_JKL012', 'TRONG', 1, 2, 0),
    (N'Bàn 05', 'CN02_TABLE_05_TOKEN_MNO345', 'TRONG', 1, 2, 0),
    (N'Bàn 06', 'CN02_TABLE_06_TOKEN_PQR678', 'TRONG', 1, 2, 0);
    PRINT N'Đã tạo 6 bàn cho Chi nhánh 2';
END

-- Khởi tạo tồn kho cho CN02 (Sao chép danh sách nguyên liệu từ CN01 với lượng tồn ban đầu)
IF NOT EXISTS (SELECT 1 FROM dbo.TonNguyenLieu WHERE MaCN = 2)
BEGIN
    INSERT INTO dbo.TonNguyenLieu (MaCN, MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
    SELECT 2, MaNL, ViTri, 50.000, NguongCanhBao, GiaVonDonVi, 0
    FROM dbo.TonNguyenLieu
    WHERE MaCN = 1;
    PRINT N'Đã khởi tạo tồn kho ban đầu cho Chi nhánh 2';
END
GO

PRINT N'==============================================================';
PRINT N'MIGRATION V1 ĐA CHI NHÁNH VÀ BACKFILL ĐÃ HOÀN TẤT THÀNH CÔNG!';
PRINT N'==============================================================';
