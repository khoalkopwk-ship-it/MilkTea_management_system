# Hướng Dẫn & Tham Khảo Bộ Dữ Liệu Mẫu (MilkTea Management System)

Tài liệu này tổng hợp toàn bộ **dữ liệu mẫu chuẩn** của hệ thống quản lý quán trà sữa MilkTea, bao gồm: cấu hình quán, danh mục, thực đơn món uống (60 biến thể size), 10 loại topping, 37 nguyên liệu thô/sơ chế, công thức pha chế 70 món, công thức sơ chế bếp và thiết lập tồn kho mẫu.

> [!NOTE]
> Để đảm bảo tính tinh gọn khi triển khai (Production Deploy), cơ sở dữ liệu mặc định chỉ khởi tạo **cấu hình quán**, **12 mã QR bàn** và **1 tài khoản mặc định cho mỗi vai trò**.
> Các thành viên có thể tham khảo bảng dữ liệu bên dưới để:
> 1. Nhập liệu thủ công qua giao diện Web Quản trị (`/admin/menu`, `/admin/inventory`, `/admin/staff`).
> 2. Hoặc copy đoạn mã SQL ở **Mục 7** chạy trực tiếp vào cơ sở dữ liệu để có ngay dữ liệu demo đầy đủ.

---

## 1. Cấu hình thông tin quán (Global Settings)

| Thuộc tính | Giá trị mẫu | Ghi chú |
| :--- | :--- | :--- |
| **Tên quán** | MilkTea Cafe | Hiển thị trên đầu trang và hóa đơn |
| **Địa chỉ** | Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh | In trên hóa đơn thanh toán |
| **Ngân hàng** | MBBank - Chi nhánh Long Khánh | Phục vụ tạo mã VietQR động |
| **Số tài khoản** | 08881706888 | Số tài khoản nhận tiền chuyển khoản |
| **Chủ tài khoản** | DANG NGUYEN MINH KHOA | Tên chủ thẻ hiển thị khi quét QR |
| **Chiết khấu chung**| 10% (`PERCENT`) | Tỷ lệ giảm giá mặc định toàn quán |

---

## 2. Danh sách tài khoản thử nghiệm & Demo

Tất cả các tài khoản demo đều sử dụng mật khẩu mặc định: `Password@123`  
*(Chuỗi mã hóa BCrypt: `$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq`)*

| Email | Họ và tên | Số điện thoại | Vai trò (Role) | Chức năng chính |
| :--- | :--- | :--- | :--- | :--- |
| `admin@milktea.vn` | Quản Trị Viên Hệ Thống | 0901000001 | `ADMIN` | Quản trị toàn quyền, menu, kho, nhân sự, báo cáo |
| `admin2@milktea.vn` | Quản Lý Vận Hành Tuấn | 0901000002 | `ADMIN` | Quản trị dự phòng |
| `cashier@milktea.vn` | Thu Ngân Quỳnh | 0901000003 | `CASHIER` | Mở/đóng phiên bàn, duyệt đơn, ghi thu tiền, in bill |
| `cashier2@milktea.vn` | Thu Ngân Hoàng | 0901000004 | `CASHIER` | Thu ngân ca chiều |
| `cashier3@milktea.vn` | Thu Ngân Thảo | 0901000005 | `CASHIER` | Thu ngân ca tối |
| `kitchen@milktea.vn` | Bếp Trưởng Lân | 0901000006 | `KITCHEN` | Màn hình bếp KDS, nhận chế biến, hoàn thành đơn, sơ chế |
| `kitchen2@milktea.vn` | Pha Chế Minh | 0901000007 | `KITCHEN` | Nhân viên pha chế |
| `kitchen3@milktea.vn` | Pha Chế Nga | 0901000008 | `KITCHEN` | Nhân viên sơ chế |
| `customer@milktea.vn` | Khách Hàng Mai | 0901000009 | `CUSTOMER` | Khách hàng đặt món, tích điểm, lịch sử đơn |
| `customer2@milktea.vn` | Khách Hàng Đức | 0901000010 | `CUSTOMER` | Khách hàng thân thiết |
| `customer3@milktea.vn` | Khách Hàng Linh | 0901000011 | `CUSTOMER` | Khách hàng vãng lai |
| `customer4@milktea.vn` | Khách Hàng Bảo | 0901000012 | `CUSTOMER` | Khách hàng online |

---

## 3. Danh sách bàn & Mã QR bàn (12 bàn)

URL truy cập tương ứng của mỗi bàn trên web: `http://<domain>/?qr=<Mã_QR>`

