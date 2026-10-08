-- 1. Tài khoản & Khôi phục mật khẩu
CREATE DATABASE milktea;
GO
USE milktea;
GO 

IF OBJECT_ID('dbo.TokenKhoiPhuc', 'U') IS NOT NULL DROP TABLE dbo.TokenKhoiPhuc;
IF OBJECT_ID('dbo.TaiKhoan', 'U') IS NOT NULL DROP TABLE dbo.TaiKhoan;
CREATE TABLE dbo.TaiKhoan (
    MaTK BIGINT IDENTITY(1, 1) PRIMARY KEY,
    Email VARCHAR(254) NOT NULL,
    MatKhauBam VARCHAR(255) NOT NULL,
    HoTen NVARCHAR(120) NULL,
    DienThoai VARCHAR(20) NULL,
    VaiTro VARCHAR(16) NOT NULL,
    -- ADMIN, CASHIER, KITCHEN, CUSTOMER
    HoatDong BIT NOT NULL CONSTRAINT DF_TaiKhoan_HoatDong DEFAULT 1,
    TokenVersion BIGINT NOT NULL CONSTRAINT DF_TaiKhoan_TokenVersion DEFAULT 1,
    TaoLuc DATETIME2 NOT NULL,
    CONSTRAINT UQ_TaiKhoan_Email UNIQUE (Email)
);
CREATE TABLE dbo.TokenKhoiPhuc (
    MaToken BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaTK BIGINT NOT NULL,
    TokenHash VARCHAR(128) NOT NULL,
    HetHanLuc DATETIME2 NOT NULL,
    DaDungLuc DATETIME2 NULL,
    CONSTRAINT FK_TokenKhoiPhuc_TaiKhoan FOREIGN KEY (MaTK) REFERENCES dbo.TaiKhoan(MaTK),
    CONSTRAINT UQ_TokenKhoiPhuc_Hash UNIQUE (TokenHash)
);
-- 2. Cấu hình chung
IF OBJECT_ID('dbo.CauHinhChung', 'U') IS NOT NULL DROP TABLE dbo.CauHinhChung;
CREATE TABLE dbo.CauHinhChung (
    MaCauHinh INT PRIMARY KEY,
    TenQuan NVARCHAR(120) NOT NULL,
    DiaChi NVARCHAR(255) NOT NULL,
    NganHang NVARCHAR(120) NULL,
    SoTaiKhoan NVARCHAR(120) NULL,
    ChuTK NVARCHAR(120) NULL,
    TyLeGiam DECIMAL(5, 2) NOT NULL CONSTRAINT DF_CauHinhChung_TyLeGiam DEFAULT 0.00,
    NguoiSua BIGINT NULL,
    SuaLuc DATETIME2 NOT NULL,
    CONSTRAINT CK_CauHinhChung_TyLeGiam CHECK (
        TyLeGiam >= 0
        AND TyLeGiam <= 100
    ),
    CONSTRAINT FK_CauHinhChung_NguoiSua FOREIGN KEY (NguoiSua) REFERENCES dbo.TaiKhoan(MaTK)
);
-- 3. Danh mục & Món uống
IF OBJECT_ID('dbo.CongThucMon', 'U') IS NOT NULL DROP TABLE dbo.CongThucMon;
IF OBJECT_ID('dbo.ChiTietGioPhien', 'U') IS NOT NULL DROP TABLE dbo.ChiTietGioPhien;
IF OBJECT_ID('dbo.MonUong', 'U') IS NOT NULL DROP TABLE dbo.MonUong;
IF OBJECT_ID('dbo.DanhMuc', 'U') IS NOT NULL DROP TABLE dbo.DanhMuc;
CREATE TABLE dbo.DanhMuc (
    MaDM BIGINT IDENTITY(1, 1) PRIMARY KEY,
    TenDM NVARCHAR(100) NOT NULL,
    HoatDong BIT NOT NULL CONSTRAINT DF_DanhMuc_HoatDong DEFAULT 1,
    CONSTRAINT UQ_DanhMuc_TenDM UNIQUE (TenDM)
);
CREATE TABLE dbo.MonUong (
    MaMon BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaDM BIGINT NOT NULL,
    TenMon NVARCHAR(120) NOT NULL,
    Size VARCHAR(12) NOT NULL,
    -- S, M, L
    MoTa NVARCHAR(1000) NULL,
    Gia DECIMAL(18, 2) NOT NULL,
    AnhUrl NVARCHAR(1000) NULL,
    AnhPublicId VARCHAR(255) NULL,
    HoatDong BIT NOT NULL CONSTRAINT DF_MonUong_HoatDong DEFAULT 1,
    CONSTRAINT CK_MonUong_Gia CHECK (Gia >= 0),
    CONSTRAINT UQ_MonUong_Ten_Size UNIQUE (TenMon, Size),
    CONSTRAINT FK_MonUong_DanhMuc FOREIGN KEY (MaDM) REFERENCES dbo.DanhMuc(MaDM)
);
-- 4. Bàn & Phiên bàn
IF OBJECT_ID('dbo.TruyCapPhienBan', 'U') IS NOT NULL DROP TABLE dbo.TruyCapPhienBan;
IF OBJECT_ID('dbo.GioPhien', 'U') IS NOT NULL DROP TABLE dbo.GioPhien;
IF OBJECT_ID('dbo.PhienBan', 'U') IS NOT NULL DROP TABLE dbo.PhienBan;
IF OBJECT_ID('dbo.Ban', 'U') IS NOT NULL DROP TABLE dbo.Ban;
CREATE TABLE dbo.Ban (
    MaBan BIGINT IDENTITY(1, 1) PRIMARY KEY,
    TenBan NVARCHAR(40) NOT NULL,
    MaQR VARCHAR(64) NOT NULL,
    TrangThai VARCHAR(16) NOT NULL CONSTRAINT DF_Ban_TrangThai DEFAULT 'TRONG',
    -- TRONG, CO_KHACH
    MaPhienDangMo BIGINT NULL,
    HoatDong BIT NOT NULL CONSTRAINT DF_Ban_HoatDong DEFAULT 1,
    Version BIGINT NOT NULL CONSTRAINT DF_Ban_Version DEFAULT 0,
    CONSTRAINT UQ_Ban_TenBan UNIQUE (TenBan),
    CONSTRAINT UQ_Ban_MaQR UNIQUE (MaQR)
);
CREATE TABLE dbo.PhienBan (
    MaPhien BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaBan BIGINT NOT NULL,
    TrangThai VARCHAR(8) NOT NULL CONSTRAINT DF_PhienBan_TrangThai DEFAULT 'OPEN',
    -- OPEN, CLOSED
    MoLuc DATETIME2 NOT NULL,
    DongLuc DATETIME2 NULL,
    NguoiMo BIGINT NULL,
    NguoiDong BIGINT NULL,
    Version BIGINT NOT NULL CONSTRAINT DF_PhienBan_Version DEFAULT 0,
    CONSTRAINT FK_PhienBan_Ban FOREIGN KEY (MaBan) REFERENCES dbo.Ban(MaBan),
    CONSTRAINT FK_PhienBan_NguoiMo FOREIGN KEY (NguoiMo) REFERENCES dbo.TaiKhoan(MaTK),
    CONSTRAINT FK_PhienBan_NguoiDong FOREIGN KEY (NguoiDong) REFERENCES dbo.TaiKhoan(MaTK)
);
-- 5. Giỏ phiên bàn & Token truy cập phiên
CREATE TABLE dbo.GioPhien (
    MaGio BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaPhien BIGINT NOT NULL,
    Version BIGINT NOT NULL CONSTRAINT DF_GioPhien_Version DEFAULT 0,
    SuaLuc DATETIME2 NOT NULL,
    CONSTRAINT UQ_GioPhien_MaPhien UNIQUE (MaPhien),
    CONSTRAINT FK_GioPhien_PhienBan FOREIGN KEY (MaPhien) REFERENCES dbo.PhienBan(MaPhien)
);
CREATE TABLE dbo.ChiTietGioPhien (
    MaDong BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaGio BIGINT NOT NULL,
    MaMon BIGINT NOT NULL,
    SoLuong INT NOT NULL,
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT CK_ChiTietGioPhien_SoLuong CHECK (SoLuong > 0),
    CONSTRAINT UQ_ChiTietGioPhien_Mon UNIQUE (MaGio, MaMon),
    CONSTRAINT FK_ChiTietGioPhien_Gio FOREIGN KEY (MaGio) REFERENCES dbo.GioPhien(MaGio),
    CONSTRAINT FK_ChiTietGioPhien_Mon FOREIGN KEY (MaMon) REFERENCES dbo.MonUong(MaMon)
);
CREATE TABLE dbo.TruyCapPhienBan (
    MaTruyCap BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaPhien BIGINT NOT NULL,
    TokenHash VARCHAR(128) NOT NULL,
    HetHanLuc DATETIME2 NOT NULL,
    ThuHoiLuc DATETIME2 NULL,
    CONSTRAINT UQ_TruyCapPhienBan_TokenHash UNIQUE (TokenHash),
    CONSTRAINT FK_TruyCapPhienBan_PhienBan FOREIGN KEY (MaPhien) REFERENCES dbo.PhienBan(MaPhien)
);
-- 6. Nguyên liệu & Tồn kho hai tầng
IF OBJECT_ID('dbo.LichSuKho', 'U') IS NOT NULL DROP TABLE dbo.LichSuKho;
IF OBJECT_ID('dbo.ChiTietMeSoChe', 'U') IS NOT NULL DROP TABLE dbo.ChiTietMeSoChe;
IF OBJECT_ID('dbo.MeSoChe', 'U') IS NOT NULL DROP TABLE dbo.MeSoChe;
IF OBJECT_ID('dbo.ChiTietCTSoChe', 'U') IS NOT NULL DROP TABLE dbo.ChiTietCTSoChe;
IF OBJECT_ID('dbo.CongThucSoChe', 'U') IS NOT NULL DROP TABLE dbo.CongThucSoChe;
IF OBJECT_ID('dbo.ChiTietXuat', 'U') IS NOT NULL DROP TABLE dbo.ChiTietXuat;
IF OBJECT_ID('dbo.PhieuXuat', 'U') IS NOT NULL DROP TABLE dbo.PhieuXuat;
IF OBJECT_ID('dbo.ChiTietNhap', 'U') IS NOT NULL DROP TABLE dbo.ChiTietNhap;
IF OBJECT_ID('dbo.PhieuNhap', 'U') IS NOT NULL DROP TABLE dbo.PhieuNhap;
IF OBJECT_ID('dbo.TonNguyenLieu', 'U') IS NOT NULL DROP TABLE dbo.TonNguyenLieu;
IF OBJECT_ID('dbo.NguyenLieu', 'U') IS NOT NULL DROP TABLE dbo.NguyenLieu;
CREATE TABLE dbo.NguyenLieu (
    MaNL BIGINT IDENTITY(1, 1) PRIMARY KEY,
    TenNL NVARCHAR(120) NOT NULL,
    Loai VARCHAR(8) NOT NULL,
    -- THO, SOCHE
    DonVi VARCHAR(16) NOT NULL,
    -- kg, g, ml, ly, cai, suat
    HoatDong BIT NOT NULL CONSTRAINT DF_NguyenLieu_HoatDong DEFAULT 1,
    CONSTRAINT UQ_NguyenLieu_TenNL UNIQUE (TenNL)
);
CREATE TABLE dbo.TonNguyenLieu (
    MaNL BIGINT NOT NULL,
    ViTri VARCHAR(8) NOT NULL,
    -- KHO, BEP
    SoLuong DECIMAL(18, 3) NOT NULL CONSTRAINT DF_TonNguyenLieu_SoLuong DEFAULT 0,
    NguongCanhBao DECIMAL(18, 3) NOT NULL CONSTRAINT DF_TonNguyenLieu_Nguong DEFAULT 0,
    Version BIGINT NOT NULL CONSTRAINT DF_TonNguyenLieu_Version DEFAULT 0,
    CONSTRAINT PK_TonNguyenLieu PRIMARY KEY (MaNL, ViTri),
    CONSTRAINT CK_TonNguyenLieu_SoLuong CHECK (SoLuong >= 0),
    CONSTRAINT CK_TonNguyenLieu_Nguong CHECK (NguongCanhBao >= 0),
    CONSTRAINT FK_TonNguyenLieu_NguyenLieu FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu(MaNL)
);
CREATE TABLE dbo.CongThucMon (
    MaMon BIGINT NOT NULL,
    MaNL BIGINT NOT NULL,
    DinhLuong DECIMAL(18, 3) NOT NULL,
    CONSTRAINT PK_CongThucMon PRIMARY KEY (MaMon, MaNL),
    CONSTRAINT CK_CongThucMon_DinhLuong CHECK (DinhLuong > 0),
    CONSTRAINT FK_CongThucMon_Mon FOREIGN KEY (MaMon) REFERENCES dbo.MonUong(MaMon),
    CONSTRAINT FK_CongThucMon_NL FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu(MaNL)
);
CREATE TABLE dbo.CongThucSoChe (
    MaCTSC BIGINT IDENTITY(1, 1) PRIMARY KEY,
    TenCT NVARCHAR(120) NOT NULL,
    MaNLDauRa BIGINT NOT NULL,
    LuongDauRaChuan DECIMAL(18, 3) NOT NULL,
    HoatDong BIT NOT NULL CONSTRAINT DF_CongThucSoChe_HoatDong DEFAULT 1,
    CONSTRAINT CK_CongThucSoChe_LuongDauRa CHECK (LuongDauRaChuan > 0),
    CONSTRAINT FK_CongThucSoChe_DauRa FOREIGN KEY (MaNLDauRa) REFERENCES dbo.NguyenLieu(MaNL)
);
CREATE TABLE dbo.ChiTietCTSoChe (
    MaCTSC BIGINT NOT NULL,
    MaNL BIGINT NOT NULL,
    LuongDauVaoChuan DECIMAL(18, 3) NOT NULL,
    CONSTRAINT PK_ChiTietCTSoChe PRIMARY KEY (MaCTSC, MaNL),
    CONSTRAINT CK_ChiTietCTSoChe_LuongDauVao CHECK (LuongDauVaoChuan > 0),
    CONSTRAINT FK_ChiTietCTSoChe_CT FOREIGN KEY (MaCTSC) REFERENCES dbo.CongThucSoChe(MaCTSC),
    CONSTRAINT FK_ChiTietCTSoChe_NL FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu(MaNL)
);
-- 7. Phiếu nhập & Phiếu xuất & Mẻ sơ chế
CREATE TABLE dbo.PhieuNhap (
    MaPhieu BIGINT IDENTITY(1, 1) PRIMARY KEY,
    NguoiLap BIGINT NOT NULL,
    LyDo NVARCHAR(500) NULL,
    NguonNhap NVARCHAR(500) NULL,
    TrangThai VARCHAR(12) NOT NULL CONSTRAINT DF_PhieuNhap_TrangThai DEFAULT 'NHAP',
    -- NHAP, DA_GHI_SO
    LapLuc DATETIME2 NOT NULL,
    GhiSoLuc DATETIME2 NULL,
    MaYeuCau VARCHAR(64) NULL,
    CONSTRAINT UQ_PhieuNhap_MaYeuCau UNIQUE (MaYeuCau),
    CONSTRAINT FK_PhieuNhap_NguoiLap FOREIGN KEY (NguoiLap) REFERENCES dbo.TaiKhoan(MaTK)
);
CREATE TABLE dbo.ChiTietNhap (
    MaPhieu BIGINT NOT NULL,
    MaNL BIGINT NOT NULL,
    SoLuong DECIMAL(18, 3) NOT NULL,
    DonGiaNhap DECIMAL(18, 2) NOT NULL,
    CONSTRAINT PK_ChiTietNhap PRIMARY KEY (MaPhieu, MaNL),
    CONSTRAINT CK_ChiTietNhap_SoLuong CHECK (SoLuong > 0),
    CONSTRAINT CK_ChiTietNhap_DonGia CHECK (DonGiaNhap >= 0),
    CONSTRAINT FK_ChiTietNhap_Phieu FOREIGN KEY (MaPhieu) REFERENCES dbo.PhieuNhap(MaPhieu),
    CONSTRAINT FK_ChiTietNhap_NL FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu(MaNL)
);
CREATE TABLE dbo.PhieuXuat (
    MaPhieu BIGINT IDENTITY(1, 1) PRIMARY KEY,
    NguoiLap BIGINT NOT NULL,
    LyDo NVARCHAR(500) NOT NULL,
    TrangThai VARCHAR(12) NOT NULL CONSTRAINT DF_PhieuXuat_TrangThai DEFAULT 'NHAP',
    -- NHAP, DA_GHI_SO
    LapLuc DATETIME2 NOT NULL,
    GhiSoLuc DATETIME2 NULL,
    MaYeuCau VARCHAR(64) NULL,
    CONSTRAINT UQ_PhieuXuat_MaYeuCau UNIQUE (MaYeuCau),
    CONSTRAINT FK_PhieuXuat_NguoiLap FOREIGN KEY (NguoiLap) REFERENCES dbo.TaiKhoan(MaTK)
);
CREATE TABLE dbo.ChiTietXuat (
    MaPhieu BIGINT NOT NULL,
    MaNL BIGINT NOT NULL,
    SoLuong DECIMAL(18, 3) NOT NULL,
    CONSTRAINT PK_ChiTietXuat PRIMARY KEY (MaPhieu, MaNL),
    CONSTRAINT CK_ChiTietXuat_SoLuong CHECK (SoLuong > 0),
    CONSTRAINT FK_ChiTietXuat_Phieu FOREIGN KEY (MaPhieu) REFERENCES dbo.PhieuXuat(MaPhieu),
    CONSTRAINT FK_ChiTietXuat_NL FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu(MaNL)
);
CREATE TABLE dbo.MeSoChe (
    MaMe BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaCTSC BIGINT NOT NULL,
    MaNLDauRa BIGINT NOT NULL,
    LuongDuKien DECIMAL(18, 3) NOT NULL,
    LuongThucThu DECIMAL(18, 3) NOT NULL CONSTRAINT DF_MeSoChe_ThucThu DEFAULT 0,
    TrangThai VARCHAR(12) NOT NULL CONSTRAINT DF_MeSoChe_TrangThai DEFAULT 'NHAP',
    -- NHAP, DA_GHI_SO
    LyDoChenhLech NVARCHAR(500) NULL,
    NguoiLap BIGINT NOT NULL,
    GhiSoLuc DATETIME2 NULL,
    MaYeuCau VARCHAR(64) NULL,
    CONSTRAINT UQ_MeSoChe_MaYeuCau UNIQUE (MaYeuCau),
    CONSTRAINT CK_MeSoChe_LuongDuKien CHECK (LuongDuKien >= 0),
    CONSTRAINT CK_MeSoChe_LuongThucThu CHECK (LuongThucThu >= 0),
    CONSTRAINT FK_MeSoChe_CT FOREIGN KEY (MaCTSC) REFERENCES dbo.CongThucSoChe(MaCTSC),
    CONSTRAINT FK_MeSoChe_DauRa FOREIGN KEY (MaNLDauRa) REFERENCES dbo.NguyenLieu(MaNL),
    CONSTRAINT FK_MeSoChe_NguoiLap FOREIGN KEY (NguoiLap) REFERENCES dbo.TaiKhoan(MaTK)
);
CREATE TABLE dbo.ChiTietMeSoChe (
    MaMe BIGINT NOT NULL,
    MaNL BIGINT NOT NULL,
    LuongDinhMuc DECIMAL(18, 3) NOT NULL,
    LuongThucDung DECIMAL(18, 3) NOT NULL,
    CONSTRAINT PK_ChiTietMeSoChe PRIMARY KEY (MaMe, MaNL),
    CONSTRAINT CK_ChiTietMeSoChe_DinhMuc CHECK (LuongDinhMuc > 0),
    CONSTRAINT CK_ChiTietMeSoChe_ThucDung CHECK (LuongThucDung > 0),
    CONSTRAINT FK_ChiTietMeSoChe_Me FOREIGN KEY (MaMe) REFERENCES dbo.MeSoChe(MaMe),
    CONSTRAINT FK_ChiTietMeSoChe_NL FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu(MaNL)
);
CREATE TABLE dbo.LichSuKho (
    MaBienDong BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaNL BIGINT NOT NULL,
    ViTri VARCHAR(8) NOT NULL,
    -- KHO, BEP
    Loai VARCHAR(16) NOT NULL,
    -- NHAP, XUAT_DI, NHAN_BEP, SOCHE_VAO, SOCHE_RA, TIEU_HAO, HAO_HUT
    LuongBienDong DECIMAL(18, 3) NOT NULL,
    TonTruoc DECIMAL(18, 3) NOT NULL,
    TonSau DECIMAL(18, 3) NOT NULL,
    NguonLoai VARCHAR(16) NOT NULL,
    -- ORDER, IMPORT, ISSUE, BATCH, WASTE, REMAKE
    NguonMa BIGINT NOT NULL,
    NguonDong BIGINT NULL,
    NguoiLap BIGINT NOT NULL,
    TaoLuc DATETIME2 NOT NULL,
    LyDo NVARCHAR(500) NULL,
    CONSTRAINT CK_LichSuKho_TonTruoc CHECK (TonTruoc >= 0),
    CONSTRAINT CK_LichSuKho_TonSau CHECK (TonSau >= 0),
    CONSTRAINT FK_LichSuKho_NL FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu(MaNL),
    CONSTRAINT FK_LichSuKho_NguoiLap FOREIGN KEY (NguoiLap) REFERENCES dbo.TaiKhoan(MaTK)
);
-- 8. Đơn hàng & Chi tiết & Hóa đơn & Snapshot định mức
IF OBJECT_ID('dbo.ThongBaoThanhToan', 'U') IS NOT NULL DROP TABLE dbo.ThongBaoThanhToan;
IF OBJECT_ID('dbo.YeuCauHoan', 'U') IS NOT NULL DROP TABLE dbo.YeuCauHoan;
IF OBJECT_ID('dbo.ThanhToan', 'U') IS NOT NULL DROP TABLE dbo.ThanhToan;
IF OBJECT_ID('dbo.HoaDon', 'U') IS NOT NULL DROP TABLE dbo.HoaDon;
IF OBJECT_ID('dbo.YeuCauHuy', 'U') IS NOT NULL DROP TABLE dbo.YeuCauHuy;
IF OBJECT_ID('dbo.DinhMucDonHang', 'U') IS NOT NULL DROP TABLE dbo.DinhMucDonHang;
IF OBJECT_ID('dbo.ChiTietDonHang', 'U') IS NOT NULL DROP TABLE dbo.ChiTietDonHang;
IF OBJECT_ID('dbo.TruyCapDonQuay', 'U') IS NOT NULL DROP TABLE dbo.TruyCapDonQuay;
IF OBJECT_ID('dbo.DonHang', 'U') IS NOT NULL DROP TABLE dbo.DonHang;
CREATE TABLE dbo.DonHang (
    MaDH BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaPhien BIGINT NULL,
    MaTK BIGINT NULL,
    Nguon VARCHAR(10) NOT NULL,
    -- TABLE, COUNTER
    TrangThai VARCHAR(24) NOT NULL,
    -- CHO_XAC_NHAN, CHO_CHE_BIEN, DANG_CHE_BIEN, HOAN_THANH, DA_HUY
    ThayTheMaDH BIGINT NULL,
    MaYeuCau VARCHAR(64) NULL,
    GhiChu NVARCHAR(500) NULL,
    TaoLuc DATETIME2 NOT NULL,
    BatDauLuc DATETIME2 NULL,
    HoanThanhLuc DATETIME2 NULL,
    Version BIGINT NOT NULL CONSTRAINT DF_DonHang_Version DEFAULT 0,
    CONSTRAINT FK_DonHang_Phien FOREIGN KEY (MaPhien) REFERENCES dbo.PhienBan(MaPhien),
    CONSTRAINT FK_DonHang_TaiKhoan FOREIGN KEY (MaTK) REFERENCES dbo.TaiKhoan(MaTK),
    CONSTRAINT FK_DonHang_ThayThe FOREIGN KEY (ThayTheMaDH) REFERENCES dbo.DonHang(MaDH)
);
CREATE TABLE dbo.ChiTietDonHang (
    MaChiTiet BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaDH BIGINT NOT NULL,
    MaMon BIGINT NOT NULL,
    TenMonChot NVARCHAR(120) NOT NULL,
    SizeChot VARCHAR(12) NOT NULL,
    SoLuong INT NOT NULL,
    DonGiaChot DECIMAL(18, 2) NOT NULL,
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT CK_ChiTietDonHang_SoLuong CHECK (SoLuong > 0),
    CONSTRAINT CK_ChiTietDonHang_DonGia CHECK (DonGiaChot >= 0),
    CONSTRAINT FK_ChiTietDonHang_DonHang FOREIGN KEY (MaDH) REFERENCES dbo.DonHang(MaDH),
    CONSTRAINT FK_ChiTietDonHang_Mon FOREIGN KEY (MaMon) REFERENCES dbo.MonUong(MaMon)
);
CREATE TABLE dbo.DinhMucDonHang (
    MaDH BIGINT NOT NULL,
    MaNL BIGINT NOT NULL,
    TongLuong DECIMAL(18, 3) NOT NULL,
    LoaiChot VARCHAR(8) NOT NULL,
    -- THO, SOCHE
    DonViChot VARCHAR(16) NOT NULL,
    CONSTRAINT PK_DinhMucDonHang PRIMARY KEY (MaDH, MaNL),
    CONSTRAINT CK_DinhMucDonHang_TongLuong CHECK (TongLuong > 0),
    CONSTRAINT FK_DinhMucDonHang_DonHang FOREIGN KEY (MaDH) REFERENCES dbo.DonHang(MaDH),
    CONSTRAINT FK_DinhMucDonHang_NL FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu(MaNL)
);
CREATE TABLE dbo.HoaDon (
    MaHD BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaDH BIGINT NOT NULL,
    TongTien DECIMAL(18, 2) NOT NULL,
    TienGiam DECIMAL(18, 2) NOT NULL CONSTRAINT DF_HoaDon_TienGiam DEFAULT 0,
    PhaiTra DECIMAL(18, 2) NOT NULL,
    TyLeGiamChot DECIMAL(5, 2) NOT NULL CONSTRAINT DF_HoaDon_TyLe DEFAULT 0,
    TrangThai VARCHAR(12) NOT NULL CONSTRAINT DF_HoaDon_TrangThai DEFAULT 'HIEU_LUC',
    -- HIEU_LUC, DA_HUY
    LapLuc DATETIME2 NOT NULL,
    HuyLuc DATETIME2 NULL,
    CONSTRAINT UQ_HoaDon_MaDH UNIQUE (MaDH),
    CONSTRAINT CK_HoaDon_TongTien CHECK (TongTien >= 0),
    CONSTRAINT CK_HoaDon_TienGiam CHECK (TienGiam >= 0),
    CONSTRAINT CK_HoaDon_PhaiTra CHECK (PhaiTra >= 0),
    CONSTRAINT FK_HoaDon_DonHang FOREIGN KEY (MaDH) REFERENCES dbo.DonHang(MaDH)
);
CREATE TABLE dbo.ThanhToan (
    MaTT BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaHD BIGINT NOT NULL,
    PhuongThuc VARCHAR(16) NOT NULL,
    -- CASH, BANK_TRANSFER
    SoTien DECIMAL(18, 2) NOT NULL,
    ThamChieu NVARCHAR(120) NULL,
    NguoiThu BIGINT NOT NULL,
    ThuLuc DATETIME2 NOT NULL,
    MaYeuCau VARCHAR(64) NULL,
    CONSTRAINT UQ_ThanhToan_MaHD UNIQUE (MaHD),
    CONSTRAINT CK_ThanhToan_SoTien CHECK (SoTien >= 0),
    CONSTRAINT FK_ThanhToan_HoaDon FOREIGN KEY (MaHD) REFERENCES dbo.HoaDon(MaHD),
    CONSTRAINT FK_ThanhToan_NguoiThu FOREIGN KEY (NguoiThu) REFERENCES dbo.TaiKhoan(MaTK)
);
CREATE TABLE dbo.ThongBaoThanhToan (
    MaTB BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaHD BIGINT NOT NULL,
    PhuongThuc VARCHAR(16) NOT NULL,
    ThamChieu NVARCHAR(120) NULL,
    TaoLuc DATETIME2 NOT NULL,
    MaYeuCau VARCHAR(64) NULL,
    CONSTRAINT FK_ThongBaoThanhToan_HoaDon FOREIGN KEY (MaHD) REFERENCES dbo.HoaDon(MaHD)
);
CREATE TABLE dbo.YeuCauHuy (
    MaHuy BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaDH BIGINT NOT NULL,
    LyDo NVARCHAR(500) NOT NULL,
    LyDoXuLy NVARCHAR(500) NULL,
    TrangThai VARCHAR(16) NOT NULL CONSTRAINT DF_YeuCauHuy_TrangThai DEFAULT 'CHO',
    -- CHO, CHAP_THUAN, TU_CHOI
    TaoLuc DATETIME2 NOT NULL,
    XuLyLuc DATETIME2 NULL,
    NguoiXuLy BIGINT NULL,
    CONSTRAINT FK_YeuCauHuy_DonHang FOREIGN KEY (MaDH) REFERENCES dbo.DonHang(MaDH),
    CONSTRAINT FK_YeuCauHuy_NguoiXuLy FOREIGN KEY (NguoiXuLy) REFERENCES dbo.TaiKhoan(MaTK)
);
CREATE TABLE dbo.YeuCauHoan (
    MaHoan BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaDH BIGINT NOT NULL,
    MaTT BIGINT NOT NULL,
    SoTien DECIMAL(18, 2) NOT NULL,
    LyDo NVARCHAR(500) NOT NULL,
    LyDoDuyet NVARCHAR(500) NULL,
    TrangThai VARCHAR(16) NOT NULL CONSTRAINT DF_YeuCauHoan_TrangThai DEFAULT 'CHO_DUYET',
    -- CHO_DUYET, DA_DUYET, TU_CHOI, DA_HOAN
    NguoiDuyet BIGINT NULL,
    NguoiHoan BIGINT NULL,
    DuyetLuc DATETIME2 NULL,
    HoanLuc DATETIME2 NULL,
    ThamChieuHoan NVARCHAR(120) NULL,
    MaYeuCau VARCHAR(64) NOT NULL,
    CONSTRAINT UQ_YeuCauHoan_MaDH UNIQUE (MaDH),
    CONSTRAINT UQ_YeuCauHoan_MaTT UNIQUE (MaTT),
    CONSTRAINT UQ_YeuCauHoan_MaYeuCau UNIQUE (MaYeuCau),
    CONSTRAINT CK_YeuCauHoan_SoTien CHECK (SoTien >= 0),
    CONSTRAINT FK_YeuCauHoan_DonHang FOREIGN KEY (MaDH) REFERENCES dbo.DonHang(MaDH),
    CONSTRAINT FK_YeuCauHoan_ThanhToan FOREIGN KEY (MaTT) REFERENCES dbo.ThanhToan(MaTT),
    CONSTRAINT FK_YeuCauHoan_NguoiDuyet FOREIGN KEY (NguoiDuyet) REFERENCES dbo.TaiKhoan(MaTK),
    CONSTRAINT FK_YeuCauHoan_NguoiHoan FOREIGN KEY (NguoiHoan) REFERENCES dbo.TaiKhoan(MaTK)
);
CREATE TABLE dbo.TruyCapDonQuay (
    MaTruyCap BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaDH BIGINT NOT NULL,
    TokenHash VARCHAR(128) NOT NULL,
    HetHanLuc DATETIME2 NOT NULL,
    ThuHoiLuc DATETIME2 NULL,
    CONSTRAINT UQ_TruyCapDonQuay_TokenHash UNIQUE (TokenHash),
    CONSTRAINT FK_TruyCapDonQuay_DonHang FOREIGN KEY (MaDH) REFERENCES dbo.DonHang(MaDH)
);
-- 9. Nhật ký nghiệp vụ & Chống lặp
IF OBJECT_ID('dbo.NhatKyNghiepVu', 'U') IS NOT NULL DROP TABLE dbo.NhatKyNghiepVu;
IF OBJECT_ID('dbo.BanGhiChongLap', 'U') IS NOT NULL DROP TABLE dbo.BanGhiChongLap;
CREATE TABLE dbo.NhatKyNghiepVu (
    MaNK BIGINT IDENTITY(1, 1) PRIMARY KEY,
    MaNKCha BIGINT NULL,
    MaPhien BIGINT NULL,
    MaDH BIGINT NULL,
    MaTK BIGINT NULL,
    HanhDong VARCHAR(40) NOT NULL,
    Truoc NVARCHAR(500) NULL,
    Sau NVARCHAR(500) NULL,
    LyDo NVARCHAR(1000) NULL,
    DuLieu NVARCHAR(MAX) NULL,
    TaoLuc DATETIME2 NOT NULL,
    CONSTRAINT FK_NhatKyNghiepVu_Cha FOREIGN KEY (MaNKCha) REFERENCES dbo.NhatKyNghiepVu(MaNK)
);
CREATE TABLE dbo.BanGhiChongLap (
    MaBanGhi BIGINT IDENTITY(1, 1) PRIMARY KEY,
    PhamVi VARCHAR(128) NOT NULL,
    KhoaYeuCau VARCHAR(64) NOT NULL,
    DauVanPayload VARCHAR(128) NULL,
    LoaiTaiNguyen VARCHAR(40) NULL,
    MaTaiNguyen BIGINT NULL,
    TaoLuc DATETIME2 NOT NULL,
    HetHanLuc DATETIME2 NOT NULL,
    CONSTRAINT UQ_BanGhiChongLap UNIQUE (PhamVi, KhoaYeuCau)
);
-- 10. Indexes tối ưu truy vấn theo database_rule.md
CREATE INDEX IX_DonHang_Phien_TaoLuc ON dbo.DonHang(MaPhien, TaoLuc);
CREATE INDEX IX_DonHang_TK_TaoLuc ON dbo.DonHang(MaTK, TaoLuc);
CREATE INDEX IX_DonHang_TrangThai_TaoLuc ON dbo.DonHang(TrangThai, TaoLuc);
CREATE INDEX IX_ThanhToan_ThuLuc ON dbo.ThanhToan(ThuLuc);
CREATE INDEX IX_YeuCauHoan_TrangThai_HoanLuc ON dbo.YeuCauHoan(TrangThai, HoanLuc);
CREATE INDEX IX_LichSuKho_NL_ViTri_TaoLuc ON dbo.LichSuKho(MaNL, ViTri, TaoLuc);
CREATE INDEX IX_PhienBan_Ban_MoLuc ON dbo.PhienBan(MaBan, MoLuc);
CREATE UNIQUE INDEX UX_ThanhToan_MaYeuCau ON dbo.ThanhToan(MaYeuCau) WHERE MaYeuCau IS NOT NULL;
GO