-- =========================================================================================
-- KỊCH BẢN CẬP NHẬT CƠ SỞ DỮ LIỆU MILKTEA CHAIN TỪ COMMIT a5f976a LÊN BẢN MỚI NHẤT
-- File: database/update_from_previous_deploy.sql
-- Mục đích: Chạy trực tiếp trên CSDL đã deploy trên internet mà KHÔNG làm mất dữ liệu cũ.
-- Tính chất: Idempotent (Chạy một lần hay nhiều lần đều an toàn, tự động kiểm tra tồn tại).
-- =========================================================================================

SET NOCOUNT ON;
PRINT N'=========================================================================================';
PRINT N'BẮT ĐẦU NÂNG CẤP CƠ SỞ DỮ LIỆU MILKTEA CHAIN TỪ COMMIT a5f976a LÊN PHIÊN BẢN MỚI NHẤT...';
PRINT N'Thời gian chạy: ' + CONVERT(NVARCHAR(30), GETDATE(), 120);
PRINT N'=========================================================================================';

-- -----------------------------------------------------------------------------------------
-- PHẦN 1: HỆ THỐNG ĐA CHI NHÁNH (BRANCH CHAIN)
-- -----------------------------------------------------------------------------------------
PRINT N'--> [1/8] Kiểm tra và khởi tạo bảng ChiNhanh...';

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
        GioMoCua NVARCHAR(120) NULL,
        CONSTRAINT PK_ChiNhanh PRIMARY KEY (MaCN),
        CONSTRAINT UQ_ChiNhanh_MaDinhDanh UNIQUE (MaDinhDanh)
    );
    PRINT N'   + Đã tạo bảng ChiNhanh.';
END
ELSE
BEGIN
    IF COL_LENGTH('dbo.ChiNhanh', 'GioMoCua') IS NULL
    BEGIN
        ALTER TABLE dbo.ChiNhanh ADD GioMoCua NVARCHAR(120) NULL;
        PRINT N'   + Đã thêm cột GioMoCua vào bảng ChiNhanh.';
    END
END
GO

-- Khởi tạo chi nhánh 1 (CN01) và chi nhánh 2 (CN02) nếu chưa có
IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01')
BEGIN
    SET IDENTITY_INSERT dbo.ChiNhanh ON;
    INSERT INTO dbo.ChiNhanh (MaCN, MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai, GioMoCua)
    VALUES (1, 'CN01', N'MilkTea Quán 01 - Võ Văn Ngân', N'1 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP.HCM', '02838968641', N'HOAT_DONG', N'08:00 – 22:00');
    SET IDENTITY_INSERT dbo.ChiNhanh OFF;
    PRINT N'   + Đã khởi tạo Chi nhánh 1 (CN01 - Trụ sở chính).';
END

IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02')
BEGIN
    SET IDENTITY_INSERT dbo.ChiNhanh ON;
    INSERT INTO dbo.ChiNhanh (MaCN, MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai, GioMoCua)
    VALUES (2, 'CN02', N'MilkTea Quán 02 - D2 Bình Thạnh', N'10 Nguyễn Gia Trí (D2), Phường 25, Bình Thạnh, TP.HCM', '02835123456', N'HOAT_DONG', N'08:00 – 22:00');
    SET IDENTITY_INSERT dbo.ChiNhanh OFF;
    PRINT N'   + Đã khởi tạo Chi nhánh 2 (CN02).';
END

IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'KHOTONG')
BEGIN
    INSERT INTO dbo.ChiNhanh (MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai, GioMoCua)
    VALUES ('KHOTONG', N'Kho Hàng Tổng (Toàn Chuỗi)', N'Tổng Kho Trung Tâm MilkTea Chain, TP.HCM', '19001234', N'HOAT_DONG', N'07:00 – 23:00');
    PRINT N'   + Đã khởi tạo Chi nhánh Kho Hàng Tổng (KHOTONG).';
END
GO

-- Bảng NhanVienChiNhanh
PRINT N'--> [2/8] Kiểm tra và khởi tạo bảng NhanVienChiNhanh & MonUongChiNhanh...';
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
    PRINT N'   + Đã tạo bảng NhanVienChiNhanh.';
END
GO

-- Bảng MonUongChiNhanh
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
    PRINT N'   + Đã tạo bảng MonUongChiNhanh.';
END
GO

