-- ============================================================================
-- V6__in_store_only_cleanup.sql
-- Chuẩn hóa hệ thống phục vụ tại quán (In-Store Only System):
-- 1. Chỉ chấp nhận nguồn đơn TABLE (quét QR tại bàn) và COUNTER (gọi tại quầy)
-- 2. Gỡ bỏ ràng buộc giá trị mặc định giao nhận DF_DonHang_HinhThucNhan
-- 3. Lưu giữ 4 cột giao nhận cũ (NguoiNhan, SoDienThoaiNhan, DiaChiGiao, HinhThucNhan)
--    và enum giá trị 'ONLINE' ở chế độ legacy read-only để bảo toàn 100% dữ liệu lịch sử
-- ============================================================================

SET NOCOUNT ON;

-- 1. Gỡ bỏ ràng buộc mặc định DF_DonHang_HinhThucNhan (không còn tự động gán 'PICKUP' cho đơn mới)
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_DonHang_HinhThucNhan')
BEGIN
    ALTER TABLE dbo.DonHang DROP CONSTRAINT DF_DonHang_HinhThucNhan;
    PRINT N'Đã gỡ bỏ ràng buộc mặc định DF_DonHang_HinhThucNhan trên DonHang';
END
GO

-- 2. Bổ sung chỉ mục tối ưu truy vấn lọc nguồn đơn tại quán (TABLE và COUNTER)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_DonHang_Nguon_TrangThai_TaoLuc' AND object_id = OBJECT_ID('dbo.DonHang'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_DonHang_Nguon_TrangThai_TaoLuc 
    ON dbo.DonHang (MaCN, Nguon, TrangThai, TaoLuc DESC);
    PRINT N'Đã tạo chỉ mục IX_DonHang_Nguon_TrangThai_TaoLuc phục vụ POS và Bếp';
END
GO
