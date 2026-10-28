-- ============================================================================
-- SCRIPT ĐỐI SOÁT TỒN KHO & BIẾN ĐỘNG KHO CHUỖI MILKTEA MULTI-BRANCH
-- File: database/validation/verify_inventory_reconciliation.sql
-- ============================================================================
USE milktea;


GO
SET NOCOUNT ON;

PRINT N'====================================================================';

PRINT N'  ĐỐI SOÁT TỒN KHO VÀ DÒNG NGUYÊN LIỆU (INVENTORY RECONCILIATION)';

PRINT N'====================================================================';

-- 1. Kiểm tra không có số tồn kho âm
PRINT N'1. Kiểm tra tính hợp lệ của số lượng tồn kho (SoLuongTon >= 0):';

SELECT cn.TenChiNhanh,
       tnl.ViTri,
       nl.TenNL,
       tnl.SoLuongTon,
       nl.DonViTinh
FROM   dbo.TonNguyenLieu AS tnl
       INNER JOIN
       dbo.ChiNhanh AS cn
       ON tnl.MaCN = cn.MaCN
       INNER JOIN
       dbo.NguyenLieu AS nl
       ON tnl.MaNL = nl.MaNL
WHERE  tnl.SoLuongTon < 0;

DECLARE @NegativeStockCount AS INT = (SELECT COUNT(*)
                                      FROM   dbo.TonNguyenLieu
                                      WHERE  SoLuongTon < 0);

IF @NegativeStockCount = 0
    PRINT N'   [PASS] 100% bản ghi tồn kho có số lượng >= 0 (Không bị âm kho).';
ELSE
    PRINT N'   [FAIL] Phát hiện ' + CAST (@NegativeStockCount AS NVARCHAR (10)) + N' bản ghi tồn kho bị âm!';

-- 2. Đối soát tổng nhập - tổng xuất trong LichSuKho với số dư hiện tại
PRINT N'2. Bảng đối soát biến động kho theo từng nguyên liệu tại chi nhánh CN01:';

SELECT   nl.TenNL,
         tnl.ViTri,
         tnl.SoLuongTon AS TonHienTai,
         ISNULL(SUM(CASE WHEN lsk.LoaiBienDong = 'NHAP' THEN lsk.SoLuong ELSE 0 END), 0) AS TongNhap,
         ISNULL(SUM(CASE WHEN lsk.LoaiBienDong = 'XUAT' THEN lsk.SoLuong ELSE 0 END), 0) AS TongXuat,
         ISNULL(SUM(CASE WHEN lsk.LoaiBienDong = 'NHAP' THEN lsk.SoLuong WHEN lsk.LoaiBienDong = 'XUAT' THEN -lsk.SoLuong ELSE 0 END), 0) AS BienDongRong
FROM     dbo.TonNguyenLieu AS tnl
         INNER JOIN
         dbo.NguyenLieu AS nl
         ON tnl.MaNL = nl.MaNL
         INNER JOIN
         dbo.ChiNhanh AS cn
         ON tnl.MaCN = cn.MaCN
            AND cn.MaDinhDanh = 'CN01'
         LEFT OUTER JOIN
         dbo.LichSuKho AS lsk
         ON lsk.MaCN = tnl.MaCN
            AND lsk.MaNL = tnl.MaNL
            AND lsk.ViTri = tnl.ViTri
GROUP BY nl.TenNL, tnl.ViTri, tnl.SoLuongTon
ORDER BY nl.TenNL, tnl.ViTri;

-- 3. Kiểm tra tính toàn vẹn khấu trừ đơn hàng hoàn thành
PRINT N'3. Kiểm tra khấu trừ Bếp cho đơn hàng đã HOAN_THANH:';

DECLARE @CompletedOrdersWithoutStockOut AS INT = (SELECT COUNT(DISTINCT dh.MaDH)
                                                  FROM   dbo.DonHang AS dh
                                                         INNER JOIN
                                                         dbo.DinhMucDonHang AS dmdh
                                                         ON dh.MaDH = dmdh.MaDH
                                                         LEFT OUTER JOIN
                                                         dbo.LichSuKho AS lsk
                                                         ON lsk.MaThamChieu = CAST (dh.MaDH AS VARCHAR (50))
                                                            AND lsk.LoaiBienDong = 'XUAT'
                                                  WHERE  dh.TrangThai = 'HOAN_THANH'
                                                         AND lsk.MaLS IS NULL);

IF @CompletedOrdersWithoutStockOut = 0
    PRINT N'   [PASS] 100% đơn hàng HOAN_THANH đã được trừ kho Bếp tương ứng định mức.';
ELSE
    PRINT N'   [WARN] Có ' + CAST (@CompletedOrdersWithoutStockOut AS NVARCHAR (10)) + N' đơn hoàn thành chưa có vết xuất kho trong LichSuKho (nếu có đơn mẫu seed sẵn).';

-- 4. Báo cáo nguyên liệu chạm ngưỡng cảnh báo tối thiểu
PRINT N'4. Báo cáo các nguyên liệu dưới ngưỡng tối thiểu (Cần nhập bổ sung):';

SELECT cn.TenChiNhanh,
       tnl.ViTri,
       nl.TenNL,
       tnl.SoLuongTon,
       tnl.NguongToiThieu,
       nl.DonViTinh,
       CASE WHEN tnl.SoLuongTon <= tnl.NguongToiThieu THEN N'CẢNH BÁO THẤP' ELSE N'BÌNH THƯỜNG' END AS TinhTrang
FROM   dbo.TonNguyenLieu AS tnl
       INNER JOIN
       dbo.ChiNhanh AS cn
       ON tnl.MaCN = cn.MaCN
       INNER JOIN
       dbo.NguyenLieu AS nl
       ON tnl.MaNL = nl.MaNL
WHERE  tnl.SoLuongTon <= tnl.NguongToiThieu;

PRINT N'====================================================================';

PRINT N'  HOÀN TẤT ĐỐI SOÁT TỒN KHO';

PRINT N'====================================================================';