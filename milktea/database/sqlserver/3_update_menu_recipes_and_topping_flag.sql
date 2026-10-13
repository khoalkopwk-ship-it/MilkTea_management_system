-- ============================================================================
-- Script cập nhật DB: Thêm ChoPhepTopping vào DanhMuc & Bổ sung công thức 70 món
-- ============================================================================
USE milktea;
GO

-- 1. Thêm cột ChoPhepTopping vào DanhMuc nếu chưa có
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'DanhMuc' AND COLUMN_NAME = 'ChoPhepTopping'
)
BEGIN
    ALTER TABLE dbo.DanhMuc ADD ChoPhepTopping BIT NOT NULL CONSTRAINT DF_DanhMuc_ChoPhepTopping DEFAULT 1;
    PRINT N'-- Đã thêm cột ChoPhepTopping vào bảng DanhMuc.';
END
GO

-- Danh mục Topping Cao Cấp (MaDM = 11 hoặc tên 'Topping Cao Cấp') tắt ChoPhepTopping
UPDATE dbo.DanhMuc 
SET ChoPhepTopping = 0 
WHERE TenDM LIKE N'%Topping%' OR MaDM = 11;

UPDATE dbo.DanhMuc 
SET ChoPhepTopping = 1 
WHERE MaDM <> 11 AND TenDM NOT LIKE N'%Topping%';

PRINT N'-- Đã cập nhật cấu hình ChoPhepTopping cho các danh mục.';
GO

-- 2. Bổ sung công thức cho toàn bộ menu (70 món)
-- Bảng tạm chứa công thức định mức cho tất cả các món cần bổ sung
DECLARE @AllRecipes TABLE (
    TenMon NVARCHAR(120), Size VARCHAR(12), TenNL NVARCHAR(120), DinhLuong DECIMAL(18,3)
);

INSERT INTO @AllRecipes VALUES
    -- === 1. TRÀ SỮA TRUYỀN THỐNG (Size M & L đã có, kiểm tra bổ sung) ===
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

    -- === 2. TRÀ SỮA THÁI XANH ===
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

    -- === 3. TRÀ SỮA THÁI ĐỎ ===
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

    -- === 4. TRÀ SỮA SOCOLA ĐẬM ĐÀ ===
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

    -- === 5. TRÀ SỮA KHOAI MÔN HOÀNG KIM ===
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

    -- === 6. TRÀ SỮA Ô LONG NƯỚNG ===
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

    -- === 7. TRÀ SỮA Ô LONG THIẾT QUAN ÂM ===
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

    -- === 8. TRÀ SỮA LÀI THƠM ===
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

    -- === 9. TRÀ SỮA LÀI KEM CHEESE ===
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

    -- === 10. TRÀ SỮA MATCHA NHẬT BẢN ===
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

    -- === 11. TRÀ ĐÀO CAM SẢ ===
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

    -- === 12. TRÀ VẢI HOA HỒNG ===
    (N'Trà Vải Hoa Hồng', 'M', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Vải Hoa Hồng', 'M', N'Nước Đường Pha Sẵn', 30.000),
    (N'Trà Vải Hoa Hồng', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Vải Hoa Hồng', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Trà Vải Hoa Hồng', 'L', N'Nước Cốt Trà Lài', 200.000),
    (N'Trà Vải Hoa Hồng', 'L', N'Nước Đường Pha Sẵn', 40.000),
    (N'Trà Vải Hoa Hồng', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Vải Hoa Hồng', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 13. TRÀ DÂU TÂY NHIỆT ĐỚI ===
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

    -- === 14. TRÀ XOÀI CHANH DÂY ===
    (N'Trà Xoài Chanh Dây', 'M', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Xoài Chanh Dây', 'M', N'Nước Đường Pha Sẵn', 30.000),
    (N'Trà Xoài Chanh Dây', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Xoài Chanh Dây', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Trà Xoài Chanh Dây', 'L', N'Nước Cốt Trà Lài', 200.000),
    (N'Trà Xoài Chanh Dây', 'L', N'Nước Đường Pha Sẵn', 40.000),
    (N'Trà Xoài Chanh Dây', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Xoài Chanh Dây', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 15. TRÀ ỔI HỒNG HẠT CHIA ===
    (N'Trà Ổi Hồng Hạt Chia', 'M', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Ổi Hồng Hạt Chia', 'M', N'Nước Đường Pha Sẵn', 30.000),
    (N'Trà Ổi Hồng Hạt Chia', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Ổi Hồng Hạt Chia', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Trà Ổi Hồng Hạt Chia', 'L', N'Nước Cốt Trà Lài', 200.000),
    (N'Trà Ổi Hồng Hạt Chia', 'L', N'Nước Đường Pha Sẵn', 40.000),
    (N'Trà Ổi Hồng Hạt Chia', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Ổi Hồng Hạt Chia', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 16. TRÀ TÁO XANH BẠC HÀ ===
    (N'Trà Táo Xanh Bạc Hà', 'M', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Táo Xanh Bạc Hà', 'M', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Táo Xanh Bạc Hà', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Táo Xanh Bạc Hà', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Trà Táo Xanh Bạc Hà', 'L', N'Nước Cốt Trà Lài', 200.000),
    (N'Trà Táo Xanh Bạc Hà', 'L', N'Nước Đường Pha Sẵn', 35.000),
    (N'Trà Táo Xanh Bạc Hà', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Táo Xanh Bạc Hà', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 17. TRÀ BƯỞI MẬT ONG ===
    (N'Trà Bưởi Mật Ong', 'M', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Bưởi Mật Ong', 'M', N'Nước Đường Pha Sẵn', 30.000),
    (N'Trà Bưởi Mật Ong', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Bưởi Mật Ong', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Trà Bưởi Mật Ong', 'L', N'Nước Cốt Trà Lài', 200.000),
    (N'Trà Bưởi Mật Ong', 'L', N'Nước Đường Pha Sẵn', 40.000),
    (N'Trà Bưởi Mật Ong', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Bưởi Mật Ong', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 18. TRÀ MÃNG CẦU XIÊM ===
    (N'Trà Mãng Cầu Xiêm', 'M', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Mãng Cầu Xiêm', 'M', N'Nước Đường Pha Sẵn', 30.000),
    (N'Trà Mãng Cầu Xiêm', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Mãng Cầu Xiêm', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Trà Mãng Cầu Xiêm', 'L', N'Nước Cốt Trà Lài', 200.000),
    (N'Trà Mãng Cầu Xiêm', 'L', N'Nước Đường Pha Sẵn', 40.000),
    (N'Trà Mãng Cầu Xiêm', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Mãng Cầu Xiêm', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 19. CÀ PHÊ ĐEN ĐÁ ===
    (N'Cà Phê Đen Đá', 'M', N'Nước Cốt Cà Phê Pha Phin', 60.000),
    (N'Cà Phê Đen Đá', 'M', N'Đường Cát Trắng', 0.020),
    (N'Cà Phê Đen Đá', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Cà Phê Đen Đá', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Cà Phê Đen Đá', 'L', N'Nước Cốt Cà Phê Pha Phin', 90.000),
    (N'Cà Phê Đen Đá', 'L', N'Đường Cát Trắng', 0.030),
    (N'Cà Phê Đen Đá', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Cà Phê Đen Đá', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 20. CÀ PHÊ SỮA ĐÁ (Size M đã có, thêm L) ===
    (N'Cà Phê Sữa Đá', 'M', N'Nước Cốt Cà Phê Pha Phin', 60.000),
    (N'Cà Phê Sữa Đá', 'M', N'Sữa Đặc Có Đường', 0.040),
    (N'Cà Phê Sữa Đá', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Cà Phê Sữa Đá', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Cà Phê Sữa Đá', 'L', N'Nước Cốt Cà Phê Pha Phin', 90.000),
    (N'Cà Phê Sữa Đá', 'L', N'Sữa Đặc Có Đường', 0.055),
    (N'Cà Phê Sữa Đá', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Cà Phê Sữa Đá', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 21. CÀ PHÊ MUỐI XỨ HUẾ ===
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

    -- === 22. BẠC XỈU BA TẦNG ===
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

    -- === 23. SỮA TƯƠI TRÂN CHÂU ĐƯỜNG ĐEN (Size M đã có, thêm L) ===
    (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Sữa Tươi Thanh Trùng', 0.200),
    (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Trân Châu Nấu Đường Đen', 60.000),
    (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Sữa Tươi Thanh Trùng', 0.280),
    (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Trân Châu Nấu Đường Đen', 80.000),
    (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 24. SỮA TƯƠI SƯƠNG SÁO THẢO MỘC ===
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

    -- === 25. TRÀ Ô LONG VÁNG SỮA MACCHIATO (Size M đã có, thêm L) ===
    (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Nước Cốt Trà Ô Long', 150.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Sốt Kem Cheese Đánh Bông', 50.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Nước Cốt Trà Ô Long', 200.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Sốt Kem Cheese Đánh Bông', 70.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 26. TRÀ ĐEN MACCHIATO MUỐI BIỂN ===
    (N'Trà Đen Macchiato Muối Biển', 'M', N'Nước Cốt Trà Đen', 150.000),
    (N'Trà Đen Macchiato Muối Biển', 'M', N'Sốt Kem Cheese Đánh Bông', 50.000),
    (N'Trà Đen Macchiato Muối Biển', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Đen Macchiato Muối Biển', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Trà Đen Macchiato Muối Biển', 'L', N'Nước Cốt Trà Đen', 200.000),
    (N'Trà Đen Macchiato Muối Biển', 'L', N'Sốt Kem Cheese Đánh Bông', 70.000),
    (N'Trà Đen Macchiato Muối Biển', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Đen Macchiato Muối Biển', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 27. MATCHA ĐÁ XAY KEM TUYẾT ===
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

    -- === 28. CÀ PHÊ ĐÁ XAY CARAMEL ===
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

    -- === 29. SINH TỐ BƠ DỪA THƠM BÉO ===
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

    -- === 30. TRÀ HOA CÚC MẬT ONG HẠT SEN ===
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Ống Hút Trân Châu', 1.000),

    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Nước Cốt Trà Lài', 200.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Nước Đường Pha Sẵn', 35.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Ống Hút Trân Châu', 1.000),

    -- === 10 LOẠI TOPPING CAO CẤP (Size M chuẩn) ===
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

-- Nạp vào CongThucMon: Chỉ thêm nếu chưa có (không ghi đè công thức hợp lệ đã có)
MERGE dbo.CongThucMon AS target
USING (
    SELECT mu.MaMon, nl.MaNL, r.DinhLuong
    FROM @AllRecipes r
    JOIN dbo.MonUong mu ON mu.TenMon = r.TenMon AND mu.Size = r.Size
    JOIN dbo.NguyenLieu nl ON nl.TenNL = r.TenNL
) AS source
ON (target.MaMon = source.MaMon AND target.MaNL = source.MaNL)
WHEN NOT MATCHED THEN
    INSERT (MaMon, MaNL, DinhLuong)
    VALUES (source.MaMon, source.MaNL, source.DinhLuong);

PRINT N'-- Đã nạp đầy đủ công thức cho 70 món trong menu!';
GO
