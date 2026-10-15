-- ============================================================================
-- MilkTea Management System - Bộ Dữ Liệu Demo Toàn Diện & Đầy Đủ (Seed Demo Full)
-- Phiên bản: 2026-10-12
-- Tiêu chuẩn nghiệp vụ:
--   1. Mật khẩu chuẩn BCrypt cho tất cả tài khoản: Password@123
--   2. Cấu hình quán có snapshot giảm giá theo % hoặc tiền cố định
--   3. >= 10 Danh mục sản phẩm
--   4. 30 Món nước uống & 10 Loại Topping
--   5. >= 10 Bàn ăn & Phiên bàn (gồm cả Bàn Trống và Bàn Có Khách gắn giỏ phiên)
--   6. >= 10 Nguyên liệu THO & >= 10 Nguyên liệu SOCHE
--   7. Tồn kho 2 tầng (KHO & BEP) với đầy đủ 4 mức cảnh báo:
--      HET_TON, TON_THAP, BINH_THUONG, CHUA_CAU_HINH
--   8. >= 10 Công thức định mức món uống & topping
--   9. >= 10 Công thức sơ chế & >= 10 Mẻ sơ chế (ghi nhận chi phí, tỷ lệ chênh lệch)
--  10. >= 10 Phiếu nhập kho (thiết lập giá vốn) & >= 10 Phiếu xuất kho
--  11. >= 10 Đơn hàng phủ đủ 5 trạng thái (CHO_XAC_NHAN, CHO_CHE_BIEN, DANG_CHE_BIEN, HOAN_THANH, DA_HUY)
--  12. >= 10 Hóa đơn có snapshot loại giảm (% và VNĐ) và tiền giảm
--  13. >= 10 Bản ghi thanh toán (tiền mặt & chuyển khoản kèm thông báo)
--  14. >= 10 Yêu cầu hủy đơn & >= 10 Yêu cầu hoàn tiền (CHO_DUYET, DA_DUYET, DA_HOAN, TU_CHOI)
--  15. >= 10 Bản ghi kết ca kiểm kê kho (KHO & BEP)
--  16. >= 10 Bản ghi lịch sử biến động kho & nhật ký nghiệp vụ
-- ============================================================================
USE milktea;


GO
SET NOCOUNT ON;

PRINT N'>>> Bắt đầu nạp bộ dữ liệu demo MilkTea Full...';


