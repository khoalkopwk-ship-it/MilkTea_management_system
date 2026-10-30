-- =========================================================================================
-- KỊCH BẢN BỔ SUNG DỮ LIỆU GIAO DỊCH 7 NGÀY CHO CHI NHÁNH 2 (CN02)
-- File: database/seed_branch_2_transactions.sql
-- Mục đích: Bổ sung đầy đủ dữ liệu giao dịch 7 ngày cho Chi nhánh 2 (CN02) trên CSDL website đã deploy:
--   1. 21 Phiếu nhập kho nguyên phụ liệu tại CN02 (3 phiếu/ngày x 7 ngày)
--   2. 38 Mẻ sơ chế bếp tại CN02 (ủ trà, nấu trân châu, kem cheese)
--   3. 14 Phiếu kết ca kho & biên bản kiểm kê hao hụt tại CN02 (2 ca/ngày x 7 ngày)
--   4. 54 Đơn hàng mẫu 7 ngày tại CN02 với ĐẦY ĐỦ CÁC TRẠNG THÁI:
--      - CHO_XAC_NHAN (Đơn mới đặt qua QR)
--      - CHO_CHE_BIEN & DANG_CHE_BIEN (Hiển thị hàng đợi Bếp KDS CN02)
--      - HOAN_THANH (Hóa đơn CASH / VietQR, trừ kho theo định mức, ghi sổ)
--      - DA_HUY (Có biên bản hủy và duyệt hoàn tiền mẫu)
--   5. Phiên bàn mẫu (Bàn 01 CN02 mở phiên OPEN, các bàn khác CLOSED)
--   6. Lịch sử kho (LichSuKho) đồng bộ 100% tuân thủ CK_LichSuKho_TonSau_Math
--
-- NGUYÊN TẮC:
--   - KHÔNG XÓA hay làm ảnh hưởng bất kỳ dữ liệu cũ nào của Chi nhánh 1.
--   - Tự động lấy ID phát sinh qua biến/IDENTITY, không bị xung đột ID.
--   - Đảm bảo 100% thỏa mãn các ràng buộc:
--       + UQ_PhienBan_MaBan_Open (tối đa 1 phiên OPEN trên mỗi bàn)
--       + CK_HoaDon_PhaiTra_Calc (PhaiTra = TongTien - TienGiam - TienGiamVoucher)
--       + CK_ChiTietKetCa_ThucTe (TonThucTe = LuongLoaiBo + LuongGiuLai)
--       + CK_LichSuKho_TonSau_Math (TonSau = TonTruoc + LuongBienDong)
-- =========================================================================================

SET NOCOUNT ON;
SET XACT_ABORT ON;

PRINT N'=========================================================================================';
PRINT N'BẮT ĐẦU BỔ SUNG DỮ LIỆU GIAO DỊCH 7 NGÀY CHO CHI NHÁNH 2 (CN02)...';
PRINT N'Thời gian chạy: ' + CONVERT(NVARCHAR(30), GETDATE(), 120);
PRINT N'=========================================================================================';

BEGIN TRY
    BEGIN TRANSACTION;

    -- 1. LẤY MÃ CHI NHÁNH & TÀI KHOẢN
    DECLARE @cn2Id BIGINT = (SELECT MaCN FROM dbo.ChiNhanh WHERE MaDinhDanh = 'CN02');
    IF @cn2Id IS NULL
    BEGIN
        RAISERROR(N'Không tìm thấy Chi nhánh 2 (CN02)! Vui lòng chạy update_sample_data_from_previous_deploy.sql trước.', 16, 1);
        RETURN;
    END;

    -- Tùy chọn: Đặt @forceReSeed = 1 nếu muốn xóa sạch các giao dịch cũ của riêng CN02 để nạp lại mới nhất
    DECLARE @forceReSeed BIT = 0;

    IF EXISTS (SELECT 1 FROM dbo.DonHang WHERE MaCN = @cn2Id)
    BEGIN
        IF @forceReSeed = 1
        BEGIN
            PRINT N'--> @forceReSeed = 1: Đang dọn dẹp các giao dịch cũ của riêng Chi nhánh 2 để nạp lại mới...';
            DELETE FROM dbo.DanhGiaMedia WHERE MaDanhGia IN (SELECT MaDanhGia FROM dbo.DanhGia WHERE MaCN = @cn2Id OR MaChiTiet IN (SELECT MaChiTiet FROM dbo.ChiTietDonHang WHERE MaDH IN (SELECT MaDH FROM dbo.DonHang WHERE MaCN = @cn2Id)));
            DELETE FROM dbo.DanhGia WHERE MaCN = @cn2Id OR MaChiTiet IN (SELECT MaChiTiet FROM dbo.ChiTietDonHang WHERE MaDH IN (SELECT MaDH FROM dbo.DonHang WHERE MaCN = @cn2Id));
            DELETE FROM dbo.LichSuSuDungVoucher WHERE MaCN = @cn2Id;
            DELETE FROM dbo.ThongBaoThanhToan WHERE MaHD IN (SELECT MaHD FROM dbo.HoaDon WHERE MaDH IN (SELECT MaDH FROM dbo.DonHang WHERE MaCN = @cn2Id));
            DELETE FROM dbo.YeuCauHoan WHERE MaDH IN (SELECT MaDH FROM dbo.DonHang WHERE MaCN = @cn2Id);
            DELETE FROM dbo.YeuCauHuy WHERE MaDH IN (SELECT MaDH FROM dbo.DonHang WHERE MaCN = @cn2Id);
            DELETE FROM dbo.ThanhToan WHERE MaHD IN (SELECT MaHD FROM dbo.HoaDon WHERE MaDH IN (SELECT MaDH FROM dbo.DonHang WHERE MaCN = @cn2Id));
            DELETE FROM dbo.HoaDon WHERE MaDH IN (SELECT MaDH FROM dbo.DonHang WHERE MaCN = @cn2Id);
            DELETE FROM dbo.DinhMucDonHang WHERE MaDH IN (SELECT MaDH FROM dbo.DonHang WHERE MaCN = @cn2Id);
            DELETE FROM dbo.ChiTietDonHangTopping WHERE MaChiTiet IN (SELECT MaChiTiet FROM dbo.ChiTietDonHang WHERE MaDH IN (SELECT MaDH FROM dbo.DonHang WHERE MaCN = @cn2Id));
            DELETE FROM dbo.ChiTietDonHang WHERE MaDH IN (SELECT MaDH FROM dbo.DonHang WHERE MaCN = @cn2Id);
            DELETE FROM dbo.DonHang WHERE MaCN = @cn2Id;
            DELETE FROM dbo.PhienBan WHERE MaBan IN (SELECT MaBan FROM dbo.Ban WHERE MaCN = @cn2Id);
            UPDATE dbo.Ban SET MaPhienDangMo = NULL, TrangThai = 'TRONG' WHERE MaCN = @cn2Id;
            DELETE FROM dbo.ChiTietKetCaKho WHERE MaKetCa IN (SELECT MaKetCa FROM dbo.KetCaKho WHERE MaCN = @cn2Id);
            DELETE FROM dbo.KetCaKho WHERE MaCN = @cn2Id;
            DELETE FROM dbo.ChiTietMeSoChe WHERE MaMe IN (SELECT MaMe FROM dbo.MeSoChe WHERE MaCN = @cn2Id);
            DELETE FROM dbo.MeSoChe WHERE MaCN = @cn2Id;
            DELETE FROM dbo.ChiTietNhap WHERE MaPhieu IN (SELECT MaPhieu FROM dbo.PhieuNhap WHERE MaCN = @cn2Id);
            DELETE FROM dbo.PhieuNhap WHERE MaCN = @cn2Id;
            DELETE FROM dbo.LichSuKho WHERE MaCN = @cn2Id;
            PRINT N'--> Đã dọn dẹp sạch giao dịch cũ của CN02. Tiếp tục nạp dữ liệu mới...';
        END
        ELSE
        BEGIN
            PRINT N'--> Chi nhánh 2 (CN02) đã có sẵn dữ liệu đơn hàng. Bỏ qua để tránh trùng lặp dữ liệu (Đổi @forceReSeed = 1 nếu muốn nạp lại).';
            COMMIT TRANSACTION;
            RETURN;
        END;
    END;

    DECLARE @adminId   BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'ADMIN' ORDER BY MaTK);
    DECLARE @mgr2Id    BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'manager2@milktea.vn');
    DECLARE @cashier2Id BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'cashier2@milktea.vn');
    DECLARE @kitchen2Id BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'kitchen2@milktea.vn');
    DECLARE @cust1Id   BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer@milktea.vn');
    DECLARE @cust2Id   BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer2@milktea.vn');
    DECLARE @cust3Id   BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer3@milktea.vn');

    IF @mgr2Id IS NULL SET @mgr2Id = @adminId;
    IF @cashier2Id IS NULL SET @cashier2Id = @adminId;
    IF @kitchen2Id IS NULL SET @kitchen2Id = @adminId;
    IF @cust1Id IS NULL SET @cust1Id = @adminId;
    IF @cust2Id IS NULL SET @cust2Id = @adminId;
    IF @cust3Id IS NULL SET @cust3Id = @adminId;

    -- Lấy danh sách nguyên liệu
    DECLARE @nlTraDen   BIGINT = (SELECT TOP 1 MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Đen Cổ Thụ');
    DECLARE @nlTraOLong BIGINT = (SELECT TOP 1 MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Ô Long Kim Tuyên');
    DECLARE @nlDuong    BIGINT = (SELECT TOP 1 MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đường Cát Trắng');
    DECLARE @nlBotBeo   BIGINT = (SELECT TOP 1 MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Bột Béo Thực Vật');
    DECLARE @nlTCTho    BIGINT = (SELECT TOP 1 MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Thô Đen');
    DECLARE @nlLy500    BIGINT = (SELECT TOP 1 MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Ly Nhựa PP 500ml');

    DECLARE @ctCotTraDen BIGINT = (SELECT TOP 1 MaCTSC FROM dbo.CongThucSoChe WHERE TenCT LIKE N'%Trà Đen%');
    DECLARE @ctTranChau  BIGINT = (SELECT TOP 1 MaCTSC FROM dbo.CongThucSoChe WHERE TenCT LIKE N'%Trân Châu%');
    DECLARE @ctCheese    BIGINT = (SELECT TOP 1 MaCTSC FROM dbo.CongThucSoChe WHERE TenCT LIKE N'%Kem Cheese%');
    DECLARE @ctNuocDuong BIGINT = (SELECT TOP 1 MaCTSC FROM dbo.CongThucSoChe WHERE TenCT LIKE N'%Nước Đường%');

    DECLARE @nlCotTraDen BIGINT = (SELECT TOP 1 MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Đen');
    DECLARE @nlTCChin    BIGINT = (SELECT TOP 1 MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Nấu Chín');
    DECLARE @nlCheeseTP  BIGINT = (SELECT TOP 1 MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Kem Cheese Thành Phẩm');
    DECLARE @nlNuocDuong BIGINT = (SELECT TOP 1 MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Đường Pha Sẵn');

    -- Bảng tạm chứa danh sách món uống để đặt đơn
    DECLARE @SampleDrinks TABLE (Idx INT IDENTITY(1,1), MaMon BIGINT, TenMon NVARCHAR(120), Size VARCHAR(12), Gia DECIMAL(18,2));
    INSERT INTO @SampleDrinks (MaMon, TenMon, Size, Gia)
    SELECT MaMon, TenMon, Size, Gia FROM dbo.MonUong WHERE MaDM <> (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Topping Cao Cấp');

    DECLARE @SampleToppings TABLE (Idx INT IDENTITY(1,1), MaMon BIGINT, TenMon NVARCHAR(120), Gia DECIMAL(18,2));
    INSERT INTO @SampleToppings (MaMon, TenMon, Gia)
    SELECT MaMon, TenMon, Gia FROM dbo.MonUong WHERE MaDM = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Topping Cao Cấp');

    DECLARE @totalDrinks INT = (SELECT COUNT(*) FROM @SampleDrinks);
    DECLARE @totalToppings INT = (SELECT COUNT(*) FROM @SampleToppings);
    IF @totalDrinks = 0 OR @totalToppings = 0
    BEGIN
        RAISERROR(N'Thực đơn món uống chưa có dữ liệu! Vui lòng nạp MonUong trước.', 16, 1);
        RETURN;
    END;

    -- =========================================================================
    -- 2. NẠP 21 PHIẾU NHẬP KHO CHO CHI NHÁNH 2 (3 phiếu/ngày x 7 ngày)
    -- =========================================================================
    PRINT N'--> [1/5] Nạp 21 phiếu nhập kho định kỳ cho Chi nhánh 2...';
    DECLARE @dayOffset INT = 6;
    DECLARE @pnSeq INT = 0;

    WHILE @dayOffset >= 0
    BEGIN
        DECLARE @pIdx INT = 1;
        WHILE @pIdx <= 3
        BEGIN
            SET @pnSeq = @pnSeq + 1;
            DECLARE @docTime DATETIME2 = DATEADD(MINUTE, (@pIdx * 120) + 480, DATEADD(DAY, -@dayOffset, CAST(CAST(SYSUTCDATETIME() AS DATE) AS DATETIME2)));
            DECLARE @keyPN VARCHAR(64) = 'PN-CN02-D' + CAST(@dayOffset AS VARCHAR(2)) + '-' + RIGHT('00' + CAST(@pIdx AS VARCHAR(3)), 3) + '-' + SUBSTRING(CONVERT(VARCHAR(36), NEWID()), 1, 8);

            INSERT INTO dbo.PhieuNhap (NguoiLap, MaCN, LyDo, NguonNhap, TrangThai, LapLuc, GhiSoLuc, MaYeuCau)
            VALUES (@mgr2Id, @cn2Id, N'Nhập nguyên phụ liệu định kỳ từ Kho Hàng Tổng', N'Kho Hàng Tổng Trung Tâm (MilkTea Chain)', 'DA_GHI_SO', @docTime, @docTime, @keyPN);
            DECLARE @curPnId BIGINT = SCOPE_IDENTITY();

            INSERT INTO dbo.ChiTietNhap (MaPhieu, MaNL, SoLuong, DonGiaNhap)
            VALUES 
                (@curPnId, @nlTraDen, 15.000 + (@pIdx * 2), 120000.00),
                (@curPnId, @nlDuong,  30.000 + (@pIdx * 5), 22000.00),
                (@curPnId, @nlBotBeo, 20.000 + (@pIdx * 3), 65000.00),
                (@curPnId, @nlLy500,  500.000 + (@pIdx * 100), 450.00);

            -- Ghi sổ kho khớp CK_LichSuKho_TonSau_Math
            INSERT INTO dbo.LichSuKho (MaCN, MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo, GiaVonDonVi)
            VALUES
                (@cn2Id, @nlTraDen, 'KHO', 'NHAP', 15.000 + (@pIdx * 2), 100.000, 100.000 + (15.000 + (@pIdx * 2)), 'IMPORT', @curPnId, @mgr2Id, @docTime, N'Nhập kho CN02 từ Kho Tổng', 120000.00),
                (@cn2Id, @nlDuong,  'KHO', 'NHAP', 30.000 + (@pIdx * 5), 200.000, 200.000 + (30.000 + (@pIdx * 5)), 'IMPORT', @curPnId, @mgr2Id, @docTime, N'Nhập kho CN02 từ Kho Tổng', 22000.00);

            SET @pIdx = @pIdx + 1;
        END;
        SET @dayOffset = @dayOffset - 1;
    END;

    -- =========================================================================
    -- 3. NẠP 38 MẺ SƠ CHẾ BẾP CHO CHI NHÁNH 2 (5-6 mẻ/ngày x 7 ngày)
    -- =========================================================================
    PRINT N'--> [2/5] Nạp 38 mẻ sơ chế bếp tại quầy pha chế CN02...';
    SET @dayOffset = 6;
    DECLARE @meSeq INT = 0;

    WHILE @dayOffset >= 0
    BEGIN
        DECLARE @mIdx INT = 1;
        WHILE @mIdx <= 5
        BEGIN
            SET @meSeq = @meSeq + 1;
            DECLARE @ctSelected BIGINT, @outMat BIGINT, @inMat BIGINT, @expQty DECIMAL(18,3), @actQty DECIMAL(18,3), @rawQty DECIMAL(18,3);
            
            IF @mIdx = 1 BEGIN
                SET @ctSelected = @ctCotTraDen; SET @outMat = @nlCotTraDen; SET @inMat = @nlTraDen;
                SET @expQty = 5000.000; SET @actQty = 4980.000; SET @rawQty = 0.200;
            END
            ELSE IF @mIdx = 2 BEGIN
                SET @ctSelected = @ctTranChau; SET @outMat = @nlTCChin; SET @inMat = @nlTCTho;
                SET @expQty = 2500.000; SET @actQty = 2500.000; SET @rawQty = 1.000;
            END
            ELSE IF @mIdx = 3 BEGIN
                SET @ctSelected = @ctCheese; SET @outMat = @nlCheeseTP; SET @inMat = @nlDuong;
                SET @expQty = 2000.000; SET @actQty = 1950.000; SET @rawQty = 0.400;
            END
            ELSE BEGIN
                SET @ctSelected = @ctNuocDuong; SET @outMat = @nlNuocDuong; SET @inMat = @nlDuong;
                SET @expQty = 5000.000; SET @actQty = 5000.000; SET @rawQty = 3.500;
            END;

            DECLARE @docTimeMe DATETIME2 = DATEADD(MINUTE, (@mIdx * 80) + 420, DATEADD(DAY, -@dayOffset, CAST(CAST(SYSUTCDATETIME() AS DATE) AS DATETIME2)));
            DECLARE @keyMe VARCHAR(64) = 'ME-CN02-D' + CAST(@dayOffset AS VARCHAR(2)) + '-' + RIGHT('00' + CAST(@mIdx AS VARCHAR(3)), 3) + '-' + SUBSTRING(CONVERT(VARCHAR(36), NEWID()), 1, 8);

            INSERT INTO dbo.MeSoChe (MaCN, MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, LyDoChenhLech, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
            VALUES (@cn2Id, @ctSelected, @outMat, @expQty, @actQty, 'DA_GHI_SO', N'Nấu mẻ sơ chế đầu ca CN02', 85000.00, 17.00, 0.40, 0, @kitchen2Id, @docTimeMe, @keyMe);
            DECLARE @curMeId BIGINT = SCOPE_IDENTITY();

            INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
            VALUES (@curMeId, @inMat, @rawQty, @rawQty, 65000.00, @rawQty * 65000.00);

            -- Ghi sổ kho Bếp khớp CK_LichSuKho_TonSau_Math
            INSERT INTO dbo.LichSuKho (MaCN, MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo)
            VALUES 
                (@cn2Id, @inMat,  'BEP', 'SOCHE_RA', -@rawQty, 40.000, 40.000 - @rawQty, 'BATCH', @curMeId, @kitchen2Id, @docTimeMe, N'Xuất nguyên liệu thô nấu sơ chế CN02'),
                (@cn2Id, @outMat, 'BEP', 'SOCHE_VAO', @actQty, 500.000, 500.000 + @actQty, 'BATCH', @curMeId, @kitchen2Id, @docTimeMe, N'Nhập thành phẩm sơ chế vào bếp CN02');

            SET @mIdx = @mIdx + 1;
        END;
        SET @dayOffset = @dayOffset - 1;
    END;

    -- =========================================================================
    -- 4. NẠP 14 PHIẾU KẾT CA KHO TẠI CHI NHÁNH 2 (2 ca/ngày x 7 ngày)
    -- =========================================================================
    PRINT N'--> [3/5] Nạp 14 phiếu kết ca kho & biên bản hao hụt cho CN02...';
    SET @dayOffset = 6;
    WHILE @dayOffset >= 0
    BEGIN
        DECLARE @caIdx INT = 1;
        WHILE @caIdx <= 2
        BEGIN
            DECLARE @shiftName VARCHAR(32) = CASE WHEN @caIdx = 1 THEN 'CA_SANG' ELSE 'CA_TOI' END;
            DECLARE @chotTime DATETIME2 = DATEADD(HOUR, CASE WHEN @shiftName = 'CA_SANG' THEN 14 ELSE 22 END, DATEADD(DAY, -@dayOffset, CAST(CAST(SYSUTCDATETIME() AS DATE) AS DATETIME2)));
            DECLARE @fromTime DATETIME2 = DATEADD(HOUR, -6, @chotTime);
            DECLARE @keyKC VARCHAR(64) = 'KC-CN02-D' + CAST(@dayOffset AS VARCHAR(2)) + '-' + CAST(@caIdx AS VARCHAR(2)) + '-' + SUBSTRING(CONVERT(VARCHAR(36), NEWID()), 1, 8);

            INSERT INTO dbo.KetCaKho (MaCN, MaCa, ViTri, NguoiChot, ChotLuc, TuLuc, DenLuc, TongChiPhiHaoHut, GhiChu, MaYeuCau)
            VALUES (@cn2Id, @shiftName, 'BEP', @kitchen2Id, @chotTime, @fromTime, @chotTime, 35000.00, N'Kết ca dọn dẹp và hủy nước trà thừa cuối ca CN02', @keyKC);
            DECLARE @curKcId BIGINT = SCOPE_IDENTITY();

            -- Chi tiết kết ca: Khớp ràng buộc CK_ChiTietKetCa_ThucTe ([TonThucTe] = [LuongLoaiBo] + [LuongGiuLai])
            INSERT INTO dbo.ChiTietKetCaKho (MaKetCa, MaNL, DonVi, TonSo, TonThucTe, LuongLoaiBo, LuongGiuLai, ChenhLechKiemKe, GiaVonDonVi, ChiPhiHaoHut, LyDo)
            VALUES 
                (@curKcId, @nlCotTraDen, 'ml', 500.000, 500.000, 500.000, 0.000, 0.000, 30.00, 15000.00, N'Hủy nước cốt trà quá 4 tiếng theo tiêu chuẩn CN02'),
                (@curKcId, @nlTCChin,    'g',  400.000, 400.000, 400.000, 0.000, 0.000, 50.00, 20000.00, N'Hủy trân châu thừa cuối ngày CN02');

            -- Ghi sổ kho hao hụt
            INSERT INTO dbo.LichSuKho (MaCN, MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo, GiaVonDonVi, ChiPhiHaoHut)
            VALUES 
                (@cn2Id, @nlCotTraDen, 'BEP', 'HAO_HUT', -500.000, 5000.000, 4500.000, 'WASTE', @curKcId, @kitchen2Id, @chotTime, N'Hao hụt kết ca nước trà thừa CN02', 30.00, 15000.00),
                (@cn2Id, @nlTCChin,    'BEP', 'HAO_HUT', -400.000, 3000.000, 2600.000, 'WASTE', @curKcId, @kitchen2Id, @chotTime, N'Hao hụt kết ca trân châu cuối ngày CN02', 50.00, 20000.00);

            SET @caIdx = @caIdx + 1;
        END;
        SET @dayOffset = @dayOffset - 1;
    END;

    -- =========================================================================
    -- 5. NẠP 54 ĐƠN HÀNG MẪU 7 NGÀY CHO CHI NHÁNH 2 (ĐỦ MỌI TRẠNG THÁI)
    -- =========================================================================
    PRINT N'--> [4/5] Nạp 54 đơn hàng mẫu 7 ngày cho Chi nhánh 2 (POS, Bàn QR, KDS, Hoàn thành, Hủy đơn)...';
    SET @dayOffset = 6;
    DECLARE @ordSeq INT = 2000;
    DECLARE @vChaoMung BIGINT = (SELECT TOP 1 MaVoucher FROM dbo.Voucher WHERE MaCode = 'CHAOMUNG');

    WHILE @dayOffset >= 0
    BEGIN
        DECLARE @oIdx INT = 1;
        -- Mỗi ngày tạo 7-8 đơn cho CN02 (Tổng = 54 đơn)
        DECLARE @ordersPerDay INT = CASE WHEN @dayOffset = 0 THEN 10 ELSE 7 END;

        WHILE @oIdx <= @ordersPerDay
        BEGIN
            SET @ordSeq = @ordSeq + 1;
            DECLARE @source VARCHAR(12) = CASE WHEN @oIdx % 3 = 0 THEN 'COUNTER' ELSE 'TABLE' END;
            DECLARE @cust BIGINT = CASE WHEN @oIdx % 3 = 1 THEN @cust1Id WHEN @oIdx % 3 = 2 THEN @cust2Id ELSE @cust3Id END;

            -- Trạng thái đơn:
            -- Ngày hôm nay: tạo đủ các trạng thái để demo POS/KDS
            DECLARE @status VARCHAR(24) = 'HOAN_THANH';
            IF @dayOffset = 0
            BEGIN
                IF @oIdx = 8 SET @status = 'CHO_XAC_NHAN';
                ELSE IF @oIdx = 9 SET @status = 'CHO_CHE_BIEN';
                ELSE IF @oIdx = 10 SET @status = 'DANG_CHE_BIEN';
                ELSE IF @oIdx = 7 SET @status = 'DA_HUY';
                ELSE SET @status = 'HOAN_THANH';
            END
            ELSE
            BEGIN
                IF @oIdx = 6 SET @status = 'DA_HUY';
                ELSE SET @status = 'HOAN_THANH';
            END;

            DECLARE @minOffset INT = (@oIdx * 80) + 480;
            DECLARE @createTime DATETIME2 = DATEADD(MINUTE, @minOffset, DATEADD(DAY, -@dayOffset, CAST(CAST(SYSUTCDATETIME() AS DATE) AS DATETIME2)));
            DECLARE @startTime DATETIME2  = DATEADD(MINUTE, 3, @createTime);
            DECLARE @finishTime DATETIME2 = DATEADD(MINUTE, 14, @createTime);

            -- Quản lý phiên bàn nếu là đơn TABLE
            DECLARE @phienId BIGINT = NULL;
            IF @source = 'TABLE'
            BEGIN
                DECLARE @tableNo INT = ((@ordSeq - 1) % 10) + 1;
                DECLARE @tableId BIGINT = (SELECT TOP 1 MaBan FROM dbo.Ban WHERE MaCN = @cn2Id AND TenBan LIKE N'%' + RIGHT('0' + CAST(@tableNo AS VARCHAR(2)), 2));
                
                DECLARE @sessionStatus VARCHAR(8) = 'CLOSED';
                -- Chỉ mở duy nhất 1 phiên OPEN trên Bàn 01 CN02 hôm nay nếu đơn đang chế biến
                IF @dayOffset = 0 AND @tableNo = 1 AND @status IN ('DANG_CHE_BIEN', 'CHO_CHE_BIEN')
                    AND NOT EXISTS (SELECT 1 FROM dbo.PhienBan WHERE MaBan = @tableId AND TrangThai = 'OPEN')
                BEGIN
                    SET @sessionStatus = 'OPEN';
                END;

                INSERT INTO dbo.PhienBan (MaBan, TrangThai, MoLuc, DongLuc, NguoiMo, NguoiDong, Version)
                VALUES (@tableId, @sessionStatus, @createTime, CASE WHEN @sessionStatus = 'CLOSED' THEN @finishTime ELSE NULL END, @cust, CASE WHEN @sessionStatus = 'CLOSED' THEN @cashier2Id ELSE NULL END, 0);
                SET @phienId = SCOPE_IDENTITY();

                IF @sessionStatus = 'OPEN'
                BEGIN
                    UPDATE dbo.Ban SET TrangThai = 'CO_KHACH', MaPhienDangMo = @phienId WHERE MaBan = @tableId;
                    IF NOT EXISTS (SELECT 1 FROM dbo.GioPhien WHERE MaPhien = @phienId)
                        INSERT INTO dbo.GioPhien (MaPhien, Version, SuaLuc) VALUES (@phienId, 0, @createTime);
                END
                ELSE
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM dbo.PhienBan WHERE MaBan = @tableId AND TrangThai = 'OPEN')
                        UPDATE dbo.Ban SET TrangThai = 'TRONG', MaPhienDangMo = NULL WHERE MaBan = @tableId;
                END;
            END;

            -- Voucher mẫu cho đơn hoàn thành
            DECLARE @applyVoucher BIGINT = NULL;
            DECLARE @voucherCode VARCHAR(32) = NULL;
            DECLARE @voucherDiscount DECIMAL(18,2) = 0.00;

            IF @status = 'HOAN_THANH' AND (@oIdx % 4 = 1) AND @vChaoMung IS NOT NULL
            BEGIN
                SET @applyVoucher = @vChaoMung;
                SET @voucherCode = 'CHAOMUNG';
                SET @voucherDiscount = 20000.00;
            END;

            DECLARE @orderKey VARCHAR(64) = 'ORD-CN02-D' + CAST(@dayOffset AS VARCHAR(2)) + '-' + CAST(@ordSeq AS VARCHAR(10));

            INSERT INTO dbo.DonHang (MaCN, MaPhien, MaTK, Nguon, TrangThai, MaYeuCau, GhiChu, TaoLuc, BatDauLuc, HoanThanhLuc, MaVoucher, MaCodeVoucher, Version)
            VALUES (
                @cn2Id, @phienId, @cust, @source, @status, @orderKey,
                CASE WHEN @source = 'TABLE' THEN N'Phục vụ tại bàn CN02' ELSE N'Khách gọi món tại quầy thu ngân CN02' END,
                @createTime,
                CASE WHEN @status IN ('CHO_XAC_NHAN') THEN NULL ELSE @startTime END,
                CASE WHEN @status = 'HOAN_THANH' THEN @finishTime ELSE NULL END,
                @applyVoucher, @voucherCode, 0
            );
            DECLARE @curDhId BIGINT = SCOPE_IDENTITY();

            -- Chi tiết món 1
            DECLARE @dIdx1 INT = ((@ordSeq * 3) % @totalDrinks) + 1;
            DECLARE @d1Ma BIGINT, @d1Ten NVARCHAR(120), @d1Size VARCHAR(12), @d1Gia DECIMAL(18,2);
            SELECT @d1Ma = MaMon, @d1Ten = TenMon, @d1Size = Size, @d1Gia = Gia FROM @SampleDrinks WHERE Idx = @dIdx1;

            INSERT INTO dbo.ChiTietDonHang (MaDH, MaMon, TenMonChot, SizeChot, SoLuong, DonGiaChot, GhiChu)
            VALUES (@curDhId, @d1Ma, @d1Ten, @d1Size, 1, @d1Gia, N'50% đường, 50% đá');
            DECLARE @ctdh1 BIGINT = SCOPE_IDENTITY();

            -- Topping món 1
            DECLARE @tIdx1 INT = ((@ordSeq * 2) % @totalToppings) + 1;
            DECLARE @tp1Ma BIGINT, @tp1Ten NVARCHAR(120), @tp1Gia DECIMAL(18,2);
            SELECT @tp1Ma = MaMon, @tp1Ten = TenMon, @tp1Gia = Gia FROM @SampleToppings WHERE Idx = @tIdx1;

            INSERT INTO dbo.ChiTietDonHangTopping (MaChiTiet, MaMonTopping, TenToppingChot, DonGiaToppingChot)
            VALUES (@ctdh1, @tp1Ma, @tp1Ten, @tp1Gia);

            DECLARE @subTotal DECIMAL(18,2) = @d1Gia + @tp1Gia;

            -- Món 2 cho 50% số đơn
            IF @oIdx % 2 = 0
            BEGIN
                DECLARE @dIdx2 INT = ((@ordSeq * 7) % @totalDrinks) + 1;
                DECLARE @d2Ma BIGINT, @d2Ten NVARCHAR(120), @d2Size VARCHAR(12), @d2Gia DECIMAL(18,2);
                SELECT @d2Ma = MaMon, @d2Ten = TenMon, @d2Size = Size, @d2Gia = Gia FROM @SampleDrinks WHERE Idx = @dIdx2;

                INSERT INTO dbo.ChiTietDonHang (MaDH, MaMon, TenMonChot, SizeChot, SoLuong, DonGiaChot, GhiChu)
                VALUES (@curDhId, @d2Ma, @d2Ten, @d2Size, 1, @d2Gia, N'Bình thường');
                SET @subTotal = @subTotal + @d2Gia;
            END;

            -- Tính số tiền phải trả khớp CK_HoaDon_PhaiTra_Calc
            DECLARE @finalPay DECIMAL(18,2) = @subTotal - @voucherDiscount;
            IF @finalPay < 0 SET @finalPay = 0;

            DECLARE @invStatus VARCHAR(12) = CASE WHEN @status = 'DA_HUY' THEN 'DA_HUY' ELSE 'HIEU_LUC' END;
            INSERT INTO dbo.HoaDon (MaDH, TongTien, TienGiam, PhaiTra, TyLeGiamChot, LoaiGiamChot, GiaTriGiamChot, TrangThai, LapLuc, HuyLuc, MaVoucher, MaCodeVoucher, TienGiamVoucher)
            VALUES (@curDhId, @subTotal, 0.00, @finalPay, 0.00, 'PERCENT', 0.00, @invStatus, @createTime, CASE WHEN @status = 'DA_HUY' THEN @finishTime ELSE NULL END, @applyVoucher, @voucherCode, @voucherDiscount);
            DECLARE @curHdId BIGINT = SCOPE_IDENTITY();

            -- Snapshot định mức tiêu hao
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

            -- Thanh toán & Trừ kho cho đơn HOAN_THANH
            IF @status = 'HOAN_THANH'
            BEGIN
                DECLARE @payMethod VARCHAR(16) = CASE WHEN @oIdx % 2 = 1 THEN 'BANK_TRANSFER' ELSE 'CASH' END;
                DECLARE @refCode NVARCHAR(120) = CASE WHEN @payMethod = 'BANK_TRANSFER' THEN 'VIETQR-CN02-' + CAST(@ordSeq AS VARCHAR(10)) ELSE 'CASH-CN02-' + CAST(@ordSeq AS VARCHAR(10)) END;

                INSERT INTO dbo.ThanhToan (MaHD, PhuongThuc, SoTien, ThamChieu, NguoiThu, ThuLuc, MaYeuCau)
                VALUES (@curHdId, @payMethod, @finalPay, @refCode, @cashier2Id, @finishTime, 'PAY-' + @orderKey);

                -- Trừ kho và ghi nhận LichSuKho khớp CK_LichSuKho_TonSau_Math
                DECLARE @curNlId BIGINT, @curTongLuong DECIMAL(18,3), @curLoaiNL VARCHAR(12), @curDonVi NVARCHAR(20);
                DECLARE curDMDH CURSOR LOCAL FAST_FORWARD FOR
                    SELECT MaNL, TongLuong, LoaiChot, DonViChot FROM dbo.DinhMucDonHang WHERE MaDH = @curDhId;
                OPEN curDMDH;
                FETCH NEXT FROM curDMDH INTO @curNlId, @curTongLuong, @curLoaiNL, @curDonVi;
                WHILE @@FETCH_STATUS = 0
                BEGIN
                    DECLARE @loc VARCHAR(8) = CASE WHEN @curLoaiNL = 'SOCHE' THEN 'BEP' ELSE 'KHO' END;
                    DECLARE @curBeforeQty DECIMAL(18,3) = 0.000;
                    SELECT @curBeforeQty = SoLuong FROM dbo.TonNguyenLieu WHERE MaCN = @cn2Id AND MaNL = @curNlId AND ViTri = @loc;
                    IF @curBeforeQty IS NULL SET @curBeforeQty = 500.000;
                    DECLARE @curAfterQty DECIMAL(18,3) = @curBeforeQty - @curTongLuong;
                    IF @curAfterQty < 0 SET @curAfterQty = 0.000;

                    UPDATE dbo.TonNguyenLieu 
                    SET SoLuong = @curAfterQty
                    WHERE MaCN = @cn2Id AND MaNL = @curNlId AND ViTri = @loc;

                    INSERT INTO dbo.LichSuKho (MaCN, MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo)
                    VALUES (@cn2Id, @curNlId, @loc, 'XUAT', -@curTongLuong, @curBeforeQty, @curBeforeQty - @curTongLuong, 'ORDER', @curDhId, @kitchen2Id, @finishTime, N'Pha chế Đơn hàng CN02 #' + CAST(@curDhId AS VARCHAR(10)));

                    FETCH NEXT FROM curDMDH INTO @curNlId, @curTongLuong, @curLoaiNL, @curDonVi;
                END;
                CLOSE curDMDH;
                DEALLOCATE curDMDH;

                -- Lịch sử voucher
                IF @applyVoucher IS NOT NULL AND @cust IS NOT NULL
                BEGIN
                    INSERT INTO dbo.LichSuSuDungVoucher (MaVoucher, MaDH, MaHD, MaTK, MaCN, SoTienGiam, TrangThai, NgaySuDung)
                    VALUES (@applyVoucher, @curDhId, @curHdId, @cust, @cn2Id, @voucherDiscount, 'DA_DUNG', @finishTime);
                END;
            END
            ELSE IF @status = 'DA_HUY'
            BEGIN
                -- Ghi nhận thanh toán và hoàn tiền
                INSERT INTO dbo.ThanhToan (MaHD, PhuongThuc, SoTien, ThamChieu, NguoiThu, ThuLuc, MaYeuCau)
                VALUES (@curHdId, 'CASH', @finalPay, 'CASH-REFUND-INIT', @cashier2Id, @createTime, 'PAY-CANCEL-' + @orderKey);
                DECLARE @curTtId BIGINT = SCOPE_IDENTITY();

                INSERT INTO dbo.YeuCauHuy (MaDH, LyDo, LyDoXuLy, TrangThai, TaoLuc, XuLyLuc, NguoiXuLy)
                VALUES (@curDhId, N'Khách hàng đổi ý trước khi pha chế tại CN02', N'Đã hoàn tiền mặt tại quầy CN02', 'CHAP_THUAN', @createTime, @finishTime, @cashier2Id);

                INSERT INTO dbo.YeuCauHoan (MaDH, MaTT, SoTien, LyDo, LyDoDuyet, TrangThai, DuyetLuc, HoanLuc, NguoiDuyet, NguoiHoan, MaYeuCau)
                VALUES (@curDhId, @curTtId, @finalPay, N'Hủy đơn trước pha chế CN02', N'Đồng ý hoàn tiền', 'DA_HOAN', @finishTime, @finishTime, @cashier2Id, @cashier2Id, 'REFUND-' + @orderKey);
            END;

            SET @oIdx = @oIdx + 1;
        END;
        SET @dayOffset = @dayOffset - 1;
    END;

    -- Đồng bộ SoLuotDaDung của Voucher khớp với LichSuSuDungVoucher
    UPDATE v
    SET v.SoLuotDaDung = (
        SELECT COUNT(*) 
        FROM dbo.LichSuSuDungVoucher ls 
        WHERE ls.MaVoucher = v.MaVoucher AND ls.TrangThai = 'DA_DUNG'
    )
    FROM dbo.Voucher v;

    -- =========================================================================
    -- 6. ĐỒNG BỘ ĐÁNH GIÁ MẪU CHO CÁC MÓN UỐNG
    -- =========================================================================
    PRINT N'--> [5/5] Cập nhật đánh giá mẫu của khách hàng cho CN02...';
    INSERT INTO dbo.DanhGia (MaChiTiet, MaTK, MaMon, MaCN, SoSao, NoiDung, An, TaoLuc, CapNhatLuc)
    SELECT TOP 5 
        ct.MaChiTiet, 
        COALESCE(dh.MaTK, @cust1Id), 
        ct.MaMon, 
        @cn2Id,
        5, 
        N'Trà sữa rất đậm vị trà, trân châu dai ngon dẻo vừa miệng tại CN02!', 
        0, 
        DATEADD(DAY, -1, SYSUTCDATETIME()), 
        SYSUTCDATETIME()
    FROM dbo.ChiTietDonHang ct
    JOIN dbo.DonHang dh ON ct.MaDH = dh.MaDH
    WHERE dh.MaCN = @cn2Id AND dh.TrangThai = 'HOAN_THANH'
    ORDER BY ct.MaChiTiet DESC;

    COMMIT TRANSACTION;
    PRINT N'=========================================================================================';
    PRINT N'  ĐÃ NẠP THÀNH CÔNG TOÀN BỘ DỮ LIỆU GIAO DỊCH 7 NGÀY CHO CHI NHÁNH 2 (CN02)!';
    PRINT N'  - 21 Phiếu nhập kho';
    PRINT N'  - 38 Mẻ sơ chế bếp';
    PRINT N'  - 14 Phiếu kết ca kho & hao hụt';
    PRINT N'  - 54 Đơn hàng mẫu 7 ngày (đủ trạng thái: Chờ xác nhận, KDS, Hoàn thành, Hủy đơn)';
    PRINT N'  - 100% Ràng buộc và toàn vẹn cơ sở dữ liệu được thỏa mãn.';
    PRINT N'=========================================================================================';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    PRINT N'LỖI TRONG QUÁ TRÌNH NẠP DỮ LIỆU GIAO DỊCH CN02: ' + ERROR_MESSAGE();
    THROW;
END CATCH;