-- -----------------------------------------------------------------------------------------
-- PHẦN 2: BỔ SUNG MaCN VÀ BACKFILL DỮ LIỆU CŨ VÀO CHI NHÁNH 1
-- -----------------------------------------------------------------------------------------
PRINT N'--> [3/8] Bổ sung cột MaCN và backfill dữ liệu cũ sang Chi nhánh 1...';

-- 1. Bảng Ban
IF COL_LENGTH('dbo.Ban', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.Ban ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.Ban SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.Ban ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.Ban ADD CONSTRAINT FK_Ban_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_Ban_TenBan' AND object_id = OBJECT_ID('dbo.Ban'))
    BEGIN
        ALTER TABLE dbo.Ban DROP CONSTRAINT UQ_Ban_TenBan;
    END

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_Ban_ChiNhanh_Ten' AND object_id = OBJECT_ID('dbo.Ban'))
    BEGIN
        ALTER TABLE dbo.Ban ADD CONSTRAINT UQ_Ban_ChiNhanh_Ten UNIQUE (MaCN, TenBan);
    END
    PRINT N'   + Đã cập nhật MaCN cho bảng Ban.';
END
GO

-- 2. Bảng DonHang
IF COL_LENGTH('dbo.DonHang', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.DonHang SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.DonHang ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.DonHang ADD CONSTRAINT FK_DonHang_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'   + Đã cập nhật MaCN cho bảng DonHang.';
END
GO

-- 3. Bảng TonNguyenLieu
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
    PRINT N'   + Đã cập nhật MaCN và PK cho bảng TonNguyenLieu.';
END
GO

-- 4. Bảng PhieuNhap
IF COL_LENGTH('dbo.PhieuNhap', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.PhieuNhap ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.PhieuNhap SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.PhieuNhap ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.PhieuNhap ADD CONSTRAINT FK_PhieuNhap_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'   + Đã cập nhật MaCN cho bảng PhieuNhap.';
END
GO

-- 5. Bảng MeSoChe
IF COL_LENGTH('dbo.MeSoChe', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.MeSoChe ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.MeSoChe SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.MeSoChe ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.MeSoChe ADD CONSTRAINT FK_MeSoChe_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'   + Đã cập nhật MaCN cho bảng MeSoChe.';
END
GO

-- 6. Bảng KetCaKho
IF COL_LENGTH('dbo.KetCaKho', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.KetCaKho ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.KetCaKho SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.KetCaKho ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.KetCaKho ADD CONSTRAINT FK_KetCaKho_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'   + Đã cập nhật MaCN cho bảng KetCaKho.';
END
GO

-- 7. Bảng LichSuKho
IF COL_LENGTH('dbo.LichSuKho', 'MaCN') IS NULL
BEGIN
    ALTER TABLE dbo.LichSuKho ADD MaCN BIGINT NULL;
    EXEC('UPDATE dbo.LichSuKho SET MaCN = 1 WHERE MaCN IS NULL');
    ALTER TABLE dbo.LichSuKho ALTER COLUMN MaCN BIGINT NOT NULL;
    ALTER TABLE dbo.LichSuKho ADD CONSTRAINT FK_LichSuKho_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN);
    PRINT N'   + Đã cập nhật MaCN cho bảng LichSuKho.';
END
GO

-- Backfill bảng MonUongChiNhanh cho Chi nhánh 1 và 2 từ bảng MonUong
INSERT INTO dbo.MonUongChiNhanh (MaCN, MaMon, KinhDoanh, ConPhucVu)
SELECT 1, m.MaMon, 1, 1
FROM dbo.MonUong m
WHERE NOT EXISTS (SELECT 1 FROM dbo.MonUongChiNhanh mucn WHERE mucn.MaCN = 1 AND mucn.MaMon = m.MaMon);

INSERT INTO dbo.MonUongChiNhanh (MaCN, MaMon, KinhDoanh, ConPhucVu)
SELECT 2, m.MaMon, 1, 1
FROM dbo.MonUong m
WHERE NOT EXISTS (SELECT 1 FROM dbo.MonUongChiNhanh mucn WHERE mucn.MaCN = 2 AND mucn.MaMon = m.MaMon);
GO

-- Phân công tài khoản nhân viên hiện tại vào Chi nhánh 1
INSERT INTO dbo.NhanVienChiNhanh (MaTK, MaCN, VaiTroChiNhanh, TrangThai)
SELECT tk.MaTK, 1, tk.VaiTro, N'HOAT_DONG'
FROM dbo.TaiKhoan tk
WHERE tk.VaiTro IN ('ADMIN', 'CASHIER', 'KITCHEN')
  AND NOT EXISTS (SELECT 1 FROM dbo.NhanVienChiNhanh nv WHERE nv.MaTK = tk.MaTK AND nv.MaCN = 1);
GO

-- Tạo tài khoản quản lý (MANAGER) mẫu nếu chưa có
IF NOT EXISTS (SELECT 1 FROM dbo.TaiKhoan WHERE Email = 'manager1@milktea.vn')
BEGIN
    INSERT INTO dbo.TaiKhoan (Email, MatKhauBam, HoTen, DienThoai, VaiTro, HoatDong, EmailXacThuc, TokenVersion, TaoLuc)
    VALUES ('manager1@milktea.vn', '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq', N'Quản lý Cơ sở 01', '0901111111', 'MANAGER', 1, 1, 0, SYSUTCDATETIME());
    
    DECLARE @m1 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.NhanVienChiNhanh (MaTK, MaCN, VaiTroChiNhanh, TrangThai)
    VALUES (@m1, 1, 'MANAGER', N'HOAT_DONG');
    PRINT N'   + Đã tạo tài khoản manager1@milktea.vn';
END

IF NOT EXISTS (SELECT 1 FROM dbo.TaiKhoan WHERE Email = 'manager2@milktea.vn')
BEGIN
    INSERT INTO dbo.TaiKhoan (Email, MatKhauBam, HoTen, DienThoai, VaiTro, HoatDong, EmailXacThuc, TokenVersion, TaoLuc)
    VALUES ('manager2@milktea.vn', '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq', N'Quản lý Cơ sở 02', '0902222222', 'MANAGER', 1, 1, 0, SYSUTCDATETIME());
    
    DECLARE @m2 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.NhanVienChiNhanh (MaTK, MaCN, VaiTroChiNhanh, TrangThai)
    VALUES (@m2, 2, 'MANAGER', N'HOAT_DONG');
    PRINT N'   + Đã tạo tài khoản manager2@milktea.vn';
END
GO

-- Khởi tạo bàn và tồn kho mẫu cho Chi nhánh 2 nếu chưa có
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
    PRINT N'   + Đã khởi tạo 6 bàn phục vụ cho Chi nhánh 2.';
END

IF NOT EXISTS (SELECT 1 FROM dbo.TonNguyenLieu WHERE MaCN = 2)
BEGIN
    INSERT INTO dbo.TonNguyenLieu (MaCN, MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
    SELECT 2, MaNL, ViTri, 50.000, NguongCanhBao, GiaVonDonVi, 0
    FROM dbo.TonNguyenLieu
    WHERE MaCN = 1;
    PRINT N'   + Đã khởi tạo tồn kho ban đầu cho Chi nhánh 2.';
END
GO

-- Đồng bộ tồn kho nguyên liệu thô vào Kho Hàng Tổng (KHOTONG)
DECLARE @khoTongId BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'KHOTONG');
IF @khoTongId IS NOT NULL
BEGIN
    INSERT INTO dbo.TonNguyenLieu (MaCN, MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
    SELECT 
        @khoTongId, 
        m.MaNL, 
        'KHO', 
        1000.000, 
        50.000, 
        COALESCE((SELECT TOP 1 GiaVonDonVi FROM dbo.TonNguyenLieu WHERE MaNL = m.MaNL AND GiaVonDonVi IS NOT NULL AND GiaVonDonVi > 0), 20000.000000), 
        0
    FROM dbo.NguyenLieu m
    WHERE m.Loai = 'THO'
      AND NOT EXISTS (SELECT 1 FROM dbo.TonNguyenLieu t WHERE t.MaCN = @khoTongId AND t.MaNL = m.MaNL AND t.ViTri = 'KHO');
    PRINT N'   + Đã đồng bộ tồn kho Kho Hàng Tổng (KHOTONG).';
END
GO

-- -----------------------------------------------------------------------------------------
-- PHẦN 3: BẢNG TRẢI NGHIỆM KHÁCH HÀNG (YÊU THÍCH & ĐÁNH GIÁ)
-- -----------------------------------------------------------------------------------------
PRINT N'--> [4/8] Khởi tạo bảng SanPhamYeuThich, DanhGia & DanhGiaMedia...';

-- 1. Bảng SanPhamYeuThich
IF OBJECT_ID('dbo.SanPhamYeuThich', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.SanPhamYeuThich (
        MaYeuThich BIGINT IDENTITY(1,1) NOT NULL,
        MaTK BIGINT NOT NULL,
        MaMon BIGINT NOT NULL,
        TaoLuc DATETIME2 NOT NULL CONSTRAINT DF_SanPhamYeuThich_TaoLuc DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_SanPhamYeuThich PRIMARY KEY CLUSTERED (MaYeuThich ASC),
        CONSTRAINT FK_SanPhamYeuThich_TaiKhoan FOREIGN KEY (MaTK) REFERENCES dbo.TaiKhoan(MaTK),
        CONSTRAINT FK_SanPhamYeuThich_MonUong FOREIGN KEY (MaMon) REFERENCES dbo.MonUong(MaMon),
        CONSTRAINT UQ_SanPhamYeuThich_TaiKhoan_Mon UNIQUE NONCLUSTERED (MaTK, MaMon)
    );
    CREATE NONCLUSTERED INDEX IX_SanPhamYeuThich_MaTK ON dbo.SanPhamYeuThich (MaTK);
    CREATE NONCLUSTERED INDEX IX_SanPhamYeuThich_MaMon ON dbo.SanPhamYeuThich (MaMon);
    PRINT N'   + Đã tạo bảng SanPhamYeuThich.';
END
GO

-- 2. Bảng DanhGia
IF OBJECT_ID('dbo.DanhGia', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.DanhGia (
        MaDanhGia BIGINT IDENTITY(1,1) NOT NULL,
        MaChiTiet BIGINT NOT NULL,
        MaTK BIGINT NOT NULL,
        MaMon BIGINT NOT NULL,
        MaCN BIGINT NOT NULL,
        SoSao INT NOT NULL,
        NoiDung NVARCHAR(1000) NOT NULL,
        An BIT NOT NULL CONSTRAINT DF_DanhGia_An DEFAULT 0,
        LyDoAn NVARCHAR(500) NULL,
        NguoiAn BIGINT NULL,
        AnLuc DATETIME2 NULL,
        TaoLuc DATETIME2 NOT NULL CONSTRAINT DF_DanhGia_TaoLuc DEFAULT SYSUTCDATETIME(),
        CapNhatLuc DATETIME2 NOT NULL CONSTRAINT DF_DanhGia_CapNhatLuc DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_DanhGia PRIMARY KEY CLUSTERED (MaDanhGia ASC),
        CONSTRAINT FK_DanhGia_ChiTietDonHang FOREIGN KEY (MaChiTiet) REFERENCES dbo.ChiTietDonHang(MaChiTiet),
        CONSTRAINT FK_DanhGia_TaiKhoan FOREIGN KEY (MaTK) REFERENCES dbo.TaiKhoan(MaTK),
        CONSTRAINT FK_DanhGia_MonUong FOREIGN KEY (MaMon) REFERENCES dbo.MonUong(MaMon),
        CONSTRAINT FK_DanhGia_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN),
        CONSTRAINT FK_DanhGia_NguoiAn FOREIGN KEY (NguoiAn) REFERENCES dbo.TaiKhoan(MaTK),
        CONSTRAINT CK_DanhGia_SoSao CHECK (SoSao BETWEEN 1 AND 5),
        CONSTRAINT CK_DanhGia_NoiDung_DoDai CHECK (LEN(LTRIM(RTRIM(NoiDung))) >= 50),
        CONSTRAINT UQ_DanhGia_ChiTietDonHang UNIQUE NONCLUSTERED (MaChiTiet)
    );
    CREATE NONCLUSTERED INDEX IX_DanhGia_MaMon ON dbo.DanhGia (MaMon, An);
    CREATE NONCLUSTERED INDEX IX_DanhGia_MaCN ON dbo.DanhGia (MaCN, An);
    CREATE NONCLUSTERED INDEX IX_DanhGia_MaTK ON dbo.DanhGia (MaTK);
    PRINT N'   + Đã tạo bảng DanhGia.';
END
GO

-- 3. Bảng DanhGiaMedia
IF OBJECT_ID('dbo.DanhGiaMedia', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.DanhGiaMedia (
        MaMedia BIGINT IDENTITY(1,1) NOT NULL,
        MaDanhGia BIGINT NOT NULL,
        Url NVARCHAR(1000) NOT NULL,
        LoaiMedia VARCHAR(16) NOT NULL,
        TaoLuc DATETIME2 NOT NULL CONSTRAINT DF_DanhGiaMedia_TaoLuc DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_DanhGiaMedia PRIMARY KEY CLUSTERED (MaMedia ASC),
        CONSTRAINT FK_DanhGiaMedia_DanhGia FOREIGN KEY (MaDanhGia) REFERENCES dbo.DanhGia(MaDanhGia) ON DELETE CASCADE
    );
    CREATE NONCLUSTERED INDEX IX_DanhGiaMedia_MaDanhGia ON dbo.DanhGiaMedia (MaDanhGia);
    PRINT N'   + Đã tạo bảng DanhGiaMedia.';
END
GO

-- -----------------------------------------------------------------------------------------
-- PHẦN 4: HỆ THỐNG VOUCHER & KHUYẾN MÃI (DISCOUNTS & PROMOTIONS)
-- -----------------------------------------------------------------------------------------
PRINT N'--> [5/8] Khởi tạo bảng Voucher, VoucherChiNhanh & LichSuSuDungVoucher...';

-- 1. Bảng Voucher
IF OBJECT_ID('dbo.Voucher', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Voucher (
        MaVoucher BIGINT IDENTITY(1,1) NOT NULL,
        MaCode NVARCHAR(50) NOT NULL,
        TenVoucher NVARCHAR(150) NOT NULL,
        MoTa NVARCHAR(500) NULL,
        LoaiGiamGia VARCHAR(20) NOT NULL,
        GiaTriGiam DECIMAL(18,2) NOT NULL,
        GiamToiDa DECIMAL(18,2) NULL,
        GiaTriDonToiThieu DECIMAL(18,2) NOT NULL CONSTRAINT DF_Voucher_GiaTriDonToiThieu DEFAULT 0,
        TongSoLuot INT NOT NULL,
        SoLuotDaDung INT NOT NULL CONSTRAINT DF_Voucher_SoLuotDaDung DEFAULT 0,
        SoLuotMoiKhach INT NOT NULL CONSTRAINT DF_Voucher_SoLuotMoiKhach DEFAULT 1,
        ApDungToanChuoi BIT NOT NULL CONSTRAINT DF_Voucher_ApDungToanChuoi DEFAULT 1,
        NgayBatDau DATETIME2 NOT NULL,
        NgayKetThuc DATETIME2 NOT NULL,
        KichHoat BIT NOT NULL CONSTRAINT DF_Voucher_KichHoat DEFAULT 1,
        TaoLuc DATETIME2 NOT NULL CONSTRAINT DF_Voucher_TaoLuc DEFAULT SYSUTCDATETIME(),
        CapNhatLuc DATETIME2 NOT NULL CONSTRAINT DF_Voucher_CapNhatLuc DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_Voucher PRIMARY KEY CLUSTERED (MaVoucher ASC),
        CONSTRAINT UQ_Voucher_MaCode UNIQUE NONCLUSTERED (MaCode),
        CONSTRAINT CK_Voucher_LoaiGiamGia CHECK (LoaiGiamGia IN ('PERCENT', 'FIXED_AMOUNT')),
        CONSTRAINT CK_Voucher_SoLuot CHECK (SoLuotDaDung <= TongSoLuot),
        CONSTRAINT CK_Voucher_ThoiHan CHECK (NgayBatDau <= NgayKetThuc)
    );
    CREATE NONCLUSTERED INDEX IX_Voucher_MaCode ON dbo.Voucher (MaCode);
    CREATE NONCLUSTERED INDEX IX_Voucher_ThoiHan ON dbo.Voucher (KichHoat, NgayBatDau, NgayKetThuc);
    PRINT N'   + Đã tạo bảng Voucher.';
END
GO

-- 2. Bảng VoucherChiNhanh
IF OBJECT_ID('dbo.VoucherChiNhanh', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.VoucherChiNhanh (
        MaVoucher BIGINT NOT NULL,
        MaCN BIGINT NOT NULL,
        TaoLuc DATETIME2 NOT NULL CONSTRAINT DF_VoucherChiNhanh_TaoLuc DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_VoucherChiNhanh PRIMARY KEY CLUSTERED (MaVoucher, MaCN),
        CONSTRAINT FK_VoucherChiNhanh_Voucher FOREIGN KEY (MaVoucher) REFERENCES dbo.Voucher(MaVoucher) ON DELETE CASCADE,
        CONSTRAINT FK_VoucherChiNhanh_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN) ON DELETE CASCADE
    );
    CREATE NONCLUSTERED INDEX IX_VoucherChiNhanh_MaCN ON dbo.VoucherChiNhanh (MaCN);
    PRINT N'   + Đã tạo bảng VoucherChiNhanh.';
END
GO

-- 3. Bổ sung cột Snapshot Voucher vào HoaDon và DonHang
IF COL_LENGTH('dbo.HoaDon', 'MaVoucher') IS NULL
BEGIN
    ALTER TABLE dbo.HoaDon ADD MaVoucher BIGINT NULL;
    ALTER TABLE dbo.HoaDon ADD CONSTRAINT FK_HoaDon_Voucher FOREIGN KEY (MaVoucher) REFERENCES dbo.Voucher(MaVoucher);
END
IF COL_LENGTH('dbo.HoaDon', 'MaCodeVoucher') IS NULL
BEGIN
    ALTER TABLE dbo.HoaDon ADD MaCodeVoucher NVARCHAR(50) NULL;
END
IF COL_LENGTH('dbo.HoaDon', 'TienGiamVoucher') IS NULL
BEGIN
    ALTER TABLE dbo.HoaDon ADD TienGiamVoucher DECIMAL(18,2) NOT NULL CONSTRAINT DF_HoaDon_TienGiamVoucher DEFAULT 0;
END
PRINT N'   + Đã bổ sung cột snapshot voucher vào bảng HoaDon.';
GO

IF COL_LENGTH('dbo.DonHang', 'MaVoucher') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD MaVoucher BIGINT NULL;
    ALTER TABLE dbo.DonHang ADD CONSTRAINT FK_DonHang_Voucher FOREIGN KEY (MaVoucher) REFERENCES dbo.Voucher(MaVoucher);
END
IF COL_LENGTH('dbo.DonHang', 'MaCodeVoucher') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD MaCodeVoucher NVARCHAR(50) NULL;
END
PRINT N'   + Đã bổ sung cột snapshot voucher vào bảng DonHang.';
GO

-- 4. Bảng LichSuSuDungVoucher
IF OBJECT_ID('dbo.LichSuSuDungVoucher', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.LichSuSuDungVoucher (
        MaLichSu BIGINT IDENTITY(1,1) NOT NULL,
        MaVoucher BIGINT NOT NULL,
        MaDH BIGINT NOT NULL,
        MaHD BIGINT NULL,
        MaTK BIGINT NULL,
        MaCN BIGINT NOT NULL,
        SoTienGiam DECIMAL(18,2) NOT NULL,
        TrangThai VARCHAR(20) NOT NULL CONSTRAINT DF_LSSDV_TrangThai DEFAULT 'DA_DUNG',
        NgaySuDung DATETIME2 NOT NULL CONSTRAINT DF_LSSDV_NgaySuDung DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_LichSuSuDungVoucher PRIMARY KEY CLUSTERED (MaLichSu ASC),
        CONSTRAINT FK_LSSDV_Voucher FOREIGN KEY (MaVoucher) REFERENCES dbo.Voucher(MaVoucher),
        CONSTRAINT FK_LSSDV_DonHang FOREIGN KEY (MaDH) REFERENCES dbo.DonHang(MaDH),
        CONSTRAINT FK_LSSDV_HoaDon FOREIGN KEY (MaHD) REFERENCES dbo.HoaDon(MaHD),
        CONSTRAINT FK_LSSDV_TaiKhoan FOREIGN KEY (MaTK) REFERENCES dbo.TaiKhoan(MaTK),
        CONSTRAINT FK_LSSDV_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN),
        CONSTRAINT CK_LSSDV_TrangThai CHECK (TrangThai IN ('DA_DUNG', 'DA_HOAN_TRA'))
    );
    CREATE NONCLUSTERED INDEX IX_LSSDV_Voucher_Khach ON dbo.LichSuSuDungVoucher (MaVoucher, MaTK);
    CREATE NONCLUSTERED INDEX IX_LSSDV_DonHang ON dbo.LichSuSuDungVoucher (MaDH);
    PRINT N'   + Đã tạo bảng LichSuSuDungVoucher.';
END
GO

-- Khởi tạo các mã voucher mẫu nếu chưa có
IF NOT EXISTS (SELECT 1 FROM dbo.Voucher WHERE MaCode = 'CHAOBANMOI')
BEGIN
    INSERT INTO dbo.Voucher (MaCode, TenVoucher, MoTa, LoaiGiamGia, GiaTriGiam, GiamToiDa, GiaTriDonToiThieu, TongSoLuot, SoLuotDaDung, SoLuotMoiKhach, ApDungToanChuoi, NgayBatDau, NgayKetThuc, KichHoat)
    VALUES ('CHAOBANMOI', N'Chào Bạn Mới Giảm 20%', N'Ưu đãi giảm 20% cho toàn bộ khách hàng ghé quán', 'PERCENT', 20.00, 30000.00, 50000.00, 500, 0, 1, 1, '2026-01-01', '2027-12-31', 1);
    PRINT N'   + Đã thêm voucher mẫu CHAOBANMOI.';
END

IF NOT EXISTS (SELECT 1 FROM dbo.Voucher WHERE MaCode = 'MILKTEA10K')
BEGIN
    INSERT INTO dbo.Voucher (MaCode, TenVoucher, MoTa, LoaiGiamGia, GiaTriGiam, GiamToiDa, GiaTriDonToiThieu, TongSoLuot, SoLuotDaDung, SoLuotMoiKhach, ApDungToanChuoi, NgayBatDau, NgayKetThuc, KichHoat)
    VALUES ('MILKTEA10K', N'Giảm Trực Tiếp 10.000đ', N'Giảm 10.000đ cho đơn hàng từ 40.000đ', 'FIXED_AMOUNT', 10000.00, NULL, 40000.00, 1000, 0, 2, 1, '2026-01-01', '2027-12-31', 1);
    PRINT N'   + Đã thêm voucher mẫu MILKTEA10K.';
END
GO

-- -----------------------------------------------------------------------------------------
-- PHẦN 5: ĐỒNG BỘ CỘT VÀ CHUẨN HÓA IN-STORE ONLY
-- -----------------------------------------------------------------------------------------
PRINT N'--> [6/8] Đồng bộ cột thời gian TaoLuc, snapshot đơn hàng & In-Store index...';

IF COL_LENGTH('dbo.MonUong', 'TaoLuc') IS NULL
BEGIN
    ALTER TABLE dbo.MonUong ADD TaoLuc DATETIME2 NULL CONSTRAINT DF_MonUong_TaoLuc DEFAULT SYSUTCDATETIME();
    EXEC('UPDATE dbo.MonUong SET TaoLuc = SYSUTCDATETIME() WHERE TaoLuc IS NULL');
    PRINT N'   + Đã thêm cột TaoLuc vào bảng MonUong.';
END
GO

IF COL_LENGTH('dbo.DonHang', 'NguoiNhan') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD NguoiNhan NVARCHAR(100) NULL;
END
IF COL_LENGTH('dbo.DonHang', 'SoDienThoaiNhan') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD SoDienThoaiNhan VARCHAR(20) NULL;
END
IF COL_LENGTH('dbo.DonHang', 'DiaChiGiao') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD DiaChiGiao NVARCHAR(500) NULL;
END
IF COL_LENGTH('dbo.DonHang', 'HinhThucNhan') IS NULL
BEGIN
    ALTER TABLE dbo.DonHang ADD HinhThucNhan VARCHAR(20) NULL;
END
GO

IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_DonHang_HinhThucNhan')
BEGIN
    ALTER TABLE dbo.DonHang DROP CONSTRAINT DF_DonHang_HinhThucNhan;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_DonHang_Nguon_TrangThai_TaoLuc' AND object_id = OBJECT_ID('dbo.DonHang'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_DonHang_Nguon_TrangThai_TaoLuc 
    ON dbo.DonHang (MaCN, Nguon, TrangThai, TaoLuc DESC);
    PRINT N'   + Đã tạo chỉ mục IX_DonHang_Nguon_TrangThai_TaoLuc.';
END
GO

-- -----------------------------------------------------------------------------------------
-- PHẦN 6: RÀNG BUỘC BẤT BIẾN HỆ THỐNG (INTEGRITY INVARIANTS)
-- -----------------------------------------------------------------------------------------
PRINT N'--> [7/8] Thiết lập ràng buộc bất biến (Filtered Unique, Check Constraints)...';

-- 1. Bất biến 1 phiên OPEN trên mỗi bàn
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_PhienBan_MaBan_Open' AND object_id = OBJECT_ID('dbo.PhienBan'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UQ_PhienBan_MaBan_Open
    ON dbo.PhienBan (MaBan)
    WHERE TrangThai = 'OPEN';
    PRINT N'   + Đã tạo chỉ mục UQ_PhienBan_MaBan_Open (1 phiên OPEN / bàn).';
END
GO

-- 2. Mỗi nguyên liệu chỉ xuất hiện 1 lần trong 1 kết ca
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_ChiTietKetCaKho_MaKetCa_MaNL' AND object_id = OBJECT_ID('dbo.ChiTietKetCaKho'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UQ_ChiTietKetCaKho_MaKetCa_MaNL
    ON dbo.ChiTietKetCaKho (MaKetCa, MaNL);
    PRINT N'   + Đã tạo chỉ mục UQ_ChiTietKetCaKho_MaKetCa_MaNL.';
END
GO

-- 3. Kiểm tra số học kết ca
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_ChiTietKetCa_ThucTe')
BEGIN
    ALTER TABLE dbo.ChiTietKetCaKho 
    ADD CONSTRAINT CK_ChiTietKetCa_ThucTe CHECK (TonThucTe = LuongLoaiBo + LuongGiuLai);
    PRINT N'   + Đã tạo ràng buộc CK_ChiTietKetCa_ThucTe.';
END
GO

-- 4. Kiểm tra số học lịch sử kho
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_LichSuKho_TonSau_Math')
BEGIN
    ALTER TABLE dbo.LichSuKho 
    ADD CONSTRAINT CK_LichSuKho_TonSau_Math CHECK (TonSau = TonTruoc + LuongBienDong);
    PRINT N'   + Đã tạo ràng buộc CK_LichSuKho_TonSau_Math.';
END
GO

-- 5. Kiểm tra công thức thanh toán hóa đơn
UPDATE dbo.HoaDon 
SET TienGiam = 0.00 
WHERE MaCodeVoucher IS NOT NULL 
  AND TienGiam = TienGiamVoucher 
  AND PhaiTra = TongTien - TienGiamVoucher;

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_HoaDon_PhaiTra_Calc')
BEGIN
    ALTER TABLE dbo.HoaDon 
    ADD CONSTRAINT CK_HoaDon_PhaiTra_Calc CHECK (
        PhaiTra = CASE 
            WHEN (TongTien - TienGiam - TienGiamVoucher) < 0 THEN 0 
            ELSE (TongTien - TienGiam - TienGiamVoucher) 
        END
    );
    PRINT N'   + Đã tạo ràng buộc CK_HoaDon_PhaiTra_Calc.';
END
GO

-- -----------------------------------------------------------------------------------------
-- PHẦN 7: BỔ SUNG CẤU HÌNH CHUNG (HOTLINE, GIỜ HOẠT ĐỘNG & HERO BANNER)
-- -----------------------------------------------------------------------------------------
PRINT N'--> [8/8] Cập nhật bảng CauHinhChung (Hotline, Giờ hoạt động, Hero Banner)...';

IF COL_LENGTH('dbo.CauHinhChung', 'Hotline') IS NULL
BEGIN
    ALTER TABLE dbo.CauHinhChung ADD Hotline NVARCHAR(30) NULL;
END
IF COL_LENGTH('dbo.CauHinhChung', 'GioHoatDong') IS NULL
BEGIN
    ALTER TABLE dbo.CauHinhChung ADD GioHoatDong NVARCHAR(120) NULL;
END
IF COL_LENGTH('dbo.CauHinhChung', 'HeroBannerUrl') IS NULL
BEGIN
    ALTER TABLE dbo.CauHinhChung ADD HeroBannerUrl NVARCHAR(1000) NULL;
END
IF COL_LENGTH('dbo.CauHinhChung', 'HeroBannerPublicId') IS NULL
BEGIN
    ALTER TABLE dbo.CauHinhChung ADD HeroBannerPublicId NVARCHAR(255) NULL;
END
GO

-- Backfill giá trị mặc định cho cấu hình chung
UPDATE dbo.CauHinhChung
SET Hotline = COALESCE(Hotline, N'0345121523'),
    GioHoatDong = COALESCE(GioHoatDong, N'08:00 – 22:00 (Hằng ngày)'),
    HeroBannerUrl = COALESCE(HeroBannerUrl, N'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=1000&q=80')
WHERE MaCauHinh = 1;

UPDATE dbo.ChiNhanh
SET GioMoCua = COALESCE(GioMoCua, N'08:00 - 22:00')
WHERE GioMoCua IS NULL;
GO

PRINT N'=========================================================================================';
PRINT N'  CẬP NHẬT CƠ SỞ DỮ LIỆU MILKTEA CHAIN HOÀN TẤT 100% THÀNH CÔNG!';
PRINT N'  Toàn bộ dữ liệu cũ đã được bảo toàn an toàn và sẵn sàng cho mã nguồn mới nhất.';
PRINT N'=========================================================================================';
