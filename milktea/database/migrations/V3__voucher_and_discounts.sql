-- =========================================================================================
-- V3__voucher_and_discounts.sql
-- Giai đoạn 5: Voucher & Khuyến mãi (CUS-07, AT-05, BR-06)
-- Tuân thủ Single Source of Truth: docs/BUSINESS_RULES.md (Mục 6) & docs/BRANCH_DEVELOPMENT_PLAN.md
-- =========================================================================================

-- 1. Bảng Voucher
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Voucher')
BEGIN
    CREATE TABLE dbo.Voucher (
        MaVoucher BIGINT IDENTITY(1,1) NOT NULL,
        MaCode NVARCHAR(50) NOT NULL,
        TenVoucher NVARCHAR(150) NOT NULL,
        MoTa NVARCHAR(500) NULL,
        LoaiGiamGia VARCHAR(20) NOT NULL, -- PERCENT, FIXED_AMOUNT
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
    PRINT 'Đã tạo bảng dbo.Voucher thành công.';
END;

-- 2. Bảng VoucherChiNhanh (Phạm vi chi nhánh áp dụng nếu không áp dụng toàn chuỗi)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'VoucherChiNhanh')
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
    PRINT 'Đã tạo bảng dbo.VoucherChiNhanh thành công.';
END;

-- 3. Bổ sung cột Snapshot Voucher vào HoaDon
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.HoaDon') AND name = 'MaVoucher')
BEGIN
    ALTER TABLE dbo.HoaDon ADD MaVoucher BIGINT NULL;
    ALTER TABLE dbo.HoaDon ADD CONSTRAINT FK_HoaDon_Voucher FOREIGN KEY (MaVoucher) REFERENCES dbo.Voucher(MaVoucher);
    PRINT 'Đã bổ sung cột MaVoucher vào dbo.HoaDon.';
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.HoaDon') AND name = 'MaCodeVoucher')
BEGIN
    ALTER TABLE dbo.HoaDon ADD MaCodeVoucher NVARCHAR(50) NULL;
    PRINT 'Đã bổ sung cột MaCodeVoucher vào dbo.HoaDon.';
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.HoaDon') AND name = 'TienGiamVoucher')
BEGIN
    ALTER TABLE dbo.HoaDon ADD TienGiamVoucher DECIMAL(18,2) NOT NULL CONSTRAINT DF_HoaDon_TienGiamVoucher DEFAULT 0;
    PRINT 'Đã bổ sung cột TienGiamVoucher vào dbo.HoaDon.';
END;

-- 4. Bổ sung cột MaVoucher & MaCodeVoucher vào DonHang
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.DonHang') AND name = 'MaVoucher')
BEGIN
    ALTER TABLE dbo.DonHang ADD MaVoucher BIGINT NULL;
    ALTER TABLE dbo.DonHang ADD CONSTRAINT FK_DonHang_Voucher FOREIGN KEY (MaVoucher) REFERENCES dbo.Voucher(MaVoucher);
    PRINT 'Đã bổ sung cột MaVoucher vào dbo.DonHang.';
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.DonHang') AND name = 'MaCodeVoucher')
BEGIN
    ALTER TABLE dbo.DonHang ADD MaCodeVoucher NVARCHAR(50) NULL;
    PRINT 'Đã bổ sung cột MaCodeVoucher vào dbo.DonHang.';
END;

