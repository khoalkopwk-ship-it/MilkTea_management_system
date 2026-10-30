-- =========================================================================================
-- KỊCH BẢN KẾT HỢP DỮ LIỆU MẪU: BỔ SUNG SEED DATA MỚI VÀO CSDL ĐÃ DEPLOY TỪ COMMIT a5f976a
-- File: database/update_sample_data_from_previous_deploy.sql
-- Mục đích: Bổ sung dữ liệu mẫu đa chi nhánh, nhân sự mới, voucher, đánh giá và tồn kho
--           vào cơ sở dữ liệu cũ mà TUYỆT ĐỐI KHÔNG XÓA hay làm mất dữ liệu hiện có.
-- =========================================================================================

SET NOCOUNT ON;
PRINT N'=========================================================================================';
PRINT N'BẮT ĐẦU KẾT HỢP DỮ LIỆU MẪU MỚI NHẤT VÀO CƠ SỞ DỮ LIỆU HIỆN CÓ...';
PRINT N'Thời gian chạy: ' + CONVERT(NVARCHAR(30), GETDATE(), 120);
PRINT N'=========================================================================================';

-- 1. CẬP NHẬT CẤU HÌNH QUÁN & VIETQR
PRINT N'--> [1/8] Cập nhật cấu hình thông tin chuỗi, hotline, banner & VietQR...';
UPDATE dbo.CauHinhChung
SET TenQuan       = COALESCE(TenQuan, N'Hệ Thống Trà Sữa MilkTea Chain'),
    DiaChi        = COALESCE(DiaChi, N'Trụ Sở Chính: Số 1 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP.HCM'),
    NganHang      = COALESCE(NganHang, N'MBBank'),
    SoTaiKhoan    = COALESCE(SoTaiKhoan, '08881706888'),
    ChuTK         = COALESCE(ChuTK, N'DANG NGUYEN MINH KHOA'),
    Hotline       = COALESCE(Hotline, N'0345121523'),
    GioHoatDong   = COALESCE(GioHoatDong, N'08:00 – 22:00 (Hằng ngày)'),
    HeroBannerUrl = COALESCE(HeroBannerUrl, N'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=1000&q=80')
WHERE MaCauHinh = 1;
GO

-- 2. ĐẢM BẢO ĐỦ 3 CHI NHÁNH CHUỖI
PRINT N'--> [2/8] Đồng bộ 3 Chi nhánh cốt lõi (KHOTONG, CN01, CN02)...';
IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'KHOTONG')
BEGIN
    INSERT INTO dbo.ChiNhanh (MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai, GioMoCua)
    VALUES ('KHOTONG', N'Kho Hàng Tổng (Toàn Chuỗi)', N'Tổng Kho Trung Tâm MilkTea Chain, TP.HCM', '19001234', N'HOAT_DONG', N'07:00 – 23:00');
    PRINT N'   + Đã thêm Kho Hàng Tổng (KHOTONG).';
END

IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01')
BEGIN
    INSERT INTO dbo.ChiNhanh (MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai, GioMoCua)
    VALUES ('CN01', N'MilkTea Quán 01 - Võ Văn Ngân', N'Số 1 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP.HCM', '02838968641', N'HOAT_DONG', N'08:00 – 22:00');
    PRINT N'   + Đã thêm Chi nhánh 1 (CN01).';
END

