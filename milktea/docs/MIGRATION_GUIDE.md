# MIGRATION GUIDE — HƯỚNG DẪN MIGRATION DỮ LIỆU ĐA CHI NHÁNH

Tài liệu này hướng dẫn chi tiết quy trình di chuyển cơ sở dữ liệu từ mô hình đơn quán hiện hữu sang mô hình chuỗi đa chi nhánh mà không làm gián đoạn hệ thống, không làm mất mát bất kỳ bản ghi lịch sử giao dịch hay số dư tồn kho nào.

---

## 1. Nguyên tắc An toàn Tuyệt đối
1. **Tuyệt đối KHÔNG chạy DROP DATABASE / DROP TABLE** trên cơ sở dữ liệu `milktea` đang chứa dữ liệu thật.
2. **Sao lưu trước khi thao tác**: Luôn tạo bản backup đầy đủ (hoặc snapshot database) trước khi thực thi bất kỳ script DDL/DML nào.
3. **Thứ tự thực hiện nghiêm ngặt**:
   - Bước 1: Sao lưu database.
   - Bước 2: Chạy các câu lệnh đối soát đếm số lượng bản ghi trước migration (Pre-check).
   - Bước 3: Thêm các bảng mới (`ChiNhanh`, `NhanVienChiNhanh`, `MonUongChiNhanh`, `DanhGia`, `Voucher`, ...).
   - Bước 4: Thêm chi nhánh mặc định `CN01` ("MilkTea Quán 01 - Võ Văn Ngân").
   - Bước 5: Thêm cột `MaCN` (cho phép NULL tạm thời) vào các bảng giao dịch & kho hiện có.
   - Bước 6: Backfill giá trị `MaCN` trỏ về chi nhánh `CN01`.
   - Bước 7: Cập nhật cột `MaCN` thành `NOT NULL`, thêm Foreign Key và điều chỉnh các Unique Constraint / Primary Key.
   - Bước 8: Chạy các câu lệnh đối soát đếm số lượng bản ghi sau migration (Post-check) và so khớp 100%.

---

## 2. Phương án Sao lưu & Phục hồi (Backup & Restore)

### Câu lệnh sao lưu Full Backup (SQL Server):
```sql
BACKUP DATABASE milktea
TO DISK = 'C:\Users\mkhoa\milktea_backup_pre_branch.bak'
WITH FORMAT, MEDIANAME = 'MilkTeaBackup', NAME = 'Full Backup before Multi-Branch Migration';
GO
```

### Phương án phục hồi khẩn cấp nếu có lỗi (Rollback):
```sql
USE master;
ALTER DATABASE milktea SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
RESTORE DATABASE milktea
FROM DISK = 'C:\Users\mkhoa\milktea_backup_pre_branch.bak'
WITH REPLACE;
ALTER DATABASE milktea SET MULTI_USER;
GO
```

---

## 3. Truy vấn Đối soát Trước và Sau Migration (Pre/Post Checks)

Để đảm bảo không mất mát dữ liệu, các chỉ số sau phải được ghi nhận và khớp chính xác 100%:

```sql
USE milktea;
GO

-- 1. Đếm số lượng thực thể cốt lõi
SELECT 'TaiKhoan' AS Bang, COUNT(*) AS SoLuong FROM dbo.TaiKhoan
UNION ALL
SELECT 'Ban', COUNT(*) FROM dbo.Ban
UNION ALL
SELECT 'DanhMuc', COUNT(*) FROM dbo.DanhMuc
UNION ALL
SELECT 'MonUong', COUNT(*) FROM dbo.MonUong
UNION ALL
SELECT 'NguyenLieu', COUNT(*) FROM dbo.NguyenLieu
UNION ALL
SELECT 'TonNguyenLieu', COUNT(*) FROM dbo.TonNguyenLieu
UNION ALL
SELECT 'CongThucMon', COUNT(*) FROM dbo.CongThucMon
UNION ALL
SELECT 'PhienBan', COUNT(*) FROM dbo.PhienBan
UNION ALL
SELECT 'GioPhien', COUNT(*) FROM dbo.GioPhien
UNION ALL
SELECT 'ChiTietGioPhien', COUNT(*) FROM dbo.ChiTietGioPhien
UNION ALL
SELECT 'DonHang', COUNT(*) FROM dbo.DonHang
UNION ALL
SELECT 'ChiTietDonHang', COUNT(*) FROM dbo.ChiTietDonHang
UNION ALL
SELECT 'ChiTietDonHangTopping', COUNT(*) FROM dbo.ChiTietDonHangTopping
UNION ALL
SELECT 'HoaDon', COUNT(*) FROM dbo.HoaDon
UNION ALL
SELECT 'ThanhToan', COUNT(*) FROM dbo.ThanhToan
UNION ALL
SELECT 'YeuCauHoan', COUNT(*) FROM dbo.YeuCauHoan
UNION ALL
SELECT 'PhieuNhap', COUNT(*) FROM dbo.PhieuNhap
UNION ALL
SELECT 'MeSoChe', COUNT(*) FROM dbo.MeSoChe
UNION ALL
SELECT 'LichSuKho', COUNT(*) FROM dbo.LichSuKho;

-- 2. Đối soát tổng tiền và số dư kho
SELECT 
    (SELECT SUM(TongTien) FROM dbo.HoaDon) AS TongTienHoaDon,
    (SELECT SUM(SoTien) FROM dbo.ThanhToan) AS TongTienThanhToan,
    (SELECT SUM(SoLuong) FROM dbo.TonNguyenLieu WHERE ViTri = 'KHO') AS TongTonKho,
    (SELECT SUM(SoLuong) FROM dbo.TonNguyenLieu WHERE ViTri = 'BEP') AS TongTonBep;
```

---

## 4. Kế hoạch chuyển đổi DDL chi tiết (Thực hiện ở Giai đoạn 2)
1. **Bảng Chi Nhánh (`ChiNhanh`)**:
   - `MaCN BIGINT IDENTITY(1,1) PRIMARY KEY`
   - `MaChiNhanh VARCHAR(32) NOT NULL UNIQUE` (vd: `CN01`)
   - `TenCN NVARCHAR(120) NOT NULL`
   - `DiaChi NVARCHAR(255) NOT NULL`
   - `SoDienThoai VARCHAR(20) NULL`
   - `GioMoCua TIME NULL`
   - `GioDongCua TIME NULL`
   - `HoatDong BIT NOT NULL DEFAULT 1`
2. **Bảng Phân công Nhân sự (`NhanVienChiNhanh`)**:
   - `MaNhanVienCN BIGINT IDENTITY(1,1) PRIMARY KEY`
   - `MaTK BIGINT NOT NULL FOREIGN KEY REFERENCES TaiKhoan(MaTK)`
   - `MaCN BIGINT NOT NULL FOREIGN KEY REFERENCES ChiNhanh(MaCN)`
   - `VaiTroTaiCN VARCHAR(16) NULL`
   - `UNIQUE(MaTK, MaCN)`
3. **Bảng Món uống theo Chi nhánh (`MonUongChiNhanh`)**:
   - `MaMonCN BIGINT IDENTITY(1,1) PRIMARY KEY`
   - `MaCN BIGINT NOT NULL FOREIGN KEY REFERENCES ChiNhanh(MaCN)`
   - `MaMon BIGINT NOT NULL FOREIGN KEY REFERENCES MonUong(MaMon)`
   - `KinhDoanh BIT NOT NULL DEFAULT 1`
   - `ConPhucVu BIT NOT NULL DEFAULT 1`
   - `UNIQUE(MaCN, MaMon)`
4. **Cập nhật Bảng Bàn (`Ban`)**:
   - Thêm cột `MaCN BIGINT NULL FOREIGN KEY REFERENCES ChiNhanh(MaCN)`.
   - Update `MaCN = 1` cho 12 bàn hiện có.
   - Set `MaCN NOT NULL`.
   - Xóa constraint cũ `UQ_Ban_TenBan`.
   - Thêm constraint mới `CONSTRAINT UQ_Ban_ChiNhanh_Ten UNIQUE (MaCN, TenBan)`.
5. **Cập nhật Bảng Tồn Kho (`TonNguyenLieu`)**:
   - Tạo bảng tạm lưu trữ số dư hiện hữu.
   - Thêm cột `MaCN` vào `TonNguyenLieu`.
   - Cập nhật khóa chính: `CONSTRAINT PK_TonNguyenLieu PRIMARY KEY (MaCN, MaNL, ViTri)`.
   - Backfill 76 bản ghi tồn kho hiện tại vào `MaCN = 1`.