-- 5. Bảng LichSuSuDungVoucher (Theo dõi lượt sử dụng và hỗ trợ hoàn trả khi hủy đơn)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'LichSuSuDungVoucher')
BEGIN
    CREATE TABLE dbo.LichSuSuDungVoucher (
        MaLichSu BIGINT IDENTITY(1,1) NOT NULL,
        MaVoucher BIGINT NOT NULL,
        MaDH BIGINT NOT NULL,
        MaHD BIGINT NULL,
        MaTK BIGINT NULL,
        MaCN BIGINT NOT NULL,
        SoTienGiam DECIMAL(18,2) NOT NULL,
        TrangThai VARCHAR(20) NOT NULL CONSTRAINT DF_LSSDV_TrangThai DEFAULT 'DA_DUNG', -- DA_DUNG, DA_HOAN_TRA
        NgaySuDung DATETIME2 NOT NULL CONSTRAINT DF_LSSDV_NgaySuDung DEFAULT SYSUTCDATETIME(),
        NgayHoanTra DATETIME2 NULL,
        LyDoHoanTra NVARCHAR(500) NULL,
        CONSTRAINT PK_LichSuSuDungVoucher PRIMARY KEY CLUSTERED (MaLichSu ASC),
        CONSTRAINT FK_LSSDV_Voucher FOREIGN KEY (MaVoucher) REFERENCES dbo.Voucher(MaVoucher),
        CONSTRAINT FK_LSSDV_DonHang FOREIGN KEY (MaDH) REFERENCES dbo.DonHang(MaDH),
        CONSTRAINT FK_LSSDV_ChiNhanh FOREIGN KEY (MaCN) REFERENCES dbo.ChiNhanh(MaCN),
        CONSTRAINT FK_LSSDV_TaiKhoan FOREIGN KEY (MaTK) REFERENCES dbo.TaiKhoan(MaTK),
        CONSTRAINT CK_LSSDV_TrangThai CHECK (TrangThai IN ('DA_DUNG', 'DA_HOAN_TRA'))
    );
    CREATE NONCLUSTERED INDEX IX_LSSDV_MaVoucher_MaTK ON dbo.LichSuSuDungVoucher (MaVoucher, MaTK, TrangThai);
    CREATE NONCLUSTERED INDEX IX_LSSDV_MaDH ON dbo.LichSuSuDungVoucher (MaDH);
    PRINT 'Đã tạo bảng dbo.LichSuSuDungVoucher thành công.';
END;

-- 6. Seed dữ liệu mẫu voucher ban đầu
IF NOT EXISTS (SELECT * FROM dbo.Voucher WHERE MaCode = 'CHAOMUNG')
BEGIN
    INSERT INTO dbo.Voucher (MaCode, TenVoucher, MoTa, LoaiGiamGia, GiaTriGiam, GiamToiDa, GiaTriDonToiThieu, TongSoLuot, SoLuotDaDung, SoLuotMoiKhach, ApDungToanChuoi, NgayBatDau, NgayKetThuc, KichHoat)
    VALUES ('CHAOMUNG', N'Chào Mừng Bạn Mới', N'Giảm ngay 20.000đ cho đơn hàng từ 50.000đ', 'FIXED_AMOUNT', 20000, NULL, 50000, 1000, 0, 1, 1, '2024-01-01', '2030-12-31', 1);
    PRINT 'Đã seed voucher CHAOMUNG.';
END;

IF NOT EXISTS (SELECT * FROM dbo.Voucher WHERE MaCode = 'MILKTEA10')
BEGIN
    INSERT INTO dbo.Voucher (MaCode, TenVoucher, MoTa, LoaiGiamGia, GiaTriGiam, GiamToiDa, GiaTriDonToiThieu, TongSoLuot, SoLuotDaDung, SoLuotMoiKhach, ApDungToanChuoi, NgayBatDau, NgayKetThuc, KichHoat)
    VALUES ('MILKTEA10', N'Giảm 10% Toàn Đơn', N'Giảm 10% tối đa 30.000đ cho đơn hàng từ 60.000đ', 'PERCENT', 10, 30000, 60000, 500, 0, 2, 1, '2024-01-01', '2030-12-31', 1);
    PRINT 'Đã seed voucher MILKTEA10.';
END;

IF NOT EXISTS (SELECT * FROM dbo.Voucher WHERE MaCode = 'CN01SPECIAL')
BEGIN
    INSERT INTO dbo.Voucher (MaCode, TenVoucher, MoTa, LoaiGiamGia, GiaTriGiam, GiamToiDa, GiaTriDonToiThieu, TongSoLuot, SoLuotDaDung, SoLuotMoiKhach, ApDungToanChuoi, NgayBatDau, NgayKetThuc, KichHoat)
    VALUES ('CN01SPECIAL', N'Tri Ân Chi Nhánh Võ Văn Ngân', N'Giảm 15.000đ cho đơn từ 45.000đ tại chi nhánh Võ Văn Ngân', 'FIXED_AMOUNT', 15000, NULL, 45000, 200, 0, 1, 0, '2024-01-01', '2030-12-31', 1);

    DECLARE @voucherId BIGINT = (SELECT MaVoucher FROM dbo.Voucher WHERE MaCode = 'CN01SPECIAL');
    DECLARE @branchId BIGINT = (SELECT TOP 1 MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
    IF @voucherId IS NOT NULL AND @branchId IS NOT NULL
    BEGIN
        INSERT INTO dbo.VoucherChiNhanh (MaVoucher, MaCN) VALUES (@voucherId, @branchId);
    END;
    PRINT 'Đã seed voucher CN01SPECIAL và gán chi nhánh CN01.';
END;