GO
-- ============================================================================
-- 1. CẤU HÌNH QUÁN (CauHinhChung)
-- ============================================================================
IF NOT EXISTS (SELECT 1
               FROM   dbo.CauHinhChung
               WHERE  MaCauHinh = 1)
    BEGIN
        INSERT  INTO dbo.CauHinhChung (
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
        VALUES                        (1, N'MilkTea Cafe', N'Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh', N'MBBank - Chi nhánh Long Khánh', '08881706888', N'DANG NGUYEN MINH KHOA', 10.00, 'PERCENT', 10.00, SYSUTCDATETIME());
    END
ELSE
    BEGIN
        UPDATE dbo.CauHinhChung
        SET    TenQuan    = N'MilkTea Cafe',
               DiaChi     = N'Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh',
               NganHang   = N'MBBank - Chi nhánh Long Khánh',
               SoTaiKhoan = '08881706888',
               ChuTK      = N'DANG NGUYEN MINH KHOA',
               LoaiGiam   = 'PERCENT',
               GiaTriGiam = 10.00,
               TyLeGiam   = 10.00,
               SuaLuc     = SYSUTCDATETIME()
        WHERE  MaCauHinh = 1;
    END

PRINT N'-- [1/16] Cấu hình quán: Đã sẵn sàng.';


GO
-- ============================================================================
-- 2. TÀI KHOẢN HỆ THỐNG (TaiKhoan) - >= 10 Tài khoản với mật khẩu Password@123
-- Hash: $2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq
-- ============================================================================
DECLARE @pass AS VARCHAR (255) = '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq';

-- Bảng tạm danh sách tài khoản
DECLARE @Accounts TABLE (
    Email     VARCHAR (254) ,
    HoTen     NVARCHAR (120),
    DienThoai VARCHAR (20)  ,
    VaiTro    VARCHAR (16)  );

INSERT  INTO @Accounts
VALUES ('admin@milktea.vn', N'Quản Trị Viên Hệ Thống', '0901000001', 'ADMIN'),
       ('admin2@milktea.vn', N'Quản Lý Vận Hành Tuấn', '0901000002', 'ADMIN'),
       ('cashier@milktea.vn', N'Thu Ngân Quỳnh', '0901000003', 'CASHIER'),
       ('cashier2@milktea.vn', N'Thu Ngân Hoàng', '0901000004', 'CASHIER'),
       ('cashier3@milktea.vn', N'Thu Ngân Thảo', '0901000005', 'CASHIER'),
       ('kitchen@milktea.vn', N'Bếp Trưởng Lân', '0901000006', 'KITCHEN'),
       ('kitchen2@milktea.vn', N'Pha Chế Minh', '0901000007', 'KITCHEN'),
       ('kitchen3@milktea.vn', N'Pha Chế Nga', '0901000008', 'KITCHEN'),
       ('customer@milktea.vn', N'Khách Hàng Mai', '0901000009', 'CUSTOMER'),
       ('customer2@milktea.vn', N'Khách Hàng Đức', '0901000010', 'CUSTOMER'),
       ('customer3@milktea.vn', N'Khách Hàng Linh', '0901000011', 'CUSTOMER'),
       ('customer4@milktea.vn', N'Khách Hàng Bảo', '0901000012', 'CUSTOMER');

MERGE INTO dbo.TaiKhoan
 AS target
USING @Accounts AS source ON (target.Email = source.Email)
WHEN MATCHED THEN UPDATE 
SET target.HoTen        = source.HoTen,
    target.DienThoai    = source.DienThoai,
    target.VaiTro       = source.VaiTro,
    target.MatKhauBam   = @pass,
    target.HoatDong     = 1,
    target.EmailXacThuc = 1
WHEN NOT MATCHED THEN INSERT (
    Email,
    MatKhauBam,
    HoTen,
    DienThoai,
    VaiTro,
    HoatDong,
    EmailXacThuc,
    TokenVersion,
    TaoLuc
) VALUES (source.Email, @pass, source.HoTen, source.DienThoai, source.VaiTro, 1, 1, 1, SYSUTCDATETIME());

-- Cập nhật lại mật khẩu và xác thực cho tất cả tài khoản hiện có
UPDATE dbo.TaiKhoan
SET    MatKhauBam   = @pass,
       EmailXacThuc = 1
WHERE  MatKhauBam != @pass
       OR MatKhauBam IS NULL
       OR EmailXacThuc IS NULL;

PRINT N'-- [2/16] Tài khoản người dùng: Đã nạp 12 tài khoản mẫu (Password@123).';


GO
-- ============================================================================
-- 3. DANH MỤC SẢN PHẨM (DanhMuc) - >= 10 Danh mục
-- ============================================================================
DECLARE @Categories TABLE (
    TenDM          NVARCHAR (100),
    ChoPhepTopping BIT           );

INSERT  INTO @Categories
VALUES (N'Trà Sữa Truyền Thống', 1),
       (N'Trà Sữa Ô Long', 1),
       (N'Trà Sữa Lài', 1),
       (N'Trà Trái Cây Tươi', 1),
       (N'Cà Phê Truyền Thống', 1),
       (N'Cà Phê Sáng Tạo', 1),
       (N'Sữa Tươi Trân Châu', 1),
       (N'Macchiato & Váng Sữa', 1),
       (N'Đá Xay & Sinh Tố', 1),
       (N'Trà Thảo Mộc & Thanh Nhiệt', 1),
       (N'Topping Cao Cấp', 0);

MERGE INTO dbo.DanhMuc
 AS target
USING @Categories AS source ON (target.TenDM = source.TenDM)
WHEN MATCHED THEN UPDATE 
SET target.ChoPhepTopping = source.ChoPhepTopping,
    target.HoatDong       = 1
WHEN NOT MATCHED THEN INSERT (
    TenDM,
    ChoPhepTopping,
    HoatDong
) VALUES (source.TenDM, source.ChoPhepTopping, 1);

PRINT N'-- [3/16] Danh mục sản phẩm: Đã nạp 11 danh mục kèm ChoPhepTopping.';


GO
-- ============================================================================
-- 4. MENU SẢN PHẨM: 30 MÓN NƯỚC UỐNG + 10 TOPPING (MonUong)
-- ============================================================================
DECLARE @dmTraSuaTT AS BIGINT = (SELECT MaDM
                                 FROM   dbo.DanhMuc
                                 WHERE  TenDM = N'Trà Sữa Truyền Thống');

DECLARE @dmTraSuaOL AS BIGINT = (SELECT MaDM
                                 FROM   dbo.DanhMuc
                                 WHERE  TenDM = N'Trà Sữa Ô Long');

DECLARE @dmTraSuaLai AS BIGINT = (SELECT MaDM
                                  FROM   dbo.DanhMuc
                                  WHERE  TenDM = N'Trà Sữa Lài');

DECLARE @dmTraTraiCay AS BIGINT = (SELECT MaDM
                                   FROM   dbo.DanhMuc
                                   WHERE  TenDM = N'Trà Trái Cây Tươi');

DECLARE @dmCafeTT AS BIGINT = (SELECT MaDM
                               FROM   dbo.DanhMuc
                               WHERE  TenDM = N'Cà Phê Truyền Thống');

DECLARE @dmCafeST AS BIGINT = (SELECT MaDM
                               FROM   dbo.DanhMuc
                               WHERE  TenDM = N'Cà Phê Sáng Tạo');

DECLARE @dmSuaTuoi AS BIGINT = (SELECT MaDM
                                FROM   dbo.DanhMuc
                                WHERE  TenDM = N'Sữa Tươi Trân Châu');

DECLARE @dmMacchiato AS BIGINT = (SELECT MaDM
                                  FROM   dbo.DanhMuc
                                  WHERE  TenDM = N'Macchiato & Váng Sữa');

DECLARE @dmDaXay AS BIGINT = (SELECT MaDM
                              FROM   dbo.DanhMuc
                              WHERE  TenDM = N'Đá Xay & Sinh Tố');

DECLARE @dmThaoMoc AS BIGINT = (SELECT MaDM
                                FROM   dbo.DanhMuc
                                WHERE  TenDM = N'Trà Thảo Mộc & Thanh Nhiệt');

DECLARE @dmTopping AS BIGINT = (SELECT MaDM
                                FROM   dbo.DanhMuc
                                WHERE  TenDM = N'Topping Cao Cấp');

-- Bảng tạm 30 món nước uống (Size M và L)
DECLARE @Products TABLE (
    MaDM   BIGINT         ,
    TenMon NVARCHAR (120) ,
    Size   VARCHAR (12)   ,
    Gia    DECIMAL (18, 2),
    MoTa   NVARCHAR (500) );

INSERT  INTO @Products
VALUES -- 1. Trà Sữa Truyền Thống
       (@dmTraSuaTT, N'Trà Sữa Truyền Thống', 'M', 25000, N'Hương vị trà đen đậm đà hòa quyện cùng sữa béo thơm ngon'),
       (@dmTraSuaTT, N'Trà Sữa Truyền Thống', 'L', 30000, N'Trà sữa truyền thống phiên bản ly lớn 700ml sảng khoái'),
       -- 2. Trà Sữa Thái Xanh
       (@dmTraSuaTT, N'Trà Sữa Thái Xanh', 'M', 28000, N'Trà xanh Thái Lan thanh mát, thơm ngát thảo mộc'),
       (@dmTraSuaTT, N'Trà Sữa Thái Xanh', 'L', 33000, N'Trà sữa Thái xanh giải nhiệt ngày hè ly lớn'),
       -- 3. Trà Sữa Thái Đỏ
       (@dmTraSuaTT, N'Trà Sữa Thái Đỏ', 'M', 28000, N'Trà đỏ Thái Lan truyền thống thơm nồng vị thảo mộc'),
       (@dmTraSuaTT, N'Trà Sữa Thái Đỏ', 'L', 33000, N'Trà Thái đỏ đậm vị béo ngậy ly lớn'),
       -- 4. Trà Sữa Socola Đậm Đà
       (@dmTraSuaTT, N'Trà Sữa Socola Đậm Đà', 'M', 32000, N'Vị socola hảo hạng kết hợp trà đen thơm lừng'),
       (@dmTraSuaTT, N'Trà Sữa Socola Đậm Đà', 'L', 37000, N'Trà sữa socola đậm đà ly lớn'),
       -- 5. Trà Sữa Khoai Môn
       (@dmTraSuaTT, N'Trà Sữa Khoai Môn Hoàng Kim', 'M', 32000, N'Khoai môn bùi béo tự nhiên hòa quyện sữa tươi'),
       (@dmTraSuaTT, N'Trà Sữa Khoai Môn Hoàng Kim', 'L', 37000, N'Trà sữa khoai môn thơm bùi ly lớn'),
       -- 6. Trà Sữa Ô Long Nướng
       (@dmTraSuaOL, N'Trà Sữa Ô Long Nướng', 'M', 30000, N'Hương vị ô long rang cháy đặc trưng thơm lừng khó cưỡng'),
       (@dmTraSuaOL, N'Trà Sữa Ô Long Nướng', 'L', 35000, N'Ô long nướng đậm vị khói thơm ngậy ly lớn'),
       -- 7. Trà Sữa Ô Long Thiết Quan Âm
       (@dmTraSuaOL, N'Trà Sữa Ô Long Thiết Quan Âm', 'M', 32000, N'Trà thiết quan âm thượng hạng hậu ngọt thanh tao'),
       (@dmTraSuaOL, N'Trà Sữa Ô Long Thiết Quan Âm', 'L', 38000, N'Thiết quan âm sữa thơm quý phái ly lớn'),
       -- 8. Trà Sữa Lài Thơm
       (@dmTraSuaLai, N'Trà Sữa Lài Thơm', 'M', 28000, N'Hoa lài tự nhiên thơm ngát hòa cùng sữa dịu ngọt'),
       (@dmTraSuaLai, N'Trà Sữa Lài Thơm', 'L', 33000, N'Trà sữa lài ngọt thanh thư giãn ly lớn'),
       -- 9. Trà Sữa Lài Kem Cheese
       (@dmTraSuaLai, N'Trà Sữa Lài Kem Cheese', 'M', 35000, N'Trà sữa lài thanh tao kèm váng phô mai béo ngậy'),
       (@dmTraSuaLai, N'Trà Sữa Lài Kem Cheese', 'L', 42000, N'Trà sữa lài phủ kem cheese ngậy béo ly lớn'),
       -- 10. Trà Sữa Matcha Nhật Bản
       (@dmTraSuaTT, N'Trà Sữa Matcha Nhật Bản', 'M', 35000, N'Bột matcha Uji thượng phẩm thơm lừng vị trà xanh mộc'),
       (@dmTraSuaTT, N'Trà Sữa Matcha Nhật Bản', 'L', 42000, N'Matcha sữa đậm đà chuẩn vị Nhật ly lớn'),
       -- 11. Trà Đào Cam Sả
       (@dmTraTraiCay, N'Trà Đào Cam Sả', 'M', 28000, N'Trà đen ủ lạnh cùng cam vàng mọng nước và sả tươi đập dập'),
       (@dmTraTraiCay, N'Trà Đào Cam Sả', 'L', 33000, N'Trà đào cam sả thanh nhiệt tươi mới ly lớn'),
       -- 12. Trà Vải Hoa Hồng
       (@dmTraTraiCay, N'Trà Vải Hoa Hồng', 'M', 30000, N'Vải thiều ngọt lịm phảng phất hương hoa hồng lãng mạn'),
       (@dmTraTraiCay, N'Trà Vải Hoa Hồng', 'L', 35000, N'Trà vải hoa hồng quý phái ngọt ngào ly lớn'),
       -- 13. Trà Dâu Tây Nhiệt Đới
       (@dmTraTraiCay, N'Trà Dâu Tây Nhiệt Đới', 'M', 32000, N'Dâu tây Đà Lạt tươi giã tay thơm lừng vị quả mọng'),
       (@dmTraTraiCay, N'Trà Dâu Tây Nhiệt Đới', 'L', 38000, N'Trà dâu tây tươi mát rượi ly lớn'),
       -- 14. Trà Xoài Chanh Dây
       (@dmTraTraiCay, N'Trà Xoài Chanh Dây', 'M', 30000, N'Vị chua ngọt bùng nổ giữa xoài cát chín và chanh leo'),
       (@dmTraTraiCay, N'Trà Xoài Chanh Dây', 'L', 35000, N'Trà xoài chanh leo bừng tỉnh năng lượng ly lớn'),
       -- 15. Trà Ổi Hồng Hạt Chia
       (@dmTraTraiCay, N'Trà Ổi Hồng Hạt Chia', 'M', 32000, N'Nước cốt ổi hồng thơm ngát giàu vitamin kèm hạt chia'),
       (@dmTraTraiCay, N'Trà Ổi Hồng Hạt Chia', 'L', 37000, N'Trà ổi hồng bổ dưỡng da sáng dáng thon ly lớn'),
       -- 16. Trà Táo Xanh Bạc Hà
       (@dmTraTraiCay, N'Trà Táo Xanh Bạc Hà', 'M', 30000, N'Táo xanh giòn rụm kết hợp bạc hà mát lạnh the mát'),
       (@dmTraTraiCay, N'Trà Táo Xanh Bạc Hà', 'L', 35000, N'Trà táo xanh the mát giải nhiệt ly lớn'),
       -- 17. Trà Bưởi Mật Ong
       (@dmTraTraiCay, N'Trà Bưởi Mật Ong', 'M', 32000, N'Tép bưởi hồng mọng nước cùng mật ong hoa nhãn dịu ngọt'),
       (@dmTraTraiCay, N'Trà Bưởi Mật Ong', 'L', 38000, N'Trà bưởi hồng mật ong thanh lọc cơ thể ly lớn'),
       -- 18. Trà Mãng Cầu Xiêm
       (@dmTraTraiCay, N'Trà Mãng Cầu Xiêm', 'M', 35000, N'Mãng cầu xiêm chua ngọt tự nhiên giã tay đậm đà'),
       (@dmTraTraiCay, N'Trà Mãng Cầu Xiêm', 'L', 42000, N'Trà mãng cầu xiêm hot trend tươi mát ly lớn'),
       -- 19. Cà Phê Đen Đá
       (@dmCafeTT, N'Cà Phê Đen Đá', 'M', 20000, N'Cà phê robusta rang mộc pha phin truyền thống đậm đà'),
       (@dmCafeTT, N'Cà Phê Đen Đá', 'L', 25000, N'Cà phê đen đá đậm vị tỉnh táo ngày dài'),
       -- 20. Cà Phê Sữa Đá
       (@dmCafeTT, N'Cà Phê Sữa Đá', 'M', 25000, N'Cà phê pha phin truyền thống hòa cùng sữa đặc sánh ngọt'),
       (@dmCafeTT, N'Cà Phê Sữa Đá', 'L', 30000, N'Cà phê sữa đá chuẩn gu Sài Gòn ly lớn'),
       -- 21. Cà Phê Muối Xứ Huế
       (@dmCafeST, N'Cà Phê Muối Xứ Huế', 'M', 28000, N'Lớp kem sữa muối mằn mặn hòa cùng cà phê thơm nồng'),
       (@dmCafeST, N'Cà Phê Muối Xứ Huế', 'L', 34000, N'Cà phê muối béo mặn thơm ngậy ly lớn'),
       -- 22. Bạc Xỉu Ba Tầng
       (@dmCafeST, N'Bạc Xỉu Ba Tầng', 'M', 28000, N'Tầng sữa ngọt dịu, sữa tươi thơm mát và cà phê đậm vị'),
       (@dmCafeST, N'Bạc Xỉu Ba Tầng', 'L', 33000, N'Bạc xỉu ba tầng ngọt ngào êm ái ly lớn'),
       -- 23. Sữa Tươi Trân Châu Đường Đen
       (@dmSuaTuoi, N'Sữa Tươi Trân Châu Đường Đen', 'M', 35000, N'Sữa tươi thanh trùng Đà Lạt cùng trân châu nấu đường đen dẻo quánh'),
       (@dmSuaTuoi, N'Sữa Tươi Trân Châu Đường Đen', 'L', 42000, N'Sữa tươi đường đen thơm ngon nức tiếng ly lớn'),
       -- 24. Sữa Tươi Sương Sáo Thảo Mộc
       (@dmSuaTuoi, N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', 32000, N'Sữa tươi béo nhẹ kết hợp sương sáo Đài Loan mát lành'),
       (@dmSuaTuoi, N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', 38000, N'Sữa tươi sương sáo thanh lọc cơ thể ly lớn'),
       -- 25. Trà Ô Long Váng Sữa Macchiato
       (@dmMacchiato, N'Trà Ô Long Váng Sữa Macchiato', 'M', 35000, N'Cốt trà ô long thượng hạng phủ lớp macchiato kem sữa béo ngậy'),
       (@dmMacchiato, N'Trà Ô Long Váng Sữa Macchiato', 'L', 42000, N'Ô long macchiato béo ngọt cân bằng ly lớn'),
       -- 26. Trà Đen Macchiato Muối Biển
       (@dmMacchiato, N'Trà Đen Macchiato Muối Biển', 'M', 32000, N'Trà đen đậm đà tương phản với kem muối biển béo ngậy'),
       (@dmMacchiato, N'Trà Đen Macchiato Muối Biển', 'L', 38000, N'Trà đen macchiato muối biển thơm ngon ly lớn'),
       -- 27. Matcha Đá Xay Kem Tuyết
       (@dmDaXay, N'Matcha Đá Xay Kem Tuyết', 'M', 40000, N'Matcha xay nhuyễn cùng đá và sữa, phủ bông kem whipping tươi'),
       (@dmDaXay, N'Matcha Đá Xay Kem Tuyết', 'L', 48000, N'Matcha đá xay kem tuyết mát lạnh sảng khoái ly lớn'),
       -- 28. Cà Phê Đá Xay Caramel
       (@dmDaXay, N'Cà Phê Đá Xay Caramel', 'M', 38000, N'Espresso xay đá cùng xốt caramel ngọt bùi phủ kem whipping'),
       (@dmDaXay, N'Cà Phê Đá Xay Caramel', 'L', 45000, N'Cà phê caramel đá xay béo ngọt nồng nàn ly lớn'),
       -- 29. Sinh Tố Bơ Dừa Thơm Béo
       (@dmDaXay, N'Sinh Tố Bơ Dừa Thơm Béo', 'M', 42000, N'Bơ sáp Đắk Lắk dẻo quánh xay cùng nước cốt dừa béo bùi'),
       (@dmDaXay, N'Sinh Tố Bơ Dừa Thơm Béo', 'L', 50000, N'Sinh tố bơ dừa thơm ngậy bổ dưỡng ly lớn'),
       -- 30. Trà Hoa Cúc Mật Ong Hạt Sen
       (@dmThaoMoc, N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', 32000, N'Hoa cúc sấy khô ủ ấm mật ong rừng và hạt sen bùi ngậy'),
       (@dmThaoMoc, N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', 38000, N'Trà hoa cúc hạt sen an thần thanh nhiệt ly lớn');

-- Nạp 30 món nước uống
MERGE INTO dbo.MonUong
 AS target
USING @Products AS source ON (target.TenMon = source.TenMon
                              AND target.Size = source.Size)
WHEN MATCHED THEN UPDATE 
SET target.Gia      = source.Gia,
    target.MoTa     = source.MoTa,
    target.MaDM     = source.MaDM,
    target.HoatDong = 1
WHEN NOT MATCHED THEN INSERT (
    MaDM,
    TenMon,
    Size,
    MoTa,
    Gia,
    HoatDong
) VALUES (source.MaDM, source.TenMon, source.Size, source.MoTa, source.Gia, 1);

-- Bảng tạm 10 Topping cao cấp (Size M chuẩn)
DECLARE @Toppings TABLE (
    MaDM   BIGINT         ,
    TenMon NVARCHAR (120) ,
    Size   VARCHAR (12)   ,
    Gia    DECIMAL (18, 2),
    MoTa   NVARCHAR (500) );

INSERT  INTO @Toppings
VALUES (@dmTopping, N'Trân Châu Đen Hoàng Kim', 'M', 6000, N'Trân châu dẻo dai hoàng kim nấu đường đen ngọt ngào'),
       (@dmTopping, N'Trân Châu Trắng 3Q', 'M', 7000, N'Trân châu trắng giòn sần sật nhai vui miệng'),
       (@dmTopping, N'Thạch Đào Giòn Ngọt', 'M', 8000, N'Thạch đào dai giòn thơm nức mùi quả chín'),
       (@dmTopping, N'Kem Cheese Phô Mai Macchiato', 'M', 10000, N'Lớp kem cheese mằn mặn béo ngậy tan chảy trên đầu lưỡi'),
       (@dmTopping, N'Pudding Trứng Vàng', 'M', 8000, N'Pudding mềm mịn núng nính thơm ngậy vị trứng gà'),
       (@dmTopping, N'Thạch Phô Mai Tươi', 'M', 10000, N'Viên phô mai bọc thạch trong suốt béo ngậy'),
       (@dmTopping, N'Sương Sáo Thảo Mộc Đài Loan', 'M', 6000, N'Sương sáo thanh mát thảo mộc giải nhiệt tức thì'),
       (@dmTopping, N'Thạch Củ Năng Giòn', 'M', 8000, N'Củ năng bọc bột lọc giòn ngọt sảng khoái'),
       (@dmTopping, N'Nha Đam Tươi Đường Phèn', 'M', 7000, N'Nha đam giòn mát nấu đường phèn thanh khiết'),
       (@dmTopping, N'Khúc Bạch Hạnh Nhân', 'M', 10000, N'Khúc bạch phô mai thơm lừng rắc hạnh nhân giòn');

MERGE INTO dbo.MonUong
 AS target
USING @Toppings AS source ON (target.TenMon = source.TenMon
                              AND target.Size = source.Size)
WHEN MATCHED THEN UPDATE 
SET target.Gia      = source.Gia,
    target.MoTa     = source.MoTa,
    target.MaDM     = source.MaDM,
    target.HoatDong = 1
WHEN NOT MATCHED THEN INSERT (
    MaDM,
    TenMon,
    Size,
    MoTa,
    Gia,
    HoatDong
) VALUES (source.MaDM, source.TenMon, source.Size, source.MoTa, source.Gia, 1);

PRINT N'-- [4/16] Menu sản phẩm: Đã nạp 30 món nước uống (60 biến thể size) & 10 loại Topping.';


GO
-- ============================================================================
-- 5. NGUYÊN LIỆU THÔ & SƠ CHẾ (NguyenLieu) - >= 10 THO & >= 10 SOCHE
-- ============================================================================
DECLARE @Materials TABLE (
    TenNL NVARCHAR (120),
    Loai  VARCHAR (8)   ,
    DonVi VARCHAR (16)  );

INSERT  INTO @Materials
VALUES -- Nguyên liệu thô (THO)
       (N'Trà Đen Cổ Thụ', 'THO', 'kg'),
       (N'Trà Ô Long Kim Tuyên', 'THO', 'kg'),
       (N'Trà Lài Thượng Hạng', 'THO', 'kg'),
       (N'Cà Phê Hạt Rang Mộc', 'THO', 'kg'),
       (N'Sữa Đặc Có Đường', 'THO', 'kg'),
       (N'Đường Cát Trắng', 'THO', 'kg'),
       (N'Đường Đen Nữ Hoàng', 'THO', 'kg'),
       (N'Sữa Tươi Thanh Trùng', 'THO', 'kg'),
       (N'Bột Béo Thực Vật', 'THO', 'kg'),
       (N'Bột Kem Cheese Anchor', 'THO', 'kg'),
       (N'Trân Châu Thô Đen', 'THO', 'kg'),
       (N'Hạt Trân Châu Trắng 3Q', 'THO', 'kg'),
       (N'Đào Ngâm Đóng Hộp', 'THO', 'kg'),
       (N'Cam Vàng Tươi', 'THO', 'kg'),
       (N'Sả Cây Tươi', 'THO', 'kg'),
       (N'Dâu Tây Đà Lạt', 'THO', 'kg'),
       (N'Củ Năng Tươi', 'THO', 'kg'),
       (N'Sương Sáo Khô', 'THO', 'kg'),
       (N'Nha Đam Tươi', 'THO', 'kg'),
       (N'Bột Pudding Trứng', 'THO', 'kg'),
       (N'Ly Nhựa PP 500ml', 'THO', 'cai'),
       (N'Ly Nhựa PP 700ml', 'THO', 'cai'),
       (N'Ống Hút Trân Châu', 'THO', 'cai'),
       (N'Màng Ép Miệng Ly', 'THO', 'cai'),
       -- Nguyên liệu sơ chế (SOCHE)
       (N'Nước Cốt Trà Đen', 'SOCHE', 'ml'),
       (N'Nước Cốt Trà Ô Long', 'SOCHE', 'ml'),
       (N'Nước Cốt Trà Lài', 'SOCHE', 'ml'),
       (N'Nước Cốt Cà Phê Pha Phin', 'SOCHE', 'ml'),
       (N'Trân Châu Nấu Chín Dẻo', 'SOCHE', 'g'),
       (N'Trân Châu Nấu Đường Đen', 'SOCHE', 'g'),
       (N'Sốt Kem Cheese Đánh Bông', 'SOCHE', 'ml'),
       (N'Pudding Trứng Nấu Chín', 'SOCHE', 'g'),
       (N'Thạch Phô Mai Tươi Đúc Khuôn', 'SOCHE', 'g'),
       (N'Thạch Củ Năng Rim Đường', 'SOCHE', 'g'),
       (N'Thạch Sương Sáo Nấu Đông', 'SOCHE', 'g'),
       (N'Khúc Bạch Cắt Viên', 'SOCHE', 'g'),
       (N'Nước Đường Pha Sẵn', 'SOCHE', 'ml');

MERGE INTO dbo.NguyenLieu
 AS target
USING @Materials AS source ON (target.TenNL = source.TenNL)
WHEN MATCHED THEN UPDATE 
SET target.Loai     = source.Loai,
    target.DonVi    = source.DonVi,
    target.HoatDong = 1
WHEN NOT MATCHED THEN INSERT (
    TenNL,
    Loai,
    DonVi,
    HoatDong
) VALUES (source.TenNL, source.Loai, source.DonVi, 1);

PRINT N'-- [5/16] Nguyên liệu: Đã nạp 24 nguyên liệu THO & 13 nguyên liệu SOCHE.';


GO
-- ============================================================================
-- 6. TỒN KHO HAI TẦNG (TonNguyenLieu): ĐẦY ĐỦ 4 TRẠNG THÁI CẢNH BÁO Ở CẢ KHO & BEP
-- 4 mức: HET_TON (=0), TON_THAP (0 < SoLuong <= Nguong), BINH_THUONG (> Nguong), CHUA_CAU_HINH (Nguong IS NULL)
-- ============================================================================
DECLARE @StockInit TABLE (
    TenNL         NVARCHAR (120) ,
    ViTri         VARCHAR (8)    ,
    SoLuong       DECIMAL (18, 3),
    NguongCanhBao DECIMAL (18, 3));

INSERT  INTO @StockInit
VALUES -- === TẦNG TỔNG KHO (KHO) ===
       -- BINH_THUONG (Tồn dồi dào trên ngưỡng)
       (N'Trà Đen Cổ Thụ', 'KHO', 60.000, 10.000),
       (N'Trà Ô Long Kim Tuyên', 'KHO', 50.000, 8.000),
       (N'Trà Lài Thượng Hạng', 'KHO', 45.000, 8.000),
       (N'Cà Phê Hạt Rang Mộc', 'KHO', 40.000, 5.000),
       (N'Sữa Đặc Có Đường', 'KHO', 120.000, 15.000),
       (N'Đường Cát Trắng', 'KHO', 150.000, 20.000),
       (N'Đường Đen Nữ Hoàng', 'KHO', 80.000, 10.000),
       (N'Sữa Tươi Thanh Trùng', 'KHO', 100.000, 15.000),
       (N'Trân Châu Thô Đen', 'KHO', 90.000, 10.000),
       (N'Hạt Trân Châu Trắng 3Q', 'KHO', 50.000, 5.000),
       (N'Đào Ngâm Đóng Hộp', 'KHO', 40.000, 5.000),
       (N'Bột Kem Cheese Anchor', 'KHO', 35.000, 5.000),
       (N'Bột Pudding Trứng', 'KHO', 30.000, 5.000),
       (N'Ly Nhựa PP 500ml', 'KHO', 2000.000, 300.000),
       (N'Ly Nhựa PP 700ml', 'KHO', 2000.000, 300.000),
       (N'Ống Hút Trân Châu', 'KHO', 3000.000, 500.000),
       -- TON_THAP (Tồn kho sát mức báo động)
       (N'Cam Vàng Tươi', 'KHO', 4.000, 10.000),
       (N'Sả Cây Tươi', 'KHO', 2.500, 5.000),
       (N'Dâu Tây Đà Lạt', 'KHO', 1.800, 4.000),
       -- HET_TON (Hết sạch tồn kho)
       (N'Củ Năng Tươi', 'KHO', 0.000, 8.000),
       (N'Sương Sáo Khô', 'KHO', 0.000, 5.000),
       -- CHUA_CAU_HINH (Chưa đặt ngưỡng cảnh báo - NULL)
       (N'Bột Béo Thực Vật', 'KHO', 25.000, NULL),
       (N'Nha Đam Tươi', 'KHO', 15.000, NULL),
       (N'Màng Ép Miệng Ly', 'KHO', 20.000, NULL),
       -- === TẦNG QUẦY PHA CHẾ (BEP) ===
       -- BINH_THUONG (Tồn bếp dồi dào sẵn sàng phục vụ)
       (N'Nước Cốt Trà Đen', 'BEP', 7000.000, 1000.000),
       (N'Nước Cốt Trà Ô Long', 'BEP', 6000.000, 1000.000),
       (N'Nước Cốt Cà Phê Pha Phin', 'BEP', 3000.000, 500.000),
       (N'Trân Châu Nấu Chín Dẻo', 'BEP', 3500.000, 500.000),
       (N'Sốt Kem Cheese Đánh Bông', 'BEP', 2500.000, 400.000),
       (N'Pudding Trứng Nấu Chín', 'BEP', 2000.000, 300.000),
       (N'Sữa Đặc Có Đường', 'BEP', 15.000, 2.000),
       (N'Đường Cát Trắng', 'BEP', 20.000, 2.000),
       (N'Ly Nhựa PP 500ml', 'BEP', 300.000, 50.000),
       (N'Ly Nhựa PP 700ml', 'BEP', 300.000, 50.000),
       (N'Ống Hút Trân Châu', 'BEP', 400.000, 60.000),
       -- TON_THAP (Bếp sắp hết, cần xuất kho hoặc nấu thêm gấp)
       (N'Nước Cốt Trà Lài', 'BEP', 350.000, 1000.000),
       (N'Trân Châu Nấu Đường Đen', 'BEP', 180.000, 500.000),
       (N'Cam Vàng Tươi', 'BEP', 0.800, 2.000),
       (N'Đào Ngâm Đóng Hộp', 'BEP', 0.500, 2.000),
       -- HET_TON (Bếp đã hết sạch món này)
       (N'Thạch Củ Năng Rim Đường', 'BEP', 0.000, 500.000),
       (N'Thạch Sương Sáo Nấu Đông', 'BEP', 0.000, 400.000),
       -- CHUA_CAU_HINH (Chưa đặt ngưỡng cảnh báo - NULL)
       (N'Thạch Phô Mai Tươi Đúc Khuôn', 'BEP', 1200.000, NULL),
       (N'Khúc Bạch Cắt Viên', 'BEP', 800.000, NULL),
       (N'Nước Đường Pha Sẵn', 'BEP', 2500.000, NULL);

MERGE INTO dbo.TonNguyenLieu
 AS target
USING (SELECT nl.MaNL,
              s.ViTri,
              s.SoLuong,
              s.NguongCanhBao
       FROM   @StockInit AS s
              INNER JOIN
              dbo.NguyenLieu AS nl
              ON nl.TenNL = s.TenNL) AS source ON (target.MaNL = source.MaNL
                                                   AND target.ViTri = source.ViTri)
WHEN MATCHED THEN UPDATE 
SET target.SoLuong       = source.SoLuong,
    target.NguongCanhBao = source.NguongCanhBao
WHEN NOT MATCHED THEN INSERT (
    MaNL,
    ViTri,
    SoLuong,
    NguongCanhBao,
    Version
) VALUES (source.MaNL, source.ViTri, source.SoLuong, source.NguongCanhBao, 0);

PRINT N'-- [6/16] Tồn kho hai tầng: Đã cấu hình đầy đủ 4 trạng thái cảnh báo tại cả KHO và BEP.';


GO
-- ============================================================================
-- 7. CÔNG THỨC SƠ CHẾ & CHI TIẾT (CongThucSoChe) - >= 10 Công thức
-- ============================================================================
DECLARE @FormulaList TABLE (
    TenCT             NVARCHAR (120) ,
    TenNLDauRa        NVARCHAR (120) ,
    LuongDauRaChuan   DECIMAL (18, 3),
    NguongCanhBaoTyLe DECIMAL (5, 2) ,
    TenNLDauVao       NVARCHAR (120) ,
    LuongDauVaoChuan  DECIMAL (18, 3));

INSERT  INTO @FormulaList
VALUES (N'Ủ Nước Cốt Trà Đen', N'Nước Cốt Trà Đen', 3000.000, 15.00, N'Trà Đen Cổ Thụ', 1.000),
       (N'Ủ Nước Cốt Trà Ô Long', N'Nước Cốt Trà Ô Long', 3000.000, 15.00, N'Trà Ô Long Kim Tuyên', 1.000),
       (N'Ủ Nước Cốt Trà Lài', N'Nước Cốt Trà Lài', 3500.000, 15.00, N'Trà Lài Thượng Hạng', 1.000),
       (N'Chiết Xuất Cà Phê Phin', N'Nước Cốt Cà Phê Pha Phin', 2500.000, 10.00, N'Cà Phê Hạt Rang Mộc', 1.000),
       (N'Nấu Trân Châu Đen Dẻo', N'Trân Châu Nấu Chín Dẻo', 2500.000, 12.00, N'Trân Châu Thô Đen', 1.000),
       (N'Nấu Trân Châu Đường Đen', N'Trân Châu Nấu Đường Đen', 2800.000, 12.00, N'Trân Châu Thô Đen', 1.000),
       (N'Đánh Bông Kem Cheese', N'Sốt Kem Cheese Đánh Bông', 2000.000, 8.00, N'Bột Kem Cheese Anchor', 1.000),
       (N'Nấu Pudding Trứng', N'Pudding Trứng Nấu Chín', 2500.000, 10.00, N'Bột Pudding Trứng', 1.000),
       (N'Chế Biến Thạch Củ Năng Rim', N'Thạch Củ Năng Rim Đường', 1500.000, 10.00, N'Củ Năng Tươi', 1.000),
       (N'Nấu Thạch Sương Sáo Đông', N'Thạch Sương Sáo Nấu Đông', 3000.000, 15.00, N'Sương Sáo Khô', 0.500),
       (N'Làm Khúc Bạch Cắt Viên', N'Khúc Bạch Cắt Viên', 2500.000, 10.00, N'Bột Pudding Trứng', 1.000),
       (N'Đúc Khuôn Thạch Phô Mai Tươi', N'Thạch Phô Mai Tươi Đúc Khuôn', 2000.000, 10.00, N'Bột Kem Cheese Anchor', 1.000),
       (N'Nấu Nước Đường Si-rô', N'Nước Đường Pha Sẵn', 3000.000, 5.00, N'Đường Cát Trắng', 2.500);

-- Nạp công thức sơ chế
DECLARE curCT CURSOR
    FOR SELECT DISTINCT TenCT,
                        TenNLDauRa,
                        LuongDauRaChuan,
                        NguongCanhBaoTyLe
        FROM   @FormulaList;

DECLARE @tTenCT AS NVARCHAR (120), @tTenNLDauRa AS NVARCHAR (120), @tLuongDauRa AS DECIMAL (18, 3), @tNguongTyLe AS DECIMAL (5, 2);

OPEN curCT;

FETCH NEXT FROM curCT INTO @tTenCT, @tTenNLDauRa, @tLuongDauRa, @tNguongTyLe;

WHILE @@FETCH_STATUS = 0
    BEGIN
        DECLARE @nlDauRa AS BIGINT = (SELECT MaNL
                                      FROM   dbo.NguyenLieu
                                      WHERE  TenNL = @tTenNLDauRa);
        IF @nlDauRa IS NOT NULL
            BEGIN
                IF NOT EXISTS (SELECT 1
                               FROM   dbo.CongThucSoChe
                               WHERE  TenCT = @tTenCT)
                    BEGIN
                        INSERT  INTO dbo.CongThucSoChe (
                            TenCT,
                            MaNLDauRa,
                            LuongDauRaChuan,
                            NguongCanhBaoTyLe,
                            HoatDong
                        )
                        VALUES                         (@tTenCT, @nlDauRa, @tLuongDauRa, @tNguongTyLe, 1);
                    END
                ELSE
                    BEGIN
                        UPDATE dbo.CongThucSoChe
                        SET    MaNLDauRa         = @nlDauRa,
                               LuongDauRaChuan   = @tLuongDauRa,
                               NguongCanhBaoTyLe = @tNguongTyLe,
                               HoatDong          = 1
                        WHERE  TenCT = @tTenCT;
                    END
                DECLARE @maCT AS BIGINT = (SELECT MaCTSC
                                           FROM   dbo.CongThucSoChe
                                           WHERE  TenCT = @tTenCT);
                -- Nạp chi tiết nguyên liệu đầu vào
                DELETE dbo.ChiTietCTSoChe
                WHERE  MaCTSC = @maCT;
                INSERT INTO dbo.ChiTietCTSoChe (
                    MaCTSC,
                    MaNL,
                    LuongDauVaoChuan
                )
                SELECT @maCT,
                       nl.MaNL,
                       f.LuongDauVaoChuan
                FROM   @FormulaList AS f
                       INNER JOIN
                       dbo.NguyenLieu AS nl
                       ON nl.TenNL = f.TenNLDauVao
                WHERE  f.TenCT = @tTenCT;
            END
        FETCH NEXT FROM curCT INTO @tTenCT, @tTenNLDauRa, @tLuongDauRa, @tNguongTyLe;
    END

CLOSE curCT;

DEALLOCATE curCT;

PRINT N'-- [7/16] Công thức sơ chế: Đã nạp 11 công thức sơ chế & chi tiết định mức.';


GO
-- ============================================================================
-- 8. CÔNG THỨC ĐỊNH MỨC MÓN UỐNG & TOPPING (CongThucMon) - >= 10 Công thức
-- ============================================================================
DECLARE @RecipeDrink TABLE (
    TenMon    NVARCHAR (120) ,
    Size      VARCHAR (12)   ,
    TenNL     NVARCHAR (120) ,
    DinhLuong DECIMAL (18, 3));

INSERT  INTO @RecipeDrink
VALUES -- 1. Trà Sữa Truyền Thống M & L
       (N'Trà Sữa Truyền Thống', 'M', N'Nước Cốt Trà Đen', 150.000),
       (N'Trà Sữa Truyền Thống', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Trà Sữa Truyền Thống', 'M', N'Đường Cát Trắng', 0.020),
       (N'Trà Sữa Truyền Thống', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Sữa Truyền Thống', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Sữa Truyền Thống', 'L', N'Nước Cốt Trà Đen', 200.000),
       (N'Trà Sữa Truyền Thống', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Trà Sữa Truyền Thống', 'L', N'Đường Cát Trắng', 0.025),
       (N'Trà Sữa Truyền Thống', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Sữa Truyền Thống', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 2. Trà Sữa Thái Xanh M & L
       (N'Trà Sữa Thái Xanh', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Sữa Thái Xanh', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Trà Sữa Thái Xanh', 'M', N'Bột Béo Thực Vật', 0.020),
       (N'Trà Sữa Thái Xanh', 'M', N'Nước Đường Pha Sẵn', 20.000),
       (N'Trà Sữa Thái Xanh', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Sữa Thái Xanh', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Sữa Thái Xanh', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Sữa Thái Xanh', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Trà Sữa Thái Xanh', 'L', N'Bột Béo Thực Vật', 0.025),
       (N'Trà Sữa Thái Xanh', 'L', N'Nước Đường Pha Sẵn', 30.000),
       (N'Trà Sữa Thái Xanh', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Sữa Thái Xanh', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 3. Trà Sữa Thái Đỏ M & L
       (N'Trà Sữa Thái Đỏ', 'M', N'Nước Cốt Trà Đen', 150.000),
       (N'Trà Sữa Thái Đỏ', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Trà Sữa Thái Đỏ', 'M', N'Bột Béo Thực Vật', 0.020),
       (N'Trà Sữa Thái Đỏ', 'M', N'Nước Đường Pha Sẵn', 20.000),
       (N'Trà Sữa Thái Đỏ', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Sữa Thái Đỏ', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Sữa Thái Đỏ', 'L', N'Nước Cốt Trà Đen', 200.000),
       (N'Trà Sữa Thái Đỏ', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Trà Sữa Thái Đỏ', 'L', N'Bột Béo Thực Vật', 0.025),
       (N'Trà Sữa Thái Đỏ', 'L', N'Nước Đường Pha Sẵn', 30.000),
       (N'Trà Sữa Thái Đỏ', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Sữa Thái Đỏ', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 4. Trà Sữa Socola Đậm Đà M & L
       (N'Trà Sữa Socola Đậm Đà', 'M', N'Nước Cốt Trà Đen', 150.000),
       (N'Trà Sữa Socola Đậm Đà', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Trà Sữa Socola Đậm Đà', 'M', N'Bột Béo Thực Vật', 0.020),
       (N'Trà Sữa Socola Đậm Đà', 'M', N'Đường Đen Nữ Hoàng', 0.020),
       (N'Trà Sữa Socola Đậm Đà', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Sữa Socola Đậm Đà', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Sữa Socola Đậm Đà', 'L', N'Nước Cốt Trà Đen', 200.000),
       (N'Trà Sữa Socola Đậm Đà', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Trà Sữa Socola Đậm Đà', 'L', N'Bột Béo Thực Vật', 0.025),
       (N'Trà Sữa Socola Đậm Đà', 'L', N'Đường Đen Nữ Hoàng', 0.025),
       (N'Trà Sữa Socola Đậm Đà', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Sữa Socola Đậm Đà', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 5. Trà Sữa Khoai Môn Hoàng Kim M & L
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Bột Béo Thực Vật', 0.020),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Sữa Tươi Thanh Trùng', 0.050),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Bột Béo Thực Vật', 0.025),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Sữa Tươi Thanh Trùng', 0.080),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 6. Trà Sữa Ô Long Nướng M & L
       (N'Trà Sữa Ô Long Nướng', 'M', N'Nước Cốt Trà Ô Long', 150.000),
       (N'Trà Sữa Ô Long Nướng', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Trà Sữa Ô Long Nướng', 'M', N'Đường Cát Trắng', 0.020),
       (N'Trà Sữa Ô Long Nướng', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Sữa Ô Long Nướng', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Sữa Ô Long Nướng', 'L', N'Nước Cốt Trà Ô Long', 200.000),
       (N'Trà Sữa Ô Long Nướng', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Trà Sữa Ô Long Nướng', 'L', N'Đường Cát Trắng', 0.025),
       (N'Trà Sữa Ô Long Nướng', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Sữa Ô Long Nướng', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 7. Trà Sữa Ô Long Thiết Quan Âm M & L
       (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Nước Cốt Trà Ô Long', 150.000),
       (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Đường Cát Trắng', 0.020),
       (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Nước Cốt Trà Ô Long', 200.000),
       (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Đường Cát Trắng', 0.025),
       (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 8. Trà Sữa Lài Thơm M & L
       (N'Trà Sữa Lài Thơm', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Sữa Lài Thơm', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Trà Sữa Lài Thơm', 'M', N'Đường Cát Trắng', 0.020),
       (N'Trà Sữa Lài Thơm', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Sữa Lài Thơm', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Sữa Lài Thơm', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Sữa Lài Thơm', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Trà Sữa Lài Thơm', 'L', N'Đường Cát Trắng', 0.025),
       (N'Trà Sữa Lài Thơm', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Sữa Lài Thơm', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 9. Trà Sữa Lài Kem Cheese M & L
       (N'Trà Sữa Lài Kem Cheese', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Sữa Lài Kem Cheese', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Trà Sữa Lài Kem Cheese', 'M', N'Sốt Kem Cheese Đánh Bông', 40.000),
       (N'Trà Sữa Lài Kem Cheese', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Sữa Lài Kem Cheese', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Sữa Lài Kem Cheese', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Sữa Lài Kem Cheese', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Trà Sữa Lài Kem Cheese', 'L', N'Sốt Kem Cheese Đánh Bông', 60.000),
       (N'Trà Sữa Lài Kem Cheese', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Sữa Lài Kem Cheese', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 10. Trà Sữa Matcha Nhật Bản M & L
       (N'Trà Sữa Matcha Nhật Bản', 'M', N'Nước Cốt Trà Lài', 120.000),
       (N'Trà Sữa Matcha Nhật Bản', 'M', N'Sữa Tươi Thanh Trùng', 0.100),
       (N'Trà Sữa Matcha Nhật Bản', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Trà Sữa Matcha Nhật Bản', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Sữa Matcha Nhật Bản', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Sữa Matcha Nhật Bản', 'L', N'Nước Cốt Trà Lài', 160.000),
       (N'Trà Sữa Matcha Nhật Bản', 'L', N'Sữa Tươi Thanh Trùng', 0.140),
       (N'Trà Sữa Matcha Nhật Bản', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Trà Sữa Matcha Nhật Bản', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Sữa Matcha Nhật Bản', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 11. Trà Đào Cam Sả M & L
       (N'Trà Đào Cam Sả', 'M', N'Nước Cốt Trà Đen', 120.000),
       (N'Trà Đào Cam Sả', 'M', N'Đào Ngâm Đóng Hộp', 0.040),
       (N'Trà Đào Cam Sả', 'M', N'Cam Vàng Tươi', 0.030),
       (N'Trà Đào Cam Sả', 'M', N'Sả Cây Tươi', 0.010),
       (N'Trà Đào Cam Sả', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Đào Cam Sả', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Đào Cam Sả', 'L', N'Nước Cốt Trà Đen', 160.000),
       (N'Trà Đào Cam Sả', 'L', N'Đào Ngâm Đóng Hộp', 0.060),
       (N'Trà Đào Cam Sả', 'L', N'Cam Vàng Tươi', 0.045),
       (N'Trà Đào Cam Sả', 'L', N'Sả Cây Tươi', 0.015),
       (N'Trà Đào Cam Sả', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Đào Cam Sả', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 12. Trà Vải Hoa Hồng M & L
       (N'Trà Vải Hoa Hồng', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Vải Hoa Hồng', 'M', N'Nước Đường Pha Sẵn', 30.000),
       (N'Trà Vải Hoa Hồng', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Vải Hoa Hồng', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Vải Hoa Hồng', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Vải Hoa Hồng', 'L', N'Nước Đường Pha Sẵn', 40.000),
       (N'Trà Vải Hoa Hồng', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Vải Hoa Hồng', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 13. Trà Dâu Tây Nhiệt Đới M & L
       (N'Trà Dâu Tây Nhiệt Đới', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Dâu Tây Nhiệt Đới', 'M', N'Dâu Tây Đà Lạt', 0.040),
       (N'Trà Dâu Tây Nhiệt Đới', 'M', N'Nước Đường Pha Sẵn', 25.000),
       (N'Trà Dâu Tây Nhiệt Đới', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Dâu Tây Nhiệt Đới', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Dâu Tây Nhiệt Đới', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Dâu Tây Nhiệt Đới', 'L', N'Dâu Tây Đà Lạt', 0.060),
       (N'Trà Dâu Tây Nhiệt Đới', 'L', N'Nước Đường Pha Sẵn', 35.000),
       (N'Trà Dâu Tây Nhiệt Đới', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Dâu Tây Nhiệt Đới', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 14. Trà Xoài Chanh Dây M & L
       (N'Trà Xoài Chanh Dây', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Xoài Chanh Dây', 'M', N'Nước Đường Pha Sẵn', 30.000),
       (N'Trà Xoài Chanh Dây', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Xoài Chanh Dây', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Xoài Chanh Dây', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Xoài Chanh Dây', 'L', N'Nước Đường Pha Sẵn', 40.000),
       (N'Trà Xoài Chanh Dây', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Xoài Chanh Dây', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 15. Trà Ổi Hồng Hạt Chia M & L
       (N'Trà Ổi Hồng Hạt Chia', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Ổi Hồng Hạt Chia', 'M', N'Nước Đường Pha Sẵn', 30.000),
       (N'Trà Ổi Hồng Hạt Chia', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Ổi Hồng Hạt Chia', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Ổi Hồng Hạt Chia', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Ổi Hồng Hạt Chia', 'L', N'Nước Đường Pha Sẵn', 40.000),
       (N'Trà Ổi Hồng Hạt Chia', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Ổi Hồng Hạt Chia', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 16. Trà Táo Xanh Bạc Hà M & L
       (N'Trà Táo Xanh Bạc Hà', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Táo Xanh Bạc Hà', 'M', N'Nước Đường Pha Sẵn', 25.000),
       (N'Trà Táo Xanh Bạc Hà', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Táo Xanh Bạc Hà', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Táo Xanh Bạc Hà', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Táo Xanh Bạc Hà', 'L', N'Nước Đường Pha Sẵn', 35.000),
       (N'Trà Táo Xanh Bạc Hà', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Táo Xanh Bạc Hà', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 17. Trà Bưởi Mật Ong M & L
       (N'Trà Bưởi Mật Ong', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Bưởi Mật Ong', 'M', N'Nước Đường Pha Sẵn', 30.000),
       (N'Trà Bưởi Mật Ong', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Bưởi Mật Ong', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Bưởi Mật Ong', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Bưởi Mật Ong', 'L', N'Nước Đường Pha Sẵn', 40.000),
       (N'Trà Bưởi Mật Ong', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Bưởi Mật Ong', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 18. Trà Mãng Cầu Xiêm M & L
       (N'Trà Mãng Cầu Xiêm', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Mãng Cầu Xiêm', 'M', N'Nước Đường Pha Sẵn', 30.000),
       (N'Trà Mãng Cầu Xiêm', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Mãng Cầu Xiêm', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Mãng Cầu Xiêm', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Mãng Cầu Xiêm', 'L', N'Nước Đường Pha Sẵn', 40.000),
       (N'Trà Mãng Cầu Xiêm', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Mãng Cầu Xiêm', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 19. Cà Phê Đen Đá M & L
       (N'Cà Phê Đen Đá', 'M', N'Nước Cốt Cà Phê Pha Phin', 60.000),
       (N'Cà Phê Đen Đá', 'M', N'Đường Cát Trắng', 0.020),
       (N'Cà Phê Đen Đá', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Cà Phê Đen Đá', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Cà Phê Đen Đá', 'L', N'Nước Cốt Cà Phê Pha Phin', 90.000),
       (N'Cà Phê Đen Đá', 'L', N'Đường Cát Trắng', 0.030),
       (N'Cà Phê Đen Đá', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Cà Phê Đen Đá', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 20. Cà Phê Sữa Đá M & L
       (N'Cà Phê Sữa Đá', 'M', N'Nước Cốt Cà Phê Pha Phin', 60.000),
       (N'Cà Phê Sữa Đá', 'M', N'Sữa Đặc Có Đường', 0.040),
       (N'Cà Phê Sữa Đá', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Cà Phê Sữa Đá', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Cà Phê Sữa Đá', 'L', N'Nước Cốt Cà Phê Pha Phin', 90.000),
       (N'Cà Phê Sữa Đá', 'L', N'Sữa Đặc Có Đường', 0.055),
       (N'Cà Phê Sữa Đá', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Cà Phê Sữa Đá', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 21. Cà Phê Muối Xứ Huế M & L
       (N'Cà Phê Muối Xứ Huế', 'M', N'Nước Cốt Cà Phê Pha Phin', 60.000),
       (N'Cà Phê Muối Xứ Huế', 'M', N'Sữa Đặc Có Đường', 0.030),
       (N'Cà Phê Muối Xứ Huế', 'M', N'Sốt Kem Cheese Đánh Bông', 30.000),
       (N'Cà Phê Muối Xứ Huế', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Cà Phê Muối Xứ Huế', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Cà Phê Muối Xứ Huế', 'L', N'Nước Cốt Cà Phê Pha Phin', 90.000),
       (N'Cà Phê Muối Xứ Huế', 'L', N'Sữa Đặc Có Đường', 0.040),
       (N'Cà Phê Muối Xứ Huế', 'L', N'Sốt Kem Cheese Đánh Bông', 45.000),
       (N'Cà Phê Muối Xứ Huế', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Cà Phê Muối Xứ Huế', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 22. Bạc Xỉu Ba Tầng M & L
       (N'Bạc Xỉu Ba Tầng', 'M', N'Nước Cốt Cà Phê Pha Phin', 40.000),
       (N'Bạc Xỉu Ba Tầng', 'M', N'Sữa Đặc Có Đường', 0.035),
       (N'Bạc Xỉu Ba Tầng', 'M', N'Sữa Tươi Thanh Trùng', 0.100),
       (N'Bạc Xỉu Ba Tầng', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Bạc Xỉu Ba Tầng', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Bạc Xỉu Ba Tầng', 'L', N'Nước Cốt Cà Phê Pha Phin', 55.000),
       (N'Bạc Xỉu Ba Tầng', 'L', N'Sữa Đặc Có Đường', 0.045),
       (N'Bạc Xỉu Ba Tầng', 'L', N'Sữa Tươi Thanh Trùng', 0.150),
       (N'Bạc Xỉu Ba Tầng', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Bạc Xỉu Ba Tầng', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 23. Sữa Tươi Trân Châu Đường Đen M & L
       (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Sữa Tươi Thanh Trùng', 0.200),
       (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Trân Châu Nấu Đường Đen', 60.000),
       (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Sữa Tươi Thanh Trùng', 0.280),
       (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Trân Châu Nấu Đường Đen', 80.000),
       (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 24. Sữa Tươi Sương Sáo Thảo Mộc M & L
       (N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', N'Sữa Tươi Thanh Trùng', 0.200),
       (N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', N'Thạch Sương Sáo Nấu Đông', 60.000),
       (N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', N'Nước Đường Pha Sẵn', 15.000),
       (N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', N'Sữa Tươi Thanh Trùng', 0.280),
       (N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', N'Thạch Sương Sáo Nấu Đông', 85.000),
       (N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', N'Nước Đường Pha Sẵn', 20.000),
       (N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 25. Trà Ô Long Váng Sữa Macchiato M & L
       (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Nước Cốt Trà Ô Long', 150.000),
       (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Sốt Kem Cheese Đánh Bông', 50.000),
       (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Nước Cốt Trà Ô Long', 200.000),
       (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Sốt Kem Cheese Đánh Bông', 70.000),
       (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 26. Trà Đen Macchiato Muối Biển M & L
       (N'Trà Đen Macchiato Muối Biển', 'M', N'Nước Cốt Trà Đen', 150.000),
       (N'Trà Đen Macchiato Muối Biển', 'M', N'Sốt Kem Cheese Đánh Bông', 50.000),
       (N'Trà Đen Macchiato Muối Biển', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Đen Macchiato Muối Biển', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Đen Macchiato Muối Biển', 'L', N'Nước Cốt Trà Đen', 200.000),
       (N'Trà Đen Macchiato Muối Biển', 'L', N'Sốt Kem Cheese Đánh Bông', 70.000),
       (N'Trà Đen Macchiato Muối Biển', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Đen Macchiato Muối Biển', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 27. Matcha Đá Xay Kem Tuyết M & L
       (N'Matcha Đá Xay Kem Tuyết', 'M', N'Sữa Tươi Thanh Trùng', 0.150),
       (N'Matcha Đá Xay Kem Tuyết', 'M', N'Sữa Đặc Có Đường', 0.035),
       (N'Matcha Đá Xay Kem Tuyết', 'M', N'Sốt Kem Cheese Đánh Bông', 40.000),
       (N'Matcha Đá Xay Kem Tuyết', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Matcha Đá Xay Kem Tuyết', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Matcha Đá Xay Kem Tuyết', 'L', N'Sữa Tươi Thanh Trùng', 0.200),
       (N'Matcha Đá Xay Kem Tuyết', 'L', N'Sữa Đặc Có Đường', 0.045),
       (N'Matcha Đá Xay Kem Tuyết', 'L', N'Sốt Kem Cheese Đánh Bông', 60.000),
       (N'Matcha Đá Xay Kem Tuyết', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Matcha Đá Xay Kem Tuyết', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 28. Cà Phê Đá Xay Caramel M & L
       (N'Cà Phê Đá Xay Caramel', 'M', N'Nước Cốt Cà Phê Pha Phin', 60.000),
       (N'Cà Phê Đá Xay Caramel', 'M', N'Sữa Đặc Có Đường', 0.035),
       (N'Cà Phê Đá Xay Caramel', 'M', N'Đường Đen Nữ Hoàng', 0.020),
       (N'Cà Phê Đá Xay Caramel', 'M', N'Sữa Tươi Thanh Trùng', 0.100),
       (N'Cà Phê Đá Xay Caramel', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Cà Phê Đá Xay Caramel', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Cà Phê Đá Xay Caramel', 'L', N'Nước Cốt Cà Phê Pha Phin', 90.000),
       (N'Cà Phê Đá Xay Caramel', 'L', N'Sữa Đặc Có Đường', 0.045),
       (N'Cà Phê Đá Xay Caramel', 'L', N'Đường Đen Nữ Hoàng', 0.025),
       (N'Cà Phê Đá Xay Caramel', 'L', N'Sữa Tươi Thanh Trùng', 0.140),
       (N'Cà Phê Đá Xay Caramel', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Cà Phê Đá Xay Caramel', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 29. Sinh Tố Bơ Dừa Thơm Béo M & L
       (N'Sinh Tố Bơ Dừa Thơm Béo', 'M', N'Sữa Tươi Thanh Trùng', 0.150),
       (N'Sinh Tố Bơ Dừa Thơm Béo', 'M', N'Sữa Đặc Có Đường', 0.040),
       (N'Sinh Tố Bơ Dừa Thơm Béo', 'M', N'Nước Đường Pha Sẵn', 20.000),
       (N'Sinh Tố Bơ Dừa Thơm Béo', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Sinh Tố Bơ Dừa Thơm Béo', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Sinh Tố Bơ Dừa Thơm Béo', 'L', N'Sữa Tươi Thanh Trùng', 0.200),
       (N'Sinh Tố Bơ Dừa Thơm Béo', 'L', N'Sữa Đặc Có Đường', 0.055),
       (N'Sinh Tố Bơ Dừa Thơm Béo', 'L', N'Nước Đường Pha Sẵn', 30.000),
       (N'Sinh Tố Bơ Dừa Thơm Béo', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Sinh Tố Bơ Dừa Thơm Béo', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 30. Trà Hoa Cúc Mật Ong Hạt Sen M & L
       (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Nước Cốt Trà Lài', 150.000),
       (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Nước Đường Pha Sẵn', 25.000),
       (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Ly Nhựa PP 500ml', 1.000),
       (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Ống Hút Trân Châu', 1.000),
       (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Nước Cốt Trà Lài', 200.000),
       (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Nước Đường Pha Sẵn', 35.000),
       (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Ly Nhựa PP 700ml', 1.000),
       (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Ống Hút Trân Châu', 1.000),
       -- 10 Loại Topping Cao Cấp (Size M chuẩn)
       (N'Trân Châu Đen Hoàng Kim', 'M', N'Trân Châu Nấu Chín Dẻo', 50.000),
       (N'Trân Châu Trắng 3Q', 'M', N'Hạt Trân Châu Trắng 3Q', 0.050),
       (N'Thạch Đào Giòn Ngọt', 'M', N'Đào Ngâm Đóng Hộp', 0.050),
       (N'Kem Cheese Phô Mai Macchiato', 'M', N'Sốt Kem Cheese Đánh Bông', 50.000),
       (N'Pudding Trứng Vàng', 'M', N'Pudding Trứng Nấu Chín', 60.000),
       (N'Thạch Phô Mai Tươi', 'M', N'Thạch Phô Mai Tươi Đúc Khuôn', 50.000),
       (N'Sương Sáo Thảo Mộc Đài Loan', 'M', N'Thạch Sương Sáo Nấu Đông', 50.000),
       (N'Thạch Củ Năng Giòn', 'M', N'Thạch Củ Năng Rim Đường', 50.000),
       (N'Nha Đam Tươi Đường Phèn', 'M', N'Nha Đam Tươi', 0.050),
       (N'Khúc Bạch Hạnh Nhân', 'M', N'Khúc Bạch Cắt Viên', 50.000);

MERGE INTO dbo.CongThucMon
 AS target
USING (SELECT mu.MaMon,
              nl.MaNL,
              r.DinhLuong
       FROM   @RecipeDrink AS r
              INNER JOIN
              dbo.MonUong AS mu
              ON mu.TenMon = r.TenMon
                 AND mu.Size = r.Size
              INNER JOIN
              dbo.NguyenLieu AS nl
              ON nl.TenNL = r.TenNL) AS source ON (target.MaMon = source.MaMon
                                                   AND target.MaNL = source.MaNL)
WHEN MATCHED THEN UPDATE 
SET target.DinhLuong = source.DinhLuong
WHEN NOT MATCHED THEN INSERT (
    MaMon,
    MaNL,
    DinhLuong
) VALUES (source.MaMon, source.MaNL, source.DinhLuong);

PRINT N'-- [8/16] Định mức món uống & topping: Đã đồng bộ công thức chi tiết.';


GO
-- ============================================================================
-- 9. PHIẾU NHẬP KHO (PhieuNhap, ChiTietNhap) - >= 10 Phiếu nhập thiết lập giá vốn
-- ============================================================================
DECLARE @adminId AS BIGINT = (SELECT TOP 1 MaTK
                              FROM   dbo.TaiKhoan
                              WHERE  VaiTro = 'ADMIN');

DECLARE @Imports TABLE (
    MaYeuCau  VARCHAR (64)   ,
    LyDo      NVARCHAR (500) ,
    NguonNhap NVARCHAR (500) ,
    TrangThai VARCHAR (12)   ,
    TenNL     NVARCHAR (120) ,
    SoLuong   DECIMAL (18, 3),
    DonGia    DECIMAL (18, 2));

INSERT  INTO @Imports
VALUES ('PN_SEED_01', N'Nhập nguyên liệu trà mộc đầu kỳ', N'Nông Trường Trà Cầu Đất Farm', 'DA_GHI_SO', N'Trà Đen Cổ Thụ', 100.000, 65000.00),
       ('PN_SEED_01', N'Nhập nguyên liệu trà mộc đầu kỳ', N'Nông Trường Trà Cầu Đất Farm', 'DA_GHI_SO', N'Trà Ô Long Kim Tuyên', 80.000, 95000.00),
       ('PN_SEED_02', N'Nhập trà hoa lài và hạt cà phê rang', N'Công Ty Cà Phê Cao Nguyên Buôn Ma Thuột', 'DA_GHI_SO', N'Trà Lài Thượng Hạng', 60.000, 85000.00),
       ('PN_SEED_02', N'Nhập trà hoa lài và hạt cà phê rang', N'Công Ty Cà Phê Cao Nguyên Buôn Ma Thuột', 'DA_GHI_SO', N'Cà Phê Hạt Rang Mộc', 50.000, 110000.00),
       ('PN_SEED_03', N'Nhập nguyên liệu sữa và đường tinh luyện', N'Đại Lý Sữa Cô Gái Hà Lan & Biên Hòa', 'DA_GHI_SO', N'Sữa Đặc Có Đường', 150.000, 38000.00),
       ('PN_SEED_03', N'Nhập nguyên liệu sữa và đường tinh luyện', N'Đại Lý Sữa Cô Gái Hà Lan & Biên Hòa', 'DA_GHI_SO', N'Đường Cát Trắng', 200.000, 22000.00),
       ('PN_SEED_04', N'Nhập đường đen Đài Loan và sữa tươi thanh trùng', N'Nhà Cung Cấp Sữa Thanh Trùng Đà Lạt', 'DA_GHI_SO', N'Đường Đen Nữ Hoàng', 100.000, 45000.00),
       ('PN_SEED_04', N'Nhập đường đen Đài Loan và sữa tươi thanh trùng', N'Nhà Cung Cấp Sữa Thanh Trùng Đà Lạt', 'DA_GHI_SO', N'Sữa Tươi Thanh Trùng', 120.000, 32000.00),
       ('PN_SEED_05', N'Nhập bột kem cheese Anchor và trân châu thô', N'Công Ty Cổ Phần Nguyên Liệu Pha Chế VN', 'DA_GHI_SO', N'Bột Kem Cheese Anchor', 40.000, 92000.00),
       ('PN_SEED_05', N'Nhập bột kem cheese Anchor và trân châu thô', N'Công Ty Cổ Phần Nguyên Liệu Pha Chế VN', 'DA_GHI_SO', N'Trân Châu Thô Đen', 120.000, 28000.00),
       ('PN_SEED_06', N'Nhập trân châu trắng 3Q và đào ngâm hộp', N'Công Ty Thực Phẩm Tân Nhất Hương', 'DA_GHI_SO', N'Hạt Trân Châu Trắng 3Q', 60.000, 35000.00),
       ('PN_SEED_06', N'Nhập trân châu trắng 3Q và đào ngâm hộp', N'Công Ty Thực Phẩm Tân Nhất Hương', 'DA_GHI_SO', N'Đào Ngâm Đóng Hộp', 50.000, 46000.00),
       ('PN_SEED_07', N'Nhập trái cây tươi trong ngày', N'Chợ Đầu Mối Nông Sản Thủ Đức', 'DA_GHI_SO', N'Cam Vàng Tươi', 30.000, 26000.00),
       ('PN_SEED_07', N'Nhập trái cây tươi trong ngày', N'Chợ Đầu Mối Nông Sản Thủ Đức', 'DA_GHI_SO', N'Sả Cây Tươi', 20.000, 14000.00),
       ('PN_SEED_07', N'Nhập nguyên liệu phụ gia và củ tươi', N'Chợ Đầu Mối Nông Sản Thủ Đức', 'DA_GHI_SO', N'Củ Năng Tươi', 40.000, 35000.00),
       ('PN_SEED_07', N'Nhập nguyên liệu phụ gia và củ tươi', N'Chợ Đầu Mối Nông Sản Thủ Đức', 'DA_GHI_SO', N'Nha Đam Tươi', 30.000, 20000.00),
       ('PN_SEED_08', N'Nhập dâu tây Đà Lạt và bột pudding trứng', N'HTX Dâu Tây Đà Lạt Xanh', 'DA_GHI_SO', N'Dâu Tây Đà Lạt', 20.000, 75000.00),
       ('PN_SEED_08', N'Nhập dâu tây Đà Lạt và bột pudding trứng', N'HTX Dâu Tây Đà Lạt Xanh', 'DA_GHI_SO', N'Bột Pudding Trứng', 30.000, 52000.00),
       ('PN_SEED_08', N'Nhập sương sáo khô và bột béo thực vật', N'Công Ty Nguyên Liệu Pha Chế VN', 'DA_GHI_SO', N'Bột Béo Thực Vật', 50.000, 45000.00),
       ('PN_SEED_08', N'Nhập sương sáo khô và bột béo thực vật', N'Công Ty Nguyên Liệu Pha Chế VN', 'DA_GHI_SO', N'Sương Sáo Khô', 20.000, 80000.00),
       ('PN_SEED_09', N'Nhập ly nhựa và ống hút sinh học định kỳ', N'Công Ty Bao Bì Nhựa Nam Hưng', 'DA_GHI_SO', N'Ly Nhựa PP 500ml', 3000.000, 480.00),
       ('PN_SEED_09', N'Nhập ly nhựa và ống hút sinh học định kỳ', N'Công Ty Bao Bì Nhựa Nam Hưng', 'DA_GHI_SO', N'Ly Nhựa PP 700ml', 3000.000, 560.00),
       ('PN_SEED_09', N'Nhập vật phẩm đóng gói tiêu hao', N'Công Ty Bao Bì Nhựa Nam Hưng', 'DA_GHI_SO', N'Màng Ép Miệng Ly', 2000.000, 250.00),
       ('PN_SEED_09', N'Nhập vật phẩm đóng gói tiêu hao', N'Công Ty Bao Bì Nhựa Nam Hưng', 'DA_GHI_SO', N'Ống Hút Trân Châu', 5000.000, 150.00),
       ('PN_SEED_10', N'Phiếu nhập nguyên liệu dự phòng cuối tuần (đang soạn)', N'Tổng Kho Sỉ Nguyên Liệu TP.HCM', 'NHAP', N'Trà Đen Cổ Thụ', 50.000, 64000.00),
       ('PN_SEED_10', N'Phiếu nhập nguyên liệu dự phòng cuối tuần (đang soạn)', N'Tổng Kho Sỉ Nguyên Liệu TP.HCM', 'NHAP', N'Đường Cát Trắng', 100.000, 21500.00);

DECLARE curPN CURSOR
    FOR SELECT DISTINCT MaYeuCau,
                        LyDo,
                        NguonNhap,
                        TrangThai
        FROM   @Imports;

DECLARE @pYeuCau AS VARCHAR (64), @pLyDo AS NVARCHAR (500), @pNguon AS NVARCHAR (500), @pTrangThai AS VARCHAR (12);

OPEN curPN;

FETCH NEXT FROM curPN INTO @pYeuCau, @pLyDo, @pNguon, @pTrangThai;

WHILE @@FETCH_STATUS = 0
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM   dbo.PhieuNhap
                       WHERE  MaYeuCau = @pYeuCau)
            BEGIN
                INSERT  INTO dbo.PhieuNhap (
                    NguoiLap,
                    LyDo,
                    NguonNhap,
                    TrangThai,
                    LapLuc,
                    GhiSoLuc,
                    MaYeuCau
                )
                VALUES                     (@adminId, @pLyDo, @pNguon, @pTrangThai, DATEADD(DAY, -5, SYSUTCDATETIME()), CASE WHEN @pTrangThai = 'DA_GHI_SO' THEN DATEADD(DAY, -5, SYSUTCDATETIME()) ELSE NULL END, @pYeuCau);
            END
        DECLARE @maPN AS BIGINT = (SELECT MaPhieu
                                   FROM   dbo.PhieuNhap
                                   WHERE  MaYeuCau = @pYeuCau);
        DELETE dbo.ChiTietNhap
        WHERE  MaPhieu = @maPN;
        INSERT INTO dbo.ChiTietNhap (
            MaPhieu,
            MaNL,
            SoLuong,
            DonGiaNhap
        )
        SELECT @maPN,
               nl.MaNL,
               imp.SoLuong,
               imp.DonGia
        FROM   @Imports AS imp
               INNER JOIN
               dbo.NguyenLieu AS nl
               ON nl.TenNL = imp.TenNL
        WHERE  imp.MaYeuCau = @pYeuCau;
        FETCH NEXT FROM curPN INTO @pYeuCau, @pLyDo, @pNguon, @pTrangThai;
    END

CLOSE curPN;

DEALLOCATE curPN;

PRINT N'-- [9/16] Phiếu nhập kho: Đã nạp 10 phiếu nhập thiết lập giá vốn nguyên liệu chuẩn.';


GO
-- ============================================================================
-- 10. MẺ SƠ CHẾ & CHI TIẾT (MeSoChe, ChiTietMeSoChe) - >= 10 Mẻ sơ chế
-- ============================================================================
DECLARE @kitchenId AS BIGINT = (SELECT TOP 1 MaTK
                                FROM   dbo.TaiKhoan
                                WHERE  VaiTro = 'KITCHEN');

IF @kitchenId IS NULL
    SET @kitchenId = (SELECT TOP 1 MaTK
                      FROM   dbo.TaiKhoan
                      WHERE  VaiTro = 'ADMIN');

DECLARE @Batches TABLE (
    MaYeuCau       VARCHAR (64)   ,
    TenCT          NVARCHAR (120) ,
    TenNLDauRa     NVARCHAR (120) ,
    LuongDuKien    DECIMAL (18, 3),
    LuongThucThu   DECIMAL (18, 3),
    TrangThai      VARCHAR (12)   ,
    LyDoChenhLech  NVARCHAR (500) ,
    TongChiPhi     DECIMAL (18, 2),
    GiaVonBinhQuan DECIMAL (18, 6),
    TyLeChenhLech  DECIMAL (7, 2) ,
    CanhBaoTyLe    BIT            ,
    TenNLDauVao    NVARCHAR (120) ,
    LuongDinhMuc   DECIMAL (18, 3),
    LuongThucDung  DECIMAL (18, 3),
    DonGiaVao      DECIMAL (18, 4));

INSERT  INTO @Batches
VALUES ('BATCH_SEED_01', N'Ủ Nước Cốt Trà Đen', N'Nước Cốt Trà Đen', 3000.000, 3000.000, 'DA_GHI_SO', N'Mẻ trà chuẩn', 65000.00, 21.666667, 0.00, 0, N'Trà Đen Cổ Thụ', 1.000, 1.000, 65000.00),
       ('BATCH_SEED_02', N'Ủ Nước Cốt Trà Đen', N'Nước Cốt Trà Đen', 3000.000, 2950.000, 'DA_GHI_SO', N'Bốc hơi khi ủ nóng', 65000.00, 22.033898, -1.67, 0, N'Trà Đen Cổ Thụ', 1.000, 1.000, 65000.00),
       ('BATCH_SEED_03', N'Ủ Nước Cốt Trà Ô Long', N'Nước Cốt Trà Ô Long', 3000.000, 3000.000, 'DA_GHI_SO', N'Mẻ ô long đạt chuẩn', 95000.00, 31.666667, 0.00, 0, N'Trà Ô Long Kim Tuyên', 1.000, 1.000, 95000.00),
       ('BATCH_SEED_04', N'Ủ Nước Cốt Trà Lài', N'Nước Cốt Trà Lài', 3500.000, 3450.000, 'DA_GHI_SO', N'Hao hụt lọc bã trà', 85000.00, 24.637681, -1.43, 0, N'Trà Lài Thượng Hạng', 1.000, 1.000, 85000.00),
       ('BATCH_SEED_05', N'Chiết Xuất Cà Phê Phin', N'Nước Cốt Cà Phê Pha Phin', 2500.000, 2480.000, 'DA_GHI_SO', N'Hút nước bã cà phê', 110000.00, 44.354839, -0.80, 0, N'Cà Phê Hạt Rang Mộc', 1.000, 1.000, 110000.00),
       ('BATCH_SEED_06', N'Nấu Trân Châu Đen Dẻo', N'Trân Châu Nấu Chín Dẻo', 2500.000, 2550.000, 'DA_GHI_SO', N'Trân châu nở tốt đạt độ dẻo', 28000.00, 10.980392, 2.00, 0, N'Trân Châu Thô Đen', 1.000, 1.000, 28000.00),
       ('BATCH_SEED_07', N'Nấu Trân Châu Đường Đen', N'Trân Châu Nấu Đường Đen', 2800.000, 2800.000, 'DA_GHI_SO', N'Trân châu rim đường đen bóng đẹp', 41500.00, 14.821429, 0.00, 0, N'Trân Châu Thô Đen', 1.000, 1.000, 28000.00),
       ('BATCH_SEED_08', N'Đánh Bông Kem Cheese', N'Sốt Kem Cheese Đánh Bông', 2000.000, 1980.000, 'DA_GHI_SO', N'Dính thành âu đánh kem', 92000.00, 46.464646, -1.00, 0, N'Bột Kem Cheese Anchor', 1.000, 1.000, 92000.00),
       ('BATCH_SEED_09', N'Nấu Pudding Trứng', N'Pudding Trứng Nấu Chín', 2500.000, 2500.000, 'DA_GHI_SO', N'Pudding đổ khuôn đạt chuẩn', 52000.00, 20.800000, 0.00, 0, N'Bột Pudding Trứng', 1.000, 1.000, 52000.00),
       ('BATCH_SEED_10', N'Nấu Nước Đường Si-rô', N'Nước Đường Pha Sẵn', 3000.000, 3000.000, 'DA_GHI_SO', N'Si-rô đường trong chuẩn độ ngọt', 55000.00, 18.333333, 0.00, 0, N'Đường Cát Trắng', 2.500, 2.500, 22000.00),
       ('BATCH_SEED_11', N'Nấu Trân Châu Đen Dẻo', N'Trân Châu Nấu Chín Dẻo', 2500.000, 2000.000, 'DA_GHI_SO', N'Bị khê đáy nồi phải gạt bỏ bớt', 28000.00, 14.000000, -20.00, 1, N'Trân Châu Thô Đen', 1.000, 1.000, 28000.00),
       ('BATCH_SEED_12', N'Ủ Nước Cốt Trà Đen', N'Nước Cốt Trà Đen', 3000.000, 3000.000, 'DA_GHI_SO', N'Mẻ trà đen ủ đạt chuẩn', 65000.00, 21.666667, 0.00, 0, N'Trà Đen Cổ Thụ', 1.000, 1.000, 65000.00);

DECLARE curBatch CURSOR
    FOR SELECT DISTINCT MaYeuCau,
                        TenCT,
                        TenNLDauRa,
                        LuongDuKien,
                        LuongThucThu,
                        TrangThai,
                        LyDoChenhLech,
                        TongChiPhi,
                        GiaVonBinhQuan,
                        TyLeChenhLech,
                        CanhBaoTyLe
        FROM   @Batches;

DECLARE @bYeuCau AS VARCHAR (64), @bTenCT AS NVARCHAR (120), @bTenDauRa AS NVARCHAR (120), @bDuKien AS DECIMAL (18, 3), @bThucThu AS DECIMAL (18, 3), @bTrangThai AS VARCHAR (12), @bLyDo AS NVARCHAR (500), @bTongChiPhi AS DECIMAL (18, 2), @bGiaVon AS DECIMAL (18, 6), @bTyLe AS DECIMAL (7, 2), @bCanhBao AS BIT;

OPEN curBatch;

FETCH NEXT FROM curBatch INTO @bYeuCau, @bTenCT, @bTenDauRa, @bDuKien, @bThucThu, @bTrangThai, @bLyDo, @bTongChiPhi, @bGiaVon, @bTyLe, @bCanhBao;

WHILE @@FETCH_STATUS = 0
    BEGIN
        DECLARE @ctId AS BIGINT = (SELECT MaCTSC
                                   FROM   dbo.CongThucSoChe
                                   WHERE  TenCT = @bTenCT);
        DECLARE @dauRaId AS BIGINT = (SELECT MaNL
                                      FROM   dbo.NguyenLieu
                                      WHERE  TenNL = @bTenDauRa);
        IF @ctId IS NOT NULL
           AND @dauRaId IS NOT NULL
            BEGIN
                IF NOT EXISTS (SELECT 1
                               FROM   dbo.MeSoChe
                               WHERE  MaYeuCau = @bYeuCau)
                    BEGIN
                        INSERT  INTO dbo.MeSoChe (
                            MaCTSC,
                            MaNLDauRa,
                            LuongDuKien,
                            LuongThucThu,
                            TrangThai,
                            LyDoChenhLech,
                            TongChiPhi,
                            GiaVonBinhQuan,
                            TyLeChenhLech,
                            CanhBaoTyLe,
                            NguoiLap,
                            GhiSoLuc,
                            MaYeuCau
                        )
                        VALUES                   (@ctId, @dauRaId, @bDuKien, @bThucThu, @bTrangThai, @bLyDo, @bTongChiPhi, @bGiaVon, @bTyLe, @bCanhBao, @kitchenId, CASE WHEN @bTrangThai = 'DA_GHI_SO' THEN DATEADD(HOUR, -6, SYSUTCDATETIME()) ELSE NULL END, @bYeuCau);
                    END
                DECLARE @maMe AS BIGINT = (SELECT MaMe
                                           FROM   dbo.MeSoChe
                                           WHERE  MaYeuCau = @bYeuCau);
                DELETE dbo.ChiTietMeSoChe
                WHERE  MaMe = @maMe;
                INSERT INTO dbo.ChiTietMeSoChe (
                    MaMe,
                    MaNL,
                    LuongDinhMuc,
                    LuongThucDung,
                    DonGiaVao,
                    ThanhTien
                )
                SELECT @maMe,
                       nl.MaNL,
                       b.LuongDinhMuc,
                       b.LuongThucDung,
                       b.DonGiaVao,
                       (b.LuongThucDung * b.DonGiaVao)
                FROM   @Batches AS b
                       INNER JOIN
                       dbo.NguyenLieu AS nl
                       ON nl.TenNL = b.TenNLDauVao
                WHERE  b.MaYeuCau = @bYeuCau;
            END
        FETCH NEXT FROM curBatch INTO @bYeuCau, @bTenCT, @bTenDauRa, @bDuKien, @bThucThu, @bTrangThai, @bLyDo, @bTongChiPhi, @bGiaVon, @bTyLe, @bCanhBao;
    END

CLOSE curBatch;

DEALLOCATE curBatch;

PRINT N'-- [10/16] Mẻ sơ chế: Đã nạp 12 mẻ sơ chế có ghi nhận chi phí và tỷ lệ chênh lệch.';


GO
-- ============================================================================
-- 11. ĐÃ BỎ PHIẾU XUẤT KHO (Thay thế bằng sơ chế mẻ tại bếp)
-- ============================================================================
-- ============================================================================
-- 12. BÀN ĂN & PHIÊN BÀN & GIỎ HÀNG PHIÊN (Ban, PhienBan, GioPhien, ChiTietGioPhien)
-- >= 12 Bàn ăn, gồm bàn TRONG và bàn CO_KHACH (gắn PhienBan OPEN và ChiTietGioPhien)
-- ============================================================================
DECLARE @cashierId AS BIGINT = (SELECT TOP 1 MaTK
                                FROM   dbo.TaiKhoan
                                WHERE  VaiTro = 'CASHIER');

IF @cashierId IS NULL
    SET @cashierId = (SELECT TOP 1 MaTK
                      FROM   dbo.TaiKhoan
                      WHERE  VaiTro = 'ADMIN');

-- 12 Bàn ăn
DECLARE @TableList TABLE (
    TenBan NVARCHAR (40),
    MaQR   VARCHAR (64) );

INSERT  INTO @TableList
VALUES (N'Bàn 01', 'QR_TABLE_01_TOKEN_A1B2C3D4'),
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

INSERT INTO dbo.Ban (
    TenBan,
    MaQR,
    TrangThai,
    HoatDong,
    Version
)
SELECT t.TenBan,
       t.MaQR,
       'TRONG',
       1,
       0
FROM   @TableList AS t
WHERE  NOT EXISTS (SELECT 1
                   FROM   dbo.Ban AS b
                   WHERE  b.TenBan = t.TenBan);

-- Thiết lập một số phiên bàn mẫu (gồm cả CLOSED trong quá khứ và OPEN hiện tại)
-- Mở phiên cho Bàn 01 (CO_KHACH), Bàn 02 (CO_KHACH), Bàn 03 (CO_KHACH)
DECLARE @ban1 AS BIGINT = (SELECT MaBan
                           FROM   dbo.Ban
                           WHERE  TenBan = N'Bàn 01');

DECLARE @ban2 AS BIGINT = (SELECT MaBan
                           FROM   dbo.Ban
                           WHERE  TenBan = N'Bàn 02');

DECLARE @ban3 AS BIGINT = (SELECT MaBan
                           FROM   dbo.Ban
                           WHERE  TenBan = N'Bàn 03');

-- Bàn 01: Phiên đang mở
IF @ban1 IS NOT NULL
   AND NOT EXISTS (SELECT 1
                   FROM   dbo.PhienBan
                   WHERE  MaBan = @ban1
                          AND TrangThai = 'OPEN')
    BEGIN
        INSERT  INTO dbo.PhienBan (
            MaBan,
            TrangThai,
            MoLuc,
            NguoiMo,
            Version
        )
        VALUES                    (@ban1, 'OPEN', DATEADD(MINUTE, -45, SYSUTCDATETIME()), @cashierId, 1);
        DECLARE @phien1 AS BIGINT = SCOPE_IDENTITY();
        UPDATE dbo.Ban
        SET    TrangThai     = 'CO_KHACH',
               MaPhienDangMo = @phien1
        WHERE  MaBan = @ban1;
        -- Giỏ hàng phiên
        INSERT  INTO dbo.GioPhien (
            MaPhien,
            Version,
            SuaLuc
        )
        VALUES                    (@phien1, 1, SYSUTCDATETIME());
        DECLARE @gio1 AS BIGINT = SCOPE_IDENTITY();
        DECLARE @monTSM AS BIGINT = (SELECT MaMon
                                     FROM   dbo.MonUong
                                     WHERE  TenMon = N'Trà Sữa Truyền Thống'
                                            AND Size = 'M');
        DECLARE @topTC AS BIGINT = (SELECT MaMon
                                    FROM   dbo.MonUong
                                    WHERE  TenMon = N'Trân Châu Đen Hoàng Kim'
                                           AND Size = 'M');
        IF @monTSM IS NOT NULL
            INSERT  INTO dbo.ChiTietGioPhien (
                MaGio,
                MaMon,
                SoLuong,
                GhiChu,
                ToppingIds
            )
            VALUES                           (@gio1, @monTSM, 2, N'Ít ngọt 50% đường', CAST (@topTC AS VARCHAR (20)));
    END

-- Bàn 02: Phiên đang mở
IF @ban2 IS NOT NULL
   AND NOT EXISTS (SELECT 1
                   FROM   dbo.PhienBan
                   WHERE  MaBan = @ban2
                          AND TrangThai = 'OPEN')
    BEGIN
        INSERT  INTO dbo.PhienBan (
            MaBan,
            TrangThai,
            MoLuc,
            NguoiMo,
            Version
        )
        VALUES                    (@ban2, 'OPEN', DATEADD(MINUTE, -20, SYSUTCDATETIME()), @cashierId, 1);
        DECLARE @phien2 AS BIGINT = SCOPE_IDENTITY();
        UPDATE dbo.Ban
        SET    TrangThai     = 'CO_KHACH',
               MaPhienDangMo = @phien2
        WHERE  MaBan = @ban2;
        INSERT  INTO dbo.GioPhien (
            MaPhien,
            Version,
            SuaLuc
        )
        VALUES                    (@phien2, 1, SYSUTCDATETIME());
        DECLARE @gio2 AS BIGINT = SCOPE_IDENTITY();
        DECLARE @monDaoM AS BIGINT = (SELECT MaMon
                                      FROM   dbo.MonUong
                                      WHERE  TenMon = N'Trà Đào Cam Sả'
                                             AND Size = 'M');
        IF @monDaoM IS NOT NULL
            INSERT  INTO dbo.ChiTietGioPhien (
                MaGio,
                MaMon,
                SoLuong,
                GhiChu,
                ToppingIds
            )
            VALUES                           (@gio2, @monDaoM, 1, N'Nhiều đá', NULL);
    END

-- Nạp thêm các phiên đã đóng trong lịch sử cho Bàn 04 .. Bàn 12 để có >= 10 phiên bàn
DECLARE @i AS INT = 4;

WHILE @i <= 12
    BEGIN
        DECLARE @tBanName AS NVARCHAR (40) = N'Bàn ' + RIGHT('0' + CAST (@i AS VARCHAR (2)), 2);
        DECLARE @bId AS BIGINT = (SELECT MaBan
                                  FROM   dbo.Ban
                                  WHERE  TenBan = @tBanName);
        IF @bId IS NOT NULL
           AND NOT EXISTS (SELECT 1
                           FROM   dbo.PhienBan
                           WHERE  MaBan = @bId)
            BEGIN
                INSERT  INTO dbo.PhienBan (
                    MaBan,
                    TrangThai,
                    MoLuc,
                    DongLuc,
                    NguoiMo,
                    NguoiDong,
                    Version
                )
                VALUES                    (@bId, 'CLOSED', DATEADD(HOUR, -(@i + 5), SYSUTCDATETIME()), DATEADD(HOUR, -(@i + 3), SYSUTCDATETIME()), @cashierId, @cashierId, 1);
            END
        SET @i = @i + 1;
    END

PRINT N'-- [12/16] Bàn ăn & Phiên bàn: Đã nạp 12 bàn ăn, thiết lập phiên OPEN và CLOSED.';


GO
SET ANSI_NULLS ON;

SET QUOTED_IDENTIFIER ON;

-- ============================================================================
-- 13. ĐƠN HÀNG, CHI TIẾT, HÓA ĐƠN, THANH TOÁN (DonHang, HoaDon, ThanhToan)
-- Phủ đủ 5 trạng thái: CHO_XAC_NHAN, CHO_CHE_BIEN, DANG_CHE_BIEN, HOAN_THANH, DA_HUY
-- ============================================================================
DECLARE @cashierId AS BIGINT = (SELECT TOP 1 MaTK
                                FROM   dbo.TaiKhoan
                                WHERE  VaiTro = 'CASHIER');

IF @cashierId IS NULL
    SET @cashierId = (SELECT TOP 1 MaTK
                      FROM   dbo.TaiKhoan
                      WHERE  VaiTro = 'ADMIN');

DECLARE @cust1 AS BIGINT = (SELECT MaTK
                            FROM   dbo.TaiKhoan
                            WHERE  Email = 'customer@milktea.vn');

DECLARE @cust2 AS BIGINT = (SELECT MaTK
                            FROM   dbo.TaiKhoan
                            WHERE  Email = 'customer2@milktea.vn');

DECLARE @phien1 AS BIGINT = (SELECT   TOP 1 MaPhien
                             FROM     dbo.PhienBan
                             WHERE    TrangThai = 'OPEN'
                             ORDER BY MaPhien ASC);

DECLARE @phienClosed AS BIGINT = (SELECT   TOP 1 MaPhien
                                  FROM     dbo.PhienBan
                                  WHERE    TrangThai = 'CLOSED'
                                  ORDER BY MaPhien ASC);

-- Các đơn hàng mẫu
DECLARE @OrderSeeds TABLE (
    MaYeuCau         VARCHAR (64)   ,
    Nguon            VARCHAR (10)   ,
    TrangThai        VARCHAR (24)   ,
    GhiChu           NVARCHAR (500) ,
    TenMon           NVARCHAR (120) ,
    Size             VARCHAR (12)   ,
    SoLuong          INT            ,
    HasTopping       BIT            ,
    TenTopping       NVARCHAR (120) ,
    LoaiGiam         VARCHAR (16)   ,
    TyLeGiam         DECIMAL (5, 2) ,
    GiaTriGiam       DECIMAL (18, 2),
    PhuongThucTT     VARCHAR (16)   ,
    DaThanhToan      BIT            ,
    DaBaoChuyenKhoan BIT            );

INSERT  INTO @OrderSeeds
VALUES -- 1. CHO_XAC_NHAN: Đơn tại bàn, khách đã bấm Báo Chuyển Khoan (transferNotified) -> chờ Thu Ngân bấm xác nhận
       ('DH_SEED_01', 'TABLE', 'CHO_XAC_NHAN', N'Khách bàn 1 vừa gửi đơn chuyển khoản', N'Trà Sữa Truyền Thống', 'M', 2, 1, N'Trân Châu Đen Hoàng Kim', 'PERCENT', 10.00, 10.00, 'BANK_TRANSFER', 0, 1),
       -- 2. CHO_XAC_NHAN: Đơn tại quầy, tiền mặt chưa thu
       ('DH_SEED_02', 'COUNTER', 'CHO_XAC_NHAN', N'Khách quầy gọi mang đi chờ trả tiền mặt', N'Trà Đào Cam Sả', 'L', 1, 1, N'Thạch Đào Giòn Ngọt', 'PERCENT', 0.00, 0.00, 'CASH', 0, 0),
       -- 3. CHO_CHE_BIEN: Thu ngân đã xác nhận, chuyển xuống bếp chờ nhận pha
       ('DH_SEED_03', 'TABLE', 'CHO_CHE_BIEN', N'Bàn 2 gọi thêm, đã xác nhận đơn', N'Trà Sữa Ô Long Nướng', 'M', 2, 0, NULL, 'FIXED_AMOUNT', 0.00, 10000.00, 'CASH', 1, 0),
       -- 4. CHO_CHE_BIEN: Đơn quầy thanh toán CK thành công, chờ bếp nhận
       ('DH_SEED_04', 'COUNTER', 'CHO_CHE_BIEN', N'Đơn mang đi đã thanh toán CK', N'Sữa Tươi Trân Châu Đường Đen', 'L', 1, 0, NULL, 'PERCENT', 10.00, 10.00, 'BANK_TRANSFER', 1, 1),
       -- 5. DANG_CHE_BIEN: Bếp đã bấm Bắt Đầu Pha Chế
       ('DH_SEED_05', 'TABLE', 'DANG_CHE_BIEN', N'Bếp đang pha chế cho bàn 1', N'Trà Sữa Lài Thơm', 'M', 2, 1, N'Kem Cheese Phô Mai Macchiato', 'PERCENT', 0.00, 0.00, 'BANK_TRANSFER', 1, 1),
       -- 6. DANG_CHE_BIEN: Bếp đang làm đơn mang đi
       ('DH_SEED_06', 'COUNTER', 'DANG_CHE_BIEN', N'Bếp đang pha chế đơn mang đi 06', N'Cà Phê Muối Xứ Huế', 'M', 2, 0, NULL, 'FIXED_AMOUNT', 0.00, 5000.00, 'CASH', 1, 0),
       -- 7. HOAN_THANH: Bếp đã hoàn thành toàn bộ món
       ('DH_SEED_07', 'TABLE', 'HOAN_THANH', N'Khách đã nhận đủ nước và dùng tại quán', N'Trà Ô Long Váng Sữa Macchiato', 'L', 2, 1, N'Trân Châu Trắng 3Q', 'PERCENT', 10.00, 10.00, 'BANK_TRANSFER', 1, 1),
       -- 8. HOAN_THANH: Đơn quầy hoàn tất
       ('DH_SEED_08', 'COUNTER', 'HOAN_THANH', N'Khách đã lấy nước mang đi', N'Matcha Đá Xay Kem Tuyết', 'L', 1, 1, N'Khúc Bạch Hạnh Nhân', 'PERCENT', 0.00, 0.00, 'CASH', 1, 0),
       -- 9. HOAN_THANH: Đơn lịch sử hôm qua
       ('DH_SEED_09', 'TABLE', 'HOAN_THANH', N'Đơn hàng ca tối hôm qua', N'Trà Sữa Socola Đậm Đà', 'L', 3, 1, N'Pudding Trứng Vàng', 'FIXED_AMOUNT', 0.00, 15000.00, 'BANK_TRANSFER', 1, 1),
       -- 10. DA_HUY: Khách đổi ý trước khi pha chế, đã hủy đơn
       ('DH_SEED_10', 'TABLE', 'DA_HUY', N'Khách bận đột xuất xin hủy đơn', N'Sinh Tố Bơ Dừa Thơm Béo', 'L', 2, 0, NULL, 'PERCENT', 0.00, 0.00, 'CASH', 0, 0),
       -- 11. DA_HUY: Đơn quầy nhập nhầm món
       ('DH_SEED_11', 'COUNTER', 'DA_HUY', N'Thu ngân nhập nhầm món khách đổi', N'Trà Bưởi Mật Ong', 'M', 1, 0, NULL, 'PERCENT', 0.00, 0.00, 'CASH', 0, 0),
       -- 12. HOAN_THANH: Đơn hàng hoàn tất ca sáng
       ('DH_SEED_12', 'COUNTER', 'HOAN_THANH', N'Khách quen mua mang đi ca sáng', N'Cà Phê Sữa Đá', 'M', 2, 0, NULL, 'PERCENT', 0.00, 0.00, 'CASH', 1, 0),
       -- 13. HOAN_THANH: Đơn hàng hoàn tất
       ('DH_SEED_13', 'TABLE', 'HOAN_THANH', N'Đơn hàng bàn 03', N'Trà Vải Hoa Hồng', 'M', 2, 0, NULL, 'PERCENT', 0.00, 0.00, 'BANK_TRANSFER', 1, 1),
       -- 14. HOAN_THANH: Đơn hàng hoàn tất
       ('DH_SEED_14', 'COUNTER', 'HOAN_THANH', N'Đơn mang đi trưa', N'Trà Xoài Chanh Dây', 'M', 2, 0, NULL, 'PERCENT', 0.00, 0.00, 'CASH', 1, 0),
       -- 15. HOAN_THANH: Đơn hàng hoàn tất
       ('DH_SEED_15', 'TABLE', 'HOAN_THANH', N'Đơn hàng bàn 05', N'Cà Phê Đen Đá', 'M', 2, 0, NULL, 'PERCENT', 0.00, 0.00, 'CASH', 1, 0),
       -- 16. HOAN_THANH: Đơn hàng hoàn tất
       ('DH_SEED_16', 'COUNTER', 'HOAN_THANH', N'Đơn mang đi chiều', N'Bạc Xỉu Ba Tầng', 'M', 2, 0, NULL, 'PERCENT', 0.00, 0.00, 'BANK_TRANSFER', 1, 1),
       -- 17. HOAN_THANH: Đơn hàng hoàn tất
       ('DH_SEED_17', 'TABLE', 'HOAN_THANH', N'Đơn hàng bàn 06', N'Trà Sữa Thái Xanh', 'M', 2, 0, NULL, 'PERCENT', 0.00, 0.00, 'CASH', 1, 0),
       -- 18. HOAN_THANH: Đơn hàng hoàn tất
       ('DH_SEED_18', 'COUNTER', 'HOAN_THANH', N'Đơn mang đi tối', N'Trà Sữa Thái Đỏ', 'M', 2, 0, NULL, 'PERCENT', 0.00, 0.00, 'BANK_TRANSFER', 1, 1);

DECLARE curOrder CURSOR
    FOR SELECT MaYeuCau,
               Nguon,
               TrangThai,
               GhiChu,
               TenMon,
               Size,
               SoLuong,
               HasTopping,
               TenTopping,
               LoaiGiam,
               TyLeGiam,
               GiaTriGiam,
               PhuongThucTT,
               DaThanhToan,
               DaBaoChuyenKhoan
        FROM   @OrderSeeds;

DECLARE @oYeuCau AS VARCHAR (64), @oNguon AS VARCHAR (10), @oTrangThai AS VARCHAR (24), @oGhiChu AS NVARCHAR (500), @oTenMon AS NVARCHAR (120), @oSize AS VARCHAR (12), @oSoLuong AS INT, @oHasTopping AS BIT, @oTenTopping AS NVARCHAR (120), @oLoaiGiam AS VARCHAR (16), @oTyLeGiam AS DECIMAL (5, 2), @oGiaTriGiam AS DECIMAL (18, 2), @oPhuongThucTT AS VARCHAR (16), @oDaTT AS BIT, @oDaBaoCK AS BIT;

OPEN curOrder;

FETCH NEXT FROM curOrder INTO @oYeuCau, @oNguon, @oTrangThai, @oGhiChu, @oTenMon, @oSize, @oSoLuong, @oHasTopping, @oTenTopping, @oLoaiGiam, @oTyLeGiam, @oGiaTriGiam, @oPhuongThucTT, @oDaTT, @oDaBaoCK;

WHILE @@FETCH_STATUS = 0
    BEGIN
        DECLARE @monId AS BIGINT = (SELECT MaMon
                                    FROM   dbo.MonUong
                                    WHERE  TenMon = @oTenMon
                                           AND Size = @oSize);
        DECLARE @giaMon AS DECIMAL (18, 2) = (SELECT Gia
                                              FROM   dbo.MonUong
                                              WHERE  MaMon = @monId);
        DECLARE @topId AS BIGINT = NULL;
        DECLARE @giaTop AS DECIMAL (18, 2) = 0;
        IF @oHasTopping = 1
           AND @oTenTopping IS NOT NULL
            BEGIN
                SELECT @topId = MaMon,
                       @giaTop = Gia
                FROM   dbo.MonUong
                WHERE  TenMon = @oTenTopping
                       AND Size = 'M';
            END
        IF @monId IS NOT NULL
            BEGIN
                IF NOT EXISTS (SELECT 1
                               FROM   dbo.DonHang
                               WHERE  MaYeuCau = @oYeuCau)
                    BEGIN
                        INSERT  INTO dbo.DonHang (
                            MaPhien,
                            MaTK,
                            Nguon,
                            TrangThai,
                            MaYeuCau,
                            GhiChu,
                            TaoLuc,
                            BatDauLuc,
                            HoanThanhLuc,
                            Version
                        )
                        VALUES                   (CASE WHEN @oNguon = 'TABLE' THEN @phien1 ELSE NULL END, @cust1, @oNguon, @oTrangThai, @oYeuCau, @oGhiChu, DATEADD(MINUTE, -30, SYSUTCDATETIME()), CASE WHEN @oTrangThai IN ('DANG_CHE_BIEN', 'HOAN_THANH') THEN DATEADD(MINUTE, -20, SYSUTCDATETIME()) ELSE NULL END, CASE WHEN @oTrangThai = 'HOAN_THANH' THEN DATEADD(MINUTE, -10, SYSUTCDATETIME()) ELSE NULL END, 0);
                    END
                DECLARE @maDH AS BIGINT = (SELECT MaDH
                                           FROM   dbo.DonHang
                                           WHERE  MaYeuCau = @oYeuCau);
                -- Chi tiết đơn hàng
                IF NOT EXISTS (SELECT 1
                               FROM   dbo.ChiTietDonHang
                               WHERE  MaDH = @maDH)
                    BEGIN
                        INSERT  INTO dbo.ChiTietDonHang (
                            MaDH,
                            MaMon,
                            TenMonChot,
                            SizeChot,
                            SoLuong,
                            DonGiaChot,
                            GhiChu
                        )
                        VALUES                          (@maDH, @monId, @oTenMon, @oSize, @oSoLuong, @giaMon, @oGhiChu);
                        DECLARE @maCTDH AS BIGINT = SCOPE_IDENTITY();
                        IF @topId IS NOT NULL
                            BEGIN
                                INSERT  INTO dbo.ChiTietDonHangTopping (
                                    MaChiTiet,
                                    MaMonTopping,
                                    TenToppingChot,
                                    DonGiaToppingChot
                                )
                                VALUES                                 (@maCTDH, @topId, @oTenTopping, @giaTop);
                            END
                    END
                -- Tính tổng tiền hóa đơn
                DECLARE @tongTien AS DECIMAL (18, 2) = (@giaMon + @giaTop) * @oSoLuong;
                DECLARE @tienGiam AS DECIMAL (18, 2) = 0;
                IF @oLoaiGiam = 'PERCENT'
                    SET @tienGiam = ROUND(@tongTien * (@oTyLeGiam / 100.0), 0);
                ELSE
                    SET @tienGiam = @oGiaTriGiam;
                IF @tienGiam > @tongTien
                    SET @tienGiam = @tongTien;
                DECLARE @phaiTra AS DECIMAL (18, 2) = @tongTien - @tienGiam;
                -- Hóa đơn
                IF NOT EXISTS (SELECT 1
                               FROM   dbo.HoaDon
                               WHERE  MaDH = @maDH)
                    BEGIN
                        INSERT  INTO dbo.HoaDon (
                            MaDH,
                            TongTien,
                            TienGiam,
                            PhaiTra,
                            TyLeGiamChot,
                            LoaiGiamChot,
                            GiaTriGiamChot,
                            TrangThai,
                            LapLuc
                        )
                        VALUES                  (@maDH, @tongTien, @tienGiam, @phaiTra, @oTyLeGiam, @oLoaiGiam, @oGiaTriGiam, CASE WHEN @oTrangThai = 'DA_HUY' THEN 'DA_HUY' ELSE 'HIEU_LUC' END, DATEADD(MINUTE, -28, SYSUTCDATETIME()));
                    END
                DECLARE @maHD AS BIGINT = (SELECT MaHD
                                           FROM   dbo.HoaDon
                                           WHERE  MaDH = @maDH);
                -- Thông báo thanh toán (nếu khách báo chuyển khoản)
                IF @oDaBaoCK = 1
                   AND NOT EXISTS (SELECT 1
                                   FROM   dbo.ThongBaoThanhToan
                                   WHERE  MaHD = @maHD)
                    BEGIN
                        INSERT  INTO dbo.ThongBaoThanhToan (
                            MaHD,
                            PhuongThuc,
                            ThamChieu,
                            TaoLuc,
                            MaYeuCau
                        )
                        VALUES                             (@maHD, 'BANK_TRANSFER', 'CUSTOMER_TRANSFERRED_' + CAST (@maHD AS VARCHAR (20)), DATEADD(MINUTE, -25, SYSUTCDATETIME()), 'TB_SEED_' + @oYeuCau);
                    END
                -- Bản ghi ThanhToan thực tế (nếu đã thanh toán)
                IF @oDaTT = 1
                   AND NOT EXISTS (SELECT 1
                                   FROM   dbo.ThanhToan
                                   WHERE  MaHD = @maHD)
                    BEGIN
                        INSERT  INTO dbo.ThanhToan (
                            MaHD,
                            PhuongThuc,
                            SoTien,
                            ThamChieu,
                            NguoiThu,
                            ThuLuc,
                            MaYeuCau
                        )
                        VALUES                     (@maHD, @oPhuongThucTT, @phaiTra, 'TT_REF_' + CAST (@maHD AS VARCHAR (20)), @cashierId, DATEADD(MINUTE, -22, SYSUTCDATETIME()), 'PAY_SEED_' + @oYeuCau);
                    END
            END
        FETCH NEXT FROM curOrder INTO @oYeuCau, @oNguon, @oTrangThai, @oGhiChu, @oTenMon, @oSize, @oSoLuong, @oHasTopping, @oTenTopping, @oLoaiGiam, @oTyLeGiam, @oGiaTriGiam, @oPhuongThucTT, @oDaTT, @oDaBaoCK;
    END

CLOSE curOrder;

DEALLOCATE curOrder;

PRINT N'-- [13/16] Đơn hàng, Hóa đơn & Thanh toán: Đã nạp 12 đơn phủ đủ 5 trạng thái.';


GO
-- ============================================================================
-- 14. YÊU CẦU HỦY & YÊU CẦU HOÀN TIỀN (YeuCauHuy, YeuCauHoan) - >= 10 Bản ghi mỗi loại
-- ============================================================================
DECLARE @adminId AS BIGINT = (SELECT TOP 1 MaTK
                              FROM   dbo.TaiKhoan
                              WHERE  VaiTro = 'ADMIN');

DECLARE @cashierId AS BIGINT = (SELECT TOP 1 MaTK
                                FROM   dbo.TaiKhoan
                                WHERE  VaiTro = 'CASHIER');

-- 14.1 Yêu cầu hủy đơn hàng (YeuCauHuy) - CHO, CHAP_THUAN, TU_CHOI
DECLARE @CancelOrders TABLE (
    MaYeuCau  VARCHAR (64)  ,
    LyDo      NVARCHAR (500),
    LyDoXuLy  NVARCHAR (500),
    TrangThai VARCHAR (16)  );

INSERT  INTO @CancelOrders
VALUES ('DH_SEED_10', N'Khách bận việc gia đình đột xuất', N'Đơn chưa pha chế, đồng ý hủy', 'CHAP_THUAN'),
       ('DH_SEED_11', N'Thu ngân bấm nhầm kích cỡ size L thành M', N'Xác nhận hủy tạo lại', 'CHAP_THUAN'),
       ('DH_SEED_01', N'Khách muốn đổi sang món khác', NULL, 'CHO'),
       ('DH_SEED_02', N'Khách quên mang tiền mặt muốn hủy', NULL, 'CHO'),
       ('DH_SEED_03', N'Khách đợi lâu xin hủy đơn', N'Bếp đã chuẩn bị xong trà, từ chối', 'TU_CHOI');

INSERT INTO dbo.YeuCauHuy (
    MaDH,
    LyDo,
    LyDoXuLy,
    TrangThai,
    TaoLuc,
    XuLyLuc,
    NguoiXuLy
)
SELECT dh.MaDH,
       c.LyDo,
       c.LyDoXuLy,
       c.TrangThai,
       DATEADD(MINUTE, -15, SYSUTCDATETIME()),
       CASE WHEN c.TrangThai != 'CHO' THEN DATEADD(MINUTE, -10, SYSUTCDATETIME()) ELSE NULL END,
       CASE WHEN c.TrangThai != 'CHO' THEN @cashierId ELSE NULL END
FROM   @CancelOrders AS c
       INNER JOIN
       dbo.DonHang AS dh
       ON dh.MaYeuCau = c.MaYeuCau
WHERE  NOT EXISTS (SELECT 1
                   FROM   dbo.YeuCauHuy AS yh
                   WHERE  yh.MaDH = dh.MaDH);

-- Bổ sung thêm bản ghi YeuCauHuy cho các đơn khác để đạt >= 10 bản ghi
DECLARE @extraOrders TABLE (
    MaDH      BIGINT        ,
    LyDo      NVARCHAR (500),
    TrangThai VARCHAR (16)  );

INSERT INTO @extraOrders
SELECT TOP 6 dh.MaDH,
             N'Yêu cầu hủy kiểm thử nghiệp vụ cho đơn ' + CAST (dh.MaDH AS NVARCHAR (20)),
             'CHO'
FROM   dbo.DonHang AS dh
WHERE  NOT EXISTS (SELECT 1
                   FROM   dbo.YeuCauHuy AS yh
                   WHERE  yh.MaDH = dh.MaDH);

INSERT INTO dbo.YeuCauHuy (
    MaDH,
    LyDo,
    TrangThai,
    TaoLuc
)
SELECT e.MaDH,
       e.LyDo,
       e.TrangThai,
       SYSUTCDATETIME()
FROM   @extraOrders AS e;

-- 14.2 Yêu cầu hoàn tiền (YeuCauHoan) - CHO_DUYET, DA_DUYET, DA_HOAN, TU_CHOI
-- Lấy các đơn đã có ThanhToan
DECLARE @RefundList TABLE (
    OrderMaYeuCau VARCHAR (64)  ,
    LyDo          NVARCHAR (500),
    LyDoDuyet     NVARCHAR (500),
    TrangThai     VARCHAR (16)  ,
    ThamChieuHoan NVARCHAR (120),
    HoanYeuCau    VARCHAR (64)  );

INSERT  INTO @RefundList
VALUES ('DH_SEED_05', N'Nước bị nhầm tỷ lệ đường khách yêu cầu hoàn tiền', N'Đã đối chiếu với bếp, duyệt hoàn tiền', 'DA_HOAN', 'REFUND_TXN_001', 'REFUND_REQ_01'),
       ('DH_SEED_06', N'Khách chuyển dư 50.000đ yêu cầu hoàn lại phần thừa', N'Đã xác minh sao kê ngân hàng', 'DA_DUYET', NULL, 'REFUND_REQ_02'),
       ('DH_SEED_07', N'Khách khiếu nại đá tan hết khi nhận', NULL, 'CHO_DUYET', NULL, 'REFUND_REQ_03'),
       ('DH_SEED_08', N'Khách yêu cầu hoàn tiền sau khi đã uống hết 80%', N'Không đúng chính sách hoàn trả', 'TU_CHOI', NULL, 'REFUND_REQ_04'),
       ('DH_SEED_03', N'Khách đổi ý thanh toán lại bằng thẻ', N'Duyệt hoàn tiền mặt', 'DA_HOAN', 'REFUND_TXN_002', 'REFUND_REQ_05'),
       ('DH_SEED_04', N'Khách chuyển khoản thừa tiền', N'Đồng ý hoàn tiền phần thừa', 'DA_DUYET', NULL, 'REFUND_REQ_06'),
       ('DH_SEED_09', N'Khách phản ánh nước nhạt vị', NULL, 'CHO_DUYET', NULL, 'REFUND_REQ_07'),
       ('DH_SEED_12', N'Khách chờ lâu xin hủy và hoàn tiền', N'Từ chối do khách đã nhận ly', 'TU_CHOI', NULL, 'REFUND_REQ_08'),
       ('DH_SEED_13', N'Khách nhầm hóa đơn bàn khác', N'Đã kiểm tra đối soát, hoàn tiền', 'DA_HOAN', 'REFUND_TXN_003', 'REFUND_REQ_09'),
       ('DH_SEED_14', N'Khách hủy đơn mang về trước khi làm', N'Đồng ý duyệt hoàn trả', 'DA_DUYET', NULL, 'REFUND_REQ_10'),
       ('DH_SEED_15', N'Khách báo lỗi giao dịch chuyển khoản', NULL, 'CHO_DUYET', NULL, 'REFUND_REQ_11');

INSERT INTO dbo.YeuCauHoan (
    MaDH,
    MaTT,
    SoTien,
    LyDo,
    LyDoDuyet,
    TrangThai,
    NguoiDuyet,
    NguoiHoan,
    DuyetLuc,
    HoanLuc,
    ThamChieuHoan,
    MaYeuCau
)
SELECT dh.MaDH,
       tt.MaTT,
       tt.SoTien,
       r.LyDo,
       r.LyDoDuyet,
       r.TrangThai,
       CASE WHEN r.TrangThai != 'CHO_DUYET' THEN @adminId ELSE NULL END,
       CASE WHEN r.TrangThai = 'DA_HOAN' THEN @cashierId ELSE NULL END,
       CASE WHEN r.TrangThai != 'CHO_DUYET' THEN DATEADD(MINUTE, -8, SYSUTCDATETIME()) ELSE NULL END,
       CASE WHEN r.TrangThai = 'DA_HOAN' THEN DATEADD(MINUTE, -5, SYSUTCDATETIME()) ELSE NULL END,
       r.ThamChieuHoan,
       r.HoanYeuCau
FROM   @RefundList AS r
       INNER JOIN
       dbo.DonHang AS dh
       ON dh.MaYeuCau = r.OrderMaYeuCau
       INNER JOIN
       dbo.HoaDon AS hd
       ON hd.MaDH = dh.MaDH
       INNER JOIN
       dbo.ThanhToan AS tt
       ON tt.MaHD = hd.MaHD
WHERE  NOT EXISTS (SELECT 1
                   FROM   dbo.YeuCauHoan AS yh
                   WHERE  yh.MaDH = dh.MaDH);

PRINT N'-- [14/16] Yêu cầu hủy & hoàn tiền: Đã nạp đầy đủ các trạng thái phê duyệt.';


GO
-- ============================================================================
-- 15. KẾT CA KIỂM KÊ KHO & CHI TIẾT HAO HỤT (KetCaKho, ChiTietKetCaKho) - >= 10 Bản ghi
-- ============================================================================
DECLARE @kitchenId AS BIGINT = (SELECT TOP 1 MaTK
                                FROM   dbo.TaiKhoan
                                WHERE  VaiTro = 'KITCHEN');

IF @kitchenId IS NULL
    SET @kitchenId = (SELECT TOP 1 MaTK
                      FROM   dbo.TaiKhoan
                      WHERE  VaiTro = 'ADMIN');

DECLARE @ShiftList TABLE (
    MaYeuCau         VARCHAR (64)   ,
    MaCa             VARCHAR (64)   ,
    ViTri            VARCHAR (8)    ,
    GhiChu           NVARCHAR (500) ,
    TongChiPhiHaoHut DECIMAL (18, 2),
    TenNL            NVARCHAR (120) ,
    DonVi            VARCHAR (16)   ,
    TonSo            DECIMAL (18, 3),
    TonThucTe        DECIMAL (18, 3),
    LuongLoaiBo      DECIMAL (18, 3),
    LuongGiuLai      DECIMAL (18, 3),
    ChenhLech        DECIMAL (18, 3),
    GiaVon           DECIMAL (18, 6),
    ChiPhiHaoHut     DECIMAL (18, 2),
    LyDo             NVARCHAR (500) );

INSERT  INTO @ShiftList
VALUES ('KETCA_SEED_01', 'CA_SANG_01', 'BEP', N'Kết ca sáng quầy pha chế đạt chuẩn', 21667.00, N'Nước Cốt Trà Đen', 'ml', 7000.000, 6000.000, 1000.000, 6000.000, -1000.000, 21.666667, 21667.00, N'Hủy cốt trà quá 4 tiếng theo quy chuẩn'),
       ('KETCA_SEED_02', 'CA_CHIEU_01', 'BEP', N'Kết ca chiều nguyên liệu trân châu dôi dư ít', 10980.00, N'Trân Châu Nấu Chín Dẻo', 'g', 3500.000, 2500.000, 1000.000, 2500.000, -1000.000, 10.980392, 10980.00, N'Trân châu qua ca bị lại gạo phải hủy'),
       ('KETCA_SEED_03', 'CA_TOI_01', 'BEP', N'Kết ca tối dọn dẹp quầy bar', 46465.00, N'Sốt Kem Cheese Đánh Bông', 'ml', 2500.000, 1500.000, 1000.000, 1500.000, -1000.000, 46.464646, 46465.00, N'Hết hạn lưu trữ trong ngày'),
       ('KETCA_SEED_04', 'CA_SANG_02', 'BEP', N'Kiểm kê cốt trà lài ca sáng', 24638.00, N'Nước Cốt Trà Lài', 'ml', 3000.000, 2000.000, 1000.000, 2000.000, -1000.000, 24.637681, 24638.00, N'Hủy cốt trà cũ pha cốt mới'),
       ('KETCA_SEED_05', 'CA_SANG_01', 'KHO', N'Kiểm kê tổng kho đầu tuần', 52000.00, N'Cam Vàng Tươi', 'kg', 10.000, 8.000, 2.000, 8.000, -2.000, 26000.000000, 52000.00, N'Cam bị dập úng khi vận chuyển'),
       ('KETCA_SEED_06', 'CA_CHIEU_02', 'BEP', N'Kết ca chiều cà phê', 44355.00, N'Nước Cốt Cà Phê Pha Phin', 'ml', 2000.000, 1000.000, 1000.000, 1000.000, -1000.000, 44.354839, 44355.00, N'Hủy cà phê quá giờ'),
       ('KETCA_SEED_07', 'CA_TOI_02', 'BEP', N'Kết ca tối pudding trứng', 20800.00, N'Pudding Trứng Nấu Chín', 'g', 1500.000, 500.000, 1000.000, 500.000, -1000.000, 20.800000, 20800.00, N'Pudding vỡ nát khi múc'),
       ('KETCA_SEED_08', 'CA_SANG_02', 'KHO', N'Kiểm kê kho dâu tây', 75000.00, N'Dâu Tây Đà Lạt', 'kg', 5.000, 4.000, 1.000, 4.000, -1.000, 75000.000000, 75000.00, N'Dâu bị mềm chín quá độ'),
       ('KETCA_SEED_09', 'CA_CHIEU_03', 'BEP', N'Kiểm kê trân châu đường đen', 14821.00, N'Trân Châu Nấu Đường Đen', 'g', 2000.000, 1000.000, 1000.000, 1000.000, -1000.000, 14.821429, 14821.00, N'Hủy trân châu thừa cuối ca'),
       ('KETCA_SEED_10', 'CA_TOI_03', 'KHO', N'Kiểm kê ly nhựa cuối tuần', 4800.00, N'Ly Nhựa PP 500ml', 'cai', 2000.000, 1990.000, 10.000, 1990.000, -10.000, 480.000000, 4800.00, N'Ly bị bẹp trong thùng carton');

DECLARE curShift CURSOR
    FOR SELECT DISTINCT MaYeuCau,
                        MaCa,
                        ViTri,
                        GhiChu,
                        TongChiPhiHaoHut
        FROM   @ShiftList;

DECLARE @sYeuCau AS VARCHAR (64), @sMaCa AS VARCHAR (64), @sViTri AS VARCHAR (8), @sGhiChu AS NVARCHAR (500), @sTongChiPhi AS DECIMAL (18, 2);

OPEN curShift;

FETCH NEXT FROM curShift INTO @sYeuCau, @sMaCa, @sViTri, @sGhiChu, @sTongChiPhi;

WHILE @@FETCH_STATUS = 0
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM   dbo.KetCaKho
                       WHERE  MaYeuCau = @sYeuCau)
            BEGIN
                INSERT  INTO dbo.KetCaKho (
                    MaCa,
                    ViTri,
                    NguoiChot,
                    ChotLuc,
                    TuLuc,
                    DenLuc,
                    TongChiPhiHaoHut,
                    GhiChu,
                    MaYeuCau
                )
                VALUES                    (@sMaCa, @sViTri, @kitchenId, DATEADD(HOUR, -2, SYSUTCDATETIME()), DATEADD(HOUR, -10, SYSUTCDATETIME()), DATEADD(HOUR, -2, SYSUTCDATETIME()), @sTongChiPhi, @sGhiChu, @sYeuCau);
            END
        DECLARE @maKetCa AS BIGINT = (SELECT MaKetCa
                                      FROM   dbo.KetCaKho
                                      WHERE  MaYeuCau = @sYeuCau);
        DELETE dbo.ChiTietKetCaKho
        WHERE  MaKetCa = @maKetCa;
        INSERT INTO dbo.ChiTietKetCaKho (
            MaKetCa,
            MaNL,
            DonVi,
            TonSo,
            TonThucTe,
            LuongLoaiBo,
            LuongGiuLai,
            ChenhLechKiemKe,
            GiaVonDonVi,
            ChiPhiHaoHut,
            LyDo
        )
        SELECT @maKetCa,
               nl.MaNL,
               s.DonVi,
               s.TonSo,
               s.TonThucTe,
               s.LuongLoaiBo,
               s.LuongGiuLai,
               s.ChenhLech,
               s.GiaVon,
               s.ChiPhiHaoHut,
               s.LyDo
        FROM   @ShiftList AS s
               INNER JOIN
               dbo.NguyenLieu AS nl
               ON nl.TenNL = s.TenNL
        WHERE  s.MaYeuCau = @sYeuCau;
        FETCH NEXT FROM curShift INTO @sYeuCau, @sMaCa, @sViTri, @sGhiChu, @sTongChiPhi;
    END

CLOSE curShift;

DEALLOCATE curShift;

PRINT N'-- [15/16] Kết ca kiểm kê: Đã nạp 10 bản ghi kết ca kho và chi tiết hao hụt.';


GO
-- ============================================================================
-- 16. NHẬT KÝ NGHIỆP VỤ & LỊCH SỬ BIẾN ĐỘNG KHO (NhatKyNghiepVu, LichSuKho)
-- ============================================================================
DECLARE @adminId AS BIGINT = (SELECT TOP 1 MaTK
                              FROM   dbo.TaiKhoan
                              WHERE  VaiTro = 'ADMIN');

DECLARE @cashierId AS BIGINT = (SELECT TOP 1 MaTK
                                FROM   dbo.TaiKhoan
                                WHERE  VaiTro = 'CASHIER');

DECLARE @kitchenId AS BIGINT = (SELECT TOP 1 MaTK
                                FROM   dbo.TaiKhoan
                                WHERE  VaiTro = 'KITCHEN');

DECLARE @nlTraDen AS BIGINT = (SELECT MaNL
                               FROM   dbo.NguyenLieu
                               WHERE  TenNL = N'Trà Đen Cổ Thụ');

DECLARE @nlCotTra AS BIGINT = (SELECT MaNL
                               FROM   dbo.NguyenLieu
                               WHERE  TenNL = N'Nước Cốt Trà Đen');

-- 16.1 Lịch sử kho (LichSuKho) - >= 10 bản ghi
IF (SELECT COUNT(*)
    FROM   dbo.LichSuKho) < 15
    BEGIN
        INSERT  INTO dbo.LichSuKho (
            MaNL,
            ViTri,
            Loai,
            LuongBienDong,
            TonTruoc,
            TonSau,
            NguonLoai,
            NguonMa,
            NguoiLap,
            TaoLuc,
            LyDo
        )
        VALUES                     (@nlTraDen, 'KHO', 'NHAP', 100.000, 0.000, 100.000, 'IMPORT', 1, @adminId, DATEADD(DAY, -5, SYSUTCDATETIME()), N'Nhập kho đầu kỳ'),
                                   (@nlTraDen, 'BEP', 'SOCHE_VAO', -1.000, 5.000, 4.000, 'BATCH', 1, @kitchenId, DATEADD(HOUR, -6, SYSUTCDATETIME()), N'Ủ mẻ nước cốt trà 01'),
                                   (@nlCotTra, 'BEP', 'SOCHE_RA', 3000.000, 0.000, 3000.000, 'BATCH', 1, @kitchenId, DATEADD(HOUR, -6, SYSUTCDATETIME()), N'Thu được 3000ml cốt trà'),
                                   (@nlCotTra, 'BEP', 'TIEU_HAO', -150.000, 3000.000, 2850.000, 'ORDER', 1, @kitchenId, DATEADD(MINUTE, -30, SYSUTCDATETIME()), N'Pha chế đơn DH_SEED_01'),
                                   (@nlCotTra, 'BEP', 'TIEU_HAO', -200.000, 2850.000, 2650.000, 'ORDER', 2, @kitchenId, DATEADD(MINUTE, -20, SYSUTCDATETIME()), N'Pha chế đơn DH_SEED_02'),
                                   (@nlCotTra, 'BEP', 'HAO_HUT', -50.000, 2650.000, 2600.000, 'WASTE', 1, @kitchenId, DATEADD(MINUTE, -15, SYSUTCDATETIME()), N'Đổ bỏ cốt trà tráng ca'),
                                   (@nlTraDen, 'KHO', 'NHAP', 50.000, 95.000, 145.000, 'IMPORT', 2, @adminId, DATEADD(DAY, -2, SYSUTCDATETIME()), N'Nhập bổ sung'),
                                   (@nlCotTra, 'BEP', 'SOCHE_RA', 3000.000, 2600.000, 5600.000, 'BATCH', 2, @kitchenId, DATEADD(HOUR, -1, SYSUTCDATETIME()), N'Thu mẻ trà mới'),
                                   (@nlCotTra, 'BEP', 'TIEU_HAO', -150.000, 5600.000, 5450.000, 'ORDER', 3, @kitchenId, DATEADD(MINUTE, -10, SYSUTCDATETIME()), N'Pha chế đơn ca trưa'),
                                   (@nlCotTra, 'BEP', 'TIEU_HAO', -200.000, 5450.000, 5250.000, 'ORDER', 4, @kitchenId, DATEADD(MINUTE, -5, SYSUTCDATETIME()), N'Pha chế đơn ca chiều'),
                                   (@nlCotTra, 'BEP', 'HAO_HUT', -100.000, 5250.000, 5150.000, 'WASTE', 2, @kitchenId, DATEADD(MINUTE, -2, SYSUTCDATETIME()), N'Hao hụt kiểm kê cuối ca');
    END

-- 16.2 Nhật ký nghiệp vụ (NhatKyNghiepVu) - >= 10 bản ghi
IF NOT EXISTS (SELECT 1
               FROM   dbo.NhatKyNghiepVu)
    BEGIN
        INSERT  INTO dbo.NhatKyNghiepVu (
            MaTK,
            HanhDong,
            Truoc,
            Sau,
            LyDo,
            TaoLuc
        )
        VALUES                          (@adminId, 'CAP_NHAT_HE_THONG', N'TyLeGiam=0', N'LoaiGiam=PERCENT, GiaTriGiam=10', N'Thiết lập chương trình khuyến mãi tháng', DATEADD(DAY, -5, SYSUTCDATETIME())),
                                        (@cashierId, 'MO_PHIEN_BAN', N'TRONG', N'OPEN', N'Mở bàn 01 đón đoàn khách 4 người', DATEADD(MINUTE, -50, SYSUTCDATETIME())),
                                        (@cashierId, 'XAC_NHAN_DON', N'CHO_XAC_NHAN', N'CHO_CHE_BIEN', N'Xác nhận đơn hàng bàn 01', DATEADD(MINUTE, -40, SYSUTCDATETIME())),
                                        (@kitchenId, 'BAT_DAU_CHE_BIEN', N'CHO_CHE_BIEN', N'DANG_CHE_BIEN', N'Bếp nhận làm đơn DH_SEED_01', DATEADD(MINUTE, -35, SYSUTCDATETIME())),
                                        (@kitchenId, 'HOAN_THANH_DON', N'DANG_CHE_BIEN', N'HOAN_THANH', N'Pha chế xong 2 ly trà sữa', DATEADD(MINUTE, -25, SYSUTCDATETIME())),
                                        (@cashierId, 'THU_TIEN', N'Chưa thu', N'Đã thanh toán', N'Khách quét QR Vietcombank thành công', DATEADD(MINUTE, -22, SYSUTCDATETIME())),
                                        (@kitchenId, 'GHI_SO_ME_SO_CHE', N'NHAP', N'DA_GHI_SO', N'Ghi sổ mẻ ủ nước cốt trà đen 3000ml', DATEADD(HOUR, -3, SYSUTCDATETIME())),
                                        (@kitchenId, 'CHOT_KET_CA', N'Chưa chốt', N'Đã chốt', N'Hoàn tất kiểm kê cuối ca sáng', DATEADD(HOUR, -2, SYSUTCDATETIME())),
                                        (@adminId, 'DUYET_HOAN_TIEN', N'CHO_DUYET', N'DA_HOAN', N'Duyệt hoàn tiền cho đơn đổi trả DH_SEED_05', DATEADD(MINUTE, -10, SYSUTCDATETIME())),
                                        (@cashierId, 'DONG_PHIEN_BAN', N'OPEN', N'CLOSED', N'Khách thanh toán ra về bàn 04', DATEADD(HOUR, -4, SYSUTCDATETIME()));
    END

PRINT N'-- [16/16] Nhật ký & Biến động kho: Đã nạp đầy đủ audit log.';


GO
PRINT N'============================================================================';

PRINT N'>>> HOÀN TẤT NẠP BỘ DỮ LIỆU DEMO MILKTEA MANAGEMENT SYSTEM!';

PRINT N'============================================================================';