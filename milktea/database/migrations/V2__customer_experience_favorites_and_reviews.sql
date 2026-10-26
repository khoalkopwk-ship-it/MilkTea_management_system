-- =========================================================================================
-- V2__customer_experience_favorites_and_reviews.sql
-- Giai đoạn 4: Trải nghiệm khách hàng (Wishlist & Đánh giá bình luận có kiểm duyệt)
-- Tuân thủ Single Source of Truth: docs/BUSINESS_RULES.md (Mục 5 & Mục 7)
-- =========================================================================================

-- 1. Bảng SanPhamYeuThich (CUS-05 Wishlist)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'SanPhamYeuThich')
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
    PRINT 'Đã tạo bảng dbo.SanPhamYeuThich thành công.';
END;

-- 2. Bảng DanhGia (CUS-06 Đánh giá & Bình luận)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DanhGia')
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
    PRINT 'Đã tạo bảng dbo.DanhGia thành công.';
END;

-- 3. Bảng DanhGiaMedia (CUS-06 Media đính kèm)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DanhGiaMedia')
BEGIN
    CREATE TABLE dbo.DanhGiaMedia (
        MaMedia BIGINT IDENTITY(1,1) NOT NULL,
        MaDanhGia BIGINT NOT NULL,
        Url NVARCHAR(1000) NOT NULL,
        LoaiMedia VARCHAR(16) NOT NULL, -- IMAGE, VIDEO
        TaoLuc DATETIME2 NOT NULL CONSTRAINT DF_DanhGiaMedia_TaoLuc DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_DanhGiaMedia PRIMARY KEY CLUSTERED (MaMedia ASC),
        CONSTRAINT FK_DanhGiaMedia_DanhGia FOREIGN KEY (MaDanhGia) REFERENCES dbo.DanhGia(MaDanhGia) ON DELETE CASCADE
    );
    CREATE NONCLUSTERED INDEX IX_DanhGiaMedia_MaDanhGia ON dbo.DanhGiaMedia (MaDanhGia);
    PRINT 'Đã tạo bảng dbo.DanhGiaMedia thành công.';
END;