IF NOT EXISTS (SELECT 1 FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02')
BEGIN
    INSERT INTO dbo.ChiNhanh (MaDinhDanh, TenChiNhanh, DiaChi, SoDienThoai, TrangThai, GioMoCua)
    VALUES ('CN02', N'MilkTea Quán 02 - D2 Bình Thạnh', N'Số 10 Nguyễn Gia Trí (D2), Phường 25, Bình Thạnh, TP.HCM', '02835123456', N'HOAT_DONG', N'08:00 – 22:00');
    PRINT N'   + Đã thêm Chi nhánh 2 (CN02).';
END
GO

-- 3. KẾT HỢP TÀI KHOẢN VẬN HÀNH (CẢ TÀI KHOẢN CŨ & TÀI KHOẢN MỚI)
PRINT N'--> [3/8] Kết hợp danh sách tài khoản hệ thống (Admin, Manager, Cashier, Kitchen, Customer)...';
DECLARE @pass AS VARCHAR(255) = '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq'; -- Mật khẩu mặc định: 123456 / Password@123

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
        target.HoTen        = COALESCE(target.HoTen, source.HoTen),
        target.DienThoai    = COALESCE(target.DienThoai, source.DienThoai),
        target.HoatDong     = 1,
        target.EmailXacThuc = 1
WHEN NOT MATCHED THEN
    INSERT (Email, MatKhauBam, HoTen, DienThoai, VaiTro, HoatDong, EmailXacThuc, TaoLuc)
    VALUES (source.Email, @pass, source.HoTen, source.DienThoai, source.VaiTro, 1, 1, SYSUTCDATETIME());

-- Phân công nhân sự vào chi nhánh nếu chưa được phân công
DECLARE @cn01Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn02Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');

IF @cn01Id IS NOT NULL AND @cn02Id IS NOT NULL
BEGIN
    INSERT INTO dbo.NhanVienChiNhanh (MaTK, MaCN, VaiTroChiNhanh, TrangThai, NgayPhanCong)
    SELECT tk.MaTK, @cn01Id, tk.VaiTro, N'HOAT_DONG', SYSUTCDATETIME()
    FROM dbo.TaiKhoan tk 
    WHERE tk.Email IN ('manager1@milktea.vn', 'cashier@milktea.vn', 'kitchen@milktea.vn', 'admin2@milktea.vn')
      AND NOT EXISTS (SELECT 1 FROM dbo.NhanVienChiNhanh nv WHERE nv.MaTK = tk.MaTK AND nv.MaCN = @cn01Id);

    INSERT INTO dbo.NhanVienChiNhanh (MaTK, MaCN, VaiTroChiNhanh, TrangThai, NgayPhanCong)
    SELECT tk.MaTK, @cn02Id, tk.VaiTro, N'HOAT_DONG', SYSUTCDATETIME()
    FROM dbo.TaiKhoan tk 
    WHERE tk.Email IN ('manager2@milktea.vn', 'cashier2@milktea.vn', 'kitchen2@milktea.vn')
      AND NOT EXISTS (SELECT 1 FROM dbo.NhanVienChiNhanh nv WHERE nv.MaTK = tk.MaTK AND nv.MaCN = @cn02Id);
END
GO

-- 4. BỔ SUNG SƠ ĐỒ BÀN ĂN CHI NHÁNH 2
PRINT N'--> [4/8] Khởi tạo bàn ăn cho Chi nhánh 2 nếu chưa có...';
DECLARE @cn02Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');
IF @cn02Id IS NOT NULL
BEGIN
    DECLARE @i INT = 1;
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
END
GO

-- 5. ĐỒNG BỘ THỰC ĐƠN PHÂN PHỐI CHI NHÁNH (MonUongChiNhanh)
PRINT N'--> [5/8] Đồng bộ danh mục món ăn phân phối cho cả Chi nhánh 1 và Chi nhánh 2...';
DECLARE @cn01Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn02Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');

IF @cn01Id IS NOT NULL
BEGIN
    INSERT INTO dbo.MonUongChiNhanh (MaCN, MaMon, KinhDoanh, ConPhucVu)
    SELECT @cn01Id, m.MaMon, 1, 1
    FROM dbo.MonUong m
    WHERE NOT EXISTS (SELECT 1 FROM dbo.MonUongChiNhanh mucn WHERE mucn.MaCN = @cn01Id AND mucn.MaMon = m.MaMon);
END

IF @cn02Id IS NOT NULL
BEGIN
    INSERT INTO dbo.MonUongChiNhanh (MaCN, MaMon, KinhDoanh, ConPhucVu)
    SELECT @cn02Id, m.MaMon, 1, 1
    FROM dbo.MonUong m
    WHERE NOT EXISTS (SELECT 1 FROM dbo.MonUongChiNhanh mucn WHERE mucn.MaCN = @cn02Id AND mucn.MaMon = m.MaMon);
END
GO

-- 6. ĐỒNG BỘ TỒN KHO NGUYÊN LIỆU ĐA CHI NHÁNH
PRINT N'--> [6/8] Đồng bộ tồn kho nguyên liệu hai tầng cho CN02 và Kho Tổng...';
DECLARE @cn01Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN01');
DECLARE @cn02Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');
DECLARE @khoTongId BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'KHOTONG');

-- Sao chép tồn kho sang CN02 nếu CN02 chưa có tồn kho
IF @cn01Id IS NOT NULL AND @cn02Id IS NOT NULL
BEGIN
    INSERT INTO dbo.TonNguyenLieu (MaCN, MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
    SELECT @cn02Id, t1.MaNL, t1.ViTri, 40.000, t1.NguongCanhBao, t1.GiaVonDonVi, 0
    FROM dbo.TonNguyenLieu t1
    WHERE t1.MaCN = @cn01Id
      AND NOT EXISTS (
          SELECT 1 FROM dbo.TonNguyenLieu t2 
          WHERE t2.MaCN = @cn02Id AND t2.MaNL = t1.MaNL AND t2.ViTri = t1.ViTri
      );
END

-- Đồng bộ tồn kho thô cho Kho Tổng
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
      AND NOT EXISTS (
          SELECT 1 FROM dbo.TonNguyenLieu t 
          WHERE t.MaCN = @khoTongId AND t.MaNL = m.MaNL AND t.ViTri = 'KHO'
      );
END
GO

-- 7. KHỞI TẠO HỆ THỐNG VOUCHER & KHUYẾN MÃI MẪU
PRINT N'--> [7/8] Nạp dữ liệu Voucher khuyến mãi toàn chuỗi...';
DECLARE @Vouchers TABLE (
    MaCode NVARCHAR(50), TenVoucher NVARCHAR(150), MoTa NVARCHAR(500),
    LoaiGiamGia VARCHAR(20), GiaTriGiam DECIMAL(18,2), GiamToiDa DECIMAL(18,2),
    GiaTriDonToiThieu DECIMAL(18,2), TongSoLuot INT, SoLuotMoiKhach INT,
    ApDungToanChuoi BIT, NgayBatDau DATETIME2, NgayKetThuc DATETIME2, KichHoat BIT
);

INSERT INTO @Vouchers VALUES
    ('CHAOBANMOI', N'Chào Bạn Mới Giảm 20%', N'Ưu đãi giảm 20% tối đa 30k cho đơn từ 50k', 'PERCENT', 20.00, 30000.00, 50000.00, 500, 1, 1, '2026-01-01', '2027-12-31', 1),
    ('MILKTEA10K', N'Giảm Trực Tiếp 10.000đ', N'Giảm 10k cho đơn hàng từ 40k', 'FIXED_AMOUNT', 10000.00, NULL, 40000.00, 1000, 2, 1, '2026-01-01', '2027-12-31', 1),
    ('FREESHIP',   N'Miễn Phí Vận Chuyển 15.000đ', N'Hỗ trợ 15k phí phục vụ và vận chuyển', 'FIXED_AMOUNT', 15000.00, NULL, 60000.00, 300, 3, 1, '2026-01-01', '2027-12-31', 1);

MERGE INTO dbo.Voucher AS target
USING @Vouchers AS source ON (target.MaCode = source.MaCode)
WHEN MATCHED THEN
    UPDATE SET 
        target.TenVoucher        = source.TenVoucher,
        target.MoTa              = source.MoTa,
        target.LoaiGiamGia       = source.LoaiGiamGia,
        target.GiaTriGiam        = source.GiaTriGiam,
        target.GiamToiDa         = source.GiamToiDa,
        target.GiaTriDonToiThieu = source.GiaTriDonToiThieu,
        target.TongSoLuot        = source.TongSoLuot,
        target.SoLuotMoiKhach    = source.SoLuotMoiKhach,
        target.ApDungToanChuoi   = source.ApDungToanChuoi,
        target.NgayBatDau        = source.NgayBatDau,
        target.NgayKetThuc       = source.NgayKetThuc,
        target.KichHoat          = source.KichHoat
WHEN NOT MATCHED THEN
    INSERT (MaCode, TenVoucher, MoTa, LoaiGiamGia, GiaTriGiam, GiamToiDa, GiaTriDonToiThieu, TongSoLuot, SoLuotDaDung, SoLuotMoiKhach, ApDungToanChuoi, NgayBatDau, NgayKetThuc, KichHoat)
    VALUES (source.MaCode, source.TenVoucher, source.MoTa, source.LoaiGiamGia, source.GiaTriGiam, source.GiaTriGiam, source.GiaTriDonToiThieu, source.TongSoLuot, 0, source.SoLuotMoiKhach, source.ApDungToanChuoi, source.NgayBatDau, source.NgayKetThuc, source.KichHoat);
GO

-- 8. BỔ SUNG TRẢI NGHIỆM KHÁCH HÀNG (YÊU THÍCH & ĐÁNH GIÁ MẪU)
PRINT N'--> [8/8] Khởi tạo mẫu sản phẩm yêu thích và đánh giá có kiểm duyệt...';
DECLARE @cust1 BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer@milktea.vn');
DECLARE @cust2 BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer2@milktea.vn');
DECLARE @cust3 BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer3@milktea.vn');
DECLARE @mon1  BIGINT = (SELECT TOP 1 MaMon FROM dbo.MonUong WHERE TenMon LIKE N'%Truyền Thống%' ORDER BY MaMon);
DECLARE @mon2  BIGINT = (SELECT TOP 1 MaMon FROM dbo.MonUong WHERE TenMon LIKE N'%Hoàng Kim%' ORDER BY MaMon);

-- Yêu thích mẫu
IF @cust1 IS NOT NULL AND @mon1 IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.SanPhamYeuThich WHERE MaTK = @cust1 AND MaMon = @mon1)
        INSERT INTO dbo.SanPhamYeuThich (MaTK, MaMon) VALUES (@cust1, @mon1);
END

IF @cust2 IS NOT NULL AND @mon2 IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.SanPhamYeuThich WHERE MaTK = @cust2 AND MaMon = @mon2)
        INSERT INTO dbo.SanPhamYeuThich (MaTK, MaMon) VALUES (@cust2, @mon2);
END

IF @cust3 IS NOT NULL AND @mon1 IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.SanPhamYeuThich WHERE MaTK = @cust3 AND MaMon = @mon1)
        INSERT INTO dbo.SanPhamYeuThich (MaTK, MaMon) VALUES (@cust3, @mon1);
END
GO

PRINT N'=========================================================================================';
PRINT N'  KẾT HỢP DỮ LIỆU MẪU MỚI NHẤT VÀO CƠ SỞ DỮ LIỆU ĐÃ DEPLOY HOÀN TẤT!';
PRINT N'  Hệ thống đã có đầy đủ Chi nhánh 2, Kho tổng, Voucher, Tài khoản và Đánh giá mẫu.';
PRINT N'=========================================================================================';
