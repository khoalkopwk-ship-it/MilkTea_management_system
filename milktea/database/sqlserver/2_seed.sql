-- ============================================================================
-- MilkTea Management System - Dữ Liệu Khởi Tạo Tối Thiểu (Essential Seed)
-- Bao gồm:
--   1. Cấu hình thông tin quán mặc định (CauHinhChung)
--   2. Đúng 1 tài khoản cho mỗi vai trò (ADMIN, CASHIER, KITCHEN, CUSTOMER)
--      (Mật khẩu mặc định: Password@123)
--   3. Danh sách 12 bàn ăn và mã QR bàn chuẩn (Ban)
-- Toàn bộ dữ liệu menu, nguyên liệu và đơn hàng mẫu đã được chuyển sang
-- tài liệu tham khảo: database/sample_data_reference.md
-- ============================================================================

USE milktea;
GO

SET NOCOUNT ON;
PRINT N'>>> Bắt đầu khởi tạo dữ liệu cốt lõi hệ thống MilkTea...';
GO

-- ============================================================================
-- 1. CẤU HÌNH QUÁN MẶC ĐỊNH (CauHinhChung)
-- ============================================================================
IF NOT EXISTS (SELECT 1 FROM dbo.CauHinhChung WHERE MaCauHinh = 1)
BEGIN
    INSERT INTO dbo.CauHinhChung (
        MaCauHinh,
        TenQuan,
        DiaChi,
        NganHang,
        SoTaiKhoan,
        ChuTK,
        TyLeGiam,
        LoaiGiam,
        GiaTriGiam,
        SuaLuc
    )
    VALUES (
        1,
        N'MilkTea Quán',
        N'Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh',
        N'MBBank',
        '08881706888',
        N'DANG NGUYEN MINH KHOA',
        0.00,
        'PERCENT',
        0.00,
        SYSUTCDATETIME()
    );
    PRINT N'-- [1/3] Đã khởi tạo cấu hình quán mặc định.';
END
ELSE
BEGIN
    PRINT N'-- [1/3] Cấu hình quán đã tồn tại.';
END
GO

-- ============================================================================
-- 2. TÀI KHOẢN MỖI VAI TRÒ (TaiKhoan) - Đúng 1 tài khoản cho mỗi role
-- Mật khẩu mặc định: Password@123
-- Hash: $2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq
-- ============================================================================
DECLARE @pass AS VARCHAR(255) = '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq';

DECLARE @RolesAccounts TABLE (
    Email     VARCHAR(254),
    HoTen     NVARCHAR(120),
    DienThoai VARCHAR(20),
    VaiTro    VARCHAR(16)
);

INSERT INTO @RolesAccounts VALUES
    ('admin@milktea.vn',    N'Quản trị viên',         '0901000001', 'ADMIN'),
    ('cashier@milktea.vn',  N'Thu ngân',              '0901000002', 'CASHIER'),
    ('kitchen@milktea.vn',  N'Bếp trưởng',            '0901000003', 'KITCHEN'),
    ('customer@milktea.vn', N'Khách hàng thân thiết', '0901000004', 'CUSTOMER');

MERGE INTO dbo.TaiKhoan AS target
USING @RolesAccounts AS source ON (target.Email = source.Email)
WHEN MATCHED THEN
    UPDATE SET 
        target.HoTen        = source.HoTen,
        target.DienThoai    = source.DienThoai,
        target.VaiTro       = source.VaiTro,
        target.MatKhauBam   = @pass,
        target.HoatDong     = 1,
        target.EmailXacThuc = 1
WHEN NOT MATCHED THEN
    INSERT (
        Email,
        MatKhauBam,
        HoTen,
        DienThoai,
        VaiTro,
        HoatDong,
        EmailXacThuc,
        TokenVersion,
        TaoLuc
    ) VALUES (
        source.Email,
        @pass,
        source.HoTen,
        source.DienThoai,
        source.VaiTro,
        1,
        1,
        1,
        SYSUTCDATETIME()
    );

PRINT N'-- [2/3] Đã tạo 4 tài khoản chuẩn cho 4 vai trò (Mật khẩu: Password@123).';
GO

-- ============================================================================
-- 3. BÀN ĂN & MÃ QR CHUẨN (Ban) - 12 Bàn
-- Tất cả ở trạng thái TRONG, sẵn sàng phục vụ
-- ============================================================================
DECLARE @TableList TABLE (
    TenBan NVARCHAR(40),
    MaQR   VARCHAR(64)
);

INSERT INTO @TableList VALUES
    (N'Bàn 01', 'QR_TABLE_01_TOKEN_A1B2C3D4'),
    (N'Bàn 02', 'QR_TABLE_02_TOKEN_A2B3C4D5'),
    (N'Bàn 03', 'QR_TABLE_03_TOKEN_A3B4C5D6'),
    (N'Bàn 04', 'QR_TABLE_04_TOKEN_A4B5C6D7'),
    (N'Bàn 05', 'QR_TABLE_05_TOKEN_A5B6C7D8'),
    (N'Bàn 06', 'QR_TABLE_06_TOKEN_A6B7C8D9'),
    (N'Bàn 07', 'QR_TABLE_07_TOKEN_A7B8C9D0'),
    (N'Bàn 08', 'QR_TABLE_08_TOKEN_A8B9C0D1'),
    (N'Bàn 09', 'QR_TABLE_09_TOKEN_A9B0C1D2'),
    (N'Bàn 10', 'QR_TABLE_10_TOKEN_B0C1D2E3'),
    (N'Bàn 11', 'QR_TABLE_11_TOKEN_B1C2D3E4'),
    (N'Bàn 12', 'QR_TABLE_12_TOKEN_B2C3D4E5');

MERGE INTO dbo.Ban AS target
USING @TableList AS source ON (target.TenBan = source.TenBan)
WHEN MATCHED THEN
    UPDATE SET 
        target.MaQR          = source.MaQR,
        target.TrangThai     = 'TRONG',
        target.MaPhienDangMo = NULL,
        target.HoatDong      = 1
WHEN NOT MATCHED THEN
    INSERT (
        TenBan,
        MaQR,
        TrangThai,
        HoatDong,
        Version
    ) VALUES (
        source.TenBan,
        source.MaQR,
        'TRONG',
        1,
        0
    );

PRINT N'-- [3/3] Đã tạo 12 bàn ăn kèm mã QR chuẩn (Trạng thái: TRONG).';
GO

PRINT N'>>> Hoàn tất khởi tạo dữ liệu cốt lõi thành công!';
GO