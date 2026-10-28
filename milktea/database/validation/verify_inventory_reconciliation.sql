-- ============================================================================
-- SCRIPT ĐỐI SOÁT TỒN KHO & BIẾN ĐỘNG KHO CHUỖI MILKTEA MULTI-BRANCH
-- File: database/validation/verify_inventory_reconciliation.sql
-- Tuân thủ V4-06 & V4-10: Đúng schema thực tế, fail-fast THROW khi vi phạm
-- ============================================================================

SET NOCOUNT ON;

PRINT N'====================================================================';
PRINT N'  ĐỐI SOÁT TỒN KHO VÀ DÒNG NGUYÊN LIỆU (INVENTORY RECONCILIATION)';
PRINT N'====================================================================';

DECLARE @ViolationCount INT = 0;

-- 1. Kiểm tra không có số tồn kho âm (SoLuong >= 0)
PRINT N'1. Kiểm tra tính hợp lệ của số lượng tồn kho (SoLuong >= 0):';

SELECT cn.TenChiNhanh,
       tnl.ViTri,
       nl.TenNL,
       tnl.SoLuong,
       nl.DonVi
FROM   dbo.TonNguyenLieu AS tnl
       INNER JOIN dbo.ChiNhanh AS cn ON tnl.MaCN = cn.MaCN
       INNER JOIN dbo.NguyenLieu AS nl ON tnl.MaNL = nl.MaNL
WHERE  tnl.SoLuong < 0;

DECLARE @NegativeStockCount AS INT = (SELECT COUNT(*) FROM dbo.TonNguyenLieu WHERE SoLuong < 0);

IF @NegativeStockCount = 0
    PRINT N'   [PASS] 100% bản ghi tồn kho có số lượng >= 0 (Không bị âm kho).';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện ' + CAST (@NegativeStockCount AS NVARCHAR (10)) + N' bản ghi tồn kho bị âm!';
    SET @ViolationCount = @ViolationCount + @NegativeStockCount;
END;

-- 2. Kiểm tra tính liên tục số học của LichSuKho: TonSau = TonTruoc + LuongBienDong
PRINT N'2. Kiểm tra tính toán số học trên từng dòng biến động LichSuKho:';

DECLARE @LedgerMathErrorCount INT = (
    SELECT COUNT(*) 
    FROM dbo.LichSuKho 
    WHERE ABS(TonSau - (TonTruoc + LuongBienDong)) > 0.001
);

IF @LedgerMathErrorCount = 0
    PRINT N'   [PASS] 100% bản ghi LichSuKho khớp tuyệt đối công thức: TonSau = TonTruoc + LuongBienDong.';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện ' + CAST(@LedgerMathErrorCount AS NVARCHAR(10)) + N' dòng biến động kho sai số học!';
    SET @ViolationCount = @ViolationCount + @LedgerMathErrorCount;
END;

-- 3. Kiểm tra tính toàn vẹn khấu trừ đơn hàng hoàn thành
PRINT N'3. Kiểm tra khấu trừ Bếp cho đơn hàng đã HOAN_THANH:';

-- Bắt buộc đơn HOAN_THANH phải có DinhMucDonHang
DECLARE @CompletedOrdersWithoutSnapshot INT = (
    SELECT COUNT(DISTINCT dh.MaDH)
    FROM dbo.DonHang dh
    LEFT JOIN dbo.DinhMucDonHang dmdh ON dh.MaDH = dmdh.MaDH
    WHERE dh.TrangThai = 'HOAN_THANH' AND dmdh.MaDH IS NULL
);

IF @CompletedOrdersWithoutSnapshot = 0
    PRINT N'   [PASS] 100% đơn hàng HOAN_THANH có đầy đủ bản chụp DinhMucDonHang.';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện ' + CAST(@CompletedOrdersWithoutSnapshot AS NVARCHAR(10)) + N' đơn hoàn thành thiếu DinhMucDonHang!';
    SET @ViolationCount = @ViolationCount + @CompletedOrdersWithoutSnapshot;
END;

