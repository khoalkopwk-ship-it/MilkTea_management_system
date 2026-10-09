GO -- 1. Tạo tài khoản Quản Trị Viên mặc định (TaiKhoan)
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
        -- Mật khẩu băm an toàn (Password@123)
        N'Quản Trị Viên Hệ Thống',
        '0901000001',
        'ADMIN',
        1,
        1,
        SYSUTCDATETIME()
    );
PRINT N'[SUCCESS] Đã tạo tài khoản Quản Trị Viên mặc định: admin@milktea.vn';
END
ELSE BEGIN PRINT N'[SKIP] Tài khoản Quản Trị Viên đã tồn tại.';
END
GO -- 2. Cấu hình quán cơ bản (CauHinhChung)
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
        N'Số 1 Võ Văn Ngân, TP. Thủ Đức, TP. Hồ Chí Minh',
        N'MBBank',
        '0987654321',
        N'CHU QUAN TRA SUA',
        0.00,
        (
            SELECT TOP 1 MaTK
            FROM dbo.TaiKhoan
            WHERE VaiTro = 'ADMIN'
        ),
        SYSUTCDATETIME()
    );
PRINT N'[SUCCESS] Đã khởi tạo cấu hình quán mặc định.';
END
ELSE BEGIN PRINT N'[SKIP] Cấu hình quán đã tồn tại.';
END
GO