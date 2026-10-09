-- 1. Tài khoản mẫu
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TaiKhoan
    WHERE Email = 'admin@milktea.vn'
) BEGIN
INSERT INTO dbo.TaiKhoan (
        Email,
        MatKhauBam,
        HoTen,
        DienThoai,
        VaiTro,
        HoatDong,
        TokenVersion,
        TaoLuc
    )
VALUES (
        'admin@milktea.vn',
        '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq',
        N'Quản Trị Viên',
        '0901000001',
        'ADMIN',
        1,
        1,
        SYSUTCDATETIME()
    );
END IF NOT EXISTS (
    SELECT 1
    FROM dbo.TaiKhoan
    WHERE Email = 'cashier@milktea.vn'
) BEGIN
INSERT INTO dbo.TaiKhoan (
        Email,
        MatKhauBam,
        HoTen,
        DienThoai,
        VaiTro,
        HoatDong,
        TokenVersion,
        TaoLuc
    )
VALUES (
        'cashier@milktea.vn',
        '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq',
        N'Thu Ngân Quỳnh',
        '0901000002',
        'CASHIER',
        1,
        1,
        SYSUTCDATETIME()
    );
END IF NOT EXISTS (
    SELECT 1
    FROM dbo.TaiKhoan
    WHERE Email = 'kitchen@milktea.vn'
) BEGIN
INSERT INTO dbo.TaiKhoan (
        Email,
        MatKhauBam,
        HoTen,
        DienThoai,
        VaiTro,
        HoatDong,
        TokenVersion,
        TaoLuc
    )
VALUES (
        'kitchen@milktea.vn',
        '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq',
        N'Bếp Trưởng Lân',
        '0901000003',
        'KITCHEN',
        1,
        1,
        SYSUTCDATETIME()
    );
END IF NOT EXISTS (
    SELECT 1
    FROM dbo.TaiKhoan
    WHERE Email = 'customer@milktea.vn'
) BEGIN
INSERT INTO dbo.TaiKhoan (
        Email,
        MatKhauBam,
        HoTen,
        DienThoai,
        VaiTro,
        HoatDong,
        TokenVersion,
        TaoLuc
    )
VALUES (
        'customer@milktea.vn',
        '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq',
        N'Khách Hàng Mai',
        '0901000004',
        'CUSTOMER',
        1,
        1,
        SYSUTCDATETIME()
    );
END -- 2. Cấu hình quán duy nhất
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CauHinhChung
    WHERE MaCauHinh = 1
) BEGIN
INSERT INTO dbo.CauHinhChung (
        MaCauHinh,
        TenQuan,
        DiaChi,
        NganHang,
        SoTaiKhoan,
        ChuTK,
        TyLeGiam,
        NguoiSua,
        SuaLuc
    )
VALUES (
        1,
        N'MilkTea Quán 01',
        N'Số 1 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức',
        N'Vietcombank',
        '999988887777',
        N'QUAN TRA SUA MILKTEA',
        10.00,
        1,
        SYSUTCDATETIME()
    );
END -- 3. Bàn mẫu & Mã QR
DECLARE @tables TABLE (TenBan NVARCHAR(40), MaQR VARCHAR(64));
INSERT INTO @tables
VALUES (N'Bàn 01', 'TABLE_QR_BAN01_SECURE_TOKEN_XYZ1'),
    (N'Bàn 02', 'TABLE_QR_BAN02_SECURE_TOKEN_XYZ2'),
    (N'Bàn 03', 'TABLE_QR_BAN03_SECURE_TOKEN_XYZ3'),
    (N'Bàn 04', 'TABLE_QR_BAN04_SECURE_TOKEN_XYZ4'),
    (N'Bàn 05', 'TABLE_QR_BAN05_SECURE_TOKEN_XYZ5');
INSERT INTO dbo.Ban (TenBan, MaQR, TrangThai, HoatDong, Version)
SELECT t.TenBan,
    t.MaQR,
    'TRONG',
    1,
    0
FROM @tables t
WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.Ban b
        WHERE b.TenBan = t.TenBan
    );
-- 4. Danh mục
IF NOT EXISTS (
    SELECT 1
    FROM dbo.DanhMuc
    WHERE TenDM = N'Trà Sữa Truyền Thống'
)
INSERT INTO dbo.DanhMuc (TenDM, HoatDong)
VALUES (N'Trà Sữa Truyền Thống', 1);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.DanhMuc
    WHERE TenDM = N'Trà Trái Cây'
)
INSERT INTO dbo.DanhMuc (TenDM, HoatDong)
VALUES (N'Trà Trái Cây', 1);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.DanhMuc
    WHERE TenDM = N'Topping'
)
INSERT INTO dbo.DanhMuc (TenDM, HoatDong)
VALUES (N'Topping', 1);
-- 5. Món uống (Mỗi biến thể size là 1 record)
DECLARE @dmTraSua BIGINT = (
        SELECT MaDM
        FROM dbo.DanhMuc
        WHERE TenDM = N'Trà Sữa Truyền Thống'
    );