-- Bắt buộc nguyên liệu trong DinhMucDonHang của đơn HOAN_THANH phải có dòng xuất kho LichSuKho tương ứng
DECLARE @CompletedOrdersWithoutStockOut INT = (
    SELECT COUNT(*)
    FROM dbo.DonHang dh
    INNER JOIN dbo.DinhMucDonHang dmdh ON dh.MaDH = dmdh.MaDH
    LEFT JOIN dbo.LichSuKho lsk ON lsk.NguonLoai = 'ORDER' 
                                AND lsk.NguonMa = dh.MaDH 
                                AND lsk.MaNL = dmdh.MaNL
                                AND lsk.Loai = 'XUAT'
    WHERE dh.TrangThai = 'HOAN_THANH'
      AND lsk.MaBienDong IS NULL
);

IF @CompletedOrdersWithoutStockOut = 0
    PRINT N'   [PASS] 100% nguyên liệu định mức của đơn HOAN_THANH đã được trừ kho Bếp/Kho trong LichSuKho.';
ELSE
BEGIN
    PRINT N'   [FAIL] Phát hiện ' + CAST(@CompletedOrdersWithoutStockOut AS NVARCHAR(10)) + N' dòng định mức chưa được ghi sổ xuất kho!';
    SET @ViolationCount = @ViolationCount + @CompletedOrdersWithoutStockOut;
END;

-- 4. Kiểm tra xuất chuyển kho 2 vế (XUAT_CHUYEN ở nguồn vs NHAP ở đích)
PRINT N'4. Kiểm tra đối soát xuất chuyển kho:';

DECLARE @UnbalancedTransfers INT = (
    SELECT COUNT(*)
    FROM dbo.LichSuKho outLsk
    LEFT JOIN dbo.LichSuKho inLsk ON inLsk.NguonLoai = 'NHAP_TONG' 
                                  AND inLsk.NguonMa = outLsk.NguonMa 
                                  AND inLsk.MaNL = outLsk.MaNL
                                  AND inLsk.Loai = 'NHAP'
    WHERE outLsk.Loai = 'XUAT_CHUYEN'
      AND inLsk.MaBienDong IS NULL
);

IF @UnbalancedTransfers = 0
    PRINT N'   [PASS] 100% phiếu điều chuyển từ Kho Tổng có đủ 2 vế xuất và nhập tại chi nhánh.';
ELSE
BEGIN
    PRINT N'   [FAIL] Có ' + CAST(@UnbalancedTransfers AS NVARCHAR(10)) + N' giao dịch xuất chuyển thiếu vế nhập!';
    SET @ViolationCount = @ViolationCount + @UnbalancedTransfers;
END;

-- 5. Báo cáo nguyên liệu chạm ngưỡng cảnh báo tối thiểu
PRINT N'5. Báo cáo các nguyên liệu dưới ngưỡng tối thiểu:';

SELECT cn.TenChiNhanh,
       tnl.ViTri,
       nl.TenNL,
       tnl.SoLuong,
       tnl.NguongToiThieu,
       nl.DonVi,
       CASE WHEN tnl.SoLuong <= tnl.NguongToiThieu THEN N'CẢNH BÁO THẤP' ELSE N'BÌNH THƯỜNG' END AS TinhTrang
FROM   dbo.TonNguyenLieu AS tnl
       INNER JOIN dbo.ChiNhanh AS cn ON tnl.MaCN = cn.MaCN
       INNER JOIN dbo.NguyenLieu AS nl ON tnl.MaNL = nl.MaNL
WHERE  tnl.SoLuong <= tnl.NguongToiThieu;

PRINT N'====================================================================';
PRINT N'  TỔNG KẾT ĐỐI SOÁT TỒN KHO';
PRINT N'====================================================================';

IF @ViolationCount > 0
BEGIN
    DECLARE @ErrMsg NVARCHAR(250) = N'ĐỐI SOÁT TỒN KHO THẤT BẠI: Phát hiện ' + CAST(@ViolationCount AS NVARCHAR(10)) + N' vi phạm dữ liệu!';
    THROW 50000, @ErrMsg, 1;
END;

PRINT N'>>> ĐỐI SOÁT TỒN KHO 100% HOÀN HẢO - KHÔNG CÓ BẤT KỲ LỖI NÀO.';
GO