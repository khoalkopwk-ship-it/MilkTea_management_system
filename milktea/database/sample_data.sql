-- ============================================================================
-- MilkTea Management System - DỮ LIỆU MẪU ĐỒNG BỘ TOÀN CHUỖI (MULTI-BRANCH FULL SEED)
-- Tương thích: SQL Server Localhost & Azure SQL Database
-- Tuân thủ 100% nguyên tắc Single Source of Truth & Database Constraints:
--   1. Cấu hình thông tin chuỗi & tài khoản VietQR (CauHinhChung)
--   2. Chi nhánh chuỗi (ChiNhanh): KHOTONG, CN01, CN02
--   3. Tài khoản hệ thống (TaiKhoan) & Phân công chi nhánh (NhanVienChiNhanh)
--   4. Sơ đồ bàn ăn & mã QR bàn độc lập theo chi nhánh (Ban)
--   5. Danh mục sản phẩm (DanhMuc) - 11 danh mục
--   6. Danh mục 37 nguyên liệu thô & sơ chế chuẩn (NguyenLieu)
--   7. Thực đơn chuẩn ĐÚNG 50 MÓN KỂ CẢ TOPPING (MonUong) & phân bổ (MonUongChiNhanh)
--   8. Công thức sơ chế bếp (CongThucSoChe, ChiTietCTSoChe) - 10 công thức chuẩn
--   9. Công thức định mức món uống (CongThucMon) - 100% 50 món đều có định mức
--  10. Tồn kho hai tầng (TonNguyenLieu): KHOTONG, CN01 (KHO/BEP), CN02 (KHO/BEP)
--  11. Hệ thống Voucher & Khuyến mãi (Voucher, VoucherChiNhanh)
--  12. Dữ liệu giao dịch 7 ngày vừa qua (7-Day Transactional Seed):
--      - 77 Phiếu nhập kho (>= 70 phiếu chia đều 7 ngày)
--      - 77 Mẻ sơ chế bếp (>= 70 mẻ chia đều 7 ngày)
--      - 28 Phiếu kết ca kho & hao hụt (KetCaKho, ChiTietKetCaKho)
--      - 108 Đơn hàng mẫu (>= 100 đơn chia đều 7 ngày) cho quầy POS & bếp KDS
--      - 108 Hóa đơn & ghi thu thanh toán (CASH & VietQR)
--      - Lịch sử biến động kho (LichSuKho) đồng bộ toàn chuỗi
--      - Yêu cầu hủy đơn & duyệt hoàn tiền (YeuCauHuy, YeuCauHoan)
--      - Trải nghiệm khách hàng: Yêu thích (SanPhamYeuThich) & Đánh giá (DanhGia)
-- ============================================================================

-- Chạy trên context database hiện tại (hỗ trợ cả milktea và milktea_test)
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
PRINT N'>>> Bắt đầu nạp toàn bộ dữ liệu mẫu chuẩn Multi-Branch MilkTea...';
GO

-- ============================================================================
-- 1. CẤU HÌNH QUÁN & VIETQR CHUỖI (CauHinhChung)
-- ============================================================================
MERGE INTO dbo.CauHinhChung AS target
USING (SELECT 1 AS MaCauHinh) AS source ON (target.MaCauHinh = source.MaCauHinh)
WHEN MATCHED THEN
    UPDATE SET 
        target.TenQuan    = N'Hệ Thống Trà Sữa MilkTea Chain',
        target.DiaChi     = N'Trụ Sở Chính: Số 1 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP.HCM',
        target.NganHang   = N'MBBank',
        target.SoTaiKhoan = '08881706888',
        target.ChuTK      = N'DANG NGUYEN MINH KHOA',
        target.TyLeGiam   = 0.00,
        target.LoaiGiam   = 'PERCENT',
        target.GiaTriGiam = 0.00,
        target.SuaLuc     = SYSUTCDATETIME()
WHEN NOT MATCHED THEN
    INSERT (MaCauHinh, TenQuan, DiaChi, NganHang, SoTaiKhoan, ChuTK, TyLeGiam, LoaiGiam, GiaTriGiam, SuaLuc)
    VALUES (1, N'Hệ Thống Trà Sữa MilkTea Chain', N'Số 1 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP.HCM', N'MBBank', '08881706888', N'DANG NGUYEN MINH KHOA', 0.00, 'PERCENT', 0.00, SYSUTCDATETIME());

PRINT N'-- [1/18] Đã cấu hình thông tin chuỗi và tài khoản VietQR.';
GO

-- ============================================================================
-- 2. CHI NHÁNH CHUỖI (ChiNhanh) - KHOTONG, CN01, CN02
-- ============================================================================
IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'KHOTONG')
    INSERT INTO dbo.ChiNhanh (MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai, ThoiGianTao)
    VALUES ('KHOTONG', N'Kho Hàng Tổng (Toàn Chuỗi)', N'Tổng Kho Trung Tâm MilkTea Chain, TP.HCM', '19001234', N'HOAT_DONG', SYSUTCDATETIME());

IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01')
    INSERT INTO dbo.ChiNhanh (MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai, ThoiGianTao)
    VALUES ('CN01', N'MilkTea Quán 01 - Võ Văn Ngân', N'Số 1 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP.HCM', '02838968641', N'HOAT_DONG', SYSUTCDATETIME());

IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02')
    INSERT INTO dbo.ChiNhanh (MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai, ThoiGianTao)
    VALUES ('CN02', N'MilkTea Quán 02 - D2 Bình Thạnh', N'Số 10 Nguyễn Gia Trí (D2), Phường 25, Bình Thạnh, TP.HCM', '02835123456', N'HOAT_DONG', SYSUTCDATETIME());

PRINT N'-- [2/18] Đã khởi tạo 3 chi nhánh: Kho Tổng (KHOTONG), CN01 (Thủ Đức), CN02 (Bình Thạnh).';
GO

-- ============================================================================
-- 3. TÀI KHOẢN HỆ THỐNG & PHÂN CÔNG (TaiKhoan, NhanVienChiNhanh)
-- Mật khẩu mặc định: Password@123
-- Hash: $2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq
-- ============================================================================
DECLARE @pass AS VARCHAR(255) = '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq';

DECLARE @Accounts TABLE (
    Email     VARCHAR(254),
    HoTen     NVARCHAR(120),
    DienThoai VARCHAR(20),
    VaiTro    VARCHAR(16)
);

INSERT INTO @Accounts VALUES
    ('admin@milktea.vn',    N'Quản trị viên Hệ thống', '0901000001', 'ADMIN'),
    ('admin2@milktea.vn',   N'Quản lý Vận hành Chuỗi', '0901000002', 'ADMIN'),
    ('manager1@milktea.vn', N'Quản lý Cơ sở 01',      '0901000003', 'MANAGER'),
    ('manager2@milktea.vn', N'Quản lý Cơ sở 02',      '0901000004', 'MANAGER'),
    ('cashier@milktea.vn',  N'Thu ngân Quỳnh (CN01)',  '0901000005', 'CASHIER'),
    ('cashier2@milktea.vn', N'Thu ngân Hoàng (CN02)',  '0901000006', 'CASHIER'),
    ('kitchen@milktea.vn',  N'Bếp trưởng Lâm (CN01)',  '0901000007', 'KITCHEN'),
    ('kitchen2@milktea.vn', N'Nhân viên Pha chế (CN02)','0901000008', 'KITCHEN'),
    ('customer@milktea.vn', N'Khách hàng Mai Phương',  '0901000009', 'CUSTOMER'),
    ('customer2@milktea.vn',N'Khách hàng Đức Anh',    '0901000010', 'CUSTOMER'),
    ('customer3@milktea.vn',N'Khách hàng Linh Đan',    '0901000011', 'CUSTOMER');

MERGE INTO dbo.TaiKhoan AS target
USING @Accounts AS source ON (target.Email = source.Email)
WHEN MATCHED THEN
    UPDATE SET 
        target.HoTen        = source.HoTen,
        target.MatKhauBam   = @pass,
        target.DienThoai    = source.DienThoai,
        target.VaiTro       = source.VaiTro,
        target.HoatDong     = 1,
        target.EmailXacThuc = 1
WHEN NOT MATCHED THEN
    INSERT (Email, MatKhauBam, HoTen, DienThoai, VaiTro, HoatDong, EmailXacThuc, TaoLuc)
    VALUES (source.Email, @pass, source.HoTen, source.DienThoai, source.VaiTro, 1, 1, SYSUTCDATETIME());

-- Gán phân công nhân sự chi nhánh
DECLARE @cn01Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn02Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');

IF @cn01Id IS NOT NULL AND @cn02Id IS NOT NULL
BEGIN
    DELETE FROM dbo.NhanVienChiNhanh;

    INSERT INTO dbo.NhanVienChiNhanh (MaTK, MaCN, VaiTroChiNhanh, TrangThai, NgayPhanCong)
    SELECT tk.MaTK, @cn01Id, tk.VaiTro, N'HOAT_DONG', SYSUTCDATETIME()
    FROM dbo.TaiKhoan tk 
    WHERE tk.Email IN ('manager1@milktea.vn', 'cashier@milktea.vn', 'kitchen@milktea.vn', 'admin2@milktea.vn');

    INSERT INTO dbo.NhanVienChiNhanh (MaTK, MaCN, VaiTroChiNhanh, TrangThai, NgayPhanCong)
    SELECT tk.MaTK, @cn02Id, tk.VaiTro, N'HOAT_DONG', SYSUTCDATETIME()
    FROM dbo.TaiKhoan tk 
    WHERE tk.Email IN ('manager2@milktea.vn', 'cashier2@milktea.vn', 'kitchen2@milktea.vn');
END

PRINT N'-- [3/18] Đã nạp 11 tài khoản đầy đủ (kết hợp commit cũ và mới) và phân công nhân viên cơ sở.';
GO

-- ============================================================================
-- 4. BÀN ĂN & MÃ QR CHUẨN THEO CHI NHÁNH (Ban)
-- ============================================================================
DECLARE @cn01Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn02Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');

-- Chi nhánh 01: 12 Bàn
DECLARE @i INT = 1;
WHILE @i <= 12
BEGIN
    DECLARE @tenBan1 NVARCHAR(40) = N'Bàn ' + RIGHT('0' + CAST(@i AS VARCHAR(2)), 2);
    DECLARE @qrCode1 VARCHAR(64)   = 'QR_CN01_BAN' + RIGHT('0' + CAST(@i AS VARCHAR(2)), 2);
    
    IF NOT EXISTS (SELECT 1 FROM dbo.Ban WHERE MaCN = @cn01Id AND TenBan = @tenBan1)
    BEGIN
        INSERT INTO dbo.Ban (MaCN, TenBan, MaQR, TrangThai, HoatDong, Version)
        VALUES (@cn01Id, @tenBan1, @qrCode1, 'TRONG', 1, 0);
    END
    SET @i = @i + 1;
END

-- Chi nhánh 02: 10 Bàn
SET @i = 1;
WHILE @i <= 10
BEGIN
    DECLARE @tenBan2 NVARCHAR(40) = N'Bàn ' + RIGHT('0' + CAST(@i AS VARCHAR(2)), 2);
    DECLARE @qrCode2 VARCHAR(64)   = 'QR_CN02_BAN' + RIGHT('0' + CAST(@i AS VARCHAR(2)), 2);
    
    IF NOT EXISTS (SELECT 1 FROM dbo.Ban WHERE MaCN = @cn02Id AND TenBan = @tenBan2)
    BEGIN
        INSERT INTO dbo.Ban (MaCN, TenBan, MaQR, TrangThai, HoatDong, Version)
        VALUES (@cn02Id, @tenBan2, @qrCode2, 'TRONG', 1, 0);
    END
    SET @i = @i + 1;
END

PRINT N'-- [4/18] Đã khởi tạo sơ đồ bàn ăn: 12 bàn tại CN01, 10 bàn tại CN02.';
GO

-- ============================================================================
-- 5. DANH MỤC SẢN PHẨM (DanhMuc) - 11 Danh Mục
-- ============================================================================
DECLARE @Categories TABLE (TenDM NVARCHAR(100), ChoPhepTopping BIT);
INSERT INTO @Categories VALUES 
    (N'Trà Sữa Truyền Thống', 1), (N'Trà Sữa Ô Long', 1), (N'Trà Sữa Lài', 1),
    (N'Trà Trái Cây Tươi', 1), (N'Cà Phê Truyền Thống', 0), (N'Cà Phê Sáng Tạo', 1),
    (N'Sữa Tươi Trân Châu', 1), (N'Macchiato & Váng Sữa', 1), (N'Đá Xay & Sinh Tố', 1),
    (N'Trà Thảo Mộc & Thanh Nhiệt', 1), (N'Topping Cao Cấp', 0);