| Tên bàn | Mã QR Token | Trạng thái ban đầu |
| :--- | :--- | :--- |
| **Bàn 01** | `QR_TABLE_01_TOKEN_A1B2C3D4` | `TRONG` |
| **Bàn 02** | `QR_TABLE_02_TOKEN_A2B3C4D5` | `TRONG` |
| **Bàn 03** | `QR_TABLE_03_TOKEN_A3B4C5D6` | `TRONG` |
| **Bàn 04** | `QR_TABLE_04_TOKEN_A4B5C6D7` | `TRONG` |
| **Bàn 05** | `QR_TABLE_05_TOKEN_A5B6C7D8` | `TRONG` |
| **Bàn 06** | `QR_TABLE_06_TOKEN_A6B7C8D9` | `TRONG` |
| **Bàn 07** | `QR_TABLE_07_TOKEN_A7B8C9D0` | `TRONG` |
| **Bàn 08** | `QR_TABLE_08_TOKEN_A8B9C0D1` | `TRONG` |
| **Bàn 09** | `QR_TABLE_09_TOKEN_A9B0C1D2` | `TRONG` |
| **Bàn 10** | `QR_TABLE_10_TOKEN_B0C1D2E3` | `TRONG` |
| **Bàn 11** | `QR_TABLE_11_TOKEN_B1C2D3E4` | `TRONG` |
| **Bàn 12** | `QR_TABLE_12_TOKEN_B2C3D4E5` | `TRONG` |

---

## 4. Danh mục sản phẩm (11 Danh mục)

| STT | Tên danh mục | Cho phép chọn Topping kèm (`ChoPhepTopping`) |
| :--- | :--- | :---: |
| 1 | **Trà Sữa Truyền Thống** | BẬT (1) |
| 2 | **Trà Sữa Ô Long** | BẬT (1) |
| 3 | **Trà Sữa Lài** | BẬT (1) |
| 4 | **Trà Trái Cây Tươi** | BẬT (1) |
| 5 | **Cà Phê Truyền Thống** | BẬT (1) |
| 6 | **Cà Phê Sáng Tạo** | BẬT (1) |
| 7 | **Sữa Tươi Trân Châu** | BẬT (1) |
| 8 | **Macchiato & Váng Sữa** | BẬT (1) |
| 9 | **Đá Xay & Sinh Tố** | BẬT (1) |
| 10 | **Trà Thảo Mộc & Thanh Nhiệt** | BẬT (1) |
| 11 | **Topping Cao Cấp** | TẮT (0) *(Bản thân là topping, không chọn kèm topping khác)* |

---

## 5. Thực đơn món uống & Topping (30 Món nước + 10 Topping)

### A. 30 Món Nước Uống (Gồm 2 Size: M - 500ml và L - 700ml)