DECLARE @dmTraTraiCay BIGINT = (
        SELECT MaDM
        FROM dbo.DanhMuc
        WHERE TenDM = N'Trà Trái Cây'
    );
DECLARE @dmTopping BIGINT = (
        SELECT MaDM
        FROM dbo.DanhMuc
        WHERE TenDM = N'Topping'
    );
IF NOT EXISTS (
    SELECT 1
    FROM dbo.MonUong
    WHERE TenMon = N'Trà Sữa Truyền Thống'
        AND Size = 'M'
)
INSERT INTO dbo.MonUong (MaDM, TenMon, Size, MoTa, Gia, HoatDong)
VALUES (
        @dmTraSua,
        N'Trà Sữa Truyền Thống',
        'M',
        N'Trà đen đậm vị kết hợp sữa tươi thơm béo',
        25000,
        1
    );
IF NOT EXISTS (
    SELECT 1
    FROM dbo.MonUong
    WHERE TenMon = N'Trà Sữa Truyền Thống'
        AND Size = 'L'
)
INSERT INTO dbo.MonUong (MaDM, TenMon, Size, MoTa, Gia, HoatDong)
VALUES (
        @dmTraSua,
        N'Trà Sữa Truyền Thống',
        'L',
        N'Trà đen đậm vị kết hợp sữa tươi thơm béo',
        30000,
        1
    );
IF NOT EXISTS (
    SELECT 1
    FROM dbo.MonUong
    WHERE TenMon = N'Trà Đào Cam Sả'
        AND Size = 'M'
)
INSERT INTO dbo.MonUong (MaDM, TenMon, Size, MoTa, Gia, HoatDong)
VALUES (
        @dmTraTraiCay,
        N'Trà Đào Cam Sả',
        'M',
        N'Trà hoa quả giải nhiệt sảng khoái',
        28000,
        1
    );
IF NOT EXISTS (
    SELECT 1
    FROM dbo.MonUong
    WHERE TenMon = N'Trà Đào Cam Sả'
        AND Size = 'L'
)
INSERT INTO dbo.MonUong (MaDM, TenMon, Size, MoTa, Gia, HoatDong)
VALUES (
        @dmTraTraiCay,
        N'Trà Đào Cam Sả',
        'L',
        N'Trà hoa quả giải nhiệt sảng khoái',
        33000,
        1
    );
IF NOT EXISTS (
    SELECT 1
    FROM dbo.MonUong
    WHERE TenMon = N'Trân Châu Đen'
        AND Size = 'M'
)
INSERT INTO dbo.MonUong (MaDM, TenMon, Size, MoTa, Gia, HoatDong)
VALUES (
        @dmTopping,
        N'Trân Châu Đen',
        'M',
        N'Trân châu dẻo dai nấu mới mỗi ngày',
        5000,
        1
    );
-- 6. Nguyên liệu (THO và SOCHE)
IF NOT EXISTS (
    SELECT 1
    FROM dbo.NguyenLieu
    WHERE TenNL = N'Trà Đen'
)
INSERT INTO dbo.NguyenLieu (TenNL, Loai, DonVi, HoatDong)
VALUES (N'Trà Đen', 'THO', 'kg', 1);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.NguyenLieu
    WHERE TenNL = N'Sữa Đặc'
)
INSERT INTO dbo.NguyenLieu (TenNL, Loai, DonVi, HoatDong)
VALUES (N'Sữa Đặc', 'THO', 'kg', 1);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.NguyenLieu
    WHERE TenNL = N'Đường Cát'
)
INSERT INTO dbo.NguyenLieu (TenNL, Loai, DonVi, HoatDong)
VALUES (N'Đường Cát', 'THO', 'kg', 1);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.NguyenLieu
    WHERE TenNL = N'Ly Nhựa'
)
INSERT INTO dbo.NguyenLieu (TenNL, Loai, DonVi, HoatDong)
VALUES (N'Ly Nhựa', 'THO', 'cai', 1);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.NguyenLieu
    WHERE TenNL = N'Trân Châu Thô'
)
INSERT INTO dbo.NguyenLieu (TenNL, Loai, DonVi, HoatDong)
VALUES (N'Trân Châu Thô', 'THO', 'kg', 1);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.NguyenLieu
    WHERE TenNL = N'Trân Châu Nấu Chín'
)
INSERT INTO dbo.NguyenLieu (TenNL, Loai, DonVi, HoatDong)
VALUES (N'Trân Châu Nấu Chín', 'SOCHE', 'suat', 1);
-- 7. Tồn kho mẫu (KHO & BEP)
DECLARE @nlTraDen BIGINT = (
        SELECT MaNL
        FROM dbo.NguyenLieu
        WHERE TenNL = N'Trà Đen'
    );
DECLARE @nlSuaDac BIGINT = (
        SELECT MaNL
        FROM dbo.NguyenLieu
        WHERE TenNL = N'Sữa Đặc'
    );
DECLARE @nlDuongCat BIGINT = (
        SELECT MaNL
        FROM dbo.NguyenLieu
        WHERE TenNL = N'Đường Cát'
    );
DECLARE @nlLyNhua BIGINT = (
        SELECT MaNL
        FROM dbo.NguyenLieu
        WHERE TenNL = N'Ly Nhựa'
    );
DECLARE @nlTCSong BIGINT = (
        SELECT MaNL
        FROM dbo.NguyenLieu
        WHERE TenNL = N'Trân Châu Thô'
    );
DECLARE @nlTCChin BIGINT = (
        SELECT MaNL
        FROM dbo.NguyenLieu
        WHERE TenNL = N'Trân Châu Nấu Chín'
    );
-- KHO
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlTraDen
        AND ViTri = 'KHO'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlTraDen, 'KHO', 50.000, 5.000);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlSuaDac
        AND ViTri = 'KHO'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlSuaDac, 'KHO', 100.000, 10.000);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlDuongCat
        AND ViTri = 'KHO'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlDuongCat, 'KHO', 100.000, 10.000);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlLyNhua
        AND ViTri = 'KHO'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlLyNhua, 'KHO', 1000.000, 100.000);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlTCSong
        AND ViTri = 'KHO'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlTCSong, 'KHO', 50.000, 5.000);
-- BEP
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlTraDen
        AND ViTri = 'BEP'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlTraDen, 'BEP', 5.000, 1.000);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlSuaDac
        AND ViTri = 'BEP'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlSuaDac, 'BEP', 10.000, 2.000);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlDuongCat
        AND ViTri = 'BEP'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlDuongCat, 'BEP', 10.000, 2.000);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlLyNhua
        AND ViTri = 'BEP'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlLyNhua, 'BEP', 200.000, 30.000);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlTCSong
        AND ViTri = 'BEP'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlTCSong, 'BEP', 10.000, 2.000);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.TonNguyenLieu
    WHERE MaNL = @nlTCChin
        AND ViTri = 'BEP'
)
INSERT INTO dbo.TonNguyenLieu (MaNL, ViTri, SoLuong, NguongCanhBao)
VALUES (@nlTCChin, 'BEP', 50.000, 5.000);
-- 8. Công thức món (ProductRecipe)
DECLARE @monTSM BIGINT = (
        SELECT MaMon
        FROM dbo.MonUong
        WHERE TenMon = N'Trà Sữa Truyền Thống'
            AND Size = 'M'
    );
DECLARE @monTSL BIGINT = (
        SELECT MaMon
        FROM dbo.MonUong
        WHERE TenMon = N'Trà Sữa Truyền Thống'
            AND Size = 'L'
    );
DECLARE @monToppingTC BIGINT = (
        SELECT MaMon
        FROM dbo.MonUong
        WHERE TenMon = N'Trân Châu Đen'
            AND Size = 'M'
    );
-- Trà Sữa M
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CongThucMon
    WHERE MaMon = @monTSM
        AND MaNL = @nlTraDen
)
INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong)
VALUES (@monTSM, @nlTraDen, 0.015);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CongThucMon
    WHERE MaMon = @monTSM
        AND MaNL = @nlSuaDac
)
INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong)
VALUES (@monTSM, @nlSuaDac, 0.030);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CongThucMon
    WHERE MaMon = @monTSM
        AND MaNL = @nlDuongCat
)
INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong)
VALUES (@monTSM, @nlDuongCat, 0.020);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CongThucMon
    WHERE MaMon = @monTSM
        AND MaNL = @nlLyNhua
)
INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong)
VALUES (@monTSM, @nlLyNhua, 1.000);
-- Trà Sữa L
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CongThucMon
    WHERE MaMon = @monTSL
        AND MaNL = @nlTraDen
)
INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong)
VALUES (@monTSL, @nlTraDen, 0.020);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CongThucMon
    WHERE MaMon = @monTSL
        AND MaNL = @nlSuaDac
)
INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong)
VALUES (@monTSL, @nlSuaDac, 0.040);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CongThucMon
    WHERE MaMon = @monTSL
        AND MaNL = @nlDuongCat
)
INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong)
VALUES (@monTSL, @nlDuongCat, 0.025);
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CongThucMon
    WHERE MaMon = @monTSL
        AND MaNL = @nlLyNhua
)
INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong)
VALUES (@monTSL, @nlLyNhua, 1.000);
-- Topping Trân Châu Đen (1 suất)
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CongThucMon
    WHERE MaMon = @monToppingTC
        AND MaNL = @nlTCChin
)
INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong)
VALUES (@monToppingTC, @nlTCChin, 1.000);
-- 9. Công thức sơ chế (5kg Trân Châu Thô -> 50 suất Trân Châu Chín)
IF NOT EXISTS (
    SELECT 1
    FROM dbo.CongThucSoChe
    WHERE TenCT = N'Nấu Trân Châu Đen'
) BEGIN
INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, HoatDong)
VALUES (N'Nấu Trân Châu Đen', @nlTCChin, 50.000, 1);
DECLARE @maCTSC BIGINT = SCOPE_IDENTITY();
INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan)
VALUES (@maCTSC, @nlTCSong, 5.000);
END