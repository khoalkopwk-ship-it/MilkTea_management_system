# HƯỚNG DẪN CẬP NHẬT & TRIỂN KHAI CƠ SỞ DỮ LIỆU MILKTEA CHAIN

Tài liệu này cung cấp hướng dẫn chi tiết để nâng cấp cơ sở dữ liệu đã deploy trên Internet (từ commit `a5f976adf919396052960a2550dcbca73562fe59`) hoặc triển khai mới từ đầu một cách nhanh chóng, an toàn và toàn vẹn dữ liệu 100%.

---

## 1. Cấu Trúc Các Tệp Tin Cơ Sở Dữ Liệu Rút Gọn

Thư mục `database/` đã được tổ chức lại tinh gọn và đầy đủ thành 4 tệp tin chính:

| Tệp tin | Loại | Mục đích sử dụng |
| :--- | :--- | :--- |
| **`database/update_from_previous_deploy.sql`** | **Update Schema & Data Core** | **Dành cho CSDL đã deploy trên Internet (commit `a5f976a`)**. Nâng cấp trực tiếp, bảo toàn 100% dữ liệu cũ (tài khoản, đơn hàng, hóa đơn, tồn kho), tự động bổ sung bảng, cột, khóa ngoại và ràng buộc toàn vẹn mới nhất. |
| **`database/update_sample_data_from_previous_deploy.sql`** | **Update Sample Data (Kết hợp dữ liệu mẫu)** | **Kết hợp dữ liệu nền tảng mới vào CSDL cũ đã deploy**. Bổ sung thêm các tài khoản quản lý, chi nhánh 2, kho tổng, tồn kho ban đầu, voucher và đánh giá mẫu vào CSDL đã deploy mà không làm mất bất kỳ đơn hàng hay dữ liệu cũ nào. |
| **`database/seed_branch_2_transactions.sql`** | **Seed Branch 2 Transactions (7-Day Multi-Status)** | **Bổ sung 54 đơn hàng mẫu 7 ngày (đủ trạng thái: Chờ xác nhận, KDS, Hoàn thành, Hủy), 21 phiếu nhập kho, 38 mẻ sơ chế và 14 kết ca kho cho riêng Chi nhánh 2 (CN02)**. Giúp website có dữ liệu sinh động, đủ các phiếu mẫu và báo cáo 2 chi nhánh mà không làm mất đơn cũ của Chi nhánh 1. |
| **`database/1_schema.sql`** | **Full Schema DDL** | **Dành cho cài đặt mới từ đầu (Fresh Install)**. Chứa toàn bộ 38 bảng hoàn chỉnh đã hợp nhất tất cả các giai đoạn (Đa chi nhánh, Yêu thích, Đánh giá, Voucher, Kho tổng, Hero banner...). |
| **`database/sample_data.sql`** | **Full Combined Seed Data** | Dữ liệu mẫu chuẩn kết hợp hoàn chỉnh từ commit cũ và mới nhất (11 tài khoản đầy đủ, 2 chi nhánh vận hành + 1 kho tổng, thực đơn 50 món, định mức, tồn kho hai tầng và các kịch bản mẫu). |

---

## 2. Kịch Bản 1: Nâng Cấp CSDL Đã Deploy Trên Internet (Từ commit a5f976a)

> [!IMPORTANT]
> **Kịch bản này KHÔNG xóa dữ liệu cũ.** Các tệp script được thiết kế theo tiêu chuẩn Idempotent (`IF NOT EXISTS`, `IF COL_LENGTH(...) IS NULL`, `MERGE`), bạn có thể chạy một lần hoặc nhiều lần mà không gây lỗi hay trùng lặp.

### Các bước thực hiện:

1. **Bước 1: Nâng cấp cấu trúc Schema & Dữ liệu cốt lõi**:
   - Mở tệp: [`database/update_from_previous_deploy.sql`](update_from_previous_deploy.sql) trong SSMS hoặc Azure Data Studio.
   - Chọn database `milktea` đang kết nối.
   - Nhấn **Execute (F5)**.

2. **Bước 2 (Tùy chọn): Kết hợp bổ sung dữ liệu mẫu nền tảng vào CSDL cũ**:
   - Mở tệp: [`database/update_sample_data_from_previous_deploy.sql`](update_sample_data_from_previous_deploy.sql).
   - Nhấn **Execute (F5)** để nạp thêm Chi nhánh 2, Kho tổng, Tài khoản quản lý, Voucher và Tồn kho ban đầu.

3. **Bước 3 (Khuyến nghị để demo/báo cáo): Bổ sung toàn bộ dữ liệu giao dịch 7 ngày cho Chi nhánh 2**:
   - Mở tệp: [`database/seed_branch_2_transactions.sql`](seed_branch_2_transactions.sql).
   - Nhấn **Execute (F5)** để nạp đầy đủ 54 đơn hàng đủ các trạng thái (KDS, Bàn, Hủy đơn, Hoàn thành), 21 phiếu nhập kho, 38 mẻ sơ chế và 14 phiếu kết ca kho cho Chi nhánh 2.
   - Dữ liệu cũ của Chi nhánh 1 vẫn được giữ nguyên 100%.

1. **Sao lưu dữ liệu trước khi chạy (Khuyến nghị)**:
   - Trên SQL Server Management Studio (SSMS) hoặc công cụ quản trị cloud: Click chuột phải vào database `milktea` -> **Tasks** -> **Back Up...** để lưu 1 bản snapshot dự phòng.

2. **Chạy script cập nhật**:
   - Mở tệp: [`database/update_from_previous_deploy.sql`](update_from_previous_deploy.sql) trong SSMS hoặc Azure Data Studio.
   - Chọn database `milktea` đang kết nối.
   - Nhấn **Execute (F5)** để thực thi.

3. **Nội dung tự động nâng cấp bao gồm**:
   - **Đa chi nhánh**: Tạo bảng `ChiNhanh`, `NhanVienChiNhanh`, `MonUongChiNhanh`. Tự động tạo `CN01`, `CN02`, `KHOTONG`.
   - **Gán chi nhánh mặc định**: Bổ sung cột `MaCN` vào các bảng `Ban`, `DonHang`, `TonNguyenLieu`, `PhieuNhap`, `MeSoChe`, `KetCaKho`, `LichSuKho` và tự động cập nhật toàn bộ dữ liệu hiện có về Chi nhánh 1 (`CN01`) để bảo toàn tính toàn vẹn.
   - **Trải nghiệm khách hàng**: Tạo bảng `SanPhamYeuThich`, `DanhGia`, `DanhGiaMedia`.
   - **Khuyến mãi & Voucher**: Tạo bảng `Voucher`, `VoucherChiNhanh`, `LichSuSuDungVoucher`. Bổ sung cột snapshot voucher vào `HoaDon` và `DonHang`.
   - **Cấu hình chung**: Bổ sung `Hotline`, `GioHoatDong`, `HeroBannerUrl`, `HeroBannerPublicId` vào `CauHinhChung` và `GioMoCua` vào `ChiNhanh`.
   - **Ràng buộc bất biến**: Bổ sung filtered index 1 phiên OPEN/bàn (`UQ_PhienBan_MaBan_Open`), kiểm tra tính toán tiền thanh toán (`CK_HoaDon_PhaiTra_Calc`), số học kết ca và lịch sử kho.

---

## 3. Kịch Bản 2: Cài Đặt Mới Hoàn Toàn (Fresh Clean Install)

Dành cho trường hợp triển khai trên môi trường máy chủ mới hoặc tạo database demo sạch từ đầu:

1. **Khởi tạo Schema**:
   - Chạy tệp [`database/1_schema.sql`](1_schema.sql).
   - Script sẽ tạo mới cơ sở dữ liệu `milktea` với toàn bộ 38 bảng hoàn chỉnh.

2. **Nạp dữ liệu mẫu**:
   - Chạy tệp [`database/sample_data.sql`](sample_data.sql).
   - Nạp đầy đủ dữ liệu cấu hình, danh mục, thực đơn, chi nhánh, công thức và nhân sự chuẩn.

---

## 4. Kiểm Tra & Đối Soát Tính Toàn Vẹn Sau Khi Triển Khai (Verification)

Sau khi chạy xong script cập nhật hoặc cài mới, bạn chỉ cần thực thi script kiểm tra tự động:

- Mở tệp: [`database/validation/verify_demo_integrity.sql`](validation/verify_demo_integrity.sql) và nhấn **Execute (F5)**.
- Kết quả mong đợi trên cửa sổ `Messages`:
  ```text
  ====================================================================
    KIỂM TRA TÍNH TOÀN VẸN DỮ LIỆU CHUỖI ĐA CHI NHÁNH (DEMO INTEGRITY)
  ====================================================================
  1. Kiểm tra danh mục Chi Nhánh:
     [PASS] Có đủ 3 chi nhánh cốt lõi (KHOTONG, CN01, CN02) đang hoạt động.
  2. Kiểm tra Tài khoản & Phân công Nhân sự chi nhánh:
     [PASS] Tất cả tài khoản vận hành cốt lõi đều active.
     [PASS] 100% nhân viên (MANAGER, CASHIER, KITCHEN) có phân công chi nhánh hoạt động.
  3. Kiểm tra Bàn Ăn, Mã QR & Bất biến 1 phiên OPEN / bàn:
     [PASS] 100% bàn ăn hợp lệ và tuân thủ bất biến 1 phiên OPEN / bàn.
  4. Kiểm tra Tồn Kho Hai Tầng (KHO & BEP):
     [PASS] Tồn kho hai tầng hợp lệ trên tất cả chi nhánh.
  5. Kiểm tra Danh mục & Thực đơn bán hàng:
     [PASS] Danh mục và thực đơn hiển thị hợp lệ.
  6. Kiểm tra Hệ thống Voucher & Khuyến mãi:
     [PASS] Hệ thống Voucher hợp lệ và sẵn sàng.
  7. Kiểm tra Hệ thống Đánh giá & Yêu thích:
     [PASS] Hệ thống Đánh giá & Wishlist hợp lệ.
  ====================================================================
    KẾT QUẢ ĐỐI SOÁT: 100% ĐẠT CHUẨN (0 lỗi vi phạm)
  ====================================================================
  ```

---

## 5. Tài Khoản Quản Trị & Đăng Nhập Sau Khi Nâng Cấp

Tất cả các tài khoản mặc định đều có mật khẩu: `123456`

| Vai trò | Email đăng nhập | Phạm vi |
| :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin@milktea.vn` | Toàn chuỗi |
| **Quản lý Chi nhánh 1** | `manager1@milktea.vn` | Chi nhánh 01 (Võ Văn Ngân) |
| **Quản lý Chi nhánh 2** | `manager2@milktea.vn` | Chi nhánh 02 (D2 Bình Thạnh) |
| **Thu ngân Chi nhánh 1** | `cashier@milktea.vn` | Chi nhánh 01 |
| **Bếp Chi nhánh 1** | `kitchen@milktea.vn` | Chi nhánh 01 |
| **Thu ngân Chi nhánh 2** | `cashier2@milktea.vn` | Chi nhánh 02 |
| **Bếp Chi nhánh 2** | `kitchen2@milktea.vn` | Chi nhánh 02 |