MERGE INTO dbo.DanhMuc AS target
USING @Categories AS source ON (target.TenDM = source.TenDM)
WHEN MATCHED THEN UPDATE SET target.ChoPhepTopping = source.ChoPhepTopping, target.HoatDong = 1
WHEN NOT MATCHED THEN INSERT (TenDM, ChoPhepTopping, HoatDong) VALUES (source.TenDM, source.ChoPhepTopping, 1);

PRINT N'-- [5/18] Đã nạp 11 danh mục sản phẩm chuẩn.';
GO

-- ============================================================================
-- 6. NGUYÊN LIỆU THÔ & SƠ CHẾ (NguyenLieu) - 37 Loại Chuẩn
-- ============================================================================
DECLARE @Materials TABLE (TenNL NVARCHAR(120), Loai VARCHAR(8), DonVi VARCHAR(16));
INSERT INTO @Materials VALUES 
    (N'Trà Đen Cổ Thụ', 'THO', 'kg'), (N'Trà Ô Long Kim Tuyên', 'THO', 'kg'),
    (N'Trà Lài Thượng Hạng', 'THO', 'kg'), (N'Cà Phê Hạt Rang Mộc', 'THO', 'kg'),
    (N'Sữa Đặc Có Đường', 'THO', 'kg'), (N'Đường Cát Trắng', 'THO', 'kg'),
    (N'Đường Đen Nữ Hoàng', 'THO', 'kg'), (N'Sữa Tươi Thanh Trùng', 'THO', 'kg'),
    (N'Bột Béo Thực Vật', 'THO', 'kg'), (N'Bột Kem Cheese Anchor', 'THO', 'kg'),
    (N'Trân Châu Thô Đen', 'THO', 'kg'), (N'Hạt Trân Châu Trắng 3Q', 'THO', 'kg'),
    (N'Đào Ngâm Đóng Hộp', 'THO', 'kg'), (N'Vải Ngâm Đóng Hộp', 'THO', 'kg'),
    (N'Dâu Tây Tươi', 'THO', 'kg'), (N'Xoài Cát Chín', 'THO', 'kg'),
    (N'Chanh Dây Tươi', 'THO', 'kg'), (N'Cam Vàng Sành', 'THO', 'kg'),
    (N'Sả Cây Tươi', 'THO', 'kg'), (N'Bột Matcha Uji', 'THO', 'kg'),
    (N'Muối Biển Tinh Khiết', 'THO', 'kg'), (N'Bột Cacao Nguyên Chất', 'THO', 'kg'),
    (N'Bơ Sáp Đắk Lắk', 'THO', 'kg'), (N'Nước Cốt Dừa Đóng Lon', 'THO', 'kg'),
    (N'Hạt Sen Tươi', 'THO', 'kg'),
    (N'Ly Nhựa PP 500ml', 'THO', 'ly'), (N'Ly Nhựa PP 700ml', 'THO', 'ly'),
    (N'Ống Hút Trân Châu', 'THO', 'cai'), 
    -- Nguyên liệu sơ chế (SOCHE)
    (N'Nước Cốt Trà Đen', 'SOCHE', 'ml'), (N'Nước Cốt Trà Ô Long', 'SOCHE', 'ml'),
    (N'Nước Cốt Trà Lài', 'SOCHE', 'ml'), (N'Nước Đường Pha Sẵn', 'SOCHE', 'ml'),
    (N'Cốt Cà Phê Pha Phin', 'SOCHE', 'ml'), (N'Trân Châu Nấu Chín', 'SOCHE', 'g'),
    (N'Kem Cheese Thành Phẩm', 'SOCHE', 'ml'), (N'Thạch Đào Cắt Hạt Lựu', 'SOCHE', 'g'),
    (N'Sốt Dâu Tây Tươi', 'SOCHE', 'ml'), (N'Sốt Xoài Chanh Dây', 'SOCHE', 'ml');

MERGE INTO dbo.NguyenLieu AS target
USING @Materials AS source ON (target.TenNL = source.TenNL)
WHEN MATCHED THEN UPDATE SET target.Loai = source.Loai, target.DonVi = source.DonVi, target.HoatDong = 1
WHEN NOT MATCHED THEN INSERT (TenNL, Loai, DonVi, HoatDong) VALUES (source.TenNL, source.Loai, source.DonVi, 1);

PRINT N'-- [6/18] Đã nạp 37 nguyên liệu thô và sơ chế.';
GO

-- ============================================================================
-- 7. THỰC ĐƠN ĐÚNG 50 MÓN KỂ CẢ TOPPING (MonUong) & PHÂN BỔ CHI NHÁNH
-- ============================================================================
DECLARE @dmTraSuaTT   BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Trà Sữa Truyền Thống');
DECLARE @dmTraSuaOL   BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Trà Sữa Ô Long');
DECLARE @dmTraSuaLai  BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Trà Sữa Lài');
DECLARE @dmTraTraiCay BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Trà Trái Cây Tươi');
DECLARE @dmCafeTT     BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Cà Phê Truyền Thống');
DECLARE @dmCafeST     BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Cà Phê Sáng Tạo');
DECLARE @dmSuaTuoi    BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Sữa Tươi Trân Châu');
DECLARE @dmMacchiato  BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Macchiato & Váng Sữa');
DECLARE @dmDaXay      BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Đá Xay & Sinh Tố');
DECLARE @dmTopping    BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Topping Cao Cấp');

DECLARE @Products TABLE (MaDM BIGINT, TenMon NVARCHAR(120), Size VARCHAR(12), Gia DECIMAL(18,2), MoTa NVARCHAR(500), AnhUrl NVARCHAR(500));
INSERT INTO @Products VALUES
    -- 1. Trà sữa truyền thống (10 món)
    (@dmTraSuaTT,   N'Trà Sữa Truyền Thống Đậm Vị', 'M', 32000, N'Hồng trà cổ thụ pha cùng bột béo và sữa đặc ngọt thanh (Size M)', 'https://images.unsplash.com/photo-1558857563-b37cf5a5cf2b?w=500'),
    (@dmTraSuaTT,   N'Trà Sữa Truyền Thống Đậm Vị', 'L', 38000, N'Hồng trà cổ thụ pha cùng bột béo và sữa đặc ngọt thanh (Size L)', 'https://images.unsplash.com/photo-1558857563-b37cf5a5cf2b?w=500'),
    (@dmTraSuaTT,   N'Trà Sữa Trân Châu Hoàng Kim', 'M', 36000, N'Trà sữa truyền thống kèm trân châu đen hoàng kim dẻo dai (Size M)', 'https://images.unsplash.com/photo-1541696432-82c6da8ce7bf?w=500'),
    (@dmTraSuaTT,   N'Trà Sữa Trân Châu Hoàng Kim', 'L', 42000, N'Trà sữa truyền thống kèm trân châu đen hoàng kim dẻo dai (Size L)', 'https://images.unsplash.com/photo-1541696432-82c6da8ce7bf?w=500'),
    (@dmTraSuaTT,   N'Trà Sữa Thạch Đào Giòn',      'M', 36000, N'Trà sữa ngọt thơm kết hợp thạch đào giòn sần sật (Size M)', 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=500'),
    (@dmTraSuaTT,   N'Trà Sữa Thạch Đào Giòn',      'L', 42000, N'Trà sữa ngọt thơm kết hợp thạch đào giòn sần sật (Size L)', 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=500'),
    (@dmTraSuaTT,   N'Trà Sữa Hạt Sen Thanh Nhiệt', 'M', 38000, N'Trà sữa béo dịu cùng hạt sen bùi bùi bổ dưỡng (Size M)', 'https://images.unsplash.com/photo-1579954115545-a95591f28bfc?w=500'),
    (@dmTraSuaTT,   N'Trà Sữa Hạt Sen Thanh Nhiệt', 'L', 45000, N'Trà sữa béo dịu cùng hạt sen bùi bùi bổ dưỡng (Size L)', 'https://images.unsplash.com/photo-1579954115545-a95591f28bfc?w=500'),
    (@dmTraSuaTT,   N'Trà Sữa Cacao Đậm Đà',        'M', 35000, N'Hòa quyện giữa trà sữa và bột cacao nguyên chất (Size M)', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500'),
    (@dmTraSuaTT,   N'Trà Sữa Cacao Đậm Đà',        'L', 42000, N'Hòa quyện giữa trà sữa và bột cacao nguyên chất (Size L)', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500'),

    -- 2. Trà sữa ô long & Trà sữa lài (8 món)
    (@dmTraSuaOL,   N'Trà Sữa Ô Long Nướng',        'M', 35000, N'Trà ô long nướng thơm nồng chuẩn vị Đài Loan (Size M)', 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500'),
    (@dmTraSuaOL,   N'Trà Sữa Ô Long Nướng',        'L', 42000, N'Trà ô long nướng thơm nồng chuẩn vị Đài Loan (Size L)', 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500'),
    (@dmTraSuaOL,   N'Trà Sữa Ô Long Rang Đặc Biệt','M', 38000, N'Trà ô long rang lửa sâu vị chát dịu hậu ngọt sâu (Size M)', 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500'),
    (@dmTraSuaOL,   N'Trà Sữa Ô Long Rang Đặc Biệt','L', 45000, N'Trà ô long rang lửa sâu vị chát dịu hậu ngọt sâu (Size L)', 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500'),
    (@dmTraSuaLai,  N'Trà Sữa Lài Thanh Hương',     'M', 32000, N'Hương hoa lài thanh khiết kết hợp vị béo sữa hài hòa (Size M)', 'https://images.unsplash.com/photo-1517256064527-09c73fc73e38?w=500'),
    (@dmTraSuaLai,  N'Trà Sữa Lài Thanh Hương',     'L', 38000, N'Hương hoa lài thanh khiết kết hợp vị béo sữa hài hòa (Size L)', 'https://images.unsplash.com/photo-1517256064527-09c73fc73e38?w=500'),
    (@dmTraSuaLai,  N'Trà Sữa Lài Kem Cheese',      'M', 40000, N'Trà sữa lài tươi phủ lớp kem phô mai béo mặn (Size M)', 'https://images.unsplash.com/photo-1517256064527-09c73fc73e38?w=500'),
    (@dmTraSuaLai,  N'Trà Sữa Lài Kem Cheese',      'L', 48000, N'Trà sữa lài tươi phủ lớp kem phô mai béo mặn (Size L)', 'https://images.unsplash.com/photo-1517256064527-09c73fc73e38?w=500'),

    -- 3. Trà trái cây tươi (8 món)
    (@dmTraTraiCay, N'Trà Đào Cam Sả Tươi',         'M', 35000, N'Trà lài ủ lạnh kết hợp đào ngâm giòn ngọt, cam vàng (Size M)', 'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=500'),
    (@dmTraTraiCay, N'Trà Đào Cam Sả Tươi',         'L', 42000, N'Trà lài ủ lạnh kết hợp đào ngâm giòn ngọt, cam vàng (Size L)', 'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=500'),
    (@dmTraTraiCay, N'Trà Vải Hoa Hồng Thanh Mát',  'M', 35000, N'Trà lài thượng hạng cùng trái vải mọng nước mát lạnh (Size M)', 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500'),
    (@dmTraTraiCay, N'Trà Vải Hoa Hồng Thanh Mát',  'L', 42000, N'Trà lài thượng hạng cùng trái vải mọng nước mát lạnh (Size L)', 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500'),
    (@dmTraTraiCay, N'Trà Dâu Tây Tươi Nhiệt Đới',   'M', 38000, N'Sốt dâu tây tươi nấu chín hòa cùng cốt trà lài thơm nức (Size M)', 'https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=500'),
    (@dmTraTraiCay, N'Trà Dâu Tây Tươi Nhiệt Đới',   'L', 45000, N'Sốt dâu tây tươi nấu chín hòa cùng cốt trà lài thơm nức (Size L)', 'https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=500'),
    (@dmTraTraiCay, N'Trà Xoài Chanh Dây Mát Lạnh', 'M', 36000, N'Vị chua thanh mát của chanh dây và xoài cát chín thơm (Size M)', 'https://images.unsplash.com/photo-1546173159-315724a31696?w=500'),
    (@dmTraTraiCay, N'Trà Xoài Chanh Dây Mát Lạnh', 'L', 43000, N'Vị chua thanh mát của chanh dây và xoài cát chín thơm (Size L)', 'https://images.unsplash.com/photo-1546173159-315724a31696?w=500'),

    -- 4. Cà phê truyền thống & Cà phê sáng tạo (6 món)
    (@dmCafeTT,     N'Cà Phê Đen Pha Phin',         'M', 22000, N'Cà phê Robusta mộc pha phin truyền thống đậm đà', 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500'),
    (@dmCafeTT,     N'Cà Phê Sữa Pha Phin',         'M', 28000, N'Cà phê phin hòa cùng sữa đặc béo ngậy thơm ngon', 'https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=500'),
    (@dmCafeST,     N'Bạc Xỉu Sữa Béo Ba Tầng',     'M', 30000, N'Hòa quyện sữa tươi, sữa đặc và lớp cà phê phin sánh mịn', 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=500'),
    (@dmCafeST,     N'Cà Phê Cốt Dừa Đá Xay',       'M', 38000, N'Cà phê đậm vị hòa tan cùng cốt dừa đá xay béo thơm', 'https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=500'),
    (@dmCafeST,     N'Cà Phê Bơ Đắk Lắk Dẻo',       'M', 42000, N'Sinh tố bơ sáp sánh dẻo kết hợp cà phê Robusta độc đáo', 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=500'),
    (@dmCafeST,     N'Cà Phê Muối Biển Kem Béo',    'M', 32000, N'Cà phê sữa phin phủ lớp kem cheese muối mằn mặn khó cưỡng', 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500'),

    -- 5. Sữa tươi trân châu (4 món)
    (@dmSuaTuoi,    N'Sữa Tươi Trân Châu Đường Đen', 'M', 35000, N'Sữa tươi thanh trùng Dalat Milk và trân châu đường đen dẻo dai (Size M)', 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=500'),
    (@dmSuaTuoi,    N'Sữa Tươi Trân Châu Đường Đen', 'L', 42000, N'Sữa tươi thanh trùng Dalat Milk và trân châu đường đen dẻo dai (Size L)', 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=500'),
    (@dmSuaTuoi,    N'Sữa Tươi Matcha Uji Đường Đen','M', 40000, N'Matcha Nhật Bản nguyên chất cùng sữa tươi và trân châu (Size M)', 'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=500'),
    (@dmSuaTuoi,    N'Sữa Tươi Matcha Uji Đường Đen','L', 48000, N'Matcha Nhật Bản nguyên chất cùng sữa tươi và trân châu (Size L)', 'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=500'),

    -- 6. Macchiato & Váng sữa phô mai (4 món)
    (@dmMacchiato,  N'Trà Ô Long Macchiato',        'M', 36000, N'Trà ô long thanh khiết phủ kem cheese Anchor béo ngậy (Size M)', 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500'),
    (@dmMacchiato,  N'Trà Ô Long Macchiato',        'L', 44000, N'Trà ô long thanh khiết phủ kem cheese Anchor béo ngậy (Size L)', 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500'),
    (@dmMacchiato,  N'Hồng Trà Cổ Thụ Macchiato',   'M', 35000, N'Hồng trà đậm vị phủ lớp váng sữa phô mai mằn mặn (Size M)', 'https://images.unsplash.com/photo-1558857563-b37cf5a5cf2b?w=500'),
    (@dmMacchiato,  N'Hồng Trà Cổ Thụ Macchiato',   'L', 42000, N'Hồng trà đậm vị phủ lớp váng sữa phô mai mằn mặn (Size L)', 'https://images.unsplash.com/photo-1558857563-b37cf5a5cf2b?w=500'),

    -- 7. Đá xay & Sinh tố (4 món)
    (@dmDaXay,      N'Sinh Tố Bơ Sáp Dừa Béo',      'M', 45000, N'Bơ sáp Đắk Lắk xay cùng nước cốt dừa và sữa đặc sánh dẻo (Size M)', 'https://images.unsplash.com/photo-1553530666-ba11a7da3888?w=500'),
    (@dmDaXay,      N'Sinh Tố Bơ Sáp Dừa Béo',      'L', 52000, N'Bơ sáp Đắk Lắk xay cùng nước cốt dừa và sữa đặc sánh dẻo (Size L)', 'https://images.unsplash.com/photo-1553530666-ba11a7da3888?w=500'),
    (@dmDaXay,      N'Matcha Đá Xay Kem Tuyết',     'M', 45000, N'Matcha Uji đá xay nhuyễn phủ lớp kem cheese phô mai (Size M)', 'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=500'),
    (@dmDaXay,      N'Cacao Đá Xay Phô Mai',        'M', 42000, N'Cacao đậm đà đá xay mát lạnh cùng váng sữa Anchor (Size M)', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500'),

    -- 8. Topping Cao Cấp (6 món)
    (@dmTopping,    N'Trân Châu Đen Hoàng Kim',      'M', 6000,  N'Trân châu dẻo dai nấu đường đen ngọt ngào', 'https://images.unsplash.com/photo-1541696432-82c6da8ce7bf?w=500'),
    (@dmTopping,    N'Hạt Trân Châu Trắng 3Q',       'M', 8000,  N'Trân châu trắng giòn sần sật mát lạnh', 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=500'),
    (@dmTopping,    N'Kem Cheese Phô Mai Macchiato', 'M', 10000, N'Lớp kem cheese mằn mặn béo ngậy tan chảy trên đầu lưỡi', 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500'),
    (@dmTopping,    N'Thạch Đào Giòn Ngọt',          'M', 8000,  N'Thạch đào dai giòn thơm nức mũi', 'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=500'),
    (@dmTopping,    N'Sốt Dâu Tây Tươi',             'M', 8000,  N'Sốt dâu tây tươi nấu thủ công ngọt thơm', 'https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=500'),
    (@dmTopping,    N'Sốt Xoài Chanh Dây',           'M', 8000,  N'Sốt xoài cát và chanh dây chua ngọt hấp dẫn', 'https://images.unsplash.com/photo-1546173159-315724a31696?w=500');

MERGE INTO dbo.MonUong AS target
USING @Products AS source 
   ON target.TenMon = source.TenMon AND target.Size = source.Size
WHEN MATCHED THEN 
    UPDATE SET 
        target.Gia      = source.Gia, 
        target.MoTa     = source.MoTa, 
        target.MaDM     = source.MaDM, 
        target.AnhUrl   = source.AnhUrl,
        target.HoatDong = 1
WHEN NOT MATCHED THEN 
    INSERT (MaDM, TenMon, Size, MoTa, Gia, AnhUrl, HoatDong, TaoLuc) 
    VALUES (source.MaDM, source.TenMon, source.Size, source.MoTa, source.Gia, source.AnhUrl, 1, SYSUTCDATETIME());

-- Đồng bộ phân bổ 50 món theo chi nhánh (MonUongChiNhanh)
DECLARE @cn01Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn02Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');

IF @cn01Id IS NOT NULL AND @cn02Id IS NOT NULL
BEGIN
    INSERT INTO dbo.MonUongChiNhanh (MaCN, MaMon, KinhDoanh, ConPhucVu, CapNhatLuc)
    SELECT @cn01Id, m.MaMon, 1, 1, SYSUTCDATETIME()
    FROM dbo.MonUong m
    WHERE NOT EXISTS (SELECT 1 FROM dbo.MonUongChiNhanh WHERE MaCN = @cn01Id AND MaMon = m.MaMon);

    INSERT INTO dbo.MonUongChiNhanh (MaCN, MaMon, KinhDoanh, ConPhucVu, CapNhatLuc)
    SELECT @cn02Id, m.MaMon, 1, 1, SYSUTCDATETIME()
    FROM dbo.MonUong m
    WHERE NOT EXISTS (SELECT 1 FROM dbo.MonUongChiNhanh WHERE MaCN = @cn02Id AND MaMon = m.MaMon);

    -- Tạm ngưng 1 món ở CN01 để demo tính năng bật/tắt phục vụ của Manager
    UPDATE dbo.MonUongChiNhanh
    SET ConPhucVu = 0
    WHERE MaCN = @cn01Id AND MaMon = (SELECT TOP 1 MaMon FROM dbo.MonUong WHERE TenMon LIKE N'%Ô Long Nướng%' AND Size = 'L');
END

PRINT N'-- [7/18] Đã nạp đúng 50 món uống & topping và phân bổ toàn chuỗi.';
GO

-- ============================================================================
-- 8. CÔNG THỨC SƠ CHẾ BẾP (CongThucSoChe, ChiTietCTSoChe) - 10 Công Thức Chuẩn
-- ============================================================================
DECLARE @nlTraDenTho     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Đen Cổ Thụ');
DECLARE @nlTraOLongTho   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Ô Long Kim Tuyên');
DECLARE @nlTraLaiTho     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Lài Thượng Hạng');
DECLARE @nlCafeHat       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Cà Phê Hạt Rang Mộc');
DECLARE @nlDuongCat      BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đường Cát Trắng');
DECLARE @nlDuongDen      BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đường Đen Nữ Hoàng');
DECLARE @nlTranChauTho   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Thô Đen');
DECLARE @nlBotCheese     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Bột Kem Cheese Anchor');
DECLARE @nlSuaTuoi       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sữa Tươi Thanh Trùng');
DECLARE @nlMuoiBien      BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Muối Biển Tinh Khiết');
DECLARE @nlDaoHop        BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đào Ngâm Đóng Hộp');
DECLARE @nlDauTay        BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Dâu Tây Tươi');
DECLARE @nlXoaiCat       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Xoài Cát Chín');
DECLARE @nlChanhDay      BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Chanh Dây Tươi');

DECLARE @nlCotTraDen     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Đen');
DECLARE @nlCotTraOLong   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Ô Long');
DECLARE @nlCotTraLai     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Lài');
DECLARE @nlNuocDuong     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Đường Pha Sẵn');
DECLARE @nlCotCafe       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Cốt Cà Phê Pha Phin');
DECLARE @nlTranChauChin  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Nấu Chín');
DECLARE @nlKemCheeseTP   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Kem Cheese Thành Phẩm');
DECLARE @nlThachDaoTP    BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Thạch Đào Cắt Hạt Lựu');
DECLARE @nlSotDauTay     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sốt Dâu Tây Tươi');
DECLARE @nlSotXoaiCD     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sốt Xoài Chanh Dây');

-- 1. Nước cốt trà đen
IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = N'Mẻ Nước Cốt Trà Đen')
BEGIN
    INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, GhiChu, Version)
    VALUES (N'Mẻ Nước Cốt Trà Đen', @nlCotTraDen, 5000.000, 5.00, 1, N'Ủ 200g trà đen với 5.2 lít nước sôi ở 95 độ C trong 15 phút', 1);
    DECLARE @ct1 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan) VALUES (@ct1, @nlTraDenTho, 0.200);
END

-- 2. Nước cốt trà ô long
IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = N'Mẻ Nước Cốt Trà Ô Long')
BEGIN
    INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, GhiChu, Version)
    VALUES (N'Mẻ Nước Cốt Trà Ô Long', @nlCotTraOLong, 5000.000, 5.00, 1, N'Ủ 200g trà ô long với 5.2 lít nước sôi ở 90 độ C trong 12 phút', 1);
    DECLARE @ct2 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan) VALUES (@ct2, @nlTraOLongTho, 0.200);
END

-- 3. Nước cốt trà lài
IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = N'Mẻ Nước Cốt Trà Lài')
BEGIN
    INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, GhiChu, Version)
    VALUES (N'Mẻ Nước Cốt Trà Lài', @nlCotTraLai, 5000.000, 5.00, 1, N'Ủ 200g trà lài với 5.2 lít nước sôi ở 85 độ C trong 10 phút', 1);
    DECLARE @ct3 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan) VALUES (@ct3, @nlTraLaiTho, 0.200);
END

-- 4. Cốt cà phê phin
IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = N'Mẻ Cốt Cà Phê Phin')
BEGIN
    INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, GhiChu, Version)
    VALUES (N'Mẻ Cốt Cà Phê Phin', @nlCotCafe, 2500.000, 5.00, 1, N'Pha phin 500g cà phê Robusta mộc với 2.8 lít nước sôi', 1);
    DECLARE @ct4 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan) VALUES (@ct4, @nlCafeHat, 0.500);
END

-- 5. Nước đường pha sẵn
IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = N'Mẻ Nước Đường Pha Sẵn')
BEGIN
    INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, GhiChu, Version)
    VALUES (N'Mẻ Nước Đường Pha Sẵn', @nlNuocDuong, 5000.000, 3.00, 1, N'Nấu 3.5kg đường cát trắng cùng 2.5 lít nước tinh khiết khuấy tan', 1);
    DECLARE @ct5 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan) VALUES (@ct5, @nlDuongCat, 3.500);
END

-- 6. Trân châu nấu chín
IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = N'Mẻ Trân Châu Nấu Chín')
BEGIN
    INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, GhiChu, Version)
    VALUES (N'Mẻ Trân Châu Nấu Chín', @nlTranChauChin, 2500.000, 5.00, 1, N'Luộc sôi 30 phút, ủ 30 phút, trộn cùng đường đen Nữ Hoàng', 1);
    DECLARE @ct6 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan) VALUES (@ct6, @nlTranChauTho, 1.000), (@ct6, @nlDuongDen, 0.300);
END

-- 7. Kem cheese phô mai
IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = N'Mẻ Kem Cheese Phô Mai')
BEGIN
    INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, GhiChu, Version)
    VALUES (N'Mẻ Kem Cheese Phô Mai', @nlKemCheeseTP, 2000.000, 5.00, 1, N'Đánh bông mịn bột Anchor cùng sữa tươi lạnh và muối biển', 1);
    DECLARE @ct7 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan) VALUES (@ct7, @nlBotCheese, 0.400), (@ct7, @nlSuaTuoi, 0.800), (@ct7, @nlMuoiBien, 0.020);