| STT | Tên món | Danh mục | Giá Size M (VNĐ) | Giá Size L (VNĐ) | Mô tả tóm tắt |
| :--- | :--- | :--- | :---: | :---: | :--- |
| 1 | **Trà Sữa Truyền Thống** | Trà Sữa Truyền Thống | 25,000 | 30,000 | Hương vị trà đen đậm đà hòa quyện cùng sữa béo thơm ngon |
| 2 | **Trà Sữa Thái Xanh** | Trà Sữa Truyền Thống | 28,000 | 33,000 | Trà xanh Thái Lan thanh mát, thơm ngát thảo mộc |
| 3 | **Trà Sữa Thái Đỏ** | Trà Sữa Truyền Thống | 28,000 | 33,000 | Trà đỏ Thái Lan truyền thống thơm nồng vị thảo mộc |
| 4 | **Trà Sữa Socola Đậm Đà** | Trà Sữa Truyền Thống | 32,000 | 37,000 | Vị socola hảo hạng kết hợp trà đen thơm lừng |
| 5 | **Trà Sữa Khoai Môn Hoàng Kim** | Trà Sữa Truyền Thống | 32,000 | 37,000 | Khoai môn bùi béo tự nhiên hòa quyện sữa tươi |
| 6 | **Trà Sữa Ô Long Nướng** | Trà Sữa Ô Long | 30,000 | 35,000 | Hương vị ô long rang cháy đặc trưng thơm lừng khó cưỡng |
| 7 | **Trà Sữa Ô Long Thiết Quan Âm**| Trà Sữa Ô Long | 32,000 | 38,000 | Trà thiết quan âm thượng hạng hậu ngọt thanh tao |
| 8 | **Trà Sữa Lài Thơm** | Trà Sữa Lài | 28,000 | 33,000 | Hoa lài tự nhiên thơm ngát hòa cùng sữa dịu ngọt |
| 9 | **Trà Sữa Lài Kem Cheese** | Trà Sữa Lài | 35,000 | 42,000 | Trà sữa lài thanh tao kèm váng phô mai béo ngậy |
| 10 | **Trà Sữa Matcha Nhật Bản** | Trà Sữa Truyền Thống | 35,000 | 42,000 | Bột matcha Uji thượng phẩm thơm lừng vị trà xanh mộc |
| 11 | **Trà Đào Cam Sả** | Trà Trái Cây Tươi | 28,000 | 33,000 | Trà đen ủ lạnh cùng cam vàng mọng nước và sả tươi đập dập |
| 12 | **Trà Vải Hoa Hồng** | Trà Trái Cây Tươi | 30,000 | 35,000 | Vải thiều ngọt lịm phảng phất hương hoa hồng lãng mạn |
| 13 | **Trà Dâu Tây Nhiệt Đới** | Trà Trái Cây Tươi | 32,000 | 38,000 | Dâu tây Đà Lạt tươi giã tay thơm lừng vị quả mọng |
| 14 | **Trà Xoài Chanh Dây** | Trà Trái Cây Tươi | 30,000 | 35,000 | Vị chua ngọt bùng nổ giữa xoài cát chín và chanh leo |
| 15 | **Trà Ổi Hồng Hạt Chia** | Trà Trái Cây Tươi | 32,000 | 37,000 | Nước cốt ổi hồng thơm ngát giàu vitamin kèm hạt chia |
| 16 | **Trà Táo Xanh Bạc Hà** | Trà Trái Cây Tươi | 30,000 | 35,000 | Táo xanh giòn rụm kết hợp bạc hà mát lạnh the mát |
| 17 | **Trà Bưởi Mật Ong** | Trà Trái Cây Tươi | 32,000 | 38,000 | Tép bưởi hồng mọng nước cùng mật ong hoa nhãn dịu ngọt |
| 18 | **Trà Mãng Cầu Xiêm** | Trà Trái Cây Tươi | 35,000 | 42,000 | Mãng cầu xiêm chua ngọt tự nhiên giã tay đậm đà |
| 19 | **Cà Phê Đen Đá** | Cà Phê Truyền Thống | 20,000 | 25,000 | Cà phê robusta rang mộc pha phin truyền thống đậm đà |
| 20 | **Cà Phê Sữa Đá** | Cà Phê Truyền Thống | 25,000 | 30,000 | Cà phê pha phin truyền thống hòa cùng sữa đặc sánh ngọt |
| 21 | **Cà Phê Muối Xứ Huế** | Cà Phê Sáng Tạo | 28,000 | 34,000 | Lớp kem sữa muối mằn mặn hòa cùng cà phê thơm nồng |
| 22 | **Bạc Xỉu Ba Tầng** | Cà Phê Sáng Tạo | 28,000 | 33,000 | Tầng sữa ngọt dịu, sữa tươi thơm mát và cà phê đậm vị |
| 23 | **Sữa Tươi Trân Châu Đường Đen**| Sữa Tươi Trân Châu | 35,000 | 42,000 | Sữa tươi thanh trùng Đà Lạt cùng trân châu đường đen dẻo quánh |
| 24 | **Sữa Tươi Sương Sáo Thảo Mộc** | Sữa Tươi Trân Châu | 32,000 | 38,000 | Sữa tươi béo nhẹ kết hợp sương sáo Đài Loan mát lành |
| 25 | **Trà Ô Long Váng Sữa Macchiato**| Macchiato & Váng Sữa | 35,000 | 42,000 | Cốt trà ô long thượng hạng phủ lớp macchiato kem sữa béo ngậy |
| 26 | **Trà Đen Macchiato Muối Biển** | Macchiato & Váng Sữa | 32,000 | 38,000 | Trà đen đậm đà tương phản với kem muối biển béo ngậy |
| 27 | **Matcha Đá Xay Kem Tuyết** | Đá Xay & Sinh Tố | 40,000 | 48,000 | Matcha xay nhuyễn cùng đá và sữa, phủ bông kem whipping tươi |
| 28 | **Cà Phê Đá Xay Caramel** | Đá Xay & Sinh Tố | 38,000 | 45,000 | Espresso xay đá cùng xốt caramel ngọt bùi phủ kem whipping |
| 29 | **Sinh Tố Bơ Dừa Thơm Béo** | Đá Xay & Sinh Tố | 42,000 | 50,000 | Bơ sáp Đắk Lắk dẻo quánh xay cùng nước cốt dừa béo bùi |
| 30 | **Trà Hoa Cúc Mật Ong Hạt Sen** | Trà Thảo Mộc & Thanh Nhiệt | 32,000 | 38,000 | Hoa cúc sấy khô ủ ấm mật ong rừng và hạt sen bùi ngậy |

### B. 10 Loại Topping Cao Cấp (Size M chuẩn)

| STT | Tên Topping | Size | Giá bán (VNĐ) | Mô tả |
| :--- | :--- | :---: | :---: | :--- |
| 1 | **Trân Châu Đen Hoàng Kim** | M | 6,000 | Trân châu dẻo dai hoàng kim nấu đường đen ngọt ngào |
| 2 | **Trân Châu Trắng 3Q** | M | 7,000 | Trân châu trắng giòn sần sật nhai vui miệng |
| 3 | **Thạch Đào Giòn Ngọt** | M | 8,000 | Thạch đào dai giòn thơm nức mùi quả chín |
| 4 | **Kem Cheese Phô Mai Macchiato**| M | 10,000 | Lớp kem cheese mằn mặn béo ngậy tan chảy trên đầu lưỡi |
| 5 | **Pudding Trứng Vàng** | M | 8,000 | Pudding mềm mịn núng nính thơm ngậy vị trứng gà |
| 6 | **Thạch Phô Mai Tươi** | M | 10,000 | Viên phô mai bọc thạch trong suốt béo ngậy |
| 7 | **Sương Sáo Thảo Mộc Đài Loan** | M | 6,000 | Sương sáo thanh mát thảo mộc giải nhiệt tức thì |
| 8 | **Thạch Củ Năng Giòn** | M | 8,000 | Củ năng bọc bột lọc giòn ngọt sảng khoái |
| 9 | **Nha Đam Tươi Đường Phèn** | M | 7,000 | Nha đam giòn mát nấu đường phèn thanh khiết |
| 10 | **Khúc Bạch Hạnh Nhân** | M | 10,000 | Khúc bạch phô mai thơm lừng rắc hạnh nhân giòn |

---

## 6. Danh mục 37 Nguyên liệu thô (THO) & Sơ chế (SOCHE)

| Tên nguyên liệu | Phân loại | Đơn vị | Định mức nhập / Giá vốn tham khảo |
| :--- | :---: | :---: | :--- |
| **Trà Đen Cổ Thụ** | `THO` | kg | 120,000 đ/kg |
| **Trà Ô Long Kim Tuyên** | `THO` | kg | 180,000 đ/kg |
| **Trà Lài Thượng Hạng** | `THO` | kg | 150,000 đ/kg |
| **Cà Phê Hạt Rang Mộc** | `THO` | kg | 160,000 đ/kg |
| **Sữa Đặc Có Đường** | `THO` | kg | 45,000 đ/kg |
| **Đường Cát Trắng** | `THO` | kg | 22,000 đ/kg |
| **Đường Đen Nữ Hoàng** | `THO` | kg | 35,000 đ/kg |
| **Sữa Tươi Thanh Trùng** | `THO` | kg | 38,000 đ/kg |
| **Bột Béo Thực Vật** | `THO` | kg | 65,000 đ/kg |
| **Bột Kem Cheese Anchor** | `THO` | kg | 220,000 đ/kg |
| **Trân Châu Thô Đen** | `THO` | kg | 30,000 đ/kg |
| **Hạt Trân Châu Trắng 3Q** | `THO` | kg | 42,000 đ/kg |
| **Đào Ngâm Đóng Hộp** | `THO` | kg | 55,000 đ/kg |
| **Vải Ngâm Đóng Hộp** | `THO` | kg | 60,000 đ/kg |
| **Dâu Tây Tươi** | `THO` | kg | 90,000 đ/kg |
| **Xoài Cát Chín** | `THO` | kg | 40,000 đ/kg |
| **Chanh Dây Tươi** | `THO` | kg | 30,000 đ/kg |
| **Cam Vàng Sành** | `THO` | kg | 35,000 đ/kg |
| **Sả Cây Tươi** | `THO` | kg | 20,000 đ/kg |
| **Bột Matcha Uji** | `THO` | kg | 650,000 đ/kg |
| **Muối Biển Tinh Khiết** | `THO` | kg | 10,000 đ/kg |
| **Bột Cacao Nguyên Chất** | `THO` | kg | 140,000 đ/kg |
| **Bơ Sáp Đắk Lắk** | `THO` | kg | 60,000 đ/kg |
| **Nước Cốt Dừa Đóng Lon** | `THO` | kg | 48,000 đ/kg |
| **Ly Nhựa PP 500ml** | `THO` | ly | 450 đ/ly |
| **Ly Nhựa PP 700ml** | `THO` | ly | 550 đ/ly |
| **Ống Hút Trân Châu** | `THO` | cai | 120 đ/cái |
| **Nước Cốt Trà Đen** | `SOCHE` | ml | Chế biến từ Trà Đen Cổ Thụ |
| **Nước Cốt Trà Ô Long** | `SOCHE` | ml | Chế biến từ Trà Ô Long Kim Tuyên |
| **Nước Cốt Trà Lài** | `SOCHE` | ml | Chế biến từ Trà Lài Thượng Hạng |
| **Nước Đường Pha Sẵn** | `SOCHE` | ml | Nấu từ Đường Cát Trắng & Nước |
| **Cốt Cà Phê Pha Phin** | `SOCHE` | ml | Pha phin từ Cà Phê Hạt Rang Mộc |
| **Trân Châu Nấu Chín** | `SOCHE` | g | Luộc và ngâm từ Trân Châu Thô Đen + Đường Đen |
| **Kem Cheese Thành Phẩm** | `SOCHE` | ml | Đánh bông từ Bột Kem Cheese + Sữa tươi |
| **Thạch Đào Cắt Hạt Lựu** | `SOCHE` | g | Sơ chế từ Đào Ngâm Đóng Hộp |
| **Sốt Dâu Tây Tươi** | `SOCHE` | ml | Giã nhuyễn từ Dâu Tây Tươi + Nước đường |
| **Sốt Xoài Chanh Dây** | `SOCHE` | ml | Nấu từ Xoài Cát Chín + Chanh Dây |

---

## 7. Script SQL Khôi phục Toàn bộ Dữ liệu Mẫu (Full Demo Restore Script)

Khi cần đưa website lên môi trường demo hoặc staging có đầy đủ menu, nguyên liệu và công thức, bạn chỉ cần mở SQL Server Management Studio (hoặc chạy qua `sqlcmd`) và chạy đoạn script sau:

```sql
USE milktea;
GO

SET NOCOUNT ON;
PRINT N'>>> Đang nạp danh mục, menu và nguyên liệu mẫu...';

-- 1. NẠP 11 DANH MỤC
DECLARE @Categories TABLE (TenDM NVARCHAR(100), ChoPhepTopping BIT);
INSERT INTO @Categories VALUES 
    (N'Trà Sữa Truyền Thống', 1), (N'Trà Sữa Ô Long', 1), (N'Trà Sữa Lài', 1),
    (N'Trà Trái Cây Tươi', 1), (N'Cà Phê Truyền Thống', 1), (N'Cà Phê Sáng Tạo', 1),
    (N'Sữa Tươi Trân Châu', 1), (N'Macchiato & Váng Sữa', 1), (N'Đá Xay & Sinh Tố', 1),
    (N'Trà Thảo Mộc & Thanh Nhiệt', 1), (N'Topping Cao Cấp', 0);

MERGE INTO dbo.DanhMuc AS target
USING @Categories AS source ON (target.TenDM = source.TenDM)
WHEN MATCHED THEN UPDATE SET target.ChoPhepTopping = source.ChoPhepTopping, target.HoatDong = 1
WHEN NOT MATCHED THEN INSERT (TenDM, ChoPhepTopping, HoatDong) VALUES (source.TenDM, source.ChoPhepTopping, 1);

-- 2. NẠP 37 NGUYÊN LIỆU
DECLARE @Materials TABLE (TenNL NVARCHAR(120), Loai VARCHAR(8), DonVi VARCHAR(16));
INSERT INTO @Materials VALUES 
    (N'Trà Đen Cổ Thụ', 'THO', 'kg'), (N'Trà Ô Long Kim Tuyên', 'THO', 'kg'),
    (N'Trà Lài Thượng Hạng', 'THO', 'kg'), (N'Cà Phê Hạt Rang Mộc', 'THO', 'kg'),
    (N'Sữa Đặc Có Đường', 'THO', 'kg'), (N'Đường Cát Trắng', 'THO', 'kg'),
    (N'Đường Đen Nữ Hoàng', 'THO', 'kg'), (N'Sữa Tươi Thanh Trùng', 'THO', 'kg'),
    (N'Bột Béo Thực Vật', 'THO', 'kg'), (N'Bột Kem Cheese Anchor', 'THO', 'kg'),
    (N'Trân Châu Thô Đen', 'THO', 'kg'), (N'Hạt Trân Châu Trắng 3Q', 'THO', 'kg'),
    (N'Đào Ngâm Đóng Hộp', 'THO', 'kg'), (N'Vải Ngâm Đóng Hộp', 'THO', 'kg'),
    (N'Dâu Tây Tươi', 'THO', 'kg'), (N'Xoài Cát Chín', 'THO', 'kg'),
    (N'Chanh Dây Tươi', 'THO', 'kg'), (N'Cam Vàng Sành', 'THO', 'kg'),
    (N'Sả Cây Tươi', 'THO', 'kg'), (N'Bột Matcha Uji', 'THO', 'kg'),
    (N'Muối Biển Tinh Khiết', 'THO', 'kg'), (N'Bột Cacao Nguyên Chất', 'THO', 'kg'),
    (N'Bơ Sáp Đắk Lắk', 'THO', 'kg'), (N'Nước Cốt Dừa Đóng Lon', 'THO', 'kg'),
    (N'Ly Nhựa PP 500ml', 'THO', 'ly'), (N'Ly Nhựa PP 700ml', 'THO', 'ly'),
    (N'Ống Hút Trân Châu', 'THO', 'cai'), (N'Nước Cốt Trà Đen', 'SOCHE', 'ml'),
    (N'Nước Cốt Trà Ô Long', 'SOCHE', 'ml'), (N'Nước Cốt Trà Lài', 'SOCHE', 'ml'),
    (N'Nước Đường Pha Sẵn', 'SOCHE', 'ml'), (N'Cốt Cà Phê Pha Phin', 'SOCHE', 'ml'),
    (N'Trân Châu Nấu Chín', 'SOCHE', 'g'), (N'Kem Cheese Thành Phẩm', 'SOCHE', 'ml'),
    (N'Thạch Đào Cắt Hạt Lựu', 'SOCHE', 'g'), (N'Sốt Dâu Tây Tươi', 'SOCHE', 'ml'),
    (N'Sốt Xoài Chanh Dây', 'SOCHE', 'ml');

MERGE INTO dbo.NguyenLieu AS target
USING @Materials AS source ON (target.TenNL = source.TenNL)
WHEN MATCHED THEN UPDATE SET target.Loai = source.Loai, target.DonVi = source.DonVi, target.HoatDong = 1
WHEN NOT MATCHED THEN INSERT (TenNL, Loai, DonVi, HoatDong) VALUES (source.TenNL, source.Loai, source.DonVi, 1);

-- 3. NẠP 30 MÓN NƯỚC UỐNG & 10 TOPPING
DECLARE @dmTraSuaTT AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Trà Sữa Truyền Thống');
DECLARE @dmTraSuaOL AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Trà Sữa Ô Long');
DECLARE @dmTraSuaLai AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Trà Sữa Lài');
DECLARE @dmTraTraiCay AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Trà Trái Cây Tươi');
DECLARE @dmCafeTT AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Cà Phê Truyền Thống');
DECLARE @dmCafeST AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Cà Phê Sáng Tạo');
DECLARE @dmSuaTuoi AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Sữa Tươi Trân Châu');
DECLARE @dmMacchiato AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Macchiato & Váng Sữa');
DECLARE @dmDaXay AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Đá Xay & Sinh Tố');
DECLARE @dmThaoMoc AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Trà Thảo Mộc & Thanh Nhiệt');
DECLARE @dmTopping AS BIGINT = (SELECT MaDM FROM dbo.DanhMuc WHERE TenDM = N'Topping Cao Cấp');

DECLARE @Products TABLE (MaDM BIGINT, TenMon NVARCHAR(120), Size VARCHAR(12), Gia DECIMAL(18,2), MoTa NVARCHAR(500));
INSERT INTO @Products VALUES
    (@dmTraSuaTT, N'Trà Sữa Truyền Thống', 'M', 25000, N'Hương vị trà đen đậm đà hòa quyện cùng sữa béo thơm ngon'),
    (@dmTraSuaTT, N'Trà Sữa Truyền Thống', 'L', 30000, N'Trà sữa truyền thống phiên bản ly lớn 700ml sảng khoái'),
    (@dmTraSuaTT, N'Trà Sữa Thái Xanh', 'M', 28000, N'Trà xanh Thái Lan thanh mát, thơm ngát thảo mộc'),
    (@dmTraSuaTT, N'Trà Sữa Thái Xanh', 'L', 33000, N'Trà sữa Thái xanh giải nhiệt ngày hè ly lớn'),
    (@dmTraSuaTT, N'Trà Sữa Thái Đỏ', 'M', 28000, N'Trà đỏ Thái Lan truyền thống thơm nồng vị thảo mộc'),
    (@dmTraSuaTT, N'Trà Sữa Thái Đỏ', 'L', 33000, N'Trà Thái đỏ đậm vị béo ngậy ly lớn'),
    (@dmTraSuaTT, N'Trà Sữa Socola Đậm Đà', 'M', 32000, N'Vị socola hảo hạng kết hợp trà đen thơm lừng'),
    (@dmTraSuaTT, N'Trà Sữa Socola Đậm Đà', 'L', 37000, N'Trà sữa socola đậm đà ly lớn'),
    (@dmTraSuaTT, N'Trà Sữa Khoai Môn Hoàng Kim', 'M', 32000, N'Khoai môn bùi béo tự nhiên hòa quyện sữa tươi'),
    (@dmTraSuaTT, N'Trà Sữa Khoai Môn Hoàng Kim', 'L', 37000, N'Trà sữa khoai môn thơm bùi ly lớn'),
    (@dmTraSuaOL, N'Trà Sữa Ô Long Nướng', 'M', 30000, N'Hương vị ô long rang cháy đặc trưng thơm lừng khó cưỡng'),
    (@dmTraSuaOL, N'Trà Sữa Ô Long Nướng', 'L', 35000, N'Ô long nướng đậm vị khói thơm ngậy ly lớn'),
    (@dmTraSuaOL, N'Trà Sữa Ô Long Thiết Quan Âm', 'M', 32000, N'Trà thiết quan âm thượng hạng hậu ngọt thanh tao'),
    (@dmTraSuaOL, N'Trà Sữa Ô Long Thiết Quan Âm', 'L', 38000, N'Thiết quan âm sữa thơm quý phái ly lớn'),
    (@dmTraSuaLai, N'Trà Sữa Lài Thơm', 'M', 28000, N'Hoa lài tự nhiên thơm ngát hòa cùng sữa dịu ngọt'),
    (@dmTraSuaLai, N'Trà Sữa Lài Thơm', 'L', 33000, N'Trà sữa lài ngọt thanh thư giãn ly lớn'),
    (@dmTraSuaLai, N'Trà Sữa Lài Kem Cheese', 'M', 35000, N'Trà sữa lài thanh tao kèm váng phô mai béo ngậy'),
    (@dmTraSuaLai, N'Trà Sữa Lài Kem Cheese', 'L', 42000, N'Trà sữa lài phủ kem cheese ngậy béo ly lớn'),
    (@dmTraSuaTT, N'Trà Sữa Matcha Nhật Bản', 'M', 35000, N'Bột matcha Uji thượng phẩm thơm lừng vị trà xanh mộc'),
    (@dmTraSuaTT, N'Trà Sữa Matcha Nhật Bản', 'L', 42000, N'Matcha sữa đậm đà chuẩn vị Nhật ly lớn'),
    (@dmTraTraiCay, N'Trà Đào Cam Sả', 'M', 28000, N'Trà đen ủ lạnh cùng cam vàng mọng nước và sả tươi đập dập'),
    (@dmTraTraiCay, N'Trà Đào Cam Sả', 'L', 33000, N'Trà đào cam sả thanh nhiệt tươi mới ly lớn'),
    (@dmTraTraiCay, N'Trà Vải Hoa Hồng', 'M', 30000, N'Vải thiều ngọt lịm phảng phất hương hoa hồng lãng mạn'),
    (@dmTraTraiCay, N'Trà Vải Hoa Hồng', 'L', 35000, N'Trà vải hoa hồng quý phái ngọt ngào ly lớn'),
    (@dmTraTraiCay, N'Trà Dâu Tây Nhiệt Đới', 'M', 32000, N'Dâu tây Đà Lạt tươi giã tay thơm lừng vị quả mọng'),
    (@dmTraTraiCay, N'Trà Dâu Tây Nhiệt Đới', 'L', 38000, N'Trà dâu tây tươi mát rượi ly lớn'),
    (@dmTraTraiCay, N'Trà Xoài Chanh Dây', 'M', 30000, N'Vị chua ngọt bùng nổ giữa xoài cát chín và chanh leo'),
    (@dmTraTraiCay, N'Trà Xoài Chanh Dây', 'L', 35000, N'Trà xoài chanh leo bừng tỉnh năng lượng ly lớn'),
    (@dmTraTraiCay, N'Trà Ổi Hồng Hạt Chia', 'M', 32000, N'Nước cốt ổi hồng thơm ngát giàu vitamin kèm hạt chia'),
    (@dmTraTraiCay, N'Trà Ổi Hồng Hạt Chia', 'L', 37000, N'Trà ổi hồng bổ dưỡng da sáng dáng thon ly lớn'),
    (@dmTraTraiCay, N'Trà Táo Xanh Bạc Hà', 'M', 30000, N'Táo xanh giòn rụm kết hợp bạc hà mát lạnh the mát'),
    (@dmTraTraiCay, N'Trà Táo Xanh Bạc Hà', 'L', 35000, N'Trà táo xanh the mát giải nhiệt ly lớn'),
    (@dmTraTraiCay, N'Trà Bưởi Mật Ong', 'M', 32000, N'Tép bưởi hồng mọng nước cùng mật ong hoa nhãn dịu ngọt'),
    (@dmTraTraiCay, N'Trà Bưởi Mật Ong', 'L', 38000, N'Trà bưởi hồng mật ong thanh lọc cơ thể ly lớn'),
    (@dmTraTraiCay, N'Trà Mãng Cầu Xiêm', 'M', 35000, N'Mãng cầu xiêm chua ngọt tự nhiên giã tay đậm đà'),
    (@dmTraTraiCay, N'Trà Mãng Cầu Xiêm', 'L', 42000, N'Trà mãng cầu xiêm hot trend tươi mát ly lớn'),
    (@dmCafeTT, N'Cà Phê Đen Đá', 'M', 20000, N'Cà phê robusta rang mộc pha phin truyền thống đậm đà'),
    (@dmCafeTT, N'Cà Phê Đen Đá', 'L', 25000, N'Cà phê đen đá đậm vị tỉnh táo ngày dài'),
    (@dmCafeTT, N'Cà Phê Sữa Đá', 'M', 25000, N'Cà phê pha phin truyền thống hòa cùng sữa đặc sánh ngọt'),
    (@dmCafeTT, N'Cà Phê Sữa Đá', 'L', 30000, N'Cà phê sữa đá chuẩn gu Sài Gòn ly lớn'),
    (@dmCafeST, N'Cà Phê Muối Xứ Huế', 'M', 28000, N'Lớp kem sữa muối mằn mặn hòa cùng cà phê thơm nồng'),
    (@dmCafeST, N'Cà Phê Muối Xứ Huế', 'L', 34000, N'Cà phê muối béo mặn thơm ngậy ly lớn'),
    (@dmCafeST, N'Bạc Xỉu Ba Tầng', 'M', 28000, N'Tầng sữa ngọt dịu, sữa tươi thơm mát và cà phê đậm vị'),
    (@dmCafeST, N'Bạc Xỉu Ba Tầng', 'L', 33000, N'Bạc xỉu ba tầng ngọt ngào êm ái ly lớn'),
    (@dmSuaTuoi, N'Sữa Tươi Trân Châu Đường Đen', 'M', 35000, N'Sữa tươi thanh trùng Đà Lạt cùng trân châu nấu đường đen dẻo quánh'),
    (@dmSuaTuoi, N'Sữa Tươi Trân Châu Đường Đen', 'L', 42000, N'Sữa tươi đường đen thơm ngon nức tiếng ly lớn'),
    (@dmSuaTuoi, N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', 32000, N'Sữa tươi béo nhẹ kết hợp sương sáo Đài Loan mát lành'),
    (@dmSuaTuoi, N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', 38000, N'Sữa tươi sương sáo thanh lọc cơ thể ly lớn'),
    (@dmMacchiato, N'Trà Ô Long Váng Sữa Macchiato', 'M', 35000, N'Cốt trà ô long thượng hạng phủ lớp macchiato kem sữa béo ngậy'),
    (@dmMacchiato, N'Trà Ô Long Váng Sữa Macchiato', 'L', 42000, N'Ô long macchiato béo ngọt cân bằng ly lớn'),
    (@dmMacchiato, N'Trà Đen Macchiato Muối Biển', 'M', 32000, N'Trà đen đậm đà tương phản với kem muối biển béo ngậy'),
    (@dmMacchiato, N'Trà Đen Macchiato Muối Biển', 'L', 38000, N'Trà đen macchiato muối biển thơm ngon ly lớn'),
    (@dmDaXay, N'Matcha Đá Xay Kem Tuyết', 'M', 40000, N'Matcha xay nhuyễn cùng đá và sữa, phủ bông kem whipping tươi'),
    (@dmDaXay, N'Matcha Đá Xay Kem Tuyết', 'L', 48000, N'Matcha đá xay kem tuyết mát lạnh sảng khoái ly lớn'),
    (@dmDaXay, N'Cà Phê Đá Xay Caramel', 'M', 38000, N'Espresso xay đá cùng xốt caramel ngọt bùi phủ kem whipping'),
    (@dmDaXay, N'Cà Phê Đá Xay Caramel', 'L', 45000, N'Cà phê caramel đá xay béo ngọt nồng nàn ly lớn'),
    (@dmDaXay, N'Sinh Tố Bơ Dừa Thơm Béo', 'M', 42000, N'Bơ sáp Đắk Lắk dẻo quánh xay cùng nước cốt dừa béo bùi'),
    (@dmDaXay, N'Sinh Tố Bơ Dừa Thơm Béo', 'L', 50000, N'Sinh tố bơ dừa thơm ngậy bổ dưỡng ly lớn'),
    (@dmThaoMoc, N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', 32000, N'Hoa cúc sấy khô ủ ấm mật ong rừng và hạt sen bùi ngậy'),
    (@dmThaoMoc, N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', 38000, N'Trà hoa cúc hạt sen an thần thanh nhiệt ly lớn'),
    -- 10 Topping
    (@dmTopping, N'Trân Châu Đen Hoàng Kim', 'M', 6000, N'Trân châu dẻo dai hoàng kim nấu đường đen ngọt ngào'),
    (@dmTopping, N'Trân Châu Trắng 3Q', 'M', 7000, N'Trân châu trắng giòn sần sật nhai vui miệng'),
    (@dmTopping, N'Thạch Đào Giòn Ngọt', 'M', 8000, N'Thạch đào dai giòn thơm nức mùi quả chín'),
    (@dmTopping, N'Kem Cheese Phô Mai Macchiato', 'M', 10000, N'Lớp kem cheese mằn mặn béo ngậy tan chảy trên đầu lưỡi'),
    (@dmTopping, N'Pudding Trứng Vàng', 'M', 8000, N'Pudding mềm mịn núng nính thơm ngậy vị trứng gà'),
    (@dmTopping, N'Thạch Phô Mai Tươi', 'M', 10000, N'Viên phô mai bọc thạch trong suốt béo ngậy'),
    (@dmTopping, N'Sương Sáo Thảo Mộc Đài Loan', 'M', 6000, N'Sương sáo thanh mát thảo mộc giải nhiệt tức thì'),
    (@dmTopping, N'Thạch Củ Năng Giòn', 'M', 8000, N'Củ năng bọc bột lọc giòn ngọt sảng khoái'),
    (@dmTopping, N'Nha Đam Tươi Đường Phèn', 'M', 7000, N'Nha đam giòn mát nấu đường phèn thanh khiết'),
    (@dmTopping, N'Khúc Bạch Hạnh Nhân', 'M', 10000, N'Khúc bạch phô mai thơm lừng rắc hạnh nhân giòn');

MERGE INTO dbo.MonUong AS target
USING @Products AS source ON (target.TenMon = source.TenMon AND target.Size = source.Size)
WHEN MATCHED THEN UPDATE SET target.Gia = source.Gia, target.MoTa = source.MoTa, target.MaDM = source.MaDM, target.HoatDong = 1
WHEN NOT MATCHED THEN INSERT (MaDM, TenMon, Size, MoTa, Gia, HoatDong) VALUES (source.MaDM, source.TenMon, source.Size, source.MoTa, source.Gia, 1);

PRINT N'>>> Đã nạp thành công 70 món uống và topping vào cơ sở dữ liệu!';
GO
```
