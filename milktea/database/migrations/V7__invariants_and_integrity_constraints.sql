-- ============================================================================
-- V7__invariants_and_integrity_constraints.sql
-- Bổ sung các ràng buộc toàn vẹn và bất biến bắt buộc theo V4-10:
-- 1. Filtered Unique Index: Mỗi bàn chỉ có tối đa 1 phiên 'OPEN' tại một thời điểm
-- 2. Unique Index: Mỗi biên bản kết ca chỉ có 1 dòng cho mỗi nguyên liệu (MaKetCa, MaNL)
-- 3. Check Constraint: Tồn thực tế kết ca = Lượng giữ lại + Lượng loại bỏ
-- 4. Check Constraint: Biến động kho TonSau = TonTruoc + LuongBienDong
-- 5. Check Constraint: Số tiền phải trả trên hóa đơn PhaiTra = max(0, TongTien - TienGiam - TienGiamVoucher)
-- ============================================================================

SET NOCOUNT ON;

-- 1. Chỉ 1 phiên OPEN trên mỗi bàn
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_PhienBan_MaBan_Open' AND object_id = OBJECT_ID('dbo.PhienBan'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UQ_PhienBan_MaBan_Open
    ON dbo.PhienBan (MaBan)
    WHERE TrangThai = 'OPEN';
    PRINT N'Đã tạo chỉ mục duy nhất UQ_PhienBan_MaBan_Open (1 phiên OPEN / bàn)';
END
GO

-- 2. Mỗi nguyên liệu chỉ xuất hiện 1 lần trong 1 biên bản kết ca
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_ChiTietKetCaKho_MaKetCa_MaNL' AND object_id = OBJECT_ID('dbo.ChiTietKetCaKho'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UQ_ChiTietKetCaKho_MaKetCa_MaNL
    ON dbo.ChiTietKetCaKho (MaKetCa, MaNL);
    PRINT N'Đã tạo chỉ mục duy nhất UQ_ChiTietKetCaKho_MaKetCa_MaNL';
END
GO

-- 3. Kiểm tra số học kết ca: TonThucTe = LuongLoaiBo + LuongGiuLai
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_ChiTietKetCa_ThucTe')
BEGIN
    ALTER TABLE dbo.ChiTietKetCaKho 
    ADD CONSTRAINT CK_ChiTietKetCa_ThucTe CHECK (TonThucTe = LuongLoaiBo + LuongGiuLai);
    PRINT N'Đã tạo ràng buộc CK_ChiTietKetCa_ThucTe';
END
GO

-- 4. Kiểm tra số học lịch sử kho: TonSau = TonTruoc + LuongBienDong
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_LichSuKho_TonSau_Math')
BEGIN
    ALTER TABLE dbo.LichSuKho 
    ADD CONSTRAINT CK_LichSuKho_TonSau_Math CHECK (TonSau = TonTruoc + LuongBienDong);
    PRINT N'Đã tạo ràng buộc CK_LichSuKho_TonSau_Math';
END
GO

-- 5. Kiểm tra công thức thanh toán hóa đơn: PhaiTra = TongTien - TienGiam - TienGiamVoucher
-- Chuẩn hóa dữ liệu lịch sử nếu bị trùng TienGiam và TienGiamVoucher từ kịch bản cũ
UPDATE dbo.HoaDon 
SET TienGiam = 0.00 
WHERE MaCodeVoucher IS NOT NULL 
  AND TienGiam = TienGiamVoucher 
  AND PhaiTra = TongTien - TienGiamVoucher;

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_HoaDon_PhaiTra_Calc')
BEGIN
    ALTER TABLE dbo.HoaDon 
    ADD CONSTRAINT CK_HoaDon_PhaiTra_Calc CHECK (
        PhaiTra = CASE 
            WHEN (TongTien - TienGiam - TienGiamVoucher) < 0 THEN 0 
            ELSE (TongTien - TienGiam - TienGiamVoucher) 
        END
    );
    PRINT N'Đã tạo ràng buộc CK_HoaDon_PhaiTra_Calc';
END
GO