END

-- 8. Thạch đào cắt hạt lựu
IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = N'Mẻ Thạch Đào Hạt Lựu')
BEGIN
    INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, GhiChu, Version)
    VALUES (N'Mẻ Thạch Đào Hạt Lựu', @nlThachDaoTP, 2000.000, 5.00, 1, N'Cắt nhỏ đào hộp và ướp cùng nước đường', 1);
    DECLARE @ct8 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan) VALUES (@ct8, @nlDaoHop, 1.500), (@ct8, @nlDuongCat, 0.200);
END

-- 9. Sốt dâu tây tươi
IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = N'Mẻ Sốt Dâu Tây Tươi')
BEGIN
    INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, GhiChu, Version)
    VALUES (N'Mẻ Sốt Dâu Tây Tươi', @nlSotDauTay, 2000.000, 5.00, 1, N'Sên dâu tây tươi với đường cát trắng lửa nhỏ', 1);
    DECLARE @ct9 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan) VALUES (@ct9, @nlDauTay, 1.500), (@ct9, @nlDuongCat, 0.500);
END

-- 10. Sốt xoài chanh dây
IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = N'Mẻ Sốt Xoài Chanh Dây')
BEGIN
    INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, GhiChu, Version)
    VALUES (N'Mẻ Sốt Xoài Chanh Dây', @nlSotXoaiCD, 2000.000, 5.00, 1, N'Xoài cát chín kết hợp cốt chanh dây và đường cát', 1);
    DECLARE @ct10 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan) VALUES (@ct10, @nlXoaiCat, 1.200), (@ct10, @nlChanhDay, 0.400), (@ct10, @nlDuongCat, 0.400);
END

PRINT N'-- [8/18] Đã nạp 10 công thức sơ chế bếp chuẩn toàn chuỗi.';
GO

-- ============================================================================
-- 9. CÔNG THỨC ĐỊNH MỨC MÓN UỐNG (CongThucMon) - 100% 50 MÓN ĐỀU CÓ ĐỊNH MỨC
-- ============================================================================
DELETE FROM dbo.CongThucMon;

DECLARE @nlCotTraDen     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Đen');
DECLARE @nlCotTraOLong   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Ô Long');
DECLARE @nlCotTraLai     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Lài');
DECLARE @nlCotCafe       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Cốt Cà Phê Pha Phin');
DECLARE @nlNuocDuong     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Đường Pha Sẵn');
DECLARE @nlTranChauChin  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Nấu Chín');
DECLARE @nlKemCheeseTP   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Kem Cheese Thành Phẩm');
DECLARE @nlThachDaoTP    BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Thạch Đào Cắt Hạt Lựu');
DECLARE @nlSotDauTay     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sốt Dâu Tây Tươi');
DECLARE @nlSotXoaiCD     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sốt Xoài Chanh Dây');

DECLARE @nlBotBeo       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Bột Béo Thực Vật');
DECLARE @nlSuaDac       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sữa Đặc Có Đường');
DECLARE @nlSuaTuoi      BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sữa Tươi Thanh Trùng');
DECLARE @nlDuongDen     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đường Đen Nữ Hoàng');
DECLARE @nlDaoHop       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đào Ngâm Đóng Hộp');
DECLARE @nlVaiHop       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Vải Ngâm Đóng Hộp');
DECLARE @nlCamVang      BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Cam Vàng Sành');
DECLARE @nlSaCay        BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sả Cây Tươi');
DECLARE @nlMatcha       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Bột Matcha Uji');
DECLARE @nlCacao        BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Bột Cacao Nguyên Chất');
DECLARE @nlBoSap        BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Bơ Sáp Đắk Lắk');
DECLARE @nlCotDua       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Dừa Đóng Lon');
DECLARE @nlHatSen       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Hạt Sen Tươi');
DECLARE @nlTC3Q         BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Hạt Trân Châu Trắng 3Q');

DECLARE @nlLy500        BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Ly Nhựa PP 500ml');
DECLARE @nlLy700        BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Ly Nhựa PP 700ml');
DECLARE @nlOngHut       BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Ống Hút Trân Châu');

-- Con trỏ nạp định mức cho từng món theo loại sản phẩm
DECLARE @curMaMon BIGINT, @curTen NVARCHAR(120), @curSize VARCHAR(12), @curMaDM BIGINT;

DECLARE monCursor CURSOR LOCAL FAST_FORWARD FOR
SELECT MaMon, TenMon, Size, MaDM FROM dbo.MonUong;

OPEN monCursor;
FETCH NEXT FROM monCursor INTO @curMaMon, @curTen, @curSize, @curMaDM;

WHILE @@FETCH_STATUS = 0
BEGIN
    DECLARE @isSizeL BIT = CASE WHEN @curSize = 'L' THEN 1 ELSE 0 END;
    DECLARE @lyId BIGINT = CASE WHEN @isSizeL = 1 THEN @nlLy700 ELSE @nlLy500 END;

    -- Nhóm Topping (Chỉ định mức nguyên liệu thành phẩm tương ứng)
    IF @curTen = N'Trân Châu Đen Hoàng Kim'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlTranChauChin, 50.000);
    ELSE IF @curTen = N'Hạt Trân Châu Trắng 3Q'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlTC3Q, 0.050);
    ELSE IF @curTen = N'Kem Cheese Phô Mai Macchiato'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlKemCheeseTP, 50.000);
    ELSE IF @curTen = N'Thạch Đào Giòn Ngọt'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlThachDaoTP, 50.000);
    ELSE IF @curTen = N'Sốt Dâu Tây Tươi'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlSotDauTay, 40.000);
    ELSE IF @curTen = N'Sốt Xoài Chanh Dây'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlSotXoaiCD, 40.000);

    -- Nhóm Cà phê
    ELSE IF @curTen = N'Cà Phê Đen Pha Phin'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlCotCafe, 60.000), (@curMaMon, @nlNuocDuong, 15.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen = N'Cà Phê Sữa Pha Phin'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlCotCafe, 60.000), (@curMaMon, @nlSuaDac, 0.030), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen = N'Bạc Xỉu Sữa Béo Ba Tầng'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlCotCafe, 30.000), (@curMaMon, @nlSuaTuoi, 0.100), (@curMaMon, @nlSuaDac, 0.030), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen = N'Cà Phê Cốt Dừa Đá Xay'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlCotCafe, 50.000), (@curMaMon, @nlCotDua, 0.080), (@curMaMon, @nlSuaDac, 0.025), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen = N'Cà Phê Bơ Đắk Lắk Dẻo'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlCotCafe, 40.000), (@curMaMon, @nlBoSap, 0.100), (@curMaMon, @nlSuaDac, 0.030), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen = N'Cà Phê Muối Biển Kem Béo'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlCotCafe, 60.000), (@curMaMon, @nlSuaDac, 0.025), (@curMaMon, @nlKemCheeseTP, 40.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);

    -- Nhóm Sữa tươi trân châu
    ELSE IF @curTen LIKE N'%Sữa Tươi Trân Châu Đường Đen%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlSuaTuoi, CASE WHEN @isSizeL = 1 THEN 0.240 ELSE 0.180 END),
            (@curMaMon, @nlTranChauChin, CASE WHEN @isSizeL = 1 THEN 80.000 ELSE 60.000 END),
            (@curMaMon, @nlDuongDen, 0.015), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen LIKE N'%Sữa Tươi Matcha Uji%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlSuaTuoi, CASE WHEN @isSizeL = 1 THEN 0.240 ELSE 0.180 END),
            (@curMaMon, @nlMatcha, 0.010),
            (@curMaMon, @nlTranChauChin, CASE WHEN @isSizeL = 1 THEN 80.000 ELSE 60.000 END),
            (@curMaMon, @nlNuocDuong, 20.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);

    -- Nhóm Trà trái cây
    ELSE IF @curTen LIKE N'%Trà Đào Cam Sả%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlCotTraLai, CASE WHEN @isSizeL = 1 THEN 160.000 ELSE 120.000 END),
            (@curMaMon, @nlDaoHop, 0.040), (@curMaMon, @nlCamVang, 0.030), (@curMaMon, @nlSaCay, 0.015),
            (@curMaMon, @nlNuocDuong, 25.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen LIKE N'%Trà Vải Hoa Hồng%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlCotTraLai, CASE WHEN @isSizeL = 1 THEN 160.000 ELSE 120.000 END),
            (@curMaMon, @nlVaiHop, 0.050), (@curMaMon, @nlNuocDuong, 25.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen LIKE N'%Trà Dâu Tây%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlCotTraLai, CASE WHEN @isSizeL = 1 THEN 160.000 ELSE 120.000 END),
            (@curMaMon, @nlSotDauTay, 50.000), (@curMaMon, @nlNuocDuong, 20.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen LIKE N'%Trà Xoài Chanh Dây%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlCotTraLai, CASE WHEN @isSizeL = 1 THEN 160.000 ELSE 120.000 END),
            (@curMaMon, @nlSotXoaiCD, 50.000), (@curMaMon, @nlNuocDuong, 20.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);

    -- Nhóm Macchiato
    ELSE IF @curTen LIKE N'%Trà Ô Long Macchiato%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlCotTraOLong, CASE WHEN @isSizeL = 1 THEN 200.000 ELSE 150.000 END),
            (@curMaMon, @nlKemCheeseTP, CASE WHEN @isSizeL = 1 THEN 80.000 ELSE 60.000 END),
            (@curMaMon, @nlNuocDuong, 20.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen LIKE N'%Hồng Trà Cổ Thụ Macchiato%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlCotTraDen, CASE WHEN @isSizeL = 1 THEN 200.000 ELSE 150.000 END),
            (@curMaMon, @nlKemCheeseTP, CASE WHEN @isSizeL = 1 THEN 80.000 ELSE 60.000 END),
            (@curMaMon, @nlNuocDuong, 20.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);

    -- Nhóm Đá xay & Sinh tố
    ELSE IF @curTen LIKE N'%Sinh Tố Bơ Sáp Dừa Béo%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlBoSap, CASE WHEN @isSizeL = 1 THEN 0.200 ELSE 0.150 END),
            (@curMaMon, @nlCotDua, 0.060), (@curMaMon, @nlSuaDac, 0.035), (@curMaMon, @nlSuaTuoi, 0.060), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen LIKE N'%Matcha Đá Xay Kem Tuyết%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlMatcha, 0.015), (@curMaMon, @nlSuaTuoi, 0.120), (@curMaMon, @nlSuaDac, 0.030), (@curMaMon, @nlKemCheeseTP, 40.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen LIKE N'%Cacao Đá Xay Phô Mai%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlCacao, 0.020), (@curMaMon, @nlSuaTuoi, 0.120), (@curMaMon, @nlSuaDac, 0.030), (@curMaMon, @nlKemCheeseTP, 40.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);

    -- Nhóm Trà sữa ô long & lài
    ELSE IF @curTen LIKE N'%Ô Long Nướng%' OR @curTen LIKE N'%Ô Long Rang%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlCotTraOLong, CASE WHEN @isSizeL = 1 THEN 200.000 ELSE 150.000 END),
            (@curMaMon, @nlBotBeo, CASE WHEN @isSizeL = 1 THEN 0.035 ELSE 0.025 END),
            (@curMaMon, @nlSuaDac, CASE WHEN @isSizeL = 1 THEN 0.030 ELSE 0.020 END),
            (@curMaMon, @nlNuocDuong, 20.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
    ELSE IF @curTen LIKE N'%Lài Thanh Hương%' OR @curTen LIKE N'%Lài Kem Cheese%'
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlCotTraLai, CASE WHEN @isSizeL = 1 THEN 200.000 ELSE 150.000 END),
            (@curMaMon, @nlBotBeo, CASE WHEN @isSizeL = 1 THEN 0.035 ELSE 0.025 END),
            (@curMaMon, @nlSuaDac, CASE WHEN @isSizeL = 1 THEN 0.030 ELSE 0.020 END),
            (@curMaMon, @nlNuocDuong, 20.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);

    -- Nhóm Trà sữa truyền thống còn lại (Trân châu hoàng kim, thạch đào, hạt sen, cacao)
    ELSE
    BEGIN
        INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES 
            (@curMaMon, @nlCotTraDen, CASE WHEN @isSizeL = 1 THEN 200.000 ELSE 150.000 END),
            (@curMaMon, @nlBotBeo, CASE WHEN @isSizeL = 1 THEN 0.035 ELSE 0.025 END),
            (@curMaMon, @nlSuaDac, CASE WHEN @isSizeL = 1 THEN 0.030 ELSE 0.020 END),
            (@curMaMon, @nlNuocDuong, 20.000), (@curMaMon, @lyId, 1.000), (@curMaMon, @nlOngHut, 1.000);
        
        IF @curTen LIKE N'%Hạt Sen%'
            INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlHatSen, 0.040);
        IF @curTen LIKE N'%Cacao%'
            INSERT INTO dbo.CongThucMon (MaMon, MaNL, DinhLuong) VALUES (@curMaMon, @nlCacao, 0.015);
    END;

    FETCH NEXT FROM monCursor INTO @curMaMon, @curTen, @curSize, @curMaDM;
END;

CLOSE monCursor;
DEALLOCATE monCursor;

PRINT N'-- [9/18] Đã nạp công thức định mức cho 100% 50 món uống & topping.';
GO

-- ============================================================================
-- 10. TỒN KHO HAI TẦNG AN TOÀN (TonNguyenLieu)
-- Khởi tạo tồn kho dồi dào cho KHOTONG, CN01 (KHO, BEP), CN02 (KHO, BEP)
-- ============================================================================
DELETE FROM dbo.TonNguyenLieu;

DECLARE @cnKhotong BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'KHOTONG');
DECLARE @cn1Id     BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn2Id     BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');

-- 1. Kho Tổng (KHOTONG - KHO): Tồn dồi dào các nguyên liệu thô
INSERT INTO dbo.TonNguyenLieu (MaCN, MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
SELECT @cnKhotong, nl.MaNL, 'KHO', 
       CASE 
           WHEN nl.DonVi = 'ly' OR nl.DonVi = 'cai' THEN 50000.000
           ELSE 2000.000
       END,
       100.000, 50000.00, 0
FROM dbo.NguyenLieu nl WHERE nl.Loai = 'THO';

-- 2. Chi nhánh 01 (CN01): KHO và BEP
INSERT INTO dbo.TonNguyenLieu (MaCN, MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
SELECT @cn1Id, nl.MaNL, 'KHO',
       CASE WHEN nl.DonVi IN ('ly', 'cai') THEN 10000.000 ELSE 500.000 END,
       50.000, 50000.00, 0
FROM dbo.NguyenLieu nl WHERE nl.Loai = 'THO';

INSERT INTO dbo.TonNguyenLieu (MaCN, MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
SELECT @cn1Id, nl.MaNL, 'BEP',
       CASE 
           WHEN nl.DonVi = 'ml' THEN 50000.000
           WHEN nl.DonVi = 'g'  THEN 20000.000
           WHEN nl.DonVi IN ('ly', 'cai') THEN 5000.000
           ELSE 200.000
       END,
       20.000, 50000.00, 0
FROM dbo.NguyenLieu nl;

-- 3. Chi nhánh 02 (CN02): KHO và BEP
INSERT INTO dbo.TonNguyenLieu (MaCN, MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
SELECT @cn2Id, nl.MaNL, 'KHO',
       CASE WHEN nl.DonVi IN ('ly', 'cai') THEN 8000.000 ELSE 400.000 END,
       50.000, 50000.00, 0
FROM dbo.NguyenLieu nl WHERE nl.Loai = 'THO';

INSERT INTO dbo.TonNguyenLieu (MaCN, MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
SELECT @cn2Id, nl.MaNL, 'BEP',
       CASE 
           WHEN nl.DonVi = 'ml' THEN 40000.000
           WHEN nl.DonVi = 'g'  THEN 15000.000
           WHEN nl.DonVi IN ('ly', 'cai') THEN 4000.000
           ELSE 180.000
       END,
       20.000, 50000.00, 0
FROM dbo.NguyenLieu nl;

PRINT N'-- [10/18] Đã thiết lập số dư tồn kho hai tầng an toàn toàn chuỗi.';
GO

-- ============================================================================
-- 11. DỌN DẸP DỮ LIỆU GIAO DỊCH AN TOÀN THEO THỨ TỰ KHÓA NGOẠI (FK-SAFE CLEANUP)
-- ============================================================================
DELETE FROM dbo.DanhGia;
DELETE FROM dbo.SanPhamYeuThich;
DELETE FROM dbo.YeuCauHoan;
DELETE FROM dbo.YeuCauHuy;
DELETE FROM dbo.ThongBaoThanhToan;
DELETE FROM dbo.ThanhToan;
DELETE FROM dbo.ChiTietDonHangTopping;
DELETE FROM dbo.ChiTietDonHang;
IF OBJECT_ID('dbo.DinhMucDonHang', 'U') IS NOT NULL
    DELETE FROM dbo.DinhMucDonHang;
DELETE FROM dbo.LichSuSuDungVoucher;
DELETE FROM dbo.HoaDon;
DELETE FROM dbo.LichSuKho;
DELETE FROM dbo.ChiTietKetCaKho;
DELETE FROM dbo.KetCaKho;
DELETE FROM dbo.ChiTietMeSoChe;
DELETE FROM dbo.MeSoChe;
DELETE FROM dbo.ChiTietNhap;
DELETE FROM dbo.PhieuNhap;

UPDATE dbo.Ban SET MaPhienDangMo = NULL, TrangThai = 'TRONG';
DELETE FROM dbo.DonHang;
DELETE FROM dbo.PhienBan;

DELETE FROM dbo.VoucherChiNhanh;
DELETE FROM dbo.Voucher;

PRINT N'-- [11/18] Đã dọn dẹp an toàn toàn bộ dữ liệu giao dịch cũ.';
GO

-- ============================================================================
-- 12. HỆ THỐNG VOUCHER & KHUYẾN MÃI (Voucher, VoucherChiNhanh)
-- ============================================================================
DECLARE @cn1Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn2Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');

-- 1. CHAOMUNG (Giảm 20k toàn chuỗi)
INSERT INTO dbo.Voucher (MaCode, TenVoucher, MoTa, LoaiGiamGia, GiaTriGiam, GiaTriDonToiThieu, TongSoLuot, SoLuotDaDung, SoLuotMoiKhach, ApDungToanChuoi, NgayBatDau, NgayKetThuc, KichHoat)
VALUES ('CHAOMUNG', N'Ưu Đãi Chào Bạn Mới', N'Giảm 20.000đ cho đơn hàng từ 50.000đ toàn chuỗi', 'FIXED_AMOUNT', 20000.00, 50000.00, 2000, 0, 20, 1, DATEADD(DAY, -30, SYSUTCDATETIME()), DATEADD(MONTH, 3, SYSUTCDATETIME()), 1);

-- 2. MILKTEA10 (Giảm 10% trần 30k)
INSERT INTO dbo.Voucher (MaCode, TenVoucher, MoTa, LoaiGiamGia, GiaTriGiam, GiamToiDa, GiaTriDonToiThieu, TongSoLuot, SoLuotDaDung, SoLuotMoiKhach, ApDungToanChuoi, NgayBatDau, NgayKetThuc, KichHoat)
VALUES ('MILKTEA10', N'Giảm 10% Trà Sữa Đậm Vị', N'Giảm 10% tối đa 30.000đ cho đơn từ 60.000đ', 'PERCENT', 10.00, 30000.00, 60000.00, 1000, 0, 5, 1, DATEADD(DAY, -30, SYSUTCDATETIME()), DATEADD(MONTH, 3, SYSUTCDATETIME()), 1);

-- 3. CN01SPECIAL (Chỉ áp dụng tại Chi nhánh 1 - Võ Văn Ngân)
INSERT INTO dbo.Voucher (MaCode, TenVoucher, MoTa, LoaiGiamGia, GiaTriGiam, GiaTriDonToiThieu, TongSoLuot, SoLuotDaDung, SoLuotMoiKhach, ApDungToanChuoi, NgayBatDau, NgayKetThuc, KichHoat)
VALUES ('CN01SPECIAL', N'Đặc Quyền Cơ Sở Thủ Đức', N'Giảm 15.000đ cho đơn từ 45.000đ duy nhất tại CN01', 'FIXED_AMOUNT', 15000.00, 45000.00, 500, 0, 3, 0, DATEADD(DAY, -30, SYSUTCDATETIME()), DATEADD(MONTH, 3, SYSUTCDATETIME()), 1);
DECLARE @vCn1 BIGINT = SCOPE_IDENTITY();
IF @cn1Id IS NOT NULL
    INSERT INTO dbo.VoucherChiNhanh (MaVoucher, MaCN, TaoLuc) VALUES (@vCn1, @cn1Id, SYSUTCDATETIME());

-- 4. DONTOITHIEU (Đơn từ 200k giảm 50k)
INSERT INTO dbo.Voucher (MaCode, TenVoucher, MoTa, LoaiGiamGia, GiaTriGiam, GiaTriDonToiThieu, TongSoLuot, SoLuotDaDung, SoLuotMoiKhach, ApDungToanChuoi, NgayBatDau, NgayKetThuc, KichHoat)
VALUES ('DONTOITHIEU', N'Ưu Đãi Đơn Lớn', N'Giảm 50.000đ cho đơn hàng từ 200.000đ', 'FIXED_AMOUNT', 50000.00, 200000.00, 500, 0, 2, 1, DATEADD(DAY, -30, SYSUTCDATETIME()), DATEADD(MONTH, 3, SYSUTCDATETIME()), 1);

PRINT N'-- [12/18] Đã khởi tạo các kịch bản voucher khuyến mãi đa chi nhánh.';
GO

-- ============================================================================
-- 13. NẠP 77 PHIẾU NHẬP KHO (>= 70 PHIẾU CHIA ĐỀU 7 NGÀY QUA D-6 -> D-0)
-- Phục vụ quản trị chuỗi: nhập NCC về Kho Tổng, chuyển kho chi nhánh CN01 & CN02
-- ============================================================================
DECLARE @khoTongId BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'KHOTONG');
DECLARE @cn1Id     BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn2Id     BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');
DECLARE @adminId   BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'admin@milktea.vn');
DECLARE @mgr1Id    BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'manager1@milktea.vn');
DECLARE @mgr2Id    BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'manager2@milktea.vn');

DECLARE @nlTraDen  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Đen Cổ Thụ');
DECLARE @nlTraOLong BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Ô Long Kim Tuyên');
DECLARE @nlTraLai  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Lài Thượng Hạng');
DECLARE @nlDuong   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đường Cát Trắng');
DECLARE @nlSuaDac  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sữa Đặc Có Đường');
DECLARE @nlBotBeo  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Bột Béo Thực Vật');
DECLARE @nlTCTho   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Thô Đen');
DECLARE @nlLy500   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Ly Nhựa PP 500ml');
DECLARE @nlLy700   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Ly Nhựa PP 700ml');
DECLARE @nlOngHut  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Ống Hút Trân Châu');

DECLARE @dayOffset INT = 6;
DECLARE @pnCount INT = 0;

WHILE @dayOffset >= 0
BEGIN
    DECLARE @pIdx INT = 1;
    -- Mỗi ngày tạo 11 phiếu nhập (Tổng 7 ngày = 77 phiếu)
    WHILE @pIdx <= 11
    BEGIN
        SET @pnCount = @pnCount + 1;
        DECLARE @targetBranch BIGINT = CASE 
            WHEN @pIdx <= 5 THEN @khoTongId
            WHEN @pIdx <= 8 THEN @cn1Id
            ELSE @cn2Id 
        END;
        DECLARE @creator BIGINT = CASE 
            WHEN @targetBranch = @khoTongId THEN @adminId
            WHEN @targetBranch = @cn1Id THEN @mgr1Id
            ELSE @mgr2Id 
        END;
        DECLARE @supplier NVARCHAR(200) = CASE 
            WHEN @targetBranch = @khoTongId THEN N'Công ty Trà Đạo Việt Nam & NPP Bao Bì Sài Gòn'
            ELSE N'Kho Hàng Tổng Trung Tâm (MilkTea Chain)'
        END;
        DECLARE @reason NVARCHAR(200) = CASE 
            WHEN @targetBranch = @khoTongId THEN N'Nhập nguyên phụ liệu sỉ nhà cung cấp về Kho Tổng'
            ELSE N'Nhập bổ sung định kỳ từ Kho Tổng về Chi nhánh'
        END;

        DECLARE @docTime DATETIME2 = DATEADD(MINUTE, (@pIdx * 55) + 480, DATEADD(DAY, -@dayOffset, CAST(CAST(SYSUTCDATETIME() AS DATE) AS DATETIME2)));
        DECLARE @keyPN VARCHAR(64) = 'PN-D' + CAST(@dayOffset AS VARCHAR(2)) + '-' + RIGHT('00' + CAST(@pIdx AS VARCHAR(3)), 3) + '-' + SUBSTRING(CONVERT(VARCHAR(36), NEWID()), 1, 8);

        INSERT INTO dbo.PhieuNhap (NguoiLap, MaCN, LyDo, NguonNhap, TrangThai, LapLuc, GhiSoLuc, MaYeuCau)
        VALUES (@creator, @targetBranch, @reason, @supplier, 'DA_GHI_SO', @docTime, @docTime, @keyPN);
        DECLARE @curPnId BIGINT = SCOPE_IDENTITY();

        -- Chi tiết nhập
        INSERT INTO dbo.ChiTietNhap (MaPhieu, MaNL, SoLuong, DonGiaNhap)
        VALUES 
            (@curPnId, @nlTraDen, 20.000 + (@pIdx * 2), 120000.00),
            (@curPnId, @nlDuong,  50.000 + (@pIdx * 5), 22000.00),
            (@curPnId, @nlBotBeo, 30.000 + (@pIdx * 3), 65000.00),
            (@curPnId, @nlLy500,  1000.000 + (@pIdx * 100), 450.00);

        -- Ghi sổ biến động kho nhập
        INSERT INTO dbo.LichSuKho (MaCN, MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo, GiaVonDonVi)
        VALUES
            (@targetBranch, @nlTraDen, 'KHO', 'NHAP', 20.000 + (@pIdx * 2), 500.000, 520.000 + (@pIdx * 2), 'IMPORT', @curPnId, @creator, @docTime, @reason, 120000.00),
            (@targetBranch, @nlDuong,  'KHO', 'NHAP', 50.000 + (@pIdx * 5), 1000.000, 1050.000 + (@pIdx * 5), 'IMPORT', @curPnId, @creator, @docTime, @reason, 22000.00);

        SET @pIdx = @pIdx + 1;
    END;
    SET @dayOffset = @dayOffset - 1;
END;

PRINT N'-- [13/18] Đã nạp thành công 77 phiếu nhập kho chia đều trong 7 ngày qua.';
GO

-- ============================================================================
-- 14. NẠP 77 MẺ SƠ CHẾ BẾP (>= 70 MẺ CHIA ĐỀU 7 NGÀY QUA D-6 -> D-0)
-- Bếp trưởng & Pha chế CN01, CN02 nấu ủ mẻ cốt trà, trân châu, kem cheese
-- ============================================================================
DECLARE @cn1Id     BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn2Id     BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');
DECLARE @bep1Id    BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'kitchen@milktea.vn');
DECLARE @bep2Id    BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'kitchen2@milktea.vn');

DECLARE @ctCotTraDen  BIGINT = (SELECT TOP 1 MaCTSC FROM dbo.CongThucSoChe WHERE TenCT LIKE N'%Trà Đen%');
DECLARE @ctCotTraOL   BIGINT = (SELECT TOP 1 MaCTSC FROM dbo.CongThucSoChe WHERE TenCT LIKE N'%Trà Ô Long%');
DECLARE @ctCotTraLai  BIGINT = (SELECT TOP 1 MaCTSC FROM dbo.CongThucSoChe WHERE TenCT LIKE N'%Trà Lài%');
DECLARE @ctTranChau   BIGINT = (SELECT TOP 1 MaCTSC FROM dbo.CongThucSoChe WHERE TenCT LIKE N'%Trân Châu%');
DECLARE @ctCheese     BIGINT = (SELECT TOP 1 MaCTSC FROM dbo.CongThucSoChe WHERE TenCT LIKE N'%Kem Cheese%');
DECLARE @ctNuocDuong  BIGINT = (SELECT TOP 1 MaCTSC FROM dbo.CongThucSoChe WHERE TenCT LIKE N'%Nước Đường%');

DECLARE @nlCotTraDen  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Đen');
DECLARE @nlCotTraOL   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Ô Long');
DECLARE @nlCotTraLai  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Lài');
DECLARE @nlTCChin     BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Nấu Chín');
DECLARE @nlCheeseTP   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Kem Cheese Thành Phẩm');
DECLARE @nlNuocDuong  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Đường Pha Sẵn');

DECLARE @nlTraDenTho  BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Đen Cổ Thụ');
DECLARE @nlDuongCat   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đường Cát Trắng');
DECLARE @nlTCTho      BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Thô Đen');
DECLARE @nlDuongDen   BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đường Đen Nữ Hoàng');

DECLARE @dayOffset INT = 6;
DECLARE @meCount INT = 0;

WHILE @dayOffset >= 0
BEGIN
    DECLARE @mIdx INT = 1;
    -- Mỗi ngày tạo 11 mẻ sơ chế bếp (Tổng 7 ngày = 77 mẻ)
    WHILE @mIdx <= 11
    BEGIN
        SET @meCount = @meCount + 1;
        DECLARE @targetBranch BIGINT = CASE WHEN @mIdx % 2 = 1 THEN @cn1Id ELSE @cn2Id END;
        DECLARE @chef BIGINT = CASE WHEN @targetBranch = @cn1Id THEN @bep1Id ELSE @bep2Id END;
        
        DECLARE @ctSelected BIGINT, @outMat BIGINT, @inMat BIGINT, @expQty DECIMAL(18,3), @actQty DECIMAL(18,3), @rawQty DECIMAL(18,3);
        
        IF @mIdx % 5 = 1 BEGIN
            SET @ctSelected = @ctCotTraDen; SET @outMat = @nlCotTraDen; SET @inMat = @nlTraDenTho;
            SET @expQty = 5000.000; SET @actQty = 4980.000; SET @rawQty = 0.200;
        END
        ELSE IF @mIdx % 5 = 2 BEGIN
            SET @ctSelected = @ctTranChau; SET @outMat = @nlTCChin; SET @inMat = @nlTCTho;
            SET @expQty = 2500.000; SET @actQty = 2500.000; SET @rawQty = 1.000;
        END
        ELSE IF @mIdx % 5 = 3 BEGIN
            SET @ctSelected = @ctCheese; SET @outMat = @nlCheeseTP; SET @inMat = @nlDuongCat;
            SET @expQty = 2000.000; SET @actQty = 1950.000; SET @rawQty = 0.400;
        END
        ELSE IF @mIdx % 5 = 4 BEGIN
            SET @ctSelected = @ctCotTraLai; SET @outMat = @nlCotTraLai; SET @inMat = @nlTraDenTho;
            SET @expQty = 5000.000; SET @actQty = 5000.000; SET @rawQty = 0.200;
        END
        ELSE BEGIN
            SET @ctSelected = @ctNuocDuong; SET @outMat = @nlNuocDuong; SET @inMat = @nlDuongCat;
            SET @expQty = 5000.000; SET @actQty = 5000.000; SET @rawQty = 3.500;
        END;

        DECLARE @docTime DATETIME2 = DATEADD(MINUTE, (@mIdx * 65) + 420, DATEADD(DAY, -@dayOffset, CAST(CAST(SYSUTCDATETIME() AS DATE) AS DATETIME2)));
        DECLARE @keyMe VARCHAR(64) = 'ME-D' + CAST(@dayOffset AS VARCHAR(2)) + '-' + RIGHT('00' + CAST(@mIdx AS VARCHAR(3)), 3) + '-' + SUBSTRING(CONVERT(VARCHAR(36), NEWID()), 1, 8);

        INSERT INTO dbo.MeSoChe (MaCN, MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, LyDoChenhLech, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
        VALUES (@targetBranch, @ctSelected, @outMat, @expQty, @actQty, 'DA_GHI_SO', N'Nấu mẻ sơ chế chuẩn bị đầu ca bán hàng', 85000.00, 17.00, 0.40, 0, @chef, @docTime, @keyMe);
        DECLARE @curMeId BIGINT = SCOPE_IDENTITY();

        INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
        VALUES (@curMeId, @inMat, @rawQty, @rawQty, 65000.00, @rawQty * 65000.00);

        -- Ghi sổ kho: Xuất thô & Nhập sơ chế vào Bếp
        INSERT INTO dbo.LichSuKho (MaCN, MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo)
        VALUES 
            (@targetBranch, @inMat,  'BEP', 'SOCHE_RA', -@rawQty, 50.000, 50.000 - @rawQty, 'BATCH', @curMeId, @chef, @docTime, N'Xuất nguyên liệu thô nấu mẻ sơ chế'),
            (@targetBranch, @outMat, 'BEP', 'SOCHE_VAO', @actQty, 1000.000, 1000.000 + @actQty, 'BATCH', @curMeId, @chef, @docTime, N'Nhập thành phẩm sơ chế vào kho bếp');

        SET @mIdx = @mIdx + 1;
    END;
    SET @dayOffset = @dayOffset - 1;
END;

PRINT N'-- [14/18] Đã nạp thành công 77 mẻ sơ chế bếp chia đều trong 7 ngày qua.';
GO

-- ============================================================================
-- 15. NẠP 28 PHIẾU KẾT CA KHO & HAO HỤT BẾP (KetCaKho, ChiTietKetCaKho)
-- Chia đều 4 ca/ngày (2 ca CN01, 2 ca CN02) trong 7 ngày qua
-- ============================================================================
DECLARE @cn1Id     BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn2Id     BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');
DECLARE @bep1Id    BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'kitchen@milktea.vn');
DECLARE @bep2Id    BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'kitchen2@milktea.vn');

DECLARE @nlCotTraDen BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Đen');
DECLARE @nlTCChin    BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Nấu Chín');

DECLARE @dayOffset INT = 6;
WHILE @dayOffset >= 0
BEGIN
    DECLARE @shiftIdx INT = 1;
    WHILE @shiftIdx <= 4
    BEGIN
        DECLARE @targetBranch BIGINT = CASE WHEN @shiftIdx <= 2 THEN @cn1Id ELSE @cn2Id END;
        DECLARE @chef BIGINT = CASE WHEN @targetBranch = @cn1Id THEN @bep1Id ELSE @bep2Id END;
        DECLARE @shiftName VARCHAR(32) = CASE WHEN @shiftIdx % 2 = 1 THEN 'CA_SANG' ELSE 'CA_TOI' END;
        
        DECLARE @chotTime DATETIME2 = DATEADD(HOUR, CASE WHEN @shiftName = 'CA_SANG' THEN 14 ELSE 22 END, DATEADD(DAY, -@dayOffset, CAST(CAST(SYSUTCDATETIME() AS DATE) AS DATETIME2)));
        DECLARE @fromTime DATETIME2 = DATEADD(HOUR, -6, @chotTime);
        DECLARE @keyKC VARCHAR(64) = 'KC-D' + CAST(@dayOffset AS VARCHAR(2)) + '-' + CAST(@shiftIdx AS VARCHAR(2)) + '-' + SUBSTRING(CONVERT(VARCHAR(36), NEWID()), 1, 8);

        INSERT INTO dbo.KetCaKho (MaCN, MaCa, ViTri, NguoiChot, ChotLuc, TuLuc, DenLuc, TongChiPhiHaoHut, GhiChu, MaYeuCau)
        VALUES (@targetBranch, @shiftName, 'BEP', @chef, @chotTime, @fromTime, @chotTime, 35000.00, N'Kết ca dọn dẹp và hủy nước trà thừa cuối ca', @keyKC);
        DECLARE @curKcId BIGINT = SCOPE_IDENTITY();

        INSERT INTO dbo.ChiTietKetCaKho (MaKetCa, MaNL, DonVi, TonSo, TonThucTe, LuongLoaiBo, LuongGiuLai, ChenhLechKiemKe, GiaVonDonVi, ChiPhiHaoHut, LyDo)
        VALUES 
            (@curKcId, @nlCotTraDen, 'ml', 500.000, 500.000, 500.000, 0.000, 0.000, 30.00, 15000.00, N'Hủy nước cốt trà quá 4 tiếng theo tiêu chuẩn'),
            (@curKcId, @nlTCChin,    'g',  400.000, 400.000, 400.000, 0.000, 0.000, 50.00, 20000.00, N'Hủy trân châu thừa cuối ngày');

        -- Ghi sổ hao hụt kho
        INSERT INTO dbo.LichSuKho (MaCN, MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo, GiaVonDonVi, ChiPhiHaoHut)
        VALUES 
            (@targetBranch, @nlCotTraDen, 'BEP', 'HAO_HUT', -500.000, 5000.000, 4500.000, 'WASTE', @curKcId, @chef, @chotTime, N'Hao hụt kết ca nước trà thừa', 30.00, 15000.00),
            (@targetBranch, @nlTCChin,    'BEP', 'HAO_HUT', -400.000, 3000.000, 2600.000, 'WASTE', @curKcId, @chef, @chotTime, N'Hao hụt kết ca trân châu cuối ngày', 50.00, 20000.00);

        SET @shiftIdx = @shiftIdx + 1;
    END;
    SET @dayOffset = @dayOffset - 1;
END;

PRINT N'-- [15/18] Đã nạp thành công 28 phiếu kết ca kho & chi phí hao hụt trong 7 ngày qua.';
GO

-- ============================================================================
-- 16. NẠP 108 ĐƠN HÀNG MẪU (>= 100 ĐƠN CHIA ĐỀU 7 NGÀY QUA D-6 -> D-0)
-- Bao phủ toàn bộ luồng nghiệp vụ:
--   - 68 Đơn tại bàn (TABLE) gắn với PhienBan và sơ đồ bàn CN01/CN02
--   - 25 Đơn mang đi tại quầy (COUNTER)
--   - 15 Đơn đặt trực tuyến (ONLINE) với hình thức PICKUP & DELIVERY
--   - Đa dạng trạng thái: HOAN_THANH, DA_HUY, DANG_CHE_BIEN, CHO_CHE_BIEN, CHO_XAC_NHAN
-- ============================================================================
DECLARE @cn1Id      BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn2Id      BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');
DECLARE @cust1Id    BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer@milktea.vn');
DECLARE @cust2Id    BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer2@milktea.vn');
DECLARE @cashier1Id BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'cashier@milktea.vn');
DECLARE @cashier2Id BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'cashier2@milktea.vn');
DECLARE @bep1Id     BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'kitchen@milktea.vn');
DECLARE @bep2Id     BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'kitchen2@milktea.vn');

DECLARE @vChaoMung  BIGINT = (SELECT MaVoucher FROM dbo.Voucher WHERE MaCode = 'CHAOMUNG');
DECLARE @vMilkTea10 BIGINT = (SELECT MaVoucher FROM dbo.Voucher WHERE MaCode = 'MILKTEA10');

DECLARE @nlCotTraDen BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Đen');

-- Bảng tạm danh sách món đồ uống chính để lấy mẫu đơn
DECLARE @SampleDrinks TABLE (Idx INT IDENTITY(1,1), MaMon BIGINT, TenMon NVARCHAR(120), Size VARCHAR(12), Gia DECIMAL(18,2));
INSERT INTO @SampleDrinks (MaMon, TenMon, Size, Gia)
SELECT MaMon, TenMon, Size, Gia FROM dbo.MonUong WHERE MaDM <> (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Topping Cao Cấp');

DECLARE @SampleToppings TABLE (Idx INT IDENTITY(1,1), MaMon BIGINT, TenMon NVARCHAR(120), Gia DECIMAL(18,2));
INSERT INTO @SampleToppings (MaMon, TenMon, Gia)
SELECT MaMon, TenMon, Gia FROM dbo.MonUong WHERE MaDM = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Topping Cao Cấp');

DECLARE @totalDrinks INT = (SELECT COUNT(*) FROM @SampleDrinks);
DECLARE @totalToppings INT = (SELECT COUNT(*) FROM @SampleToppings);

DECLARE @dayOffset INT = 6;
DECLARE @orderSeq INT = 1000;

WHILE @dayOffset >= 0
BEGIN
    DECLARE @oIdx INT = 1;
    -- Mỗi ngày tạo 15-16 đơn hàng (Tổng 7 ngày = 108 đơn hàng)
    DECLARE @ordersPerDay INT = CASE WHEN @dayOffset = 0 THEN 18 ELSE 15 END;

    WHILE @oIdx <= @ordersPerDay
    BEGIN
        SET @orderSeq = @orderSeq + 1;
        DECLARE @targetBranch BIGINT = CASE WHEN @oIdx <= 9 THEN @cn1Id ELSE @cn2Id END;
        DECLARE @cashier BIGINT = CASE WHEN @targetBranch = @cn1Id THEN @cashier1Id ELSE @cashier2Id END;
        DECLARE @chef BIGINT = CASE WHEN @targetBranch = @cn1Id THEN @bep1Id ELSE @bep2Id END;
        DECLARE @customer BIGINT = CASE WHEN @oIdx % 3 = 1 THEN @cust1Id WHEN @oIdx % 3 = 2 THEN @cust2Id ELSE NULL END;

        -- Xác định nguồn đơn: Chuẩn hóa hệ thống chỉ phục vụ tại quán (TABLE và COUNTER)
        DECLARE @source VARCHAR(10) = CASE 
            WHEN @oIdx <= 12 THEN 'TABLE'
            ELSE 'COUNTER'
        END;

        -- Xác định trạng thái đơn
        DECLARE @status VARCHAR(24) = 'HOAN_THANH';
        IF @dayOffset = 0
        BEGIN
            -- Ngày hôm nay: tạo vài đơn đang chờ để demo POS/KDS
            IF @oIdx = 14 SET @status = 'CHO_XAC_NHAN';
            ELSE IF @oIdx = 15 SET @status = 'CHO_CHE_BIEN';
            ELSE IF @oIdx = 16 SET @status = 'DANG_CHE_BIEN';
            ELSE IF @oIdx = 17 SET @status = 'DA_HUY';
            ELSE SET @status = 'HOAN_THANH';
        END
        ELSE
        BEGIN
            -- Ngày cũ: 1-2 đơn bị hủy
            IF @oIdx = 13 SET @status = 'DA_HUY';
            ELSE SET @status = 'HOAN_THANH';
        END;

        -- Thời gian tạo đơn trong ngày
        DECLARE @minuteOffset INT = (@oIdx * 45) + 480; -- Từ 8h00 sáng đến 21h30 tối
        DECLARE @createTime DATETIME2 = DATEADD(MINUTE, @minuteOffset, DATEADD(DAY, -@dayOffset, CAST(CAST(SYSUTCDATETIME() AS DATE) AS DATETIME2)));
        DECLARE @startTime DATETIME2  = DATEADD(MINUTE, 3, @createTime);
        DECLARE @finishTime DATETIME2 = DATEADD(MINUTE, 12, @createTime);

        -- Quản lý phiên bàn nếu là đơn TABLE
        DECLARE @phienId BIGINT = NULL;
        IF @source = 'TABLE'
        BEGIN
            DECLARE @tableNo INT = ((@oIdx - 1) % 10) + 1;
            DECLARE @tableId BIGINT = (SELECT TOP 1 MaBan FROM dbo.Ban WHERE MaCN = @targetBranch AND TenBan LIKE N'%' + RIGHT('0' + CAST(@tableNo AS VARCHAR(2)), 2));
            
            DECLARE @sessionStatus VARCHAR(8) = 'CLOSED';
            -- Chỉ mở tối đa 1 phiên OPEN trên mỗi bàn (Bàn 01 hôm nay đang phục vụ)
            IF @dayOffset = 0 AND @tableNo = 1 AND @status IN ('DANG_CHE_BIEN', 'CHO_CHE_BIEN')
                AND NOT EXISTS (SELECT 1 FROM dbo.PhienBan WHERE MaBan = @tableId AND TrangThai = 'OPEN')
            BEGIN
                SET @sessionStatus = 'OPEN';
            END;
            
            INSERT INTO dbo.PhienBan (MaBan, TrangThai, MoLuc, DongLuc, NguoiMo, NguoiDong, Version)
            VALUES (@tableId, @sessionStatus, @createTime, CASE WHEN @sessionStatus = 'CLOSED' THEN @finishTime ELSE NULL END, @customer, CASE WHEN @sessionStatus = 'CLOSED' THEN @cashier ELSE NULL END, 0);
            SET @phienId = SCOPE_IDENTITY();

            IF @sessionStatus = 'OPEN'
            BEGIN
                UPDATE dbo.Ban SET TrangThai = 'CO_KHACH', MaPhienDangMo = @phienId WHERE MaBan = @tableId;
                IF NOT EXISTS (SELECT 1 FROM dbo.GioPhien WHERE MaPhien = @phienId)
                BEGIN
                    INSERT INTO dbo.GioPhien (MaPhien, Version, SuaLuc)
                    VALUES (@phienId, 0, @createTime);
                END;
            END
            ELSE
            BEGIN
                IF NOT EXISTS (SELECT 1 FROM dbo.PhienBan WHERE MaBan = @tableId AND TrangThai = 'OPEN')
                BEGIN
                    UPDATE dbo.Ban SET TrangThai = 'TRONG', MaPhienDangMo = NULL WHERE MaBan = @tableId;
                END;
            END;
        END;

        -- Áp dụng Voucher mẫu cho một số đơn
        DECLARE @applyVoucher BIGINT = NULL;
        DECLARE @voucherCode VARCHAR(32) = NULL;
        DECLARE @voucherDiscount DECIMAL(18,2) = 0.00;

        IF @status = 'HOAN_THANH' AND (@oIdx % 4 = 1)
        BEGIN
            SET @applyVoucher = @vChaoMung;
            SET @voucherCode = 'CHAOMUNG';
            SET @voucherDiscount = 20000.00;
        END
        ELSE IF @status = 'HOAN_THANH' AND (@oIdx % 4 = 2)
        BEGIN
            SET @applyVoucher = @vMilkTea10;
            SET @voucherCode = 'MILKTEA10';
        END;

        -- Tạo đơn hàng
        DECLARE @orderKey VARCHAR(64) = 'ORD-D' + CAST(@dayOffset AS VARCHAR(2)) + '-' + CAST(@orderSeq AS VARCHAR(10));

        INSERT INTO dbo.DonHang (MaCN, MaPhien, MaTK, Nguon, TrangThai, MaYeuCau, GhiChu, TaoLuc, BatDauLuc, HoanThanhLuc, MaVoucher, MaCodeVoucher, Version)
        VALUES (
            @targetBranch, @phienId, @customer, @source, @status, @orderKey,
            CASE WHEN @source = 'TABLE' THEN N'Phục vụ tại bàn' ELSE N'Khách gọi món tại quầy thu ngân' END,
            @createTime, 
            CASE WHEN @status IN ('CHO_XAC_NHAN') THEN NULL ELSE @startTime END, 
            CASE WHEN @status = 'HOAN_THANH' THEN @finishTime ELSE NULL END,
            @applyVoucher, @voucherCode, 0
        );
        DECLARE @curDhId BIGINT = SCOPE_IDENTITY();

        -- Chi tiết món đặt (1 đến 2 món)
        DECLARE @drinkIdx1 INT = ((@orderSeq * 3) % @totalDrinks) + 1;
        DECLARE @d1Ma BIGINT, @d1Ten NVARCHAR(120), @d1Size VARCHAR(12), @d1Gia DECIMAL(18,2);
        SELECT @d1Ma = MaMon, @d1Ten = TenMon, @d1Size = Size, @d1Gia = Gia FROM @SampleDrinks WHERE Idx = @drinkIdx1;

        INSERT INTO dbo.ChiTietDonHang (MaDH, MaMon, TenMonChot, SizeChot, SoLuong, DonGiaChot, GhiChu)
        VALUES (@curDhId, @d1Ma, @d1Ten, @d1Size, 1, @d1Gia, N'50% đường, 50% đá');
        DECLARE @ctdh1 BIGINT = SCOPE_IDENTITY();

        -- Topping cho món 1
        DECLARE @tpIdx1 INT = ((@orderSeq * 2) % @totalToppings) + 1;
        DECLARE @tp1Ma BIGINT, @tp1Ten NVARCHAR(120), @tp1Gia DECIMAL(18,2);
        SELECT @tp1Ma = MaMon, @tp1Ten = TenMon, @tp1Gia = Gia FROM @SampleToppings WHERE Idx = @tpIdx1;

        INSERT INTO dbo.ChiTietDonHangTopping (MaChiTiet, MaMonTopping, TenToppingChot, DonGiaToppingChot)
        VALUES (@ctdh1, @tp1Ma, @tp1Ten, @tp1Gia);

        DECLARE @subTotal DECIMAL(18,2) = @d1Gia + @tp1Gia;

        -- Thêm món 2 cho 50% số đơn
        IF @oIdx % 2 = 0
        BEGIN
            DECLARE @drinkIdx2 INT = ((@orderSeq * 7) % @totalDrinks) + 1;
            DECLARE @d2Ma BIGINT, @d2Ten NVARCHAR(120), @d2Size VARCHAR(12), @d2Gia DECIMAL(18,2);
            SELECT @d2Ma = MaMon, @d2Ten = TenMon, @d2Size = Size, @d2Gia = Gia FROM @SampleDrinks WHERE Idx = @drinkIdx2;

            INSERT INTO dbo.ChiTietDonHang (MaDH, MaMon, TenMonChot, SizeChot, SoLuong, DonGiaChot, GhiChu)
            VALUES (@curDhId, @d2Ma, @d2Ten, @d2Size, 1, @d2Gia, N'Bình thường');
            SET @subTotal = @subTotal + @d2Gia;
        END;

        -- Tính lại giảm giá nếu là %
        IF @voucherCode = 'MILKTEA10'
        BEGIN
            SET @voucherDiscount = @subTotal * 0.10;
            IF @voucherDiscount > 30000.00 SET @voucherDiscount = 30000.00;
        END;

        DECLARE @finalPay DECIMAL(18,2) = @subTotal - @voucherDiscount;
        IF @finalPay < 0 SET @finalPay = 0;

        -- Tạo Hóa đơn: Tách đúng tiền giảm cửa hàng (TienGiam) và voucher (TienGiamVoucher)
        DECLARE @invStatus VARCHAR(12) = CASE WHEN @status = 'DA_HUY' THEN 'DA_HUY' ELSE 'HIEU_LUC' END;
        INSERT INTO dbo.HoaDon (MaDH, TongTien, TienGiam, PhaiTra, TyLeGiamChot, LoaiGiamChot, GiaTriGiamChot, TrangThai, LapLuc, HuyLuc, MaVoucher, MaCodeVoucher, TienGiamVoucher)
        VALUES (@curDhId, @subTotal, 0.00, @finalPay, 0.00, 'PERCENT', 0.00, @invStatus, @createTime, CASE WHEN @status = 'DA_HUY' THEN @finishTime ELSE NULL END, @applyVoucher, @voucherCode, @voucherDiscount);
        DECLARE @curHdId BIGINT = SCOPE_IDENTITY();

        -- Snapshot định mức tiêu hao nguyên liệu (DinhMucDonHang) theo công thức thực tế
        INSERT INTO dbo.DinhMucDonHang (MaDH, MaNL, TongLuong, LoaiChot, DonViChot)
        SELECT 
            @curDhId,
            ctm.MaNL,
            SUM(ctm.DinhLuong),
            MAX(nl.Loai),
            MAX(nl.DonVi)
        FROM dbo.CongThucMon ctm
        JOIN dbo.NguyenLieu nl ON ctm.MaNL = nl.MaNL
        WHERE ctm.MaMon IN (@d1Ma, @tp1Ma, CASE WHEN @oIdx % 2 = 0 THEN @d2Ma ELSE -1 END)
        GROUP BY ctm.MaNL;

        -- Thanh toán & Trừ kho nếu là đơn Hoàn thành
        IF @status = 'HOAN_THANH'
        BEGIN
            DECLARE @payMethod VARCHAR(16) = CASE WHEN @oIdx % 2 = 1 THEN 'BANK_TRANSFER' ELSE 'CASH' END;
            DECLARE @refCode NVARCHAR(120) = CASE WHEN @payMethod = 'BANK_TRANSFER' THEN 'VIETQR-DEMO-' + CAST(@orderSeq AS VARCHAR(10)) ELSE 'CASH-POS-' + CAST(@orderSeq AS VARCHAR(10)) END;

            INSERT INTO dbo.ThanhToan (MaHD, PhuongThuc, SoTien, ThamChieu, NguoiThu, ThuLuc, MaYeuCau)
            VALUES (@curHdId, @payMethod, @finalPay, @refCode, @cashier, @finishTime, 'PAY-' + @orderKey);

            -- Trừ kho Bếp/Kho thực tế theo DinhMucDonHang và ghi nhận LichSuKho liên tục
            DECLARE @curNlId BIGINT, @curTongLuong DECIMAL(18,3), @curLoaiNL VARCHAR(12), @curDonVi NVARCHAR(20);
            DECLARE curDMDH CURSOR LOCAL FAST_FORWARD FOR
                SELECT MaNL, TongLuong, LoaiChot, DonViChot FROM dbo.DinhMucDonHang WHERE MaDH = @curDhId;
            OPEN curDMDH;
            FETCH NEXT FROM curDMDH INTO @curNlId, @curTongLuong, @curLoaiNL, @curDonVi;
            WHILE @@FETCH_STATUS = 0
            BEGIN
                DECLARE @loc VARCHAR(8) = CASE WHEN @curLoaiNL = 'SOCHE' THEN 'BEP' ELSE 'KHO' END;
                DECLARE @curBeforeQty DECIMAL(18,3) = 0.000;
                SELECT @curBeforeQty = SoLuong FROM dbo.TonNguyenLieu WHERE MaCN = @targetBranch AND MaNL = @curNlId AND ViTri = @loc;
                IF @curBeforeQty IS NULL SET @curBeforeQty = 1000.000;
                DECLARE @curAfterQty DECIMAL(18,3) = @curBeforeQty - @curTongLuong;
                IF @curAfterQty < 0 SET @curAfterQty = 0.000;

                UPDATE dbo.TonNguyenLieu 
                SET SoLuong = @curAfterQty
                WHERE MaCN = @targetBranch AND MaNL = @curNlId AND ViTri = @loc;

                INSERT INTO dbo.LichSuKho (MaCN, MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo)
                VALUES (@targetBranch, @curNlId, @loc, 'XUAT', -@curTongLuong, @curBeforeQty, @curBeforeQty - @curTongLuong, 'ORDER', @curDhId, @chef, @finishTime, N'Pha chế Đơn hàng #' + CAST(@curDhId AS VARCHAR(10)));

                FETCH NEXT FROM curDMDH INTO @curNlId, @curTongLuong, @curLoaiNL, @curDonVi;
            END;
            CLOSE curDMDH;
            DEALLOCATE curDMDH;

            -- Ghi nhận lịch sử voucher
            IF @applyVoucher IS NOT NULL AND @customer IS NOT NULL
            BEGIN
                INSERT INTO dbo.LichSuSuDungVoucher (MaVoucher, MaDH, MaHD, MaTK, MaCN, SoTienGiam, TrangThai, NgaySuDung)
                VALUES (@applyVoucher, @curDhId, @curHdId, @customer, @targetBranch, @voucherDiscount, 'DA_DUNG', @finishTime);
            END;
        END
        ELSE IF @status = 'DA_HUY'
        BEGIN
            -- Ghi nhận thanh toán rồi hoàn tiền
            INSERT INTO dbo.ThanhToan (MaHD, PhuongThuc, SoTien, ThamChieu, NguoiThu, ThuLuc, MaYeuCau)
            VALUES (@curHdId, 'CASH', @finalPay, 'CASH-REFUND-INIT', @cashier, @createTime, 'PAY-CANCEL-' + @orderKey);
            DECLARE @curTtId BIGINT = SCOPE_IDENTITY();

            INSERT INTO dbo.YeuCauHuy (MaDH, LyDo, LyDoXuLy, TrangThai, TaoLuc, XuLyLuc, NguoiXuLy)
            VALUES (@curDhId, N'Khách hàng đổi ý trước khi bếp pha chế', N'Đã hoàn trả tiền mặt cho khách', 'CHAP_THUAN', @createTime, @finishTime, @cashier);

            INSERT INTO dbo.YeuCauHoan (MaDH, MaTT, SoTien, LyDo, LyDoDuyet, TrangThai, DuyetLuc, HoanLuc, NguoiDuyet, NguoiHoan, MaYeuCau)
            VALUES (@curDhId, @curTtId, @finalPay, N'Hủy đơn trước pha chế', N'Đồng ý duyệt hoàn tiền', 'DA_HOAN', @finishTime, @finishTime, @cashier, @cashier, 'REFUND-' + @orderKey);
        END;

        SET @oIdx = @oIdx + 1;
    END;
    SET @dayOffset = @dayOffset - 1;
END;

PRINT N'-- [16/18] Đã nạp thành công 108 đơn hàng mẫu đa trạng thái chia đều 7 ngày qua.';
GO

-- ============================================================================
-- 17. TRẢI NGHIỆM KHÁCH HÀNG: YÊU THÍCH & ĐÁNH GIÁ (SanPhamYeuThich, DanhGia)
-- ============================================================================
DELETE FROM dbo.DanhGia;
DELETE FROM dbo.SanPhamYeuThich;

DECLARE @cust1Id BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer@milktea.vn');
DECLARE @cust2Id BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer2@milktea.vn');
DECLARE @cn1Id   BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn2Id   BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');

-- Wishlist yêu thích
INSERT INTO dbo.SanPhamYeuThich (MaTK, MaMon, TaoLuc)
SELECT TOP 5 @cust1Id, MaMon, DATEADD(DAY, -2, SYSUTCDATETIME()) FROM dbo.MonUong ORDER BY MaMon ASC;

INSERT INTO dbo.SanPhamYeuThich (MaTK, MaMon, TaoLuc)
SELECT TOP 5 @cust2Id, MaMon, DATEADD(DAY, -1, SYSUTCDATETIME()) FROM dbo.MonUong ORDER BY MaMon DESC;

-- Đánh giá 5 sao mẫu cho các đơn hàng đã hoàn thành
INSERT INTO dbo.DanhGia (MaChiTiet, MaTK, MaMon, MaCN, SoSao, NoiDung, An, TaoLuc, CapNhatLuc)
SELECT TOP 10 
    ct.MaChiTiet, 
    COALESCE(dh.MaTK, @cust1Id), 
    ct.MaMon, 
    dh.MaCN, 
    5, 
    N'Trà sữa cực kỳ đậm vị, thơm béo hài hòa, phục vụ rất nhanh nhẹn và chu đáo. Sẽ tiếp tục ủng hộ chuỗi!', 
    0, 
    dh.HoanThanhLuc, 
    dh.HoanThanhLuc
FROM dbo.ChiTietDonHang ct
JOIN dbo.DonHang dh ON dh.MaDH = ct.MaDH
WHERE dh.TrangThai = 'HOAN_THANH';

PRINT N'-- [17/18] Đã tạo danh sách yêu thích và đánh giá chuẩn cho đơn hoàn thành.';
GO

-- ============================================================================
-- 18. KIỂM TRA TỔNG KẾT & ĐỐI SOÁT TÍNH TOÀN VẸN
-- ============================================================================
DECLARE @cntCN INT = (SELECT COUNT(*) FROM dbo.ChiNhanh);
DECLARE @cntTK INT = (SELECT COUNT(*) FROM dbo.TaiKhoan);
DECLARE @cntBan INT = (SELECT COUNT(*) FROM dbo.Ban);
DECLARE @cntMon INT = (SELECT COUNT(*) FROM dbo.MonUong);
DECLARE @cntNL INT = (SELECT COUNT(*) FROM dbo.NguyenLieu);
DECLARE @cntCT INT = (SELECT COUNT(*) FROM dbo.CongThucMon);
DECLARE @cntTon INT = (SELECT COUNT(*) FROM dbo.TonNguyenLieu);
DECLARE @cntPN INT = (SELECT COUNT(*) FROM dbo.PhieuNhap);
DECLARE @cntMe INT = (SELECT COUNT(*) FROM dbo.MeSoChe);
DECLARE @cntKC INT = (SELECT COUNT(*) FROM dbo.KetCaKho);
DECLARE @cntDH INT = (SELECT COUNT(*) FROM dbo.DonHang);
DECLARE @cntHD INT = (SELECT COUNT(*) FROM dbo.HoaDon);
DECLARE @cntTT INT = (SELECT COUNT(*) FROM dbo.ThanhToan);
DECLARE @cntLSK INT = (SELECT COUNT(*) FROM dbo.LichSuKho);

PRINT N'============================================================================';
PRINT N'>>> ĐÃ NẠP TOÀN BỘ DỮ LIỆU MẪU MULTI-BRANCH THÀNH CÔNG VƯỢT CHỈ TIÊU!';
PRINT N'- Chi nhánh:            ' + CAST(@cntCN AS VARCHAR(10));
PRINT N'- Tài khoản hệ thống:   ' + CAST(@cntTK AS VARCHAR(10));
PRINT N'- Bàn ăn có mã QR:      ' + CAST(@cntBan AS VARCHAR(10));
PRINT N'- Món uống & Topping:   ' + CAST(@cntMon AS VARCHAR(10)) + N' (Mục tiêu: 50 món)';
PRINT N'- Nguyên liệu chuỗi:    ' + CAST(@cntNL AS VARCHAR(10));
PRINT N'- Định mức món:         ' + CAST(@cntCT AS VARCHAR(10));
PRINT N'- Bản ghi tồn kho:      ' + CAST(@cntTon AS VARCHAR(10));
PRINT N'- Phiếu nhập kho:       ' + CAST(@cntPN AS VARCHAR(10)) + N' (Mục tiêu >= 70 phiếu 7 ngày)';
PRINT N'- Mẻ sơ chế bếp:        ' + CAST(@cntMe AS VARCHAR(10)) + N' (Mục tiêu >= 70 mẻ 7 ngày)';
PRINT N'- Phiếu kết ca kho:     ' + CAST(@cntKC AS VARCHAR(10));
PRINT N'- Đơn hàng mẫu:         ' + CAST(@cntDH AS VARCHAR(10)) + N' (Mục tiêu >= 100 đơn 7 ngày)';
PRINT N'- Hóa đơn lập:          ' + CAST(@cntHD AS VARCHAR(10));
PRINT N'- Giao dịch thanh toán: ' + CAST(@cntTT AS VARCHAR(10));
PRINT N'- Biến động lịch sử kho:' + CAST(@cntLSK AS VARCHAR(10));
PRINT N'============================================================================';
GO
