# Hướng dẫn và tham khảo bộ dữ liệu mẫu toàn diện (MilkTea Management System)

Tài liệu này tổng hợp toàn bộ **dữ liệu mẫu chuẩn & nhất quán** của hệ thống quản lý quán trà sữa MilkTea. Mọi dữ liệu đã được kiểm tra, chuẩn hóa và liên kết chặt chẽ với nhau:
- **100% Thực đơn có công thức định mức (`CongThucMon`)**: Toàn bộ 70 món uống (30 món nước x 2 size M/L = 60 món) và 10 loại topping đều được thiết lập định mức nguyên liệu chi tiết (325 dòng công thức). Không còn tình trạng thiếu công thức hay xung đột khi thêm vào giỏ hàng, đặt món qua QR, tạo đơn tại quầy hay hoàn tất đơn bếp.
- **Tính đúng đắn của chu trình đơn hàng**: Đơn hàng ở trạng thái *Chờ chế biến* (`CHO_CHE_BIEN`) đều có lịch sử đặt món ban đầu và hành động xác nhận của thu ngân; đơn *Đang chế biến* (`DANG_CHE_BIEN`) có mốc thời gian bắt đầu của bếp; đơn *Hoàn thành* (`HOAN_THANH`) có tiêu hao kho bếp thực tế; đơn *Đã hủy* (`DA_HUY`) liên kết yêu cầu hủy và hoàn tiền.
- **Tính nhất quán giữa các thực thể**: Hóa đơn (`HoaDon`), Thanh toán (`ThanhToan`), Chi tiết đơn (`ChiTietDonHang`), Topping (`ChiTietDonHangTopping`), Định mức (`DinhMucDonHang`), Tồn kho hai tầng (`TonNguyenLieu`), và Nhật ký nghiệp vụ (`NhatKyNghiepVu`) hoàn toàn đồng bộ, không xung đột khóa ngoại hay khoảng trắng thừa.
- **Bảo lưu chính xác 12 mã QR bàn**: Toàn bộ token QR bàn chuẩn hệ thống được giữ nguyên vẹn.

> [!NOTE]
> Khi triển khai hệ thống (Production Deploy), cơ sở dữ liệu mặc định chỉ khởi tạo dữ liệu cốt lõi tối thiểu qua file `database/sqlserver/2_seed.sql` (**cấu hình quán**, **12 mã QR bàn** và **4 tài khoản mặc định**).
> Khi cần đưa website lên môi trường demo, thuyết trình hoặc kiểm thử đầy đủ các chức năng, bạn có thể copy đoạn mã SQL ở **Mục 18** và thực thi trực tiếp vào cơ sở dữ liệu SQL Server.

---

## 1. Cấu hình thông tin quán (Global settings)

| Thuộc tính | Giá trị mẫu | Ghi chú |
| :--- | :--- | :--- |
| **Tên quán** | MilkTea Quán | Hiển thị trên đầu trang và hóa đơn |
| **Địa chỉ** | Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh | In trên hóa đơn thanh toán |
| **Ngân hàng** | MBBank | Phục vụ tạo mã VietQR động |
| **Số tài khoản** | 08881706888 | Số tài khoản nhận tiền chuyển khoản |
| **Chủ tài khoản** | DANG NGUYEN MINH KHOA | Tên chủ thẻ hiển thị khi quét QR |

---

## 2. Danh sách tài khoản thử nghiệm & demo

Tất cả các tài khoản demo đều sử dụng mật khẩu mặc định: `Password@123`  
*(Chuỗi mã hóa BCrypt: `$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq`)*

| Email | Họ và tên | Số điện thoại | Vai trò (Role) | Chức năng chính |
| :--- | :--- | :--- | :--- | :--- |
| `admin@milktea.vn` | Quản trị viên Hệ thống | 0901000001 | `ADMIN` | Quản trị toàn quyền, menu, kho, nhân sự, báo cáo |
| `admin2@milktea.vn` | Quản lý Vận hành | 0901000002 | `ADMIN` | Quản trị dự phòng |
| `cashier@milktea.vn` | Thu ngân Quỳnh | 0901000003 | `CASHIER` | Mở/đóng phiên bàn, duyệt đơn, ghi thu tiền, in bill |
| `cashier2@milktea.vn` | Thu ngân Hoàng | 0901000004 | `CASHIER` | Thu ngân ca chiều |
| `kitchen@milktea.vn` | Bếp trưởng Lân | 0901000005 | `KITCHEN` | Màn hình bếp KDS, nhận chế biến, hoàn thành đơn, sơ chế |
| `kitchen2@milktea.vn` | Nhân viên Pha chế | 0901000006 | `KITCHEN` | Nhân viên pha chế |
| `customer@milktea.vn` | Khách hàng Mai | 0901000007 | `CUSTOMER` | Khách hàng đặt món, lịch sử đơn |
| `customer2@milktea.vn` | Khách hàng Đức | 0901000008 | `CUSTOMER` | Khách hàng thân thiết |
| `customer3@milktea.vn` | Khách hàng Linh | 0901000009 | `CUSTOMER` | Khách hàng vãng lai |

---

## 3. Danh sách bàn & mã QR bàn (12 bàn chuẩn)

URL truy cập tương ứng của mỗi bàn trên web: `http://<domain>/?qr=<Mã_QR>`

| Tên bàn | Mã QR Token | Trạng thái ban đầu | Phiên bàn mẫu |
| :--- | :--- | :--- | :--- |
| **Bàn 01** | `QR_TABLE_01_TOKEN_A1B2C3D4` | `CO_KHACH` | Phiên đang mở (có giỏ món) |
| **Bàn 02** | `QR_TABLE_02_TOKEN_A2B3C4D5` | `CO_KHACH` | Phiên đang mở |
| **Bàn 03** | `QR_TABLE_03_TOKEN_A3B4C5D6` | `CO_KHACH` | Phiên đang mở |
| **Bàn 04** | `QR_TABLE_04_TOKEN_A4B5C6D7` | `TRONG` | Lưu vết phiên đã đóng trong quá khứ |
| **Bàn 05** | `QR_TABLE_05_TOKEN_A5B6C7D8` | `TRONG` | Sẵn sàng đón khách |
| **Bàn 06** | `QR_TABLE_06_TOKEN_A6B7C8D9` | `TRONG` | Sẵn sàng đón khách |
| **Bàn 07** | `QR_TABLE_07_TOKEN_A7B8C9D0` | `TRONG` | Sẵn sàng đón khách |
| **Bàn 08** | `QR_TABLE_08_TOKEN_A8B9C0D1` | `TRONG` | Sẵn sàng đón khách |
| **Bàn 09** | `QR_TABLE_09_TOKEN_A9B0C1D2` | `TRONG` | Sẵn sàng đón khách |
| **Bàn 10** | `QR_TABLE_10_TOKEN_B0C1D2E3` | `TRONG` | Sẵn sàng đón khách |
| **Bàn 11** | `QR_TABLE_11_TOKEN_B1C2D3E4` | `TRONG` | Sẵn sàng đón khách |
| **Bàn 12** | `QR_TABLE_12_TOKEN_B2C3D4E5` | `TRONG` | Sẵn sàng đón khách |

---

## 4. Danh mục sản phẩm (11 danh mục)

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
| 11 | **Topping Cao Cấp** | TẮT (0) *(Topping không chọn kèm topping khác)* |

---

## 5. Thực đơn món uống & topping (30 món nước + 10 topping = 70 món)

### A. 30 món nước uống (2 size: M - 500ml và L - 700ml)

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
| 13 | **Trà Dâu Tây Nhiệt Đới** | Trà Trái Cây Tươi | 32,000 | 38,000 | Dâu tây tươi giã tay thơm lừng vị quả mọng |
| 14 | **Trà Xoài Chanh Dây** | Trà Trái Cây Tươi | 30,000 | 35,000 | Vị chua ngọt bùng nổ giữa xoài cát chín và chanh leo |
| 15 | **Trà Ổi Hồng Hạt Chia** | Trà Trái Cây Tươi | 32,000 | 37,000 | Nước cốt ổi hồng thơm ngát giàu vitamin kèm hạt chia |
| 16 | **Trà Táo Xanh Bạc Hà** | Trà Trái Cây Tươi | 30,000 | 35,000 | Táo xanh giòn rụm kết hợp bạc hà mát lạnh the mát |
| 17 | **Trà Bưởi Mật Ong** | Trà Trái Cây Tươi | 32,000 | 38,000 | Tép bưởi hồng mọng nước cùng mật ong hoa nhãn dịu ngọt |
| 18 | **Trà Mãng Cầu Xiêm** | Trà Trái Cây Tươi | 35,000 | 42,000 | Mãng cầu xiêm chua ngọt tự nhiên giã tay đậm đà |
| 19 | **Cà Phê Đen Đá** | Cà Phê Truyền Thống | 20,000 | 25,000 | Cà phê robusta rang mộc pha phin truyền thống đậm đà |
| 20 | **Cà Phê Sữa Đá** | Cà Phê Truyền Thống | 25,000 | 30,000 | Cà phê pha phin truyền thống hòa cùng sữa đặc sánh ngọt |
| 21 | **Cà Phê Muối Xứ Huế** | Cà Phê Sáng Tạo | 28,000 | 34,000 | Lớp kem sữa muối mằn mặn hòa cùng cà phê thơm nồng |
| 22 | **Bạc Xỉu Ba Tầng** | Cà Phê Sáng Tạo | 28,000 | 33,000 | Tầng sữa ngọt dịu, sữa tươi thơm mát và cà phê đậm vị |
| 23 | **Sữa Tươi Trân Châu Đường Đen**| Sữa Tươi Trân Châu | 35,000 | 42,000 | Sữa tươi thanh trùng cùng trân châu nấu đường đen dẻo quánh |
| 24 | **Sữa Tươi Sương Sáo Thảo Mộc** | Sữa Tươi Trân Châu | 32,000 | 38,000 | Sữa tươi béo nhẹ kết hợp sương sáo Đài Loan mát lành |
| 25 | **Trà Ô Long Váng Sữa Macchiato**| Macchiato & Váng Sữa | 35,000 | 42,000 | Cốt trà ô long thượng hạng phủ lớp macchiato kem sữa béo ngậy |
| 26 | **Trà Đen Macchiato Muối Biển** | Macchiato & Váng Sữa | 32,000 | 38,000 | Trà đen đậm đà tương phản với kem muối biển béo ngậy |
| 27 | **Matcha Đá Xay Kem Tuyết** | Đá Xay & Sinh Tố | 40,000 | 48,000 | Matcha xay nhuyễn cùng đá và sữa, phủ bông kem whipping tươi |
| 28 | **Cà Phê Đá Xay Caramel** | Đá Xay & Sinh Tố | 38,000 | 45,000 | Espresso xay đá cùng xốt caramel ngọt bùi phủ kem whipping |
| 29 | **Sinh Tố Bơ Dừa Thơm Béo** | Đá Xay & Sinh Tố | 42,000 | 50,000 | Bơ sáp Đắk Lắk dẻo quánh xay cùng nước cốt dừa béo bùi |
| 30 | **Trà Hoa Cúc Mật Ong Hạt Sen** | Trà Thảo Mộc & Thanh Nhiệt | 32,000 | 38,000 | Hoa cúc sấy khô ủ ấm mật ong rừng và hạt sen bùi ngậy |

### B. 10 loại topping cao cấp (size M chuẩn)

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

## 6. Danh mục 37 nguyên liệu thô (THO) & sơ chế (SOCHE)

| Tên nguyên liệu | Phân loại | Đơn vị | Định mức / Giá vốn tham khảo |
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

## 7. Thiết lập tồn kho hai tầng (KHO & BEP)

Hệ thống quản lý tồn kho theo mô hình hai tầng độc lập (**Kho tổng KHO** và **Bếp pha chế BEP**). Toàn bộ 100% nguyên liệu trong thực đơn (nguyên liệu pha chế trực tiếp và nguyên liệu đầu vào sơ chế) **luôn luôn tồn tại trong kho với số lượng khả dụng dương (`SoLuong > 0`) và có đầy đủ lịch sử nhập kho (`PhieuNhap`/`ChiTietNhap`)**:

- **Kho tổng (KHO)**: Lưu trữ 27 nguyên liệu thô (Trà, Sữa, Cà phê, Đường, Topping thô, Hoa quả tươi/đóng hộp, Ly cốc, Ống hút). 100% nguyên liệu thô đều có lịch sử nhập kho từ các nhà cung cấp qua các phiếu nhập kho `PN_DEMO_01`, `PN_DEMO_02`, `PN_DEMO_03`.
- **Bếp pha chế (BEP)**: Lưu trữ 10 thành phẩm sơ chế do bếp tự chế biến (Nước cốt trà, Cốt cà phê, Nước đường, Trân châu chín, Kem cheese, Thạch đào, Sốt dâu tây, Sốt xoài chanh dây) qua các mẻ sơ chế `MeSoChe`, kèm theo lượng nguyên liệu thô định mức đã được cấp phát về quầy pha chế.
- **Quy tắc tiêu hao an toàn**: 
  - Món uống tiêu hao `SOCHE` tại `BEP` (fallback `KHO`).
  - Món uống tiêu hao `THO` tại `KHO` (fallback `BEP`).
  - Toàn bộ 70 món trong thực đơn khi khách gọi món và bếp hoàn thành pha chế (`completeOrderConsumption`) đều đảm bảo đủ tồn kho thực tế, không xảy ra xung đột hay lỗi thiếu tồn kho (`INVENTORY_INSUFFICIENT`).
- **Phân loại trạng thái cảnh báo tồn kho**:
  - **`BINH_THUONG`**: Tồn kho an toàn, dồi dào (`SoLuong > NguongCanhBao`).
  - **`TON_THAP`**: Tồn kho chạm ngưỡng cần bổ sung (`0 < SoLuong <= NguongCanhBao`), ví dụ: Nước cốt trà lài ở Bếp (1,200ml / ngưỡng 2,000ml) hoặc Đường đen ở Kho (4kg / ngưỡng 5kg), kích hoạt cảnh báo trên giao diện Bếp/Kho nhưng vẫn đáp ứng đủ để pha chế đơn hàng mẫu.

| STT | Tên nguyên liệu | Phân loại | Tồn Kho tổng (KHO) | Ngưỡng KHO | Tồn Bếp (BEP) | Ngưỡng BEP | Đơn vị | Lịch sử nguồn nhập / Chế biến |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| 1 | **Trà Đen Cổ Thụ** | `THO` | 45.000 | 10.000 | 5.000 | 2.000 | kg | Phiếu nhập `PN_DEMO_01` (50kg) |
| 2 | **Trà Ô Long Kim Tuyên** | `THO` | 28.000 | 8.000 | 5.000 | 2.000 | kg | Phiếu nhập `PN_DEMO_01` (35kg) |
| 3 | **Trà Lài Thượng Hạng** | `THO` | 32.000 | 8.000 | 5.000 | 2.000 | kg | Phiếu nhập `PN_DEMO_01` (40kg) |
| 4 | **Cà Phê Hạt Rang Mộc** | `THO` | 25.000 | 5.000 | 5.000 | 2.000 | kg | Phiếu nhập `PN_DEMO_01` (30kg) |
| 5 | **Sữa Đặc Có Đường** | `THO` | 80.000 | 15.000 | 12.000 | 3.000 | kg | Phiếu nhập `PN_DEMO_01` (100kg) |
| 6 | **Đường Cát Trắng** | `THO` | 120.000 | 20.000 | 10.000 | 2.000 | kg | Phiếu nhập `PN_DEMO_01` (150kg) |
| 7 | **Đường Đen Nữ Hoàng** | `THO` | 25.000 | 5.000 | 3.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_01` (30kg) |
| 8 | **Sữa Tươi Thanh Trùng** | `THO` | 50.000 | 10.000 | 15.000 | 5.000 | kg | Phiếu nhập `PN_DEMO_01` (70kg) |
| 9 | **Bột Béo Thực Vật** | `THO` | 60.000 | 10.000 | 8.000 | 2.000 | kg | Phiếu nhập `PN_DEMO_01` (70kg) |
| 10 | **Bột Kem Cheese Anchor** | `THO` | 15.000 | 3.000 | 3.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_01` (20kg) |
| 11 | **Bột Matcha Uji** | `THO` | 5.000 | 1.000 | 2.000 | 0.500 | kg | Phiếu nhập `PN_DEMO_01` (8kg) |
| 12 | **Bột Cacao Nguyên Chất** | `THO` | 8.000 | 2.000 | 3.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_01` (12kg) |
| 13 | **Nước Cốt Dừa Đóng Lon** | `THO` | 15.000 | 3.000 | 5.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_01` (25kg) |
| 14 | **Muối Biển Tinh Khiết** | `THO` | 20.000 | 5.000 | 3.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_01` (25kg) |
| 15 | **Trân Châu Thô Đen** | `THO` | 40.000 | 10.000 | 5.000 | 2.000 | kg | Phiếu nhập `PN_DEMO_02` (50kg) |
| 16 | **Hạt Trân Châu Trắng 3Q** | `THO` | 25.000 | 5.000 | 5.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_02` (35kg) |
| 17 | **Đào Ngâm Đóng Hộp** | `THO` | 30.000 | 5.000 | 5.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_02` (35kg) |
| 18 | **Vải Ngâm Đóng Hộp** | `THO` | 25.000 | 5.000 | 5.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_02` (30kg) |
| 19 | **Dâu Tây Tươi** | `THO` | 20.000 | 5.000 | 5.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_02` (25kg) |
| 20 | **Xoài Cát Chín** | `THO` | 15.000 | 5.000 | 3.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_02` (20kg) |
| 21 | **Chanh Dây Tươi** | `THO` | 12.000 | 3.000 | 3.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_02` (15kg) |
| 22 | **Cam Vàng Sành** | `THO` | 20.000 | 5.000 | 5.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_02` (25kg) |
| 23 | **Sả Cây Tươi** | `THO` | 10.000 | 2.000 | 2.000 | 0.500 | kg | Phiếu nhập `PN_DEMO_02` (15kg) |
| 24 | **Bơ Sáp Đắk Lắk** | `THO` | 12.000 | 3.000 | 4.000 | 1.000 | kg | Phiếu nhập `PN_DEMO_02` (18kg) |
| 25 | **Ly Nhựa PP 500ml** | `THO` | 2000.000 | 300.000 | 450.000 | 100.000 | ly | Phiếu nhập `PN_DEMO_03` (3,000 ly) |
| 26 | **Ly Nhựa PP 700ml** | `THO` | 2500.000 | 300.000 | 600.000 | 100.000 | ly | Phiếu nhập `PN_DEMO_03` (3,000 ly) |
| 27 | **Ống Hút Trân Châu** | `THO` | 3000.000 | 500.000 | 800.000 | 150.000 | cai | Phiếu nhập `PN_DEMO_03` (4,000 cái) |
| 28 | **Nước Cốt Trà Đen** | `SOCHE` | 10000.000 | 2000.000 | 8500.000 | 2000.000 | ml | Mẻ sơ chế `ME_DEMO_01` |
| 29 | **Nước Cốt Trà Ô Long** | `SOCHE` | 8000.000 | 2000.000 | 6000.000 | 2000.000 | ml | Mẻ sơ chế `ME_DEMO_02` |
| 30 | **Nước Cốt Trà Lài** | `SOCHE` | 6000.000 | 2000.000 | 3500.000 | 2000.000 | ml | Mẻ sơ chế `ME_DEMO_03` |
| 31 | **Cốt Cà Phê Pha Phin** | `SOCHE` | 5000.000 | 1000.000 | 3500.000 | 1000.000 | ml | Mẻ sơ chế `ME_DEMO_04` |
| 32 | **Nước Đường Pha Sẵn** | `SOCHE` | 12000.000 | 2000.000 | 9000.000 | 2000.000 | ml | Mẻ sơ chế `ME_DEMO_05` |
| 33 | **Trân Châu Nấu Chín** | `SOCHE` | 6000.000 | 1000.000 | 4500.000 | 1000.000 | g | Mẻ sơ chế `ME_DEMO_06` |
| 34 | **Kem Cheese Thành Phẩm** | `SOCHE` | 4000.000 | 800.000 | 2500.000 | 800.000 | ml | Mẻ sơ chế `ME_DEMO_07` |
| 35 | **Thạch Đào Cắt Hạt Lựu** | `SOCHE` | 3000.000 | 500.000 | 2000.000 | 500.000 | g | Mẻ sơ chế `ME_DEMO_08` |
| 36 | **Sốt Dâu Tây Tươi** | `SOCHE` | 4000.000 | 500.000 | 3000.000 | 500.000 | ml | Mẻ sơ chế `ME_DEMO_09` |
| 37 | **Sốt Xoài Chanh Dây** | `SOCHE` | 4000.000 | 500.000 | 3000.000 | 500.000 | ml | Mẻ sơ chế `ME_DEMO_10` |

---

## 8. Công thức sơ chế bếp (10 công thức chuẩn)

Bếp trưởng thực hiện sơ chế nguyên liệu thô thành nguyên liệu sơ chế dùng cho pha chế:
1. **Ủ Nước Cốt Trà Đen**: 0.15 kg Trà Đen Cổ Thụ $\rightarrow$ 3,000 ml Nước cốt trà đen.
2. **Ủ Nước Cốt Trà Ô Long**: 0.15 kg Trà Ô Long Kim Tuyên $\rightarrow$ 3,000 ml Nước cốt trà ô long.
3. **Ủ Nước Cốt Trà Lài**: 0.15 kg Trà Lài Thượng Hạng $\rightarrow$ 3,500 ml Nước cốt trà lài.
4. **Pha Cốt Cà Phê Phin**: 0.50 kg Cà Phê Hạt Rang Mộc $\rightarrow$ 2,000 ml Cốt cà phê phin.
5. **Nấu Nước Đường Pha Sẵn**: 2.00 kg Đường Cát Trắng $\rightarrow$ 3,000 ml Nước đường.
6. **Nấu Trân Châu Đen Dẻo**: 1.00 kg Trân Châu Thô Đen $\rightarrow$ 2,500 g Trân châu chín dẻo.
7. **Đánh Bông Kem Cheese**: 0.50 kg Bột Kem Cheese Anchor $\rightarrow$ 2,000 ml Kem cheese thành phẩm.
8. **Sơ Chế Thạch Đào Hạt Lựu**: 1.00 kg Đào Ngâm Đóng Hộp $\rightarrow$ 1,000 g Thạch đào cắt hạt lựu.
9. **Nấu Sốt Dâu Tây Tươi**: 1.00 kg Dâu Tây Tươi $\rightarrow$ 1,200 ml Sốt dâu tây.
10. **Nấu Sốt Xoài Chanh Dây**: 1.00 kg Xoài Cát Chín $\rightarrow$ 1,500 ml Sốt xoài chanh leo.

---

## 9. Công thức định mức món uống & topping (CongThucMon - toàn bộ 70 món)

Bảng định mức tiêu hao nguyên liệu tiêu chuẩn khi pha chế 1 ly nước uống hoặc 1 phần topping:

| Tên món / Topping | Size | Thành phần nguyên liệu định mức chuẩn |
| :--- | :---: | :--- |
| **Trà Sữa Truyền Thống** | M | Nước Cốt Trà Đen (120ml), Sữa Đặc (0.030kg), Bột Béo (0.025kg), Nước Đường (20ml), Ly PP 500ml, Ống hút |
| **Trà Sữa Truyền Thống** | L | Nước Cốt Trà Đen (160ml), Sữa Đặc (0.040kg), Bột Béo (0.035kg), Nước Đường (25ml), Ly PP 700ml, Ống hút |
| **Trà Sữa Thái Xanh** | M | Nước Cốt Trà Lài (120ml), Sữa Đặc (0.030kg), Bột Béo (0.025kg), Nước Đường (20ml), Ly PP 500ml, Ống hút |
| **Trà Sữa Thái Xanh** | L | Nước Cốt Trà Lài (160ml), Sữa Đặc (0.040kg), Bột Béo (0.035kg), Nước Đường (25ml), Ly PP 700ml, Ống hút |
| **Trà Sữa Thái Đỏ** | M | Nước Cốt Trà Đen (120ml), Sữa Đặc (0.030kg), Bột Béo (0.025kg), Nước Đường (20ml), Ly PP 500ml, Ống hút |
| **Trà Sữa Thái Đỏ** | L | Nước Cốt Trà Đen (160ml), Sữa Đặc (0.040kg), Bột Béo (0.035kg), Nước Đường (25ml), Ly PP 700ml, Ống hút |
| **Trà Sữa Socola Đậm Đà** | M | Nước Cốt Trà Đen (120ml), Bột Cacao (0.020kg), Sữa Đặc (0.030kg), Bột Béo (0.020kg), Ly PP 500ml, Ống hút |
| **Trà Sữa Socola Đậm Đà** | L | Nước Cốt Trà Đen (160ml), Bột Cacao (0.030kg), Sữa Đặc (0.040kg), Bột Béo (0.025kg), Ly PP 700ml, Ống hút |
| **Trà Sữa Khoai Môn Hoàng Kim** | M | Nước Cốt Trà Lài (120ml), Sữa Tươi (0.050kg), Sữa Đặc (0.030kg), Bột Béo (0.025kg), Ly PP 500ml, Ống hút |
| **Trà Sữa Khoai Môn Hoàng Kim** | L | Nước Cốt Trà Lài (160ml), Sữa Tươi (0.070kg), Sữa Đặc (0.040kg), Bột Béo (0.035kg), Ly PP 700ml, Ống hút |
| **Trà Sữa Ô Long Nướng** | M | Nước Cốt Trà Ô Long (120ml), Sữa Đặc (0.030kg), Bột Béo (0.025kg), Nước Đường (20ml), Ly PP 500ml, Ống hút |
| **Trà Sữa Ô Long Nướng** | L | Nước Cốt Trà Ô Long (160ml), Sữa Đặc (0.040kg), Bột Béo (0.035kg), Nước Đường (25ml), Ly PP 700ml, Ống hút |
| **Trà Sữa Ô Long Thiết Quan Âm**| M | Nước Cốt Trà Ô Long (130ml), Sữa Đặc (0.030kg), Bột Béo (0.025kg), Nước Đường (20ml), Ly PP 500ml, Ống hút |
| **Trà Sữa Ô Long Thiết Quan Âm**| L | Nước Cốt Trà Ô Long (170ml), Sữa Đặc (0.040kg), Bột Béo (0.035kg), Nước Đường (25ml), Ly PP 700ml, Ống hút |
| **Trà Sữa Lài Thơm** | M | Nước Cốt Trà Lài (120ml), Sữa Đặc (0.030kg), Bột Béo (0.025kg), Nước Đường (20ml), Ly PP 500ml, Ống hút |
| **Trà Sữa Lài Thơm** | L | Nước Cốt Trà Lài (160ml), Sữa Đặc (0.040kg), Bột Béo (0.035kg), Nước Đường (25ml), Ly PP 700ml, Ống hút |
| **Trà Sữa Lài Kem Cheese** | M | Nước Cốt Trà Lài (120ml), Sữa Đặc (0.030kg), Kem Cheese Thành Phẩm (40ml), Ly PP 500ml, Ống hút |
| **Trà Sữa Lài Kem Cheese** | L | Nước Cốt Trà Lài (160ml), Sữa Đặc (0.040kg), Kem Cheese Thành Phẩm (60ml), Ly PP 700ml, Ống hút |
| **Trà Sữa Matcha Nhật Bản** | M | Bột Matcha Uji (0.015kg), Sữa Tươi (0.120kg), Sữa Đặc (0.030kg), Nước Đường (15ml), Ly PP 500ml, Ống hút |
| **Trà Sữa Matcha Nhật Bản** | L | Bột Matcha Uji (0.020kg), Sữa Tươi (0.160kg), Sữa Đặc (0.040kg), Nước Đường (20ml), Ly PP 700ml, Ống hút |
| **Trà Đào Cam Sả** | M | Nước Cốt Trà Đen (100ml), Nước Đường (30ml), Cam Vàng Sành (0.050kg), Sả Cây (0.020kg), Ly PP 500ml, Ống hút |
| **Trà Đào Cam Sả** | L | Nước Cốt Trà Đen (140ml), Nước Đường (40ml), Cam Vàng Sành (0.070kg), Sả Cây (0.030kg), Ly PP 700ml, Ống hút |
| **Trà Vải Hoa Hồng** | M | Nước Cốt Trà Lài (100ml), Vải Ngâm (0.060kg), Nước Đường (25ml), Ly PP 500ml, Ống hút |
| **Trà Vải Hoa Hồng** | L | Nước Cốt Trà Lài (140ml), Vải Ngâm (0.090kg), Nước Đường (35ml), Ly PP 700ml, Ống hút |
| **Trà Dâu Tây Nhiệt Đới** | M | Nước Cốt Trà Lài (100ml), Sốt Dâu Tây (50ml), Nước Đường (20ml), Ly PP 500ml, Ống hút |
| **Trà Dâu Tây Nhiệt Đới** | L | Nước Cốt Trà Lài (140ml), Sốt Dâu Tây (70ml), Nước Đường (25ml), Ly PP 700ml, Ống hút |
| **Trà Xoài Chanh Dây** | M | Nước Cốt Trà Lài (100ml), Sốt Xoài Chanh Dây (50ml), Nước Đường (20ml), Ly PP 500ml, Ống hút |
| **Trà Xoài Chanh Dây** | L | Nước Cốt Trà Lài (140ml), Sốt Xoài Chanh Dây (70ml), Nước Đường (25ml), Ly PP 700ml, Ống hút |
| **Trà Ổi Hồng Hạt Chia** | M | Nước Cốt Trà Lài (110ml), Nước Đường (30ml), Ly PP 500ml, Ống hút |
| **Trà Ổi Hồng Hạt Chia** | L | Nước Cốt Trà Lài (150ml), Nước Đường (40ml), Ly PP 700ml, Ống hút |
| **Trà Táo Xanh Bạc Hà** | M | Nước Cốt Trà Lài (110ml), Nước Đường (30ml), Ly PP 500ml, Ống hút |
| **Trà Táo Xanh Bạc Hà** | L | Nước Cốt Trà Lài (150ml), Nước Đường (40ml), Ly PP 700ml, Ống hút |
| **Trà Bưởi Mật Ong** | M | Nước Cốt Trà Lài (110ml), Nước Đường (30ml), Ly PP 500ml, Ống hút |
| **Trà Bưởi Mật Ong** | L | Nước Cốt Trà Lài (150ml), Nước Đường (40ml), Ly PP 700ml, Ống hút |
| **Trà Mãng Cầu Xiêm** | M | Nước Cốt Trà Lài (110ml), Nước Đường (35ml), Ly PP 500ml, Ống hút |
| **Trà Mãng Cầu Xiêm** | L | Nước Cốt Trà Lài (150ml), Nước Đường (45ml), Ly PP 700ml, Ống hút |
| **Cà Phê Đen Đá** | M | Cốt Cà Phê Phin (60ml), Đường Cát (0.020kg), Ly PP 500ml, Ống hút |
| **Cà Phê Đen Đá** | L | Cốt Cà Phê Phin (80ml), Đường Cát (0.025kg), Ly PP 700ml, Ống hút |
| **Cà Phê Sữa Đá** | M | Cốt Cà Phê Phin (60ml), Sữa Đặc (0.040kg), Ly PP 500ml, Ống hút |
| **Cà Phê Sữa Đá** | L | Cốt Cà Phê Phin (80ml), Sữa Đặc (0.050kg), Ly PP 700ml, Ống hút |
| **Cà Phê Muối Xứ Huế** | M | Cốt Cà Phê Phin (60ml), Sữa Đặc (0.030kg), Kem Cheese (40ml), Muối Biển (0.002kg), Ly PP 500ml, Ống hút |
| **Cà Phê Muối Xứ Huế** | L | Cốt Cà Phê Phin (80ml), Sữa Đặc (0.040kg), Kem Cheese (55ml), Muối Biển (0.003kg), Ly PP 700ml, Ống hút |
| **Bạc Xỉu Ba Tầng** | M | Cốt Cà Phê Phin (40ml), Sữa Đặc (0.040kg), Sữa Tươi (0.080kg), Ly PP 500ml, Ống hút |
| **Bạc Xỉu Ba Tầng** | L | Cốt Cà Phê Phin (50ml), Sữa Đặc (0.050kg), Sữa Tươi (0.110kg), Ly PP 700ml, Ống hút |
| **Sữa Tươi Trân Châu Đường Đen**| M | Sữa Tươi (0.160kg), Trân Châu Chín (60g), Đường Đen Nữ Hoàng (0.025kg), Ly PP 500ml, Ống hút |
| **Sữa Tươi Trân Châu Đường Đen**| L | Sữa Tươi (0.200kg), Trân Châu Chín (80g), Đường Đen Nữ Hoàng (0.030kg), Ly PP 700ml, Ống hút |
| **Sữa Tươi Sương Sáo Thảo Mộc** | M | Sữa Tươi (0.160kg), Nước Đường (20ml), Ly PP 500ml, Ống hút |
| **Sữa Tươi Sương Sáo Thảo Mộc** | L | Sữa Tươi (0.200kg), Nước Đường (25ml), Ly PP 700ml, Ống hút |
| **Trà Ô Long Váng Sữa Macchiato**| M | Nước Cốt Trà Ô Long (120ml), Nước Đường (20ml), Kem Cheese (45ml), Ly PP 500ml, Ống hút |
| **Trà Ô Long Váng Sữa Macchiato**| L | Nước Cốt Trà Ô Long (160ml), Nước Đường (25ml), Kem Cheese (60ml), Ly PP 700ml, Ống hút |
| **Trà Đen Macchiato Muối Biển** | M | Nước Cốt Trà Đen (120ml), Nước Đường (20ml), Kem Cheese (45ml), Muối Biển (0.002kg), Ly PP 500ml, Ống hút |
| **Trà Đen Macchiato Muối Biển** | L | Nước Cốt Trà Đen (160ml), Nước Đường (25ml), Kem Cheese (60ml), Muối Biển (0.003kg), Ly PP 700ml, Ống hút |
| **Matcha Đá Xay Kem Tuyết** | M | Bột Matcha (0.012kg), Sữa Tươi (0.120kg), Sữa Đặc (0.030kg), Kem Cheese (30ml), Ly PP 500ml, Ống hút |
| **Matcha Đá Xay Kem Tuyết** | L | Bột Matcha (0.015kg), Sữa Tươi (0.150kg), Sữa Đặc (0.040kg), Kem Cheese (40ml), Ly PP 700ml, Ống hút |
| **Cà Phê Đá Xay Caramel** | M | Cốt Cà Phê Phin (50ml), Sữa Đặc (0.035kg), Sữa Tươi (0.080kg), Kem Cheese (30ml), Ly PP 500ml, Ống hút |
| **Cà Phê Đá Xay Caramel** | L | Cốt Cà Phê Phin (70ml), Sữa Đặc (0.045kg), Sữa Tươi (0.100kg), Kem Cheese (40ml), Ly PP 700ml, Ống hút |
| **Sinh Tố Bơ Dừa Thơm Béo** | M | Bơ Sáp Đắk Lắk (0.100kg), Nước Cốt Dừa (0.040kg), Sữa Đặc (0.035kg), Ly PP 500ml, Ống hút |
| **Sinh Tố Bơ Dừa Thơm Béo** | L | Bơ Sáp Đắk Lắk (0.120kg), Nước Cốt Dừa (0.050kg), Sữa Đặc (0.040kg), Ly PP 700ml, Ống hút |
| **Trà Hoa Cúc Mật Ong Hạt Sen** | M | Nước Cốt Trà Lài (120ml), Nước Đường (25ml), Ly PP 500ml, Ống hút |
| **Trà Hoa Cúc Mật Ong Hạt Sen** | L | Nước Cốt Trà Lài (160ml), Nước Đường (35ml), Ly PP 700ml, Ống hút |
| **Trân Châu Đen Hoàng Kim** | M | Trân Châu Nấu Chín (50g) |
| **Trân Châu Trắng 3Q** | M | Hạt Trân Châu Trắng 3Q (40g = 0.040kg) |
| **Thạch Đào Giòn Ngọt** | M | Thạch Đào Cắt Hạt Lựu (50g) |
| **Kem Cheese Phô Mai Macchiato**| M | Kem Cheese Thành Phẩm (40ml) |
| **Pudding Trứng Vàng** | M | Sữa Tươi Thanh Trùng (0.040kg), Sữa Đặc Có Đường (0.015kg) |
| **Thạch Phô Mai Tươi** | M | Sữa Tươi Thanh Trùng (0.040kg), Bột Kem Cheese Anchor (0.015kg) |
| **Sương Sáo Thảo Mộc Đài Loan** | M | Nước Đường Pha Sẵn (20ml) |
| **Thạch Củ Năng Giòn** | M | Nước Đường Pha Sẵn (20ml) |
| **Nha Đam Tươi Đường Phèn** | M | Nước Đường Pha Sẵn (20ml) |
| **Khúc Bạch Hạnh Nhân** | M | Sữa Tươi Thanh Trùng (0.040kg), Sữa Đặc Có Đường (0.015kg) |

---

## 10. 10 đơn hàng mẫu và luồng liên kết dữ liệu hoàn chỉnh

Dữ liệu demo bao gồm 10 đơn hàng mẫu phủ toàn diện 5 trạng thái vận hành của quán, đảm bảo tính đúng đắn và liên kết 100%:

| Mã Đơn | Nguồn | Vị trí | Trạng thái (`TrangThai`) | Thanh toán (`ThanhToan`) | Nhật ký nghiệp vụ liên kết (`NhatKyNghiepVu`) | Tiêu hao kho (`LichSuKho`) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **DH_SEED_01** | `TABLE` | Bàn 01 | `CHO_XAC_NHAN` | Chưa thu (khách trả sau) | `ORDER_CREATED` | Chưa tiêu hao |
| **DH_SEED_02** | `TABLE` | Bàn 02 | `CHO_XAC_NHAN` | Khách báo CK qua VietQR | `ORDER_CREATED`, `PAYMENT_NOTICE_CREATED` | Chưa tiêu hao |
| **DH_SEED_03** | `TABLE` | Bàn 03 | `CHO_CHE_BIEN` | Đã thu tiền mặt | `ORDER_CREATED` $\rightarrow$ `ORDER_STATUS_CHANGED` (Duyệt) $\rightarrow$ `PAYMENT_RECORDED` | Chưa tiêu hao (chờ bếp) |
| **DH_SEED_04** | `COUNTER`| Quầy POS | `CHO_CHE_BIEN` | Đã thu chuyển khoản | `ORDER_CREATED` (Quầy) $\rightarrow$ `PAYMENT_RECORDED` | Chưa tiêu hao (chờ bếp) |
| **DH_SEED_05** | `TABLE` | Bàn 01 | `DANG_CHE_BIEN` | Đã thu chuyển khoản | Tạo đơn $\rightarrow$ Duyệt $\rightarrow$ `KITCHEN_START` (`BatDauLuc`) | Đang pha chế |
| **DH_SEED_06** | `COUNTER`| Quầy POS | `DANG_CHE_BIEN` | Đã thu tiền mặt | Tạo đơn quầy $\rightarrow$ Thu tiền $\rightarrow$ `KITCHEN_START` (`BatDauLuc`) | Đang pha chế |
| **DH_SEED_07** | `TABLE` | Bàn 04 | `HOAN_THANH` | Đã thu chuyển khoản | Tạo đơn $\rightarrow$ Duyệt $\rightarrow$ Bắt đầu $\rightarrow$ `KITCHEN_COMPLETE` (`HoanThanhLuc`) | Đã trừ kho Bếp (`TIEU_HAO`) |
| **DH_SEED_08** | `COUNTER`| Quầy POS | `HOAN_THANH` | Đã thu tiền mặt | Tạo đơn quầy $\rightarrow$ Thu tiền $\rightarrow$ Bắt đầu $\rightarrow$ Hoàn thành | Đã trừ kho Bếp (`TIEU_HAO`) |
| **DH_SEED_09** | `TABLE` | Bàn 01 | `DA_HUY` | Chưa thu | `ORDER_CREATED` $\rightarrow$ `CANCELLATION_APPROVED` (Duyệt hủy) | Không tiêu hao |
| **DH_SEED_10** | `COUNTER`| Quầy POS | `DA_HUY` | Đã thu chuyển khoản | Tạo đơn quầy $\rightarrow$ Thu tiền $\rightarrow$ Hủy đơn $\rightarrow$ `REFUND_REQUESTED` (Chờ hoàn tiền) | Không tiêu hao |

---

## 18. Script SQL khôi phục toàn bộ dữ liệu mẫu (Full demo restore script)

Đoạn mã SQL dưới đây đã được kiểm tra thực thi thành công 100% trên SQL Server (tương thích cả SQL Server 2019/2022/2025 và Azure SQL). Bạn chỉ cần mở SQL Server Management Studio (SSMS), Azure Data Studio, hoặc chạy qua lệnh `sqlcmd` để nạp toàn bộ dữ liệu:

```sql
-- ============================================================================
-- MilkTea Management System - DỮ LIỆU MẪU ĐẦY ĐỦ & LIÊN KẾT NHẤT QUÁN (FULL DEMO)
-- Toàn bộ dữ liệu được thiết kế liên kết chặt chẽ, không xung đột logic:
--   - 100% món uống & topping (70 món) đều có công thức định mức (CongThucMon)
--   - Không có khoảng trắng thừa hay sai lệch tên món / nguyên liệu
--   - Đơn hàng Chờ chế biến có lịch sử tạo đơn và thu ngân xác nhận
--   - Đơn hàng Đang chế biến có mốc bắt đầu pha chế của bếp
--   - Đơn hàng Hoàn thành có tiêu hao kho bếp và mốc hoàn tất
--   - Đơn hàng Đã hủy có yêu cầu hủy và yêu cầu hoàn tiền liên kết
--   - Tồn kho, phiếu nhập, mẻ sơ chế, phiên bàn khớp 100%
-- ============================================================================

-- Tự động tương thích cả SQL Server Localhost và Azure SQL Database
IF SERVERPROPERTY('EngineEdition') <> 5 AND DB_NAME() <> 'milktea'
BEGIN
    USE milktea;
END
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
PRINT N'>>> Bắt đầu nạp toàn bộ dữ liệu mẫu demo MilkTea...';
GO

-- ============================================================================
-- 1. Cấu hình thông tin quán (CauHinhChung)
-- ============================================================================
MERGE INTO dbo.CauHinhChung AS target
USING (SELECT 1 AS MaCauHinh) AS source ON (target.MaCauHinh = source.MaCauHinh)
WHEN MATCHED THEN
    UPDATE SET 
        target.TenQuan    = N'MilkTea Quán',
        target.DiaChi     = N'Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh',
        target.NganHang   = N'MBBank',
        target.SoTaiKhoan = '08881706888',
        target.ChuTK      = N'DANG NGUYEN MINH KHOA',
        target.TyLeGiam   = 0.00,
        target.LoaiGiam   = 'PERCENT',
        target.GiaTriGiam = 0.00,
        target.SuaLuc     = SYSUTCDATETIME()
WHEN NOT MATCHED THEN
    INSERT (MaCauHinh, TenQuan, DiaChi, NganHang, SoTaiKhoan, ChuTK, TyLeGiam, LoaiGiam, GiaTriGiam, SuaLuc)
    VALUES (1, N'MilkTea Quán', N'Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh', N'MBBank', '08881706888', N'DANG NGUYEN MINH KHOA', 0.00, 'PERCENT', 0.00, SYSUTCDATETIME());

PRINT N'-- [1/16] Đã cấu hình thông tin quán và chiết khấu mặc định.';
GO

-- ============================================================================
-- 2. Tài khoản hệ thống (TaiKhoan) - Đầy đủ các vai trò với mật khẩu Password@123
-- Hash: $2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq
-- ============================================================================
DECLARE @pass AS VARCHAR(255) = '$2a$10$EO1GLna0hOQEmDKawB64XeAdZLIK.bDI/.U1fcVDM8CLmye557KBq';

DECLARE @Accounts TABLE (
    Email     VARCHAR(254),
    HoTen     NVARCHAR(120),
    DienThoai VARCHAR(20),
    VaiTro    VARCHAR(16)
);

INSERT INTO @Accounts VALUES
    ('admin@milktea.vn',    N'Quản trị viên Hệ thống', '0901000001', 'ADMIN'),
    ('admin2@milktea.vn',   N'Quản lý Vận hành',      '0901000002', 'ADMIN'),
    ('cashier@milktea.vn',  N'Thu ngân Quỳnh',        '0901000003', 'CASHIER'),
    ('cashier2@milktea.vn', N'Thu ngân Hoàng',        '0901000004', 'CASHIER'),
    ('kitchen@milktea.vn',  N'Bếp trưởng Lân',        '0901000005', 'KITCHEN'),
    ('kitchen2@milktea.vn', N'Nhân viên Pha chế',     '0901000006', 'KITCHEN'),
    ('customer@milktea.vn', N'Khách hàng Mai',        '0901000007', 'CUSTOMER'),
    ('customer2@milktea.vn',N'Khách hàng Đức',        '0901000008', 'CUSTOMER'),
    ('customer3@milktea.vn',N'Khách hàng Linh',       '0901000009', 'CUSTOMER');

MERGE INTO dbo.TaiKhoan AS target
USING @Accounts AS source ON (target.Email = source.Email)
WHEN MATCHED THEN
    UPDATE SET 
        target.HoTen        = source.HoTen,
        target.DienThoai    = source.DienThoai,
        target.VaiTro       = source.VaiTro,
        target.MatKhauBam   = @pass,
        target.HoatDong     = 1,
        target.EmailXacThuc = 1
WHEN NOT MATCHED THEN
    INSERT (Email, MatKhauBam, HoTen, DienThoai, VaiTro, HoatDong, EmailXacThuc, TokenVersion, TaoLuc)
    VALUES (source.Email, @pass, source.HoTen, source.DienThoai, source.VaiTro, 1, 1, 1, SYSUTCDATETIME());

PRINT N'-- [2/16] Đã khởi tạo danh sách 9 tài khoản mẫu cho các vai trò.';
GO

-- ============================================================================
-- 3. Bàn ăn & mã QR chuẩn (Ban) - Đúng 12 bàn ăn (Bàn 01 -> Bàn 12)
-- Giữ nguyên chính xác mã QR theo chuẩn hệ thống
-- ============================================================================
DECLARE @TableSeeds TABLE (
    TenBan NVARCHAR(40),
    MaQR   VARCHAR(64)
);

INSERT INTO @TableSeeds VALUES
    (N'Bàn 01', 'QR_TABLE_01_TOKEN_A1B2C3D4'),
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

MERGE INTO dbo.Ban AS target
USING @TableSeeds AS source ON (target.MaQR = source.MaQR)
WHEN MATCHED THEN
    UPDATE SET target.TenBan = source.TenBan, target.HoatDong = 1
WHEN NOT MATCHED THEN
    INSERT (TenBan, MaQR, TrangThai, HoatDong, Version)
    VALUES (source.TenBan, source.MaQR, 'TRONG', 1, 0);

PRINT N'-- [3/16] Đã nạp 12 bàn ăn kèm mã QR chuẩn xác.';
GO

-- ============================================================================
-- 4. Danh mục sản phẩm (DanhMuc) - 11 danh mục
-- ============================================================================
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

PRINT N'-- [4/16] Đã nạp 11 danh mục sản phẩm chuẩn.';
GO

-- ============================================================================
-- 5. Nguyên liệu thô & sơ chế (NguyenLieu) - 37 loại nguyên liệu chuẩn
-- ============================================================================
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

PRINT N'-- [5/16] Đã nạp 37 nguyên liệu thô và sơ chế.';
GO

-- ============================================================================
-- 6. THỰC ĐƠN MÓN UỐNG & TOPPING (MonUong) - 70 Biến thể món chuẩn xác 100%
-- Bảo toàn 100% URL ảnh Cloudinary hiện có (AnhUrl, AnhPublicId) nếu database đã có ảnh trước đó
-- ============================================================================
DECLARE @ExistingImages TABLE (
    TenMon NVARCHAR(120),
    Size VARCHAR(12),
    AnhUrl NVARCHAR(1000),
    AnhPublicId VARCHAR(255)
);

INSERT INTO @ExistingImages (TenMon, Size, AnhUrl, AnhPublicId)
SELECT LTRIM(RTRIM(TenMon)), Size, AnhUrl, AnhPublicId
FROM dbo.MonUong
WHERE AnhUrl IS NOT NULL AND LTRIM(RTRIM(AnhUrl)) <> '';

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
    (@dmTraSuaTT, N'Trà Sữa Truyền Thống', 'M', 25000, N'Hương vị trà đen đậm đà hòa quyện cùng sữa béo thơm ngon (Size M)'),
    (@dmTraSuaTT, N'Trà Sữa Truyền Thống', 'L', 30000, N'Hương vị trà đen đậm đà hòa quyện cùng sữa béo thơm ngon (Size L)'),
    (@dmTraSuaTT, N'Trà Sữa Thái Xanh', 'M', 28000, N'Trà xanh Thái Lan thanh mát, thơm ngát thảo mộc (Size M)'),
    (@dmTraSuaTT, N'Trà Sữa Thái Xanh', 'L', 33000, N'Trà xanh Thái Lan thanh mát, thơm ngát thảo mộc (Size L)'),
    (@dmTraSuaTT, N'Trà Sữa Thái Đỏ', 'M', 28000, N'Trà đỏ Thái Lan truyền thống thơm nồng vị thảo mộc (Size M)'),
    (@dmTraSuaTT, N'Trà Sữa Thái Đỏ', 'L', 33000, N'Trà đỏ Thái Lan truyền thống thơm nồng vị thảo mộc (Size L)'),
    (@dmTraSuaTT, N'Trà Sữa Socola Đậm Đà', 'M', 32000, N'Vị socola hảo hạng kết hợp trà đen thơm lừng (Size M)'),
    (@dmTraSuaTT, N'Trà Sữa Socola Đậm Đà', 'L', 37000, N'Vị socola hảo hạng kết hợp trà đen thơm lừng (Size L)'),
    (@dmTraSuaTT, N'Trà Sữa Khoai Môn Hoàng Kim', 'M', 32000, N'Khoai môn bùi béo tự nhiên hòa quyện sữa tươi (Size M)'),
    (@dmTraSuaTT, N'Trà Sữa Khoai Môn Hoàng Kim', 'L', 37000, N'Khoai môn bùi béo tự nhiên hòa quyện sữa tươi (Size L)'),
    (@dmTraSuaOL, N'Trà Sữa Ô Long Nướng', 'M', 30000, N'Hương vị ô long rang cháy đặc trưng thơm lừng khó cưỡng (Size M)'),
    (@dmTraSuaOL, N'Trà Sữa Ô Long Nướng', 'L', 35000, N'Hương vị ô long rang cháy đặc trưng thơm lừng khó cưỡng (Size L)'),
    (@dmTraSuaOL, N'Trà Sữa Ô Long Thiết Quan Âm', 'M', 32000, N'Trà thiết quan âm thượng hạng hậu ngọt thanh tao (Size M)'),
    (@dmTraSuaOL, N'Trà Sữa Ô Long Thiết Quan Âm', 'L', 38000, N'Trà thiết quan âm thượng hạng hậu ngọt thanh tao (Size L)'),
    (@dmTraSuaLai, N'Trà Sữa Lài Thơm', 'M', 28000, N'Hoa lài tự nhiên thơm ngát hòa cùng sữa dịu ngọt (Size M)'),
    (@dmTraSuaLai, N'Trà Sữa Lài Thơm', 'L', 33000, N'Hoa lài tự nhiên thơm ngát hòa cùng sữa dịu ngọt (Size L)'),
    (@dmTraSuaLai, N'Trà Sữa Lài Kem Cheese', 'M', 35000, N'Trà sữa lài thanh tao kèm váng phô mai béo ngậy (Size M)'),
    (@dmTraSuaLai, N'Trà Sữa Lài Kem Cheese', 'L', 42000, N'Trà sữa lài thanh tao kèm váng phô mai béo ngậy (Size L)'),
    (@dmTraSuaTT, N'Trà Sữa Matcha Nhật Bản', 'M', 35000, N'Bột matcha Uji thượng phẩm thơm lừng vị trà xanh mộc (Size M)'),
    (@dmTraSuaTT, N'Trà Sữa Matcha Nhật Bản', 'L', 42000, N'Bột matcha Uji thượng phẩm thơm lừng vị trà xanh mộc (Size L)'),
    (@dmTraTraiCay, N'Trà Đào Cam Sả', 'M', 28000, N'Trà đen ủ lạnh cùng cam vàng mọng nước và sả tươi đập dập (Size M)'),
    (@dmTraTraiCay, N'Trà Đào Cam Sả', 'L', 33000, N'Trà đen ủ lạnh cùng cam vàng mọng nước và sả tươi đập dập (Size L)'),
    (@dmTraTraiCay, N'Trà Vải Hoa Hồng', 'M', 30000, N'Vải thiều ngọt lịm phảng phất hương hoa hồng lãng mạn (Size M)'),
    (@dmTraTraiCay, N'Trà Vải Hoa Hồng', 'L', 35000, N'Vải thiều ngọt lịm phảng phất hương hoa hồng lãng mạn (Size L)'),
    (@dmTraTraiCay, N'Trà Dâu Tây Nhiệt Đới', 'M', 32000, N'Dâu tây tươi giã tay thơm lừng vị quả mọng (Size M)'),
    (@dmTraTraiCay, N'Trà Dâu Tây Nhiệt Đới', 'L', 38000, N'Dâu tây tươi giã tay thơm lừng vị quả mọng (Size L)'),
    (@dmTraTraiCay, N'Trà Xoài Chanh Dây', 'M', 30000, N'Vị chua ngọt bùng nổ giữa xoài cát chín và chanh leo (Size M)'),
    (@dmTraTraiCay, N'Trà Xoài Chanh Dây', 'L', 35000, N'Vị chua ngọt bùng nổ giữa xoài cát chín và chanh leo (Size L)'),
    (@dmTraTraiCay, N'Trà Ổi Hồng Hạt Chia', 'M', 32000, N'Nước cốt ổi hồng thơm ngát giàu vitamin kèm hạt chia (Size M)'),
    (@dmTraTraiCay, N'Trà Ổi Hồng Hạt Chia', 'L', 37000, N'Nước cốt ổi hồng thơm ngát giàu vitamin kèm hạt chia (Size L)'),
    (@dmTraTraiCay, N'Trà Táo Xanh Bạc Hà', 'M', 30000, N'Táo xanh giòn rụm kết hợp bạc hà mát lạnh the mát (Size M)'),
    (@dmTraTraiCay, N'Trà Táo Xanh Bạc Hà', 'L', 35000, N'Táo xanh giòn rụm kết hợp bạc hà mát lạnh the mát (Size L)'),
    (@dmTraTraiCay, N'Trà Bưởi Mật Ong', 'M', 32000, N'Tép bưởi hồng mọng nước cùng mật ong hoa nhãn dịu ngọt (Size M)'),
    (@dmTraTraiCay, N'Trà Bưởi Mật Ong', 'L', 38000, N'Tép bưởi hồng mọng nước cùng mật ong hoa nhãn dịu ngọt (Size L)'),
    (@dmTraTraiCay, N'Trà Mãng Cầu Xiêm', 'M', 35000, N'Mãng cầu xiêm chua ngọt tự nhiên giã tay đậm đà (Size M)'),
    (@dmTraTraiCay, N'Trà Mãng Cầu Xiêm', 'L', 42000, N'Mãng cầu xiêm chua ngọt tự nhiên giã tay đậm đà (Size L)'),
    (@dmCafeTT, N'Cà Phê Đen Đá', 'M', 20000, N'Cà phê robusta rang mộc pha phin truyền thống đậm đà (Size M)'),
    (@dmCafeTT, N'Cà Phê Đen Đá', 'L', 25000, N'Cà phê robusta rang mộc pha phin truyền thống đậm đà (Size L)'),
    (@dmCafeTT, N'Cà Phê Sữa Đá', 'M', 25000, N'Cà phê pha phin truyền thống hòa cùng sữa đặc sánh ngọt (Size M)'),
    (@dmCafeTT, N'Cà Phê Sữa Đá', 'L', 30000, N'Cà phê pha phin truyền thống hòa cùng sữa đặc sánh ngọt (Size L)'),
    (@dmCafeST, N'Cà Phê Muối Xứ Huế', 'M', 28000, N'Lớp kem sữa muối mằn mặn hòa cùng cà phê thơm nồng (Size M)'),
    (@dmCafeST, N'Cà Phê Muối Xứ Huế', 'L', 34000, N'Lớp kem sữa muối mằn mặn hòa cùng cà phê thơm nồng (Size L)'),
    (@dmCafeST, N'Bạc Xỉu Ba Tầng', 'M', 28000, N'Tầng sữa ngọt dịu, sữa tươi thơm mát và cà phê đậm vị (Size M)'),
    (@dmCafeST, N'Bạc Xỉu Ba Tầng', 'L', 33000, N'Tầng sữa ngọt dịu, sữa tươi thơm mát và cà phê đậm vị (Size L)'),
    (@dmSuaTuoi, N'Sữa Tươi Trân Châu Đường Đen', 'M', 35000, N'Sữa tươi thanh trùng cùng trân châu nấu đường đen dẻo quánh (Size M)'),
    (@dmSuaTuoi, N'Sữa Tươi Trân Châu Đường Đen', 'L', 42000, N'Sữa tươi thanh trùng cùng trân châu nấu đường đen dẻo quánh (Size L)'),
    (@dmSuaTuoi, N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', 32000, N'Sữa tươi béo nhẹ kết hợp sương sáo Đài Loan mát lành (Size M)'),
    (@dmSuaTuoi, N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', 38000, N'Sữa tươi béo nhẹ kết hợp sương sáo Đài Loan mát lành (Size L)'),
    (@dmMacchiato, N'Trà Ô Long Váng Sữa Macchiato', 'M', 35000, N'Cốt trà ô long thượng hạng phủ lớp macchiato kem sữa béo ngậy (Size M)'),
    (@dmMacchiato, N'Trà Ô Long Váng Sữa Macchiato', 'L', 42000, N'Cốt trà ô long thượng hạng phủ lớp macchiato kem sữa béo ngậy (Size L)'),
    (@dmMacchiato, N'Trà Đen Macchiato Muối Biển', 'M', 32000, N'Trà đen đậm đà tương phản với kem muối biển béo ngậy (Size M)'),
    (@dmMacchiato, N'Trà Đen Macchiato Muối Biển', 'L', 38000, N'Trà đen đậm đà tương phản với kem muối biển béo ngậy (Size L)'),
    (@dmDaXay, N'Matcha Đá Xay Kem Tuyết', 'M', 40000, N'Matcha xay nhuyễn cùng đá và sữa, phủ bông kem whipping tươi (Size M)'),
    (@dmDaXay, N'Matcha Đá Xay Kem Tuyết', 'L', 48000, N'Matcha xay nhuyễn cùng đá và sữa, phủ bông kem whipping tươi (Size L)'),
    (@dmDaXay, N'Cà Phê Đá Xay Caramel', 'M', 38000, N'Espresso xay đá cùng xốt caramel ngọt bùi phủ kem whipping (Size M)'),
    (@dmDaXay, N'Cà Phê Đá Xay Caramel', 'L', 45000, N'Espresso xay đá cùng xốt caramel ngọt bùi phủ kem whipping (Size L)'),
    (@dmDaXay, N'Sinh Tố Bơ Dừa Thơm Béo', 'M', 42000, N'Bơ sáp Đắk Lắk dẻo quánh xay cùng nước cốt dừa béo bùi (Size M)'),
    (@dmDaXay, N'Sinh Tố Bơ Dừa Thơm Béo', 'L', 50000, N'Bơ sáp Đắk Lắk dẻo quánh xay cùng nước cốt dừa béo bùi (Size L)'),
    (@dmThaoMoc, N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', 32000, N'Hoa cúc sấy khô ủ ấm mật ong rừng và hạt sen bùi ngậy (Size M)'),
    (@dmThaoMoc, N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', 38000, N'Hoa cúc sấy khô ủ ấm mật ong rừng và hạt sen bùi ngậy (Size L)'),
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
USING @Products AS source 
   ON (target.TenMon = source.TenMon OR LTRIM(RTRIM(target.TenMon)) = LTRIM(RTRIM(source.TenMon))) 
  AND target.Size = source.Size
WHEN MATCHED THEN 
    UPDATE SET 
        target.TenMon   = source.TenMon,
        target.Gia      = source.Gia, 
        target.MoTa     = source.MoTa, 
        target.MaDM     = source.MaDM, 
        target.HoatDong = 1
WHEN NOT MATCHED THEN 
    INSERT (MaDM, TenMon, Size, MoTa, Gia, HoatDong) 
    VALUES (source.MaDM, source.TenMon, source.Size, source.MoTa, source.Gia, 1);

-- Khôi phục và bảo toàn 100% ảnh Cloudinary hiện có cho các món
UPDATE m
SET m.AnhUrl = img.AnhUrl,
    m.AnhPublicId = img.AnhPublicId
FROM dbo.MonUong m
JOIN @ExistingImages img 
  ON (m.TenMon = img.TenMon OR LTRIM(RTRIM(m.TenMon)) = img.TenMon)
 AND m.Size = img.Size
WHERE m.AnhUrl IS NULL OR LTRIM(RTRIM(m.AnhUrl)) = '';

-- Kế thừa ảnh từ size khác của cùng món nếu món đó chưa có ảnh
UPDATE m
SET m.AnhUrl = img.AnhUrl,
    m.AnhPublicId = img.AnhPublicId
FROM dbo.MonUong m
JOIN @ExistingImages img 
  ON (m.TenMon = img.TenMon OR LTRIM(RTRIM(m.TenMon)) = img.TenMon)
WHERE (m.AnhUrl IS NULL OR LTRIM(RTRIM(m.AnhUrl)) = '')
  AND img.AnhUrl IS NOT NULL;

PRINT N'-- [6/16] Đã nạp đầy đủ 70 món uống và topping chuẩn.';
GO

-- ============================================================================
-- 7. TỒN KHO HAI TẦNG (TonNguyenLieu) - KHO & BEP Đầy Đủ Cho 100% Thực Đơn
-- ============================================================================
DECLARE @StockSetup TABLE (
    TenNL           NVARCHAR(120),
    ViTri           VARCHAR(8),
    SoLuong         DECIMAL(18,3),
    NguongCanhBao   DECIMAL(18,3),
    GiaVonDonVi     DECIMAL(18,6)
);

INSERT INTO @StockSetup VALUES
    -- Kho nguyên liệu thô (KHO) - Đầy đủ 27 nguyên liệu thô có lịch sử nhập kho
    (N'Trà Đen Cổ Thụ',        'KHO', 45.000, 10.000, 120000.00),
    (N'Trà Ô Long Kim Tuyên',   'KHO', 28.000, 8.000,  180000.00),
    (N'Trà Lài Thượng Hạng',    'KHO', 32.000, 8.000,  150000.00),
    (N'Cà Phê Hạt Rang Mộc',   'KHO', 25.000, 5.000,  160000.00),
    (N'Sữa Đặc Có Đường',       'KHO', 80.000, 15.000, 45000.00),
    (N'Đường Cát Trắng',        'KHO', 120.000, 20.000,22000.00),
    (N'Đường Đen Nữ Hoàng',     'KHO', 25.000,  5.000,  35000.00),
    (N'Sữa Tươi Thanh Trùng',   'KHO', 50.000, 10.000, 38000.00),
    (N'Bột Béo Thực Vật',       'KHO', 60.000, 10.000, 65000.00),
    (N'Bột Kem Cheese Anchor',  'KHO', 15.000, 3.000,  220000.00),
    (N'Trân Châu Thô Đen',      'KHO', 40.000, 10.000, 30000.00),
    (N'Hạt Trân Châu Trắng 3Q', 'KHO', 25.000, 5.000,  42000.00),
    (N'Đào Ngâm Đóng Hộp',      'KHO', 30.000, 5.000,  55000.00),
    (N'Vải Ngâm Đóng Hộp',      'KHO', 25.000, 5.000,  60000.00),
    (N'Dâu Tây Tươi',           'KHO', 20.000, 5.000,  90000.00),
    (N'Xoài Cát Chín',          'KHO', 15.000, 5.000,  40000.00),
    (N'Chanh Dây Tươi',         'KHO', 12.000, 3.000,  30000.00),
    (N'Cam Vàng Sành',          'KHO', 20.000, 5.000,  35000.00),
    (N'Sả Cây Tươi',            'KHO', 10.000, 2.000,  20000.00),
    (N'Bột Matcha Uji',         'KHO', 5.000,  1.000,  650000.00),
    (N'Bột Cacao Nguyên Chất',  'KHO', 8.000,  2.000,  140000.00),
    (N'Bơ Sáp Đắk Lắk',         'KHO', 12.000, 3.000,  60000.00),
    (N'Nước Cốt Dừa Đóng Lon',  'KHO', 15.000, 3.000,  48000.00),
    (N'Muối Biển Tinh Khiết',   'KHO', 20.000, 5.000,  10000.00),
    (N'Ly Nhựa PP 500ml',       'KHO', 2000.000, 300.000, 450.00),
    (N'Ly Nhựa PP 700ml',       'KHO', 2500.000, 300.000, 550.00),
    (N'Ống Hút Trân Châu',      'KHO', 3000.000, 500.000, 120.00),
    (N'Nước Cốt Trà Đen',       'KHO', 10000.000, 2000.000, 45.00),
    (N'Nước Cốt Trà Ô Long',    'KHO', 8000.000,  2000.000, 65.00),
    (N'Nước Cốt Trà Lài',       'KHO', 6000.000,  2000.000, 50.00),
    (N'Cốt Cà Phê Pha Phin',    'KHO', 5000.000,  1000.000, 85.00),
    (N'Nước Đường Pha Sẵn',     'KHO', 12000.000, 2000.000, 20.00),
    (N'Trân Châu Nấu Chín',     'KHO', 6000.000,  1000.000, 28.00),
    (N'Kem Cheese Thành Phẩm',  'KHO', 4000.000,  800.000,  95.00),
    (N'Thạch Đào Cắt Hạt Lựu',  'KHO', 3000.000,  500.000,  60.00),
    (N'Sốt Dâu Tây Tươi',       'KHO', 4000.000,  500.000,  80.00),
    (N'Sốt Xoài Chanh Dây',     'KHO', 4000.000,  500.000,  55.00),

    -- Bếp pha chế (BEP) - Thành phẩm sơ chế & nguyên liệu cấp phát quầy (đủ 37 nguyên liệu)
    (N'Nước Cốt Trà Đen',       'BEP', 8500.000, 2000.000, 45.00),
    (N'Nước Cốt Trà Ô Long',    'BEP', 6000.000, 2000.000, 65.00),
    (N'Nước Cốt Trà Lài',       'BEP', 3500.000, 2000.000, 50.00),
    (N'Cốt Cà Phê Pha Phin',    'BEP', 3500.000, 1000.000, 85.00),
    (N'Nước Đường Pha Sẵn',     'BEP', 9000.000, 2000.000, 20.00),
    (N'Trân Châu Nấu Chín',     'BEP', 4500.000, 1000.000, 28.00),
    (N'Kem Cheese Thành Phẩm',  'BEP', 2500.000, 800.000,  95.00),
    (N'Thạch Đào Cắt Hạt Lựu',  'BEP', 2000.000, 500.000,  60.00),
    (N'Sốt Dâu Tây Tươi',       'BEP', 3000.000, 500.000,  80.00),
    (N'Sốt Xoài Chanh Dây',     'BEP', 3000.000, 500.000,  55.00),

    (N'Trà Đen Cổ Thụ',        'BEP', 5.000,    2.000,    120000.00),
    (N'Trà Ô Long Kim Tuyên',   'BEP', 5.000,    2.000,    180000.00),
    (N'Trà Lài Thượng Hạng',    'BEP', 5.000,    2.000,    150000.00),
    (N'Cà Phê Hạt Rang Mộc',   'BEP', 5.000,    2.000,    160000.00),
    (N'Trân Châu Thô Đen',      'BEP', 5.000,    2.000,    30000.00),
    (N'Đào Ngâm Đóng Hộp',      'BEP', 5.000,    1.000,    55000.00),
    (N'Dâu Tây Tươi',           'BEP', 5.000,    1.000,    90000.00),
    (N'Xoài Cát Chín',          'BEP', 3.000,    1.000,    40000.00),
    (N'Chanh Dây Tươi',         'BEP', 3.000,    1.000,    30000.00),
    (N'Hạt Trân Châu Trắng 3Q', 'BEP', 5.000,    1.000,    42000.00),
    (N'Sữa Đặc Có Đường',       'BEP', 12.000,   3.000,    45000.00),
    (N'Sữa Tươi Thanh Trùng',   'BEP', 15.000,   5.000,    38000.00),
    (N'Bột Béo Thực Vật',       'BEP', 8.000,    2.000,    65000.00),
    (N'Bột Kem Cheese Anchor',  'BEP', 3.000,    1.000,    220000.00),
    (N'Bột Matcha Uji',         'BEP', 2.000,    0.500,    650000.00),
    (N'Bột Cacao Nguyên Chất',  'BEP', 3.000,    1.000,    140000.00),
    (N'Bơ Sáp Đắk Lắk',         'BEP', 4.000,    1.000,    60000.00),
    (N'Nước Cốt Dừa Đóng Lon',  'BEP', 5.000,    1.000,    48000.00),
    (N'Muối Biển Tinh Khiết',   'BEP', 3.000,    1.000,    10000.00),
    (N'Đường Cát Trắng',        'BEP', 10.000,   2.000,    22000.00),
    (N'Đường Đen Nữ Hoàng',     'BEP', 3.000,    1.000,    35000.00),
    (N'Cam Vàng Sành',          'BEP', 5.000,    1.000,    35000.00),
    (N'Sả Cây Tươi',            'BEP', 2.000,    0.500,    20000.00),
    (N'Vải Ngâm Đóng Hộp',      'BEP', 5.000,    1.000,    60000.00),
    (N'Ly Nhựa PP 500ml',       'BEP', 450.000,  100.000,  450.00),
    (N'Ly Nhựa PP 700ml',       'BEP', 600.000,  100.000,  550.00),
    (N'Ống Hút Trân Châu',      'BEP', 800.000,  150.000,  120.00);

MERGE INTO dbo.TonNguyenLieu AS target
USING (
    SELECT nl.MaNL, s.ViTri, s.SoLuong, s.NguongCanhBao, s.GiaVonDonVi
    FROM @StockSetup s
    JOIN dbo.NguyenLieu nl ON nl.TenNL = s.TenNL
) AS source ON (target.MaNL = source.MaNL AND target.ViTri = source.ViTri)
WHEN MATCHED THEN
    UPDATE SET target.SoLuong = source.SoLuong, target.NguongCanhBao = source.NguongCanhBao, target.GiaVonDonVi = source.GiaVonDonVi
WHEN NOT MATCHED THEN
    INSERT (MaNL, ViTri, SoLuong, NguongCanhBao, GiaVonDonVi, Version)
    VALUES (source.MaNL, source.ViTri, source.SoLuong, source.NguongCanhBao, source.GiaVonDonVi, 0);

PRINT N'-- [7/16] Đã thiết lập tồn kho hai tầng KHO & BEP đầy đủ 100% nguyên liệu cho thực đơn.';
GO

-- ============================================================================
-- 8. CÔNG THỨC SƠ CHẾ & CHI TIẾT (CongThucSoChe, ChiTietCTSoChe)
-- ============================================================================
DECLARE @RecipeSoChe TABLE (
    TenCT             NVARCHAR(120),
    TenNLDauRa        NVARCHAR(120),
    LuongDauRaChuan   DECIMAL(18,3),
    NguongCanhBaoTyLe DECIMAL(5,2),
    TenNLDauVao       NVARCHAR(120),
    LuongDauVaoChuan  DECIMAL(18,3)
);

INSERT INTO @RecipeSoChe VALUES
    (N'Ủ Nước Cốt Trà Đen',        N'Nước Cốt Trà Đen',       3000.000, 15.00, N'Trà Đen Cổ Thụ',       0.150),
    (N'Ủ Nước Cốt Trà Ô Long',     N'Nước Cốt Trà Ô Long',    3000.000, 15.00, N'Trà Ô Long Kim Tuyên', 0.150),
    (N'Ủ Nước Cốt Trà Lài',        N'Nước Cốt Trà Lài',       3500.000, 15.00, N'Trà Lài Thượng Hạng',  0.150),
    (N'Pha Cốt Cà Phê Phin',       N'Cốt Cà Phê Pha Phin',    2000.000, 10.00, N'Cà Phê Hạt Rang Mộc',  0.500),
    (N'Nấu Nước Đường Pha Sẵn',    N'Nước Đường Pha Sẵn',     3000.000, 5.00,  N'Đường Cát Trắng',      2.000),
    (N'Nấu Trân Châu Đen Dẻo',     N'Trân Châu Nấu Chín',     2500.000, 10.00, N'Trân Châu Thô Đen',    1.000),
    (N'Đánh Bông Kem Cheese',      N'Kem Cheese Thành Phẩm',  2000.000, 8.00,  N'Bột Kem Cheese Anchor',0.500),
    (N'Sơ Chế Thạch Đào Hạt Lựu',  N'Thạch Đào Cắt Hạt Lựu',  1000.000, 5.00,  N'Đào Ngâm Đóng Hộp',    1.000),
    (N'Nấu Sốt Dâu Tây Tươi',      N'Sốt Dâu Tây Tươi',       1200.000, 10.00, N'Dâu Tây Tươi',         1.000),
    (N'Nấu Sốt Xoài Chanh Dây',    N'Sốt Xoài Chanh Dây',     1500.000, 10.00, N'Xoài Cát Chín',        1.000);

DECLARE curCT CURSOR FOR
    SELECT DISTINCT TenCT, TenNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe FROM @RecipeSoChe;

DECLARE @tTenCT NVARCHAR(120), @tTenNLDauRa NVARCHAR(120), @tLuongDauRa DECIMAL(18,3), @tNguongTyLe DECIMAL(5,2);

OPEN curCT;
FETCH NEXT FROM curCT INTO @tTenCT, @tTenNLDauRa, @tLuongDauRa, @tNguongTyLe;

WHILE @@FETCH_STATUS = 0
BEGIN
    DECLARE @nlDauRa AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = @tTenNLDauRa);
    IF @nlDauRa IS NOT NULL
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM dbo.CongThucSoChe WHERE TenCT = @tTenCT)
        BEGIN
            INSERT INTO dbo.CongThucSoChe (TenCT, MaNLDauRa, LuongDauRaChuan, NguongCanhBaoTyLe, HoatDong, Version)
            VALUES (@tTenCT, @nlDauRa, @tLuongDauRa, @tNguongTyLe, 1, 1);
        END
        ELSE
        BEGIN
            UPDATE dbo.CongThucSoChe 
            SET MaNLDauRa = @nlDauRa, LuongDauRaChuan = @tLuongDauRa, NguongCanhBaoTyLe = @tNguongTyLe, HoatDong = 1
            WHERE TenCT = @tTenCT;
        END

        DECLARE @maCT AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = @tTenCT);
        DELETE FROM dbo.ChiTietCTSoChe WHERE MaCTSC = @maCT;

        INSERT INTO dbo.ChiTietCTSoChe (MaCTSC, MaNL, LuongDauVaoChuan)
        SELECT @maCT, nl.MaNL, r.LuongDauVaoChuan
        FROM @RecipeSoChe r
        JOIN dbo.NguyenLieu nl ON nl.TenNL = r.TenNLDauVao
        WHERE r.TenCT = @tTenCT;
    END
    FETCH NEXT FROM curCT INTO @tTenCT, @tTenNLDauRa, @tLuongDauRa, @tNguongTyLe;
END

CLOSE curCT;
DEALLOCATE curCT;

PRINT N'-- [8/16] Đã nạp 10 công thức sơ chế chuẩn kèm định mức đầu vào.';
GO

-- ============================================================================
-- 9. CÔNG THỨC ĐỊNH MỨC MÓN UỐNG & TOPPING (CongThucMon) - 100% 70 MÓN ĐỀU CÓ CÔNG THỨC
-- ============================================================================
DECLARE @DrinkRecipes TABLE (
    TenMon    NVARCHAR(120),
    Size      VARCHAR(12),
    TenNL     NVARCHAR(120),
    DinhLuong DECIMAL(18,3)
);

INSERT INTO @DrinkRecipes VALUES
    (N'Trà Sữa Truyền Thống', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Sữa Truyền Thống', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Truyền Thống', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Sữa Truyền Thống', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Truyền Thống', 'M', N'Nước Cốt Trà Đen', 120.000),
    (N'Trà Sữa Truyền Thống', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Trà Sữa Truyền Thống', 'M', N'Bột Béo Thực Vật', 0.025),
    (N'Trà Sữa Truyền Thống', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Sữa Truyền Thống', 'L', N'Nước Cốt Trà Đen', 160.000),
    (N'Trà Sữa Truyền Thống', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Sữa Truyền Thống', 'L', N'Bột Béo Thực Vật', 0.035),
    (N'Trà Sữa Truyền Thống', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Sữa Thái Xanh', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Sữa Thái Xanh', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Thái Xanh', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Sữa Thái Xanh', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Thái Xanh', 'M', N'Nước Cốt Trà Lài', 120.000),
    (N'Trà Sữa Thái Xanh', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Trà Sữa Thái Xanh', 'M', N'Bột Béo Thực Vật', 0.025),
    (N'Trà Sữa Thái Xanh', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Sữa Thái Xanh', 'L', N'Nước Cốt Trà Lài', 160.000),
    (N'Trà Sữa Thái Xanh', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Sữa Thái Xanh', 'L', N'Bột Béo Thực Vật', 0.035),
    (N'Trà Sữa Thái Xanh', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Sữa Thái Đỏ', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Sữa Thái Đỏ', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Thái Đỏ', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Sữa Thái Đỏ', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Thái Đỏ', 'M', N'Nước Cốt Trà Đen', 120.000),
    (N'Trà Sữa Thái Đỏ', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Trà Sữa Thái Đỏ', 'M', N'Bột Béo Thực Vật', 0.025),
    (N'Trà Sữa Thái Đỏ', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Sữa Thái Đỏ', 'L', N'Nước Cốt Trà Đen', 160.000),
    (N'Trà Sữa Thái Đỏ', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Sữa Thái Đỏ', 'L', N'Bột Béo Thực Vật', 0.035),
    (N'Trà Sữa Thái Đỏ', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Sữa Socola Đậm Đà', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Sữa Socola Đậm Đà', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Socola Đậm Đà', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Sữa Socola Đậm Đà', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Socola Đậm Đà', 'M', N'Nước Cốt Trà Đen', 120.000),
    (N'Trà Sữa Socola Đậm Đà', 'M', N'Bột Cacao Nguyên Chất', 0.020),
    (N'Trà Sữa Socola Đậm Đà', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Trà Sữa Socola Đậm Đà', 'M', N'Bột Béo Thực Vật', 0.020),
    (N'Trà Sữa Socola Đậm Đà', 'L', N'Nước Cốt Trà Đen', 160.000),
    (N'Trà Sữa Socola Đậm Đà', 'L', N'Bột Cacao Nguyên Chất', 0.030),
    (N'Trà Sữa Socola Đậm Đà', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Sữa Socola Đậm Đà', 'L', N'Bột Béo Thực Vật', 0.025),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Nước Cốt Trà Lài', 120.000),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Sữa Tươi Thanh Trùng', 0.050),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'M', N'Bột Béo Thực Vật', 0.025),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Nước Cốt Trà Lài', 160.000),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Sữa Tươi Thanh Trùng', 0.070),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Sữa Khoai Môn Hoàng Kim', 'L', N'Bột Béo Thực Vật', 0.035),
    (N'Trà Sữa Ô Long Nướng', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Sữa Ô Long Nướng', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Ô Long Nướng', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Sữa Ô Long Nướng', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Ô Long Nướng', 'M', N'Nước Cốt Trà Ô Long', 120.000),
    (N'Trà Sữa Ô Long Nướng', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Trà Sữa Ô Long Nướng', 'M', N'Bột Béo Thực Vật', 0.025),
    (N'Trà Sữa Ô Long Nướng', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Sữa Ô Long Nướng', 'L', N'Nước Cốt Trà Ô Long', 160.000),
    (N'Trà Sữa Ô Long Nướng', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Sữa Ô Long Nướng', 'L', N'Bột Béo Thực Vật', 0.035),
    (N'Trà Sữa Ô Long Nướng', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Nước Cốt Trà Ô Long', 130.000),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Bột Béo Thực Vật', 0.025),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Nước Cốt Trà Ô Long', 170.000),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Bột Béo Thực Vật', 0.035),
    (N'Trà Sữa Ô Long Thiết Quan Âm', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Sữa Lài Thơm', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Sữa Lài Thơm', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Lài Thơm', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Sữa Lài Thơm', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Lài Thơm', 'M', N'Nước Cốt Trà Lài', 120.000),
    (N'Trà Sữa Lài Thơm', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Trà Sữa Lài Thơm', 'M', N'Bột Béo Thực Vật', 0.025),
    (N'Trà Sữa Lài Thơm', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Sữa Lài Thơm', 'L', N'Nước Cốt Trà Lài', 160.000),
    (N'Trà Sữa Lài Thơm', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Sữa Lài Thơm', 'L', N'Bột Béo Thực Vật', 0.035),
    (N'Trà Sữa Lài Thơm', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Sữa Lài Kem Cheese', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Sữa Lài Kem Cheese', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Lài Kem Cheese', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Sữa Lài Kem Cheese', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Lài Kem Cheese', 'M', N'Nước Cốt Trà Lài', 120.000),
    (N'Trà Sữa Lài Kem Cheese', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Trà Sữa Lài Kem Cheese', 'M', N'Kem Cheese Thành Phẩm', 40.000),
    (N'Trà Sữa Lài Kem Cheese', 'L', N'Nước Cốt Trà Lài', 160.000),
    (N'Trà Sữa Lài Kem Cheese', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Sữa Lài Kem Cheese', 'L', N'Kem Cheese Thành Phẩm', 60.000),
    (N'Trà Sữa Matcha Nhật Bản', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Sữa Matcha Nhật Bản', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Matcha Nhật Bản', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Sữa Matcha Nhật Bản', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Sữa Matcha Nhật Bản', 'M', N'Bột Matcha Uji', 0.015),
    (N'Trà Sữa Matcha Nhật Bản', 'M', N'Sữa Tươi Thanh Trùng', 0.120),
    (N'Trà Sữa Matcha Nhật Bản', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Trà Sữa Matcha Nhật Bản', 'M', N'Nước Đường Pha Sẵn', 15.000),
    (N'Trà Sữa Matcha Nhật Bản', 'L', N'Bột Matcha Uji', 0.020),
    (N'Trà Sữa Matcha Nhật Bản', 'L', N'Sữa Tươi Thanh Trùng', 0.160),
    (N'Trà Sữa Matcha Nhật Bản', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Sữa Matcha Nhật Bản', 'L', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Đào Cam Sả', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Đào Cam Sả', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Đào Cam Sả', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Đào Cam Sả', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Đào Cam Sả', 'M', N'Nước Cốt Trà Đen', 100.000),
    (N'Trà Đào Cam Sả', 'M', N'Nước Đường Pha Sẵn', 30.000),
    (N'Trà Đào Cam Sả', 'M', N'Cam Vàng Sành', 0.050),
    (N'Trà Đào Cam Sả', 'M', N'Sả Cây Tươi', 0.020),
    (N'Trà Đào Cam Sả', 'L', N'Nước Cốt Trà Đen', 140.000),
    (N'Trà Đào Cam Sả', 'L', N'Nước Đường Pha Sẵn', 40.000),
    (N'Trà Đào Cam Sả', 'L', N'Cam Vàng Sành', 0.070),
    (N'Trà Đào Cam Sả', 'L', N'Sả Cây Tươi', 0.030),
    (N'Trà Vải Hoa Hồng', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Vải Hoa Hồng', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Vải Hoa Hồng', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Vải Hoa Hồng', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Vải Hoa Hồng', 'M', N'Nước Cốt Trà Lài', 100.000),
    (N'Trà Vải Hoa Hồng', 'M', N'Vải Ngâm Đóng Hộp', 0.060),
    (N'Trà Vải Hoa Hồng', 'M', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Vải Hoa Hồng', 'L', N'Nước Cốt Trà Lài', 140.000),
    (N'Trà Vải Hoa Hồng', 'L', N'Vải Ngâm Đóng Hộp', 0.090),
    (N'Trà Vải Hoa Hồng', 'L', N'Nước Đường Pha Sẵn', 35.000),
    (N'Trà Dâu Tây Nhiệt Đới', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Dâu Tây Nhiệt Đới', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Dâu Tây Nhiệt Đới', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Dâu Tây Nhiệt Đới', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Dâu Tây Nhiệt Đới', 'M', N'Nước Cốt Trà Lài', 100.000),
    (N'Trà Dâu Tây Nhiệt Đới', 'M', N'Sốt Dâu Tây Tươi', 50.000),
    (N'Trà Dâu Tây Nhiệt Đới', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Dâu Tây Nhiệt Đới', 'L', N'Nước Cốt Trà Lài', 140.000),
    (N'Trà Dâu Tây Nhiệt Đới', 'L', N'Sốt Dâu Tây Tươi', 70.000),
    (N'Trà Dâu Tây Nhiệt Đới', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Xoài Chanh Dây', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Xoài Chanh Dây', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Xoài Chanh Dây', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Xoài Chanh Dây', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Xoài Chanh Dây', 'M', N'Nước Cốt Trà Lài', 100.000),
    (N'Trà Xoài Chanh Dây', 'M', N'Sốt Xoài Chanh Dây', 50.000),
    (N'Trà Xoài Chanh Dây', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Xoài Chanh Dây', 'L', N'Nước Cốt Trà Lài', 140.000),
    (N'Trà Xoài Chanh Dây', 'L', N'Sốt Xoài Chanh Dây', 70.000),
    (N'Trà Xoài Chanh Dây', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Ổi Hồng Hạt Chia', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Ổi Hồng Hạt Chia', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Ổi Hồng Hạt Chia', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Ổi Hồng Hạt Chia', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Ổi Hồng Hạt Chia', 'M', N'Nước Cốt Trà Lài', 110.000),
    (N'Trà Ổi Hồng Hạt Chia', 'M', N'Nước Đường Pha Sẵn', 30.000),
    (N'Trà Ổi Hồng Hạt Chia', 'L', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Ổi Hồng Hạt Chia', 'L', N'Nước Đường Pha Sẵn', 40.000),
    (N'Trà Táo Xanh Bạc Hà', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Táo Xanh Bạc Hà', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Táo Xanh Bạc Hà', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Táo Xanh Bạc Hà', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Táo Xanh Bạc Hà', 'M', N'Nước Cốt Trà Lài', 110.000),
    (N'Trà Táo Xanh Bạc Hà', 'M', N'Nước Đường Pha Sẵn', 30.000),
    (N'Trà Táo Xanh Bạc Hà', 'L', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Táo Xanh Bạc Hà', 'L', N'Nước Đường Pha Sẵn', 40.000),
    (N'Trà Bưởi Mật Ong', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Bưởi Mật Ong', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Bưởi Mật Ong', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Bưởi Mật Ong', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Bưởi Mật Ong', 'M', N'Nước Cốt Trà Lài', 110.000),
    (N'Trà Bưởi Mật Ong', 'M', N'Nước Đường Pha Sẵn', 30.000),
    (N'Trà Bưởi Mật Ong', 'L', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Bưởi Mật Ong', 'L', N'Nước Đường Pha Sẵn', 40.000),
    (N'Trà Mãng Cầu Xiêm', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Mãng Cầu Xiêm', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Mãng Cầu Xiêm', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Mãng Cầu Xiêm', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Mãng Cầu Xiêm', 'M', N'Nước Cốt Trà Lài', 110.000),
    (N'Trà Mãng Cầu Xiêm', 'M', N'Nước Đường Pha Sẵn', 35.000),
    (N'Trà Mãng Cầu Xiêm', 'L', N'Nước Cốt Trà Lài', 150.000),
    (N'Trà Mãng Cầu Xiêm', 'L', N'Nước Đường Pha Sẵn', 45.000),
    (N'Cà Phê Đen Đá', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Cà Phê Đen Đá', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Cà Phê Đen Đá', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Cà Phê Đen Đá', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Cà Phê Đen Đá', 'M', N'Cốt Cà Phê Pha Phin', 60.000),
    (N'Cà Phê Đen Đá', 'M', N'Đường Cát Trắng', 0.020),
    (N'Cà Phê Đen Đá', 'L', N'Cốt Cà Phê Pha Phin', 80.000),
    (N'Cà Phê Đen Đá', 'L', N'Đường Cát Trắng', 0.025),
    (N'Cà Phê Sữa Đá', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Cà Phê Sữa Đá', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Cà Phê Sữa Đá', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Cà Phê Sữa Đá', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Cà Phê Sữa Đá', 'M', N'Cốt Cà Phê Pha Phin', 60.000),
    (N'Cà Phê Sữa Đá', 'M', N'Sữa Đặc Có Đường', 0.040),
    (N'Cà Phê Sữa Đá', 'L', N'Cốt Cà Phê Pha Phin', 80.000),
    (N'Cà Phê Sữa Đá', 'L', N'Sữa Đặc Có Đường', 0.050),
    (N'Cà Phê Muối Xứ Huế', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Cà Phê Muối Xứ Huế', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Cà Phê Muối Xứ Huế', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Cà Phê Muối Xứ Huế', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Cà Phê Muối Xứ Huế', 'M', N'Cốt Cà Phê Pha Phin', 60.000),
    (N'Cà Phê Muối Xứ Huế', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Cà Phê Muối Xứ Huế', 'M', N'Kem Cheese Thành Phẩm', 40.000),
    (N'Cà Phê Muối Xứ Huế', 'M', N'Muối Biển Tinh Khiết', 0.002),
    (N'Cà Phê Muối Xứ Huế', 'L', N'Cốt Cà Phê Pha Phin', 80.000),
    (N'Cà Phê Muối Xứ Huế', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Cà Phê Muối Xứ Huế', 'L', N'Kem Cheese Thành Phẩm', 55.000),
    (N'Cà Phê Muối Xứ Huế', 'L', N'Muối Biển Tinh Khiết', 0.003),
    (N'Bạc Xỉu Ba Tầng', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Bạc Xỉu Ba Tầng', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Bạc Xỉu Ba Tầng', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Bạc Xỉu Ba Tầng', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Bạc Xỉu Ba Tầng', 'M', N'Cốt Cà Phê Pha Phin', 40.000),
    (N'Bạc Xỉu Ba Tầng', 'M', N'Sữa Đặc Có Đường', 0.040),
    (N'Bạc Xỉu Ba Tầng', 'M', N'Sữa Tươi Thanh Trùng', 0.080),
    (N'Bạc Xỉu Ba Tầng', 'L', N'Cốt Cà Phê Pha Phin', 50.000),
    (N'Bạc Xỉu Ba Tầng', 'L', N'Sữa Đặc Có Đường', 0.050),
    (N'Bạc Xỉu Ba Tầng', 'L', N'Sữa Tươi Thanh Trùng', 0.110),
    (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Sữa Tươi Thanh Trùng', 0.160),
    (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Trân Châu Nấu Chín', 60.000),
    (N'Sữa Tươi Trân Châu Đường Đen', 'M', N'Đường Đen Nữ Hoàng', 0.025),
    (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Sữa Tươi Thanh Trùng', 0.200),
    (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Trân Châu Nấu Chín', 80.000),
    (N'Sữa Tươi Trân Châu Đường Đen', 'L', N'Đường Đen Nữ Hoàng', 0.030),
    (N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', N'Sữa Tươi Thanh Trùng', 0.160),
    (N'Sữa Tươi Sương Sáo Thảo Mộc', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', N'Sữa Tươi Thanh Trùng', 0.200),
    (N'Sữa Tươi Sương Sáo Thảo Mộc', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Nước Cốt Trà Ô Long', 120.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'M', N'Kem Cheese Thành Phẩm', 45.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Nước Cốt Trà Ô Long', 160.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Ô Long Váng Sữa Macchiato', 'L', N'Kem Cheese Thành Phẩm', 60.000),
    (N'Trà Đen Macchiato Muối Biển', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Đen Macchiato Muối Biển', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Đen Macchiato Muối Biển', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Đen Macchiato Muối Biển', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Đen Macchiato Muối Biển', 'M', N'Nước Cốt Trà Đen', 120.000),
    (N'Trà Đen Macchiato Muối Biển', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Trà Đen Macchiato Muối Biển', 'M', N'Kem Cheese Thành Phẩm', 45.000),
    (N'Trà Đen Macchiato Muối Biển', 'M', N'Muối Biển Tinh Khiết', 0.002),
    (N'Trà Đen Macchiato Muối Biển', 'L', N'Nước Cốt Trà Đen', 160.000),
    (N'Trà Đen Macchiato Muối Biển', 'L', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Đen Macchiato Muối Biển', 'L', N'Kem Cheese Thành Phẩm', 60.000),
    (N'Trà Đen Macchiato Muối Biển', 'L', N'Muối Biển Tinh Khiết', 0.003),
    (N'Matcha Đá Xay Kem Tuyết', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Matcha Đá Xay Kem Tuyết', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Matcha Đá Xay Kem Tuyết', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Matcha Đá Xay Kem Tuyết', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Matcha Đá Xay Kem Tuyết', 'M', N'Bột Matcha Uji', 0.012),
    (N'Matcha Đá Xay Kem Tuyết', 'M', N'Sữa Tươi Thanh Trùng', 0.120),
    (N'Matcha Đá Xay Kem Tuyết', 'M', N'Sữa Đặc Có Đường', 0.030),
    (N'Matcha Đá Xay Kem Tuyết', 'M', N'Kem Cheese Thành Phẩm', 30.000),
    (N'Matcha Đá Xay Kem Tuyết', 'L', N'Bột Matcha Uji', 0.015),
    (N'Matcha Đá Xay Kem Tuyết', 'L', N'Sữa Tươi Thanh Trùng', 0.150),
    (N'Matcha Đá Xay Kem Tuyết', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Matcha Đá Xay Kem Tuyết', 'L', N'Kem Cheese Thành Phẩm', 40.000),
    (N'Cà Phê Đá Xay Caramel', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Cà Phê Đá Xay Caramel', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Cà Phê Đá Xay Caramel', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Cà Phê Đá Xay Caramel', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Cà Phê Đá Xay Caramel', 'M', N'Cốt Cà Phê Pha Phin', 50.000),
    (N'Cà Phê Đá Xay Caramel', 'M', N'Sữa Đặc Có Đường', 0.035),
    (N'Cà Phê Đá Xay Caramel', 'M', N'Sữa Tươi Thanh Trùng', 0.080),
    (N'Cà Phê Đá Xay Caramel', 'M', N'Kem Cheese Thành Phẩm', 30.000),
    (N'Cà Phê Đá Xay Caramel', 'L', N'Cốt Cà Phê Pha Phin', 70.000),
    (N'Cà Phê Đá Xay Caramel', 'L', N'Sữa Đặc Có Đường', 0.045),
    (N'Cà Phê Đá Xay Caramel', 'L', N'Sữa Tươi Thanh Trùng', 0.100),
    (N'Cà Phê Đá Xay Caramel', 'L', N'Kem Cheese Thành Phẩm', 40.000),
    (N'Sinh Tố Bơ Dừa Thơm Béo', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Sinh Tố Bơ Dừa Thơm Béo', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Sinh Tố Bơ Dừa Thơm Béo', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Sinh Tố Bơ Dừa Thơm Béo', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Sinh Tố Bơ Dừa Thơm Béo', 'M', N'Bơ Sáp Đắk Lắk', 0.100),
    (N'Sinh Tố Bơ Dừa Thơm Béo', 'M', N'Nước Cốt Dừa Đóng Lon', 0.040),
    (N'Sinh Tố Bơ Dừa Thơm Béo', 'M', N'Sữa Đặc Có Đường', 0.035),
    (N'Sinh Tố Bơ Dừa Thơm Béo', 'L', N'Bơ Sáp Đắk Lắk', 0.120),
    (N'Sinh Tố Bơ Dừa Thơm Béo', 'L', N'Nước Cốt Dừa Đóng Lon', 0.050),
    (N'Sinh Tố Bơ Dừa Thơm Béo', 'L', N'Sữa Đặc Có Đường', 0.040),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Ly Nhựa PP 500ml', 1.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Ly Nhựa PP 700ml', 1.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Ống Hút Trân Châu', 1.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Nước Cốt Trà Lài', 120.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'M', N'Nước Đường Pha Sẵn', 25.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Nước Cốt Trà Lài', 160.000),
    (N'Trà Hoa Cúc Mật Ong Hạt Sen', 'L', N'Nước Đường Pha Sẵn', 35.000),
    (N'Trân Châu Đen Hoàng Kim', 'M', N'Trân Châu Nấu Chín', 50.000),
    (N'Trân Châu Trắng 3Q', 'M', N'Hạt Trân Châu Trắng 3Q', 0.040),
    (N'Thạch Đào Giòn Ngọt', 'M', N'Thạch Đào Cắt Hạt Lựu', 50.000),
    (N'Kem Cheese Phô Mai Macchiato', 'M', N'Kem Cheese Thành Phẩm', 40.000),
    (N'Pudding Trứng Vàng', 'M', N'Sữa Tươi Thanh Trùng', 0.040),
    (N'Pudding Trứng Vàng', 'M', N'Sữa Đặc Có Đường', 0.015),
    (N'Thạch Phô Mai Tươi', 'M', N'Sữa Tươi Thanh Trùng', 0.040),
    (N'Thạch Phô Mai Tươi', 'M', N'Bột Kem Cheese Anchor', 0.015),
    (N'Sương Sáo Thảo Mộc Đài Loan', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Thạch Củ Năng Giòn', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Nha Đam Tươi Đường Phèn', 'M', N'Nước Đường Pha Sẵn', 20.000),
    (N'Khúc Bạch Hạnh Nhân', 'M', N'Sữa Tươi Thanh Trùng', 0.040),
    (N'Khúc Bạch Hạnh Nhân', 'M', N'Sữa Đặc Có Đường', 0.015);

MERGE INTO dbo.CongThucMon AS target
USING (
    SELECT m.MaMon, nl.MaNL, dr.DinhLuong
    FROM @DrinkRecipes dr
    JOIN dbo.MonUong m ON m.TenMon = dr.TenMon AND m.Size = dr.Size
    JOIN dbo.NguyenLieu nl ON nl.TenNL = dr.TenNL
) AS source ON (target.MaMon = source.MaMon AND target.MaNL = source.MaNL)
WHEN MATCHED THEN
    UPDATE SET target.DinhLuong = source.DinhLuong
WHEN NOT MATCHED THEN
    INSERT (MaMon, MaNL, DinhLuong) VALUES (source.MaMon, source.MaNL, source.DinhLuong);

PRINT N'-- [9/16] Đã nạp công thức định mức cho toàn bộ 70 món uống & topping (325 dòng định mức).';
GO

-- ============================================================================
-- 10. PHIẾU NHẬP KHO (PhieuNhap, ChiTietNhap) - 3 Phiếu Nhập Bao Phủ 100% 27 Nguyên Liệu Thô
-- ============================================================================
DECLARE @adminId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'ADMIN');

-- Phiếu nhập 1: Trà, Cà phê, Sữa, Đường, Bột pha chế & Muối biển
IF NOT EXISTS (SELECT 1 FROM dbo.PhieuNhap WHERE MaYeuCau = 'PN_DEMO_01')
BEGIN
    INSERT INTO dbo.PhieuNhap (NguoiLap, LyDo, NguonNhap, TrangThai, LapLuc, GhiSoLuc, MaYeuCau)
    VALUES (@adminId, N'Nhập nguyên liệu định kỳ từ Tổng kho Vinamilk, Tân Cương & Anchor', N'Tổng kho Nguyên liệu TP.HCM', 'DA_GHI_SO', DATEADD(DAY, -4, SYSUTCDATETIME()), DATEADD(DAY, -4, SYSUTCDATETIME()), 'PN_DEMO_01');

    DECLARE @pn1 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietNhap (MaPhieu, MaNL, SoLuong, DonGiaNhap)
    SELECT @pn1, MaNL, 50.000, 120000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Trà Đen Cổ Thụ'
    UNION ALL SELECT @pn1, MaNL, 35.000, 180000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Trà Ô Long Kim Tuyên'
    UNION ALL SELECT @pn1, MaNL, 40.000, 150000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Trà Lài Thượng Hạng'
    UNION ALL SELECT @pn1, MaNL, 30.000, 160000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Cà Phê Hạt Rang Mộc'
    UNION ALL SELECT @pn1, MaNL, 100.000, 45000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Sữa Đặc Có Đường'
    UNION ALL SELECT @pn1, MaNL, 150.000, 22000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Đường Cát Trắng'
    UNION ALL SELECT @pn1, MaNL, 30.000, 35000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Đường Đen Nữ Hoàng'
    UNION ALL SELECT @pn1, MaNL, 70.000, 38000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Sữa Tươi Thanh Trùng'
    UNION ALL SELECT @pn1, MaNL, 70.000, 65000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Bột Béo Thực Vật'
    UNION ALL SELECT @pn1, MaNL, 20.000, 220000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Bột Kem Cheese Anchor'
    UNION ALL SELECT @pn1, MaNL, 8.000, 650000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Bột Matcha Uji'
    UNION ALL SELECT @pn1, MaNL, 12.000, 140000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Bột Cacao Nguyên Chất'
    UNION ALL SELECT @pn1, MaNL, 25.000, 48000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Dừa Đóng Lon'
    UNION ALL SELECT @pn1, MaNL, 25.000, 10000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Muối Biển Tinh Khiết';
END

-- Phiếu nhập 2: Topping thô, Thạch 3Q, Hoa quả tươi & Hoa quả đóng hộp
IF NOT EXISTS (SELECT 1 FROM dbo.PhieuNhap WHERE MaYeuCau = 'PN_DEMO_02')
BEGIN
    INSERT INTO dbo.PhieuNhap (NguoiLap, LyDo, NguonNhap, TrangThai, LapLuc, GhiSoLuc, MaYeuCau)
    VALUES (@adminId, N'Nhập trân châu thô, thạch 3Q, hoa quả tươi và đóng hộp Đà Lạt', N'Tổng kho Nông sản & Trái cây Tươi Đà Lạt', 'DA_GHI_SO', DATEADD(DAY, -3, SYSUTCDATETIME()), DATEADD(DAY, -3, SYSUTCDATETIME()), 'PN_DEMO_02');

    DECLARE @pn2 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietNhap (MaPhieu, MaNL, SoLuong, DonGiaNhap)
    SELECT @pn2, MaNL, 50.000, 30000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Thô Đen'
    UNION ALL SELECT @pn2, MaNL, 35.000, 42000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Hạt Trân Châu Trắng 3Q'
    UNION ALL SELECT @pn2, MaNL, 35.000, 55000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Đào Ngâm Đóng Hộp'
    UNION ALL SELECT @pn2, MaNL, 30.000, 60000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Vải Ngâm Đóng Hộp'
    UNION ALL SELECT @pn2, MaNL, 25.000, 90000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Dâu Tây Tươi'
    UNION ALL SELECT @pn2, MaNL, 20.000, 40000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Xoài Cát Chín'
    UNION ALL SELECT @pn2, MaNL, 15.000, 30000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Chanh Dây Tươi'
    UNION ALL SELECT @pn2, MaNL, 25.000, 35000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Cam Vàng Sành'
    UNION ALL SELECT @pn2, MaNL, 18.000, 60000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Bơ Sáp Đắk Lắk'
    UNION ALL SELECT @pn2, MaNL, 15.000, 20000.00 FROM dbo.NguyenLieu WHERE TenNL = N'Sả Cây Tươi';
END

-- Phiếu nhập 3: Ly nhựa PP và Ống hút bao bì
IF NOT EXISTS (SELECT 1 FROM dbo.PhieuNhap WHERE MaYeuCau = 'PN_DEMO_03')
BEGIN
    INSERT INTO dbo.PhieuNhap (NguoiLap, LyDo, NguonNhap, TrangThai, LapLuc, GhiSoLuc, MaYeuCau)
    VALUES (@adminId, N'Nhập ly nhựa PP cao cấp và ống hút bọc màng', N'Xưởng In Bao Bì Tân Phú', 'DA_GHI_SO', DATEADD(DAY, -2, SYSUTCDATETIME()), DATEADD(DAY, -2, SYSUTCDATETIME()), 'PN_DEMO_03');

    DECLARE @pn3 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietNhap (MaPhieu, MaNL, SoLuong, DonGiaNhap)
    SELECT @pn3, MaNL, 3000.000, 450.00 FROM dbo.NguyenLieu WHERE TenNL = N'Ly Nhựa PP 500ml'
    UNION ALL SELECT @pn3, MaNL, 3000.000, 550.00 FROM dbo.NguyenLieu WHERE TenNL = N'Ly Nhựa PP 700ml'
    UNION ALL SELECT @pn3, MaNL, 4000.000, 120.00 FROM dbo.NguyenLieu WHERE TenNL = N'Ống Hút Trân Châu';
END

PRINT N'-- [10/16] Đã nạp 3 phiếu nhập kho với đầy đủ 27 nguyên liệu thô đầu vào.';
GO

-- ============================================================================
-- 11. MẺ SƠ CHẾ BẾP (MeSoChe, ChiTietMeSoChe) - 10 Mẻ Sơ Chế Cho 10 Thành Phẩm
-- ============================================================================
DECLARE @kitchenId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'KITCHEN');

-- 1. Mẻ Nước Cốt Trà Đen
IF NOT EXISTS (SELECT 1 FROM dbo.MeSoChe WHERE MaYeuCau = 'ME_DEMO_01')
BEGIN
    DECLARE @ct1 AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = N'Ủ Nước Cốt Trà Đen');
    DECLARE @ra1 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Đen');
    DECLARE @vao1 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Đen Cổ Thụ');

    INSERT INTO dbo.MeSoChe (MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
    VALUES (@ct1, @ra1, 3000.000, 2950.000, 'DA_GHI_SO', 18000.00, 6.10, -1.67, 0, @kitchenId, DATEADD(HOUR, -6, SYSUTCDATETIME()), 'ME_DEMO_01');

    DECLARE @me1 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
    VALUES (@me1, @vao1, 0.150, 0.150, 120000.00, 18000.00);
END

-- 2. Mẻ Nước Cốt Trà Ô Long
IF NOT EXISTS (SELECT 1 FROM dbo.MeSoChe WHERE MaYeuCau = 'ME_DEMO_02')
BEGIN
    DECLARE @ct2 AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = N'Ủ Nước Cốt Trà Ô Long');
    DECLARE @ra2 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Ô Long');
    DECLARE @vao2 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Ô Long Kim Tuyên');

    INSERT INTO dbo.MeSoChe (MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
    VALUES (@ct2, @ra2, 3000.000, 3000.000, 'DA_GHI_SO', 27000.00, 9.00, 0.00, 0, @kitchenId, DATEADD(HOUR, -6, SYSUTCDATETIME()), 'ME_DEMO_02');

    DECLARE @me2 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
    VALUES (@me2, @vao2, 0.150, 0.150, 180000.00, 27000.00);
END

-- 3. Mẻ Nước Cốt Trà Lài
IF NOT EXISTS (SELECT 1 FROM dbo.MeSoChe WHERE MaYeuCau = 'ME_DEMO_03')
BEGIN
    DECLARE @ct3 AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = N'Ủ Nước Cốt Trà Lài');
    DECLARE @ra3 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Cốt Trà Lài');
    DECLARE @vao3 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trà Lài Thượng Hạng');

    INSERT INTO dbo.MeSoChe (MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
    VALUES (@ct3, @ra3, 3500.000, 3500.000, 'DA_GHI_SO', 22500.00, 6.43, 0.00, 0, @kitchenId, DATEADD(HOUR, -5, SYSUTCDATETIME()), 'ME_DEMO_03');

    DECLARE @me3 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
    VALUES (@me3, @vao3, 0.150, 0.150, 150000.00, 22500.00);
END

-- 4. Mẻ Cốt Cà Phê Phin
IF NOT EXISTS (SELECT 1 FROM dbo.MeSoChe WHERE MaYeuCau = 'ME_DEMO_04')
BEGIN
    DECLARE @ct4 AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = N'Pha Cốt Cà Phê Phin');
    DECLARE @ra4 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Cốt Cà Phê Pha Phin');
    DECLARE @vao4 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Cà Phê Hạt Rang Mộc');

    INSERT INTO dbo.MeSoChe (MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
    VALUES (@ct4, @ra4, 2000.000, 2000.000, 'DA_GHI_SO', 80000.00, 40.00, 0.00, 0, @kitchenId, DATEADD(HOUR, -5, SYSUTCDATETIME()), 'ME_DEMO_04');

    DECLARE @me4 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
    VALUES (@me4, @vao4, 0.500, 0.500, 160000.00, 80000.00);
END

-- 5. Mẻ Nước Đường Pha Sẵn
IF NOT EXISTS (SELECT 1 FROM dbo.MeSoChe WHERE MaYeuCau = 'ME_DEMO_05')
BEGIN
    DECLARE @ct5 AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = N'Nấu Nước Đường Pha Sẵn');
    DECLARE @ra5 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Nước Đường Pha Sẵn');
    DECLARE @vao5 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đường Cát Trắng');

    INSERT INTO dbo.MeSoChe (MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
    VALUES (@ct5, @ra5, 3000.000, 3000.000, 'DA_GHI_SO', 44000.00, 14.67, 0.00, 0, @kitchenId, DATEADD(HOUR, -5, SYSUTCDATETIME()), 'ME_DEMO_05');

    DECLARE @me5 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
    VALUES (@me5, @vao5, 2.000, 2.000, 22000.00, 44000.00);
END

-- 6. Mẻ Trân Châu Nấu Chín
IF NOT EXISTS (SELECT 1 FROM dbo.MeSoChe WHERE MaYeuCau = 'ME_DEMO_06')
BEGIN
    DECLARE @ct6 AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = N'Nấu Trân Châu Đen Dẻo');
    DECLARE @ra6 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Nấu Chín');
    DECLARE @vao6 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Trân Châu Thô Đen');

    INSERT INTO dbo.MeSoChe (MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
    VALUES (@ct6, @ra6, 2500.000, 2500.000, 'DA_GHI_SO', 30000.00, 12.00, 0.00, 0, @kitchenId, DATEADD(HOUR, -4, SYSUTCDATETIME()), 'ME_DEMO_06');

    DECLARE @me6 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
    VALUES (@me6, @vao6, 1.000, 1.000, 30000.00, 30000.00);
END

-- 7. Mẻ Kem Cheese Thành Phẩm
IF NOT EXISTS (SELECT 1 FROM dbo.MeSoChe WHERE MaYeuCau = 'ME_DEMO_07')
BEGIN
    DECLARE @ct7 AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = N'Đánh Bông Kem Cheese');
    DECLARE @ra7 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Kem Cheese Thành Phẩm');
    DECLARE @vao7 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Bột Kem Cheese Anchor');

    INSERT INTO dbo.MeSoChe (MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
    VALUES (@ct7, @ra7, 2000.000, 2000.000, 'DA_GHI_SO', 110000.00, 55.00, 0.00, 0, @kitchenId, DATEADD(HOUR, -4, SYSUTCDATETIME()), 'ME_DEMO_07');

    DECLARE @me7 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
    VALUES (@me7, @vao7, 0.500, 0.500, 220000.00, 110000.00);
END

-- 8. Mẻ Thạch Đào Cắt Hạt Lựu
IF NOT EXISTS (SELECT 1 FROM dbo.MeSoChe WHERE MaYeuCau = 'ME_DEMO_08')
BEGIN
    DECLARE @ct8 AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = N'Sơ Chế Thạch Đào Hạt Lựu');
    DECLARE @ra8 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Thạch Đào Cắt Hạt Lựu');
    DECLARE @vao8 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Đào Ngâm Đóng Hộp');

    INSERT INTO dbo.MeSoChe (MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
    VALUES (@ct8, @ra8, 1000.000, 1000.000, 'DA_GHI_SO', 55000.00, 55.00, 0.00, 0, @kitchenId, DATEADD(HOUR, -4, SYSUTCDATETIME()), 'ME_DEMO_08');

    DECLARE @me8 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
    VALUES (@me8, @vao8, 1.000, 1.000, 55000.00, 55000.00);
END

-- 9. Mẻ Sốt Dâu Tây Tươi
IF NOT EXISTS (SELECT 1 FROM dbo.MeSoChe WHERE MaYeuCau = 'ME_DEMO_09')
BEGIN
    DECLARE @ct9 AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = N'Nấu Sốt Dâu Tây Tươi');
    DECLARE @ra9 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sốt Dâu Tây Tươi');
    DECLARE @vao9 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Dâu Tây Tươi');

    INSERT INTO dbo.MeSoChe (MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
    VALUES (@ct9, @ra9, 1200.000, 1200.000, 'DA_GHI_SO', 90000.00, 75.00, 0.00, 0, @kitchenId, DATEADD(HOUR, -4, SYSUTCDATETIME()), 'ME_DEMO_09');

    DECLARE @me9 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
    VALUES (@me9, @vao9, 1.000, 1.000, 90000.00, 90000.00);
END

-- 10. Mẻ Sốt Xoài Chanh Dây
IF NOT EXISTS (SELECT 1 FROM dbo.MeSoChe WHERE MaYeuCau = 'ME_DEMO_10')
BEGIN
    DECLARE @ct10 AS BIGINT = (SELECT MaCTSC FROM dbo.CongThucSoChe WHERE TenCT = N'Nấu Sốt Xoài Chanh Dây');
    DECLARE @ra10 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Sốt Xoài Chanh Dây');
    DECLARE @vao10 AS BIGINT = (SELECT MaNL FROM dbo.NguyenLieu WHERE TenNL = N'Xoài Cát Chín');

    INSERT INTO dbo.MeSoChe (MaCTSC, MaNLDauRa, LuongDuKien, LuongThucThu, TrangThai, TongChiPhi, GiaVonBinhQuan, TyLeChenhLech, CanhBaoTyLe, NguoiLap, GhiSoLuc, MaYeuCau)
    VALUES (@ct10, @ra10, 1500.000, 1500.000, 'DA_GHI_SO', 50000.00, 33.33, 0.00, 0, @kitchenId, DATEADD(HOUR, -3, SYSUTCDATETIME()), 'ME_DEMO_10');

    DECLARE @me10 AS BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.ChiTietMeSoChe (MaMe, MaNL, LuongDinhMuc, LuongThucDung, DonGiaVao, ThanhTien)
    VALUES (@me10, @vao10, 1.000, 1.000, 40000.00, 40000.00);
END

PRINT N'-- [11/16] Đã nạp 10 mẻ sơ chế của bếp phục vụ đầy đủ 10 thành phẩm pha chế.';
GO

-- ============================================================================
-- 12. PHIÊN BÀN & GIỎ HÀNG PHIÊN (PhienBan, GioPhien, ChiTietGioPhien)
-- Khởi tạo phiên đang mở (OPEN) cho Bàn 01, Bàn 02, Bàn 03
-- Và các phiên đã đóng (CLOSED) cho các bàn khác để lưu vết lịch sử
-- ============================================================================
DECLARE @cashierId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'CASHIER');
DECLARE @ban1 AS BIGINT = (SELECT TOP 1 MaBan FROM dbo.Ban WHERE MaQR = 'QR_TABLE_01_TOKEN_A1B2C3D4');
DECLARE @ban2 AS BIGINT = (SELECT TOP 1 MaBan FROM dbo.Ban WHERE MaQR = 'QR_TABLE_02_TOKEN_A2B3C4D5');
DECLARE @ban3 AS BIGINT = (SELECT TOP 1 MaBan FROM dbo.Ban WHERE MaQR = 'QR_TABLE_03_TOKEN_A3B4C5D6');
DECLARE @ban4 AS BIGINT = (SELECT TOP 1 MaBan FROM dbo.Ban WHERE MaQR = 'QR_TABLE_04_TOKEN_A4B5C6D7');

-- Tạo hoặc lấy phiên mở cho Bàn 01
DECLARE @phien1 AS BIGINT = (SELECT MaPhienDangMo FROM dbo.Ban WHERE MaBan = @ban1);
IF @phien1 IS NULL OR NOT EXISTS (SELECT 1 FROM dbo.PhienBan WHERE MaPhien = @phien1 AND TrangThai = 'OPEN')
BEGIN
    INSERT INTO dbo.PhienBan (MaBan, TrangThai, MoLuc, NguoiMo, Version)
    VALUES (@ban1, 'OPEN', DATEADD(MINUTE, -45, SYSUTCDATETIME()), @cashierId, 1);
    SET @phien1 = SCOPE_IDENTITY();
    UPDATE dbo.Ban SET TrangThai = 'CO_KHACH', MaPhienDangMo = @phien1 WHERE MaBan = @ban1;
END

-- Giỏ phiên cho Bàn 01
IF NOT EXISTS (SELECT 1 FROM dbo.GioPhien WHERE MaPhien = @phien1)
BEGIN
    INSERT INTO dbo.GioPhien (MaPhien, SuaLuc) VALUES (@phien1, SYSUTCDATETIME());
    DECLARE @gio1 AS BIGINT = SCOPE_IDENTITY();

    DECLARE @m1 AS BIGINT = (SELECT TOP 1 MaMon FROM dbo.MonUong WHERE TenMon = N'Trà Sữa Truyền Thống' AND Size = 'M');
    DECLARE @top1 AS BIGINT = (SELECT TOP 1 MaMon FROM dbo.MonUong WHERE TenMon = N'Trân Châu Đen Hoàng Kim');
    IF @m1 IS NOT NULL
        INSERT INTO dbo.ChiTietGioPhien (MaGio, MaMon, SoLuong, GhiChu, ToppingIds)
        VALUES (@gio1, @m1, 2, N'Ít đá 50%, ngọt bình thường', CAST(@top1 AS VARCHAR(20)));
END

-- Bàn 02
DECLARE @phien2 AS BIGINT = (SELECT MaPhienDangMo FROM dbo.Ban WHERE MaBan = @ban2);
IF @phien2 IS NULL OR NOT EXISTS (SELECT 1 FROM dbo.PhienBan WHERE MaPhien = @phien2 AND TrangThai = 'OPEN')
BEGIN
    INSERT INTO dbo.PhienBan (MaBan, TrangThai, MoLuc, NguoiMo, Version)
    VALUES (@ban2, 'OPEN', DATEADD(MINUTE, -30, SYSUTCDATETIME()), @cashierId, 1);
    SET @phien2 = SCOPE_IDENTITY();
    UPDATE dbo.Ban SET TrangThai = 'CO_KHACH', MaPhienDangMo = @phien2 WHERE MaBan = @ban2;
END

-- Bàn 03
DECLARE @phien3 AS BIGINT = (SELECT MaPhienDangMo FROM dbo.Ban WHERE MaBan = @ban3);
IF @phien3 IS NULL OR NOT EXISTS (SELECT 1 FROM dbo.PhienBan WHERE MaPhien = @phien3 AND TrangThai = 'OPEN')
BEGIN
    INSERT INTO dbo.PhienBan (MaBan, TrangThai, MoLuc, NguoiMo, Version)
    VALUES (@ban3, 'OPEN', DATEADD(MINUTE, -25, SYSUTCDATETIME()), @cashierId, 1);
    SET @phien3 = SCOPE_IDENTITY();
    UPDATE dbo.Ban SET TrangThai = 'CO_KHACH', MaPhienDangMo = @phien3 WHERE MaBan = @ban3;
END

-- Phiên đã đóng trong lịch sử cho Bàn 04
DECLARE @phienClosed AS BIGINT = (SELECT TOP 1 MaPhien FROM dbo.PhienBan WHERE MaBan = @ban4 AND TrangThai = 'CLOSED');
IF @phienClosed IS NULL
BEGIN
    INSERT INTO dbo.PhienBan (MaBan, TrangThai, MoLuc, DongLuc, NguoiMo, NguoiDong, Version)
    VALUES (@ban4, 'CLOSED', DATEADD(HOUR, -4, SYSUTCDATETIME()), DATEADD(HOUR, -2, SYSUTCDATETIME()), @cashierId, @cashierId, 1);
    SET @phienClosed = SCOPE_IDENTITY();
END

PRINT N'-- [12/16] Đã mở phiên bàn có khách và giỏ phiên bàn chuẩn xác.';
GO

-- ============================================================================
-- 13. ĐƠN HÀNG, CHI TIẾT, HÓA ĐƠN, THANH TOÁN (10 Đơn hàng mẫu liên kết toàn diện)
-- ============================================================================
DECLARE @cashierId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'CASHIER');
DECLARE @kitchenId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'KITCHEN');
DECLARE @custMai AS BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer@milktea.vn');
DECLARE @custDuc AS BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer2@milktea.vn');

DECLARE @ban1 AS BIGINT = (SELECT TOP 1 MaBan FROM dbo.Ban WHERE MaQR = 'QR_TABLE_01_TOKEN_A1B2C3D4');
DECLARE @phien1 AS BIGINT = (SELECT MaPhienDangMo FROM dbo.Ban WHERE MaBan = @ban1);

DECLARE @ban2 AS BIGINT = (SELECT TOP 1 MaBan FROM dbo.Ban WHERE MaQR = 'QR_TABLE_02_TOKEN_A2B3C4D5');
DECLARE @phien2 AS BIGINT = (SELECT MaPhienDangMo FROM dbo.Ban WHERE MaBan = @ban2);

DECLARE @ban3 AS BIGINT = (SELECT TOP 1 MaBan FROM dbo.Ban WHERE MaQR = 'QR_TABLE_03_TOKEN_A3B4C5D6');
DECLARE @phien3 AS BIGINT = (SELECT MaPhienDangMo FROM dbo.Ban WHERE MaBan = @ban3);

DECLARE @ban4 AS BIGINT = (SELECT TOP 1 MaBan FROM dbo.Ban WHERE MaQR = 'QR_TABLE_04_TOKEN_A4B5C6D7');
DECLARE @phienClosed AS BIGINT = (SELECT TOP 1 MaPhien FROM dbo.PhienBan WHERE MaBan = @ban4 AND TrangThai = 'CLOSED');

-- Bảng dữ liệu định nghĩa 10 đơn hàng mẫu
DECLARE @OrderDefs TABLE (
    MaYeuCau         VARCHAR(64),
    MaPhien          BIGINT,
    MaTK             BIGINT,
    Nguon            VARCHAR(10),
    TrangThai        VARCHAR(24),
    GhiChu           NVARCHAR(500),
    TenMon           NVARCHAR(120),
    Size             VARCHAR(12),
    SoLuong          INT,
    HasTopping       BIT,
    TenTopping       NVARCHAR(120),
    LoaiGiam         VARCHAR(16),
    TyLeGiam         DECIMAL(5,2),
    GiaTriGiam       DECIMAL(18,2),
    PhuongThucTT     VARCHAR(16),
    DaThanhToan      BIT,
    DaBaoChuyenKhoan BIT,
    PhutTao          INT,
    PhutBatDau       INT,
    PhutHoanThanh    INT
);

INSERT INTO @OrderDefs VALUES
    -- 1. CHO_XAC_NHAN: Đơn tại Bàn 01, khách vừa đặt, chưa thanh toán (chờ thu ngân duyệt)
    ('DH_SEED_01', @phien1, @custMai, 'TABLE', 'CHO_XAC_NHAN', N'Khách bàn 1 vừa gửi đơn, trả tiền mặt sau', N'Trà Sữa Truyền Thống', 'M', 2, 1, N'Trân Châu Đen Hoàng Kim', 'PERCENT', 10.00, 10.00, 'CASH', 0, 0, -20, NULL, NULL),

    -- 2. CHO_XAC_NHAN: Đơn tại Bàn 02, khách quét QR báo đã chuyển khoản (transferNotified)
    ('DH_SEED_02', @phien2, @custDuc, 'TABLE', 'CHO_XAC_NHAN', N'Khách bàn 2 đã chuyển khoản MB, chờ thu ngân đối soát', N'Trà Đào Cam Sả', 'L', 1, 1, N'Thạch Đào Giòn Ngọt', 'PERCENT', 0.00, 0.00, 'BANK_TRANSFER', 0, 1, -15, NULL, NULL),

    -- 3. CHO_CHE_BIEN: Đơn tại Bàn 03, Thu ngân ĐÃ XÁC NHẬN, chuyển xuống bếp chờ pha chế
    ('DH_SEED_03', @phien3, @custMai, 'TABLE', 'CHO_CHE_BIEN', N'Thu ngân đã xác nhận đơn bàn 3, khách đã thanh toán tiền mặt', N'Trà Sữa Ô Long Nướng', 'M', 2, 0, NULL, 'PERCENT', 10.00, 10.00, 'CASH', 1, 0, -25, NULL, NULL),

    -- 4. CHO_CHE_BIEN: Đơn tạo tại quầy, đã thanh toán chuyển khoản, chờ bếp nhận
    ('DH_SEED_04', NULL, @cashierId, 'COUNTER', 'CHO_CHE_BIEN', N'Đơn mang đi tạo tại quầy thu ngân, đã thu chuyển khoản', N'Sữa Tươi Trân Châu Đường Đen', 'L', 1, 0, NULL, 'PERCENT', 0.00, 0.00, 'BANK_TRANSFER', 1, 1, -10, NULL, NULL),

    -- 5. DANG_CHE_BIEN: Bàn 01, Bếp đã bấm Bắt đầu pha chế (có BatDauLuc)
    ('DH_SEED_05', @phien1, @custDuc, 'TABLE', 'DANG_CHE_BIEN', N'Bếp đang pha chế cho bàn 1', N'Trà Sữa Lài Thơm', 'M', 2, 1, N'Kem Cheese Phô Mai Macchiato', 'PERCENT', 10.00, 10.00, 'BANK_TRANSFER', 1, 1, -30, -10, NULL),

    -- 6. DANG_CHE_BIEN: Đơn tại quầy, bếp đang chế biến
    ('DH_SEED_06', NULL, @cashierId, 'COUNTER', 'DANG_CHE_BIEN', N'Đơn mang đi số 06 đang pha chế', N'Cà Phê Muối Xứ Huế', 'M', 2, 0, NULL, 'PERCENT', 0.00, 0.00, 'CASH', 1, 0, -20, -8, NULL),

    -- 7. HOAN_THANH: Bàn 04 (phiên lịch sử), bếp đã hoàn tất toàn bộ
    ('DH_SEED_07', @phienClosed, @custMai, 'TABLE', 'HOAN_THANH', N'Đơn đã hoàn thành trọn vẹn tại bàn 4', N'Trà Ô Long Váng Sữa Macchiato', 'L', 2, 1, N'Trân Châu Trắng 3Q', 'PERCENT', 10.00, 10.00, 'BANK_TRANSFER', 1, 1, -120, -100, -85),

    -- 8. HOAN_THANH: Đơn tại quầy đã giao khách
    ('DH_SEED_08', NULL, @cashierId, 'COUNTER', 'HOAN_THANH', N'Đơn mang đi đã giao nước cho khách', N'Matcha Đá Xay Kem Tuyết', 'L', 1, 1, N'Khúc Bạch Hạnh Nhân', 'PERCENT', 0.00, 0.00, 'CASH', 1, 0, -90, -75, -60),

    -- 9. DA_HUY: Đơn tại Bàn 01, khách đổi ý xin hủy trước khi chế biến (chưa thanh toán)
    ('DH_SEED_09', @phien1, @custDuc, 'TABLE', 'DA_HUY', N'Khách có việc bận đột xuất xin hủy đơn', N'Sinh Tố Bơ Dừa Thơm Béo', 'L', 1, 0, NULL, 'PERCENT', 0.00, 0.00, 'CASH', 0, 0, -40, NULL, NULL),

    -- 10. DA_HUY: Đơn quầy đã thanh toán chuyển khoản, khách xin hủy trước pha chế -> Chờ hoàn tiền
    ('DH_SEED_10', NULL, @cashierId, 'COUNTER', 'DA_HUY', N'Khách mua mang đi xin hủy đơn trước pha chế, chờ hoàn tiền', N'Cà Phê Sữa Đá', 'L', 2, 0, NULL, 'PERCENT', 0.00, 0.00, 'BANK_TRANSFER', 1, 1, -35, NULL, NULL);

-- Duyệt từng đơn hàng mẫu và khởi tạo liên kết đầy đủ
DECLARE curDH CURSOR FOR
    SELECT MaYeuCau, MaPhien, MaTK, Nguon, TrangThai, GhiChu, TenMon, Size, SoLuong, HasTopping, TenTopping, LoaiGiam, TyLeGiam, GiaTriGiam, PhuongThucTT, DaThanhToan, DaBaoChuyenKhoan, PhutTao, PhutBatDau, PhutHoanThanh
    FROM @OrderDefs;

DECLARE @dMaYeuCau VARCHAR(64), @dMaPhien BIGINT, @dMaTK BIGINT, @dNguon VARCHAR(10), @dTrangThai VARCHAR(24), @dGhiChu NVARCHAR(500);
DECLARE @dTenMon NVARCHAR(120), @dSize VARCHAR(12), @dSoLuong INT, @dHasTopping BIT, @dTenTopping NVARCHAR(120);
DECLARE @dLoaiGiam VARCHAR(16), @dTyLeGiam DECIMAL(5,2), @dGiaTriGiam DECIMAL(18,2), @dPhuongThucTT VARCHAR(16);
DECLARE @dDaThanhToan BIT, @dDaBaoChuyenKhoan BIT, @dPhutTao INT, @dPhutBatDau INT, @dPhutHoanThanh INT;

OPEN curDH;
FETCH NEXT FROM curDH INTO @dMaYeuCau, @dMaPhien, @dMaTK, @dNguon, @dTrangThai, @dGhiChu, @dTenMon, @dSize, @dSoLuong, @dHasTopping, @dTenTopping, @dLoaiGiam, @dTyLeGiam, @dGiaTriGiam, @dPhuongThucTT, @dDaThanhToan, @dDaBaoChuyenKhoan, @dPhutTao, @dPhutBatDau, @dPhutHoanThanh;

WHILE @@FETCH_STATUS = 0
BEGIN
    DECLARE @curDHId AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = @dMaYeuCau);
    DECLARE @timeTao AS DATETIME2 = DATEADD(MINUTE, @dPhutTao, SYSUTCDATETIME());
    DECLARE @timeBatDau AS DATETIME2 = CASE WHEN @dPhutBatDau IS NOT NULL THEN DATEADD(MINUTE, @dPhutBatDau, SYSUTCDATETIME()) ELSE NULL END;
    DECLARE @timeHoanThanh AS DATETIME2 = CASE WHEN @dPhutHoanThanh IS NOT NULL THEN DATEADD(MINUTE, @dPhutHoanThanh, SYSUTCDATETIME()) ELSE NULL END;

    IF @curDHId IS NULL
    BEGIN
        INSERT INTO dbo.DonHang (MaPhien, MaTK, Nguon, TrangThai, MaYeuCau, GhiChu, TaoLuc, BatDauLuc, HoanThanhLuc, Version)
        VALUES (@dMaPhien, @dMaTK, @dNguon, @dTrangThai, @dMaYeuCau, @dGhiChu, @timeTao, @timeBatDau, @timeHoanThanh, 0);
        SET @curDHId = SCOPE_IDENTITY();
    END
    ELSE
    BEGIN
        UPDATE dbo.DonHang
        SET MaPhien = @dMaPhien, MaTK = @dMaTK, Nguon = @dNguon, TrangThai = @dTrangThai, GhiChu = @dGhiChu,
            TaoLuc = @timeTao, BatDauLuc = @timeBatDau, HoanThanhLuc = @timeHoanThanh
        WHERE MaDH = @curDHId;
    END

    -- 13.1 Chi tiết món & Topping
    DECLARE @maMonChon AS BIGINT = (SELECT MaMon FROM dbo.MonUong WHERE TenMon = @dTenMon AND Size = @dSize);
    DECLARE @giaMon AS DECIMAL(18,2) = ISNULL((SELECT Gia FROM dbo.MonUong WHERE MaMon = @maMonChon), 30000.00);

    DELETE FROM dbo.ChiTietDonHang WHERE MaDH = @curDHId;
    INSERT INTO dbo.ChiTietDonHang (MaDH, MaMon, TenMonChot, SizeChot, SoLuong, DonGiaChot, GhiChu)
    VALUES (@curDHId, @maMonChon, @dTenMon, @dSize, @dSoLuong, @giaMon, @dGhiChu);

    DECLARE @ctdhId AS BIGINT = SCOPE_IDENTITY();
    DECLARE @tienTopping AS DECIMAL(18,2) = 0.00;

    IF @dHasTopping = 1 AND @dTenTopping IS NOT NULL
    BEGIN
        DECLARE @maTop AS BIGINT = (SELECT MaMon FROM dbo.MonUong WHERE TenMon = @dTenTopping AND Size = 'M');
        DECLARE @giaTop AS DECIMAL(18,2) = ISNULL((SELECT Gia FROM dbo.MonUong WHERE MaMon = @maTop), 6000.00);
        INSERT INTO dbo.ChiTietDonHangTopping (MaChiTiet, MaMonTopping, TenToppingChot, DonGiaToppingChot)
        VALUES (@ctdhId, @maTop, @dTenTopping, @giaTop);
        SET @tienTopping = @giaTop * @dSoLuong;
    END

    -- 13.2 Tính toán tiền hóa đơn
    DECLARE @tongTienMon AS DECIMAL(18,2) = (@giaMon * @dSoLuong) + @tienTopping;
    DECLARE @tienGiam AS DECIMAL(18,2) = 0.00;
    IF @dLoaiGiam = 'PERCENT' AND @dTyLeGiam > 0
        SET @tienGiam = ROUND(@tongTienMon * (@dTyLeGiam / 100.0), 0);
    ELSE IF @dLoaiGiam = 'FIXED_AMOUNT' AND @dGiaTriGiam > 0
        SET @tienGiam = CASE WHEN @dGiaTriGiam > @tongTienMon THEN @tongTienMon ELSE @dGiaTriGiam END;

    DECLARE @phaiTra AS DECIMAL(18,2) = @tongTienMon - @tienGiam;
    DECLARE @trangThaiHD AS VARCHAR(12) = CASE WHEN @dTrangThai = 'DA_HUY' THEN 'DA_HUY' ELSE 'HIEU_LUC' END;

    -- Nạp hóa đơn
    MERGE INTO dbo.HoaDon AS target
    USING (SELECT @curDHId AS MaDH) AS source ON (target.MaDH = source.MaDH)
    WHEN MATCHED THEN
        UPDATE SET TongTien = @tongTienMon, TienGiam = @tienGiam, PhaiTra = @phaiTra,
                   TyLeGiamChot = @dTyLeGiam, LoaiGiamChot = @dLoaiGiam, GiaTriGiamChot = @dGiaTriGiam,
                   TrangThai = @trangThaiHD, LapLuc = @timeTao, HuyLuc = CASE WHEN @dTrangThai = 'DA_HUY' THEN SYSUTCDATETIME() ELSE NULL END
    WHEN NOT MATCHED THEN
        INSERT (MaDH, TongTien, TienGiam, PhaiTra, TyLeGiamChot, LoaiGiamChot, GiaTriGiamChot, TrangThai, LapLuc, HuyLuc)
        VALUES (@curDHId, @tongTienMon, @tienGiam, @phaiTra, @dTyLeGiam, @dLoaiGiam, @dGiaTriGiam, @trangThaiHD, @timeTao, CASE WHEN @dTrangThai = 'DA_HUY' THEN SYSUTCDATETIME() ELSE NULL END);

    DECLARE @hdId AS BIGINT = (SELECT MaHD FROM dbo.HoaDon WHERE MaDH = @curDHId);

    -- 13.3 Thanh toán nếu có
    IF @dDaThanhToan = 1
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM dbo.ThanhToan WHERE MaHD = @hdId)
        BEGIN
            INSERT INTO dbo.ThanhToan (MaHD, PhuongThuc, SoTien, ThamChieu, NguoiThu, ThuLuc, MaYeuCau)
            VALUES (@hdId, @dPhuongThucTT, @phaiTra, N'TT-' + @dMaYeuCau, @cashierId, DATEADD(MINUTE, 2, @timeTao), 'TT_' + @dMaYeuCau);
        END
    END
    ELSE IF @dDaBaoChuyenKhoan = 1
    BEGIN
        -- Khách báo chuyển khoản nhưng chưa ghi thu -> lưu ThongBaoThanhToan
        IF NOT EXISTS (SELECT 1 FROM dbo.ThongBaoThanhToan WHERE MaHD = @hdId)
        BEGIN
            INSERT INTO dbo.ThongBaoThanhToan (MaHD, PhuongThuc, ThamChieu, TaoLuc, MaYeuCau)
            VALUES (@hdId, 'BANK_TRANSFER', N'Thông báo CK ' + @dMaYeuCau, DATEADD(MINUTE, 2, @timeTao), 'TB_' + @dMaYeuCau);
        END
    END

    -- 13.4 Snapshot định mức tiêu hao (DinhMucDonHang)
    DELETE FROM dbo.DinhMucDonHang WHERE MaDH = @curDHId;
    INSERT INTO dbo.DinhMucDonHang (MaDH, MaNL, TongLuong, LoaiChot, DonViChot)
    SELECT @curDHId, ctm.MaNL, SUM(ctm.DinhLuong * @dSoLuong), nl.Loai, nl.DonVi
    FROM dbo.CongThucMon ctm
    JOIN dbo.NguyenLieu nl ON nl.MaNL = ctm.MaNL
    WHERE ctm.MaMon = @maMonChon
    GROUP BY ctm.MaNL, nl.Loai, nl.DonVi;

    FETCH NEXT FROM curDH INTO @dMaYeuCau, @dMaPhien, @dMaTK, @dNguon, @dTrangThai, @dGhiChu, @dTenMon, @dSize, @dSoLuong, @dHasTopping, @dTenTopping, @dLoaiGiam, @dTyLeGiam, @dGiaTriGiam, @dPhuongThucTT, @dDaThanhToan, @dDaBaoChuyenKhoan, @dPhutTao, @dPhutBatDau, @dPhutHoanThanh;
END

CLOSE curDH;
DEALLOCATE curDH;

PRINT N'-- [13/16] Đã khởi tạo 10 đơn hàng mẫu liên kết toàn diện.';
GO

-- ============================================================================
-- 14. NHẬT KÝ NGHIỆP VỤ (NhatKyNghiepVu) - Khớp 100% các mốc sự kiện
-- ============================================================================
DECLARE @adminId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'ADMIN');
DECLARE @cashierId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'CASHIER');
DECLARE @kitchenId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'KITCHEN');
DECLARE @custMai AS BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer@milktea.vn');
DECLARE @custDuc AS BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer2@milktea.vn');

-- Ghi nhật ký các đơn
DECLARE @dh1 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_01');
DECLARE @dh2 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_02');
DECLARE @dh3 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_03');
DECLARE @dh4 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_04');
DECLARE @dh5 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_05');
DECLARE @dh6 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_06');
DECLARE @dh7 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_07');
DECLARE @dh8 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_08');
DECLARE @dh9 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_09');
DECLARE @dh10 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_10');

DELETE FROM dbo.NhatKyNghiepVu WHERE MaDH IN (@dh1, @dh2, @dh3, @dh4, @dh5, @dh6, @dh7, @dh8, @dh9, @dh10);

-- DH1: Tạo đơn
INSERT INTO dbo.NhatKyNghiepVu (MaDH, MaTK, HanhDong, Truoc, Sau, TaoLuc)
VALUES (@dh1, @custMai, 'ORDER_CREATED', NULL, 'CHO_XAC_NHAN', DATEADD(MINUTE, -20, SYSUTCDATETIME()));

-- DH2: Tạo đơn & khách báo CK
INSERT INTO dbo.NhatKyNghiepVu (MaDH, MaTK, HanhDong, Truoc, Sau, TaoLuc)
VALUES 
    (@dh2, @custDuc, 'ORDER_CREATED', NULL, 'CHO_XAC_NHAN', DATEADD(MINUTE, -15, SYSUTCDATETIME())),
    (@dh2, @custDuc, 'PAYMENT_NOTICE_CREATED', 'CHO_XAC_NHAN', 'CHO_XAC_NHAN', DATEADD(MINUTE, -13, SYSUTCDATETIME()));

-- DH3: Tạo đơn -> Xác nhận -> Thu tiền
INSERT INTO dbo.NhatKyNghiepVu (MaDH, MaTK, HanhDong, Truoc, Sau, TaoLuc)
VALUES 
    (@dh3, @custMai, 'ORDER_CREATED', NULL, 'CHO_XAC_NHAN', DATEADD(MINUTE, -25, SYSUTCDATETIME())),
    (@dh3, @cashierId, 'ORDER_STATUS_CHANGED', 'CHO_XAC_NHAN', 'CHO_CHE_BIEN', DATEADD(MINUTE, -23, SYSUTCDATETIME())),
    (@dh3, @cashierId, 'PAYMENT_RECORDED', 'CHO_CHE_BIEN', 'CHO_CHE_BIEN', DATEADD(MINUTE, -22, SYSUTCDATETIME()));

-- DH4: Đơn quầy
INSERT INTO dbo.NhatKyNghiepVu (MaDH, MaTK, HanhDong, Truoc, Sau, TaoLuc)
VALUES 
    (@dh4, @cashierId, 'ORDER_CREATED', NULL, 'CHO_CHE_BIEN', DATEADD(MINUTE, -10, SYSUTCDATETIME())),
    (@dh4, @cashierId, 'PAYMENT_RECORDED', 'CHO_CHE_BIEN', 'CHO_CHE_BIEN', DATEADD(MINUTE, -9, SYSUTCDATETIME()));

-- DH5: Đang chế biến
INSERT INTO dbo.NhatKyNghiepVu (MaDH, MaTK, HanhDong, Truoc, Sau, TaoLuc)
VALUES 
    (@dh5, @custDuc, 'ORDER_CREATED', NULL, 'CHO_XAC_NHAN', DATEADD(MINUTE, -30, SYSUTCDATETIME())),
    (@dh5, @cashierId, 'ORDER_STATUS_CHANGED', 'CHO_XAC_NHAN', 'CHO_CHE_BIEN', DATEADD(MINUTE, -28, SYSUTCDATETIME())),
    (@dh5, @cashierId, 'PAYMENT_RECORDED', 'CHO_CHE_BIEN', 'CHO_CHE_BIEN', DATEADD(MINUTE, -27, SYSUTCDATETIME())),
    (@dh5, @kitchenId, 'KITCHEN_START', 'CHO_CHE_BIEN', 'DANG_CHE_BIEN', DATEADD(MINUTE, -10, SYSUTCDATETIME()));

-- DH6: Đang chế biến quầy
INSERT INTO dbo.NhatKyNghiepVu (MaDH, MaTK, HanhDong, Truoc, Sau, TaoLuc)
VALUES 
    (@dh6, @cashierId, 'ORDER_CREATED', NULL, 'CHO_CHE_BIEN', DATEADD(MINUTE, -20, SYSUTCDATETIME())),
    (@dh6, @cashierId, 'PAYMENT_RECORDED', 'CHO_CHE_BIEN', 'CHO_CHE_BIEN', DATEADD(MINUTE, -19, SYSUTCDATETIME())),
    (@dh6, @kitchenId, 'KITCHEN_START', 'CHO_CHE_BIEN', 'DANG_CHE_BIEN', DATEADD(MINUTE, -8, SYSUTCDATETIME()));

-- DH7: Hoàn thành bàn 4
INSERT INTO dbo.NhatKyNghiepVu (MaDH, MaTK, HanhDong, Truoc, Sau, TaoLuc)
VALUES 
    (@dh7, @custMai, 'ORDER_CREATED', NULL, 'CHO_XAC_NHAN', DATEADD(MINUTE, -120, SYSUTCDATETIME())),
    (@dh7, @cashierId, 'ORDER_STATUS_CHANGED', 'CHO_XAC_NHAN', 'CHO_CHE_BIEN', DATEADD(MINUTE, -118, SYSUTCDATETIME())),
    (@dh7, @cashierId, 'PAYMENT_RECORDED', 'CHO_CHE_BIEN', 'CHO_CHE_BIEN', DATEADD(MINUTE, -115, SYSUTCDATETIME())),
    (@dh7, @kitchenId, 'KITCHEN_START', 'CHO_CHE_BIEN', 'DANG_CHE_BIEN', DATEADD(MINUTE, -100, SYSUTCDATETIME())),
    (@dh7, @kitchenId, 'KITCHEN_COMPLETE', 'DANG_CHE_BIEN', 'HOAN_THANH', DATEADD(MINUTE, -85, SYSUTCDATETIME()));

-- DH8: Hoàn thành quầy
INSERT INTO dbo.NhatKyNghiepVu (MaDH, MaTK, HanhDong, Truoc, Sau, TaoLuc)
VALUES 
    (@dh8, @cashierId, 'ORDER_CREATED', NULL, 'CHO_CHE_BIEN', DATEADD(MINUTE, -90, SYSUTCDATETIME())),
    (@dh8, @cashierId, 'PAYMENT_RECORDED', 'CHO_CHE_BIEN', 'CHO_CHE_BIEN', DATEADD(MINUTE, -89, SYSUTCDATETIME())),
    (@dh8, @kitchenId, 'KITCHEN_START', 'CHO_CHE_BIEN', 'DANG_CHE_BIEN', DATEADD(MINUTE, -75, SYSUTCDATETIME())),
    (@dh8, @kitchenId, 'KITCHEN_COMPLETE', 'DANG_CHE_BIEN', 'HOAN_THANH', DATEADD(MINUTE, -60, SYSUTCDATETIME()));

-- DH9: Hủy đơn bàn
INSERT INTO dbo.NhatKyNghiepVu (MaDH, MaTK, HanhDong, Truoc, Sau, TaoLuc)
VALUES 
    (@dh9, @custDuc, 'ORDER_CREATED', NULL, 'CHO_XAC_NHAN', DATEADD(MINUTE, -40, SYSUTCDATETIME())),
    (@dh9, @cashierId, 'CANCELLATION_APPROVED', 'CHO_XAC_NHAN', 'DA_HUY', DATEADD(MINUTE, -35, SYSUTCDATETIME()));

-- DH10: Hủy đơn quầy đã thu -> Refund
INSERT INTO dbo.NhatKyNghiepVu (MaDH, MaTK, HanhDong, Truoc, Sau, TaoLuc)
VALUES 
    (@dh10, @cashierId, 'ORDER_CREATED', NULL, 'CHO_CHE_BIEN', DATEADD(MINUTE, -35, SYSUTCDATETIME())),
    (@dh10, @cashierId, 'PAYMENT_RECORDED', 'CHO_CHE_BIEN', 'CHO_CHE_BIEN', DATEADD(MINUTE, -34, SYSUTCDATETIME())),
    (@dh10, @cashierId, 'REFUND_REQUESTED', 'CHO_CHE_BIEN', 'DA_HUY', DATEADD(MINUTE, -20, SYSUTCDATETIME()));

PRINT N'-- [14/16] Đã nạp nhật ký nghiệp vụ hoàn chỉnh cho các mốc thời gian.';
GO

-- ============================================================================
-- 15. LỊCH SỬ KHO (LichSuKho) - Tiêu Hao Thực Tế Của Đơn Hoàn Thành & Lịch Sử Nhập/Sơ Chế
-- ============================================================================
DECLARE @adminId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'ADMIN');
DECLARE @kitchenId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'KITCHEN');
DECLARE @dh7 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_07');
DECLARE @dh8 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_08');

-- Lịch sử tiêu hao đơn hàng hoàn tất tại Bếp (chỉ nạp nếu chưa có)
IF @dh7 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.LichSuKho WHERE NguonLoai = 'ORDER' AND NguonMa = @dh7)
BEGIN
    INSERT INTO dbo.LichSuKho (MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo)
    SELECT nl.MaNL, 'BEP', 'TIEU_HAO', -320.000, 6320.000, 6000.000, 'ORDER', @dh7, @kitchenId, DATEADD(MINUTE, -85, SYSUTCDATETIME()), N'Pha chế hoàn tất đơn DH_SEED_07 (2 ly Trà Ô Long Váng Sữa)'
    FROM dbo.NguyenLieu nl WHERE nl.TenNL = N'Nước Cốt Trà Ô Long'
    UNION ALL
    SELECT nl.MaNL, 'BEP', 'TIEU_HAO', -120.000, 2620.000, 2500.000, 'ORDER', @dh7, @kitchenId, DATEADD(MINUTE, -85, SYSUTCDATETIME()), N'Pha chế hoàn tất đơn DH_SEED_07 (Kem cheese)'
    FROM dbo.NguyenLieu nl WHERE nl.TenNL = N'Kem Cheese Thành Phẩm';
END

IF @dh8 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.LichSuKho WHERE NguonLoai = 'ORDER' AND NguonMa = @dh8)
BEGIN
    INSERT INTO dbo.LichSuKho (MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo)
    SELECT nl.MaNL, 'BEP', 'TIEU_HAO', -0.015, 2.015, 2.000, 'ORDER', @dh8, @kitchenId, DATEADD(MINUTE, -60, SYSUTCDATETIME()), N'Pha chế hoàn tất đơn DH_SEED_08 (Matcha Đá Xay)'
    FROM dbo.NguyenLieu nl WHERE nl.TenNL = N'Bột Matcha Uji';
END

-- Lịch sử biến động nhập kho của các phiếu nhập mẫu
DECLARE @pnDemo1 AS BIGINT = (SELECT MaPhieu FROM dbo.PhieuNhap WHERE MaYeuCau = 'PN_DEMO_01');
DECLARE @pnDemo2 AS BIGINT = (SELECT MaPhieu FROM dbo.PhieuNhap WHERE MaYeuCau = 'PN_DEMO_02');
DECLARE @pnDemo3 AS BIGINT = (SELECT MaPhieu FROM dbo.PhieuNhap WHERE MaYeuCau = 'PN_DEMO_03');

IF @pnDemo1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.LichSuKho WHERE NguonLoai = 'IMPORT' AND NguonMa = @pnDemo1)
BEGIN
    INSERT INTO dbo.LichSuKho (MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo)
    SELECT ctn.MaNL, 'KHO', 'NHAP', ctn.SoLuong, 0.000, ctn.SoLuong, 'IMPORT', @pnDemo1, @adminId, DATEADD(DAY, -4, SYSUTCDATETIME()), N'Ghi sổ phiếu nhập ' + pn.MaYeuCau
    FROM dbo.ChiTietNhap ctn
    JOIN dbo.PhieuNhap pn ON pn.MaPhieu = ctn.MaPhieu
    WHERE pn.MaPhieu = @pnDemo1;
END

IF @pnDemo2 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.LichSuKho WHERE NguonLoai = 'IMPORT' AND NguonMa = @pnDemo2)
BEGIN
    INSERT INTO dbo.LichSuKho (MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo)
    SELECT ctn.MaNL, 'KHO', 'NHAP', ctn.SoLuong, 0.000, ctn.SoLuong, 'IMPORT', @pnDemo2, @adminId, DATEADD(DAY, -3, SYSUTCDATETIME()), N'Ghi sổ phiếu nhập ' + pn.MaYeuCau
    FROM dbo.ChiTietNhap ctn
    JOIN dbo.PhieuNhap pn ON pn.MaPhieu = ctn.MaPhieu
    WHERE pn.MaPhieu = @pnDemo2;
END

IF @pnDemo3 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.LichSuKho WHERE NguonLoai = 'IMPORT' AND NguonMa = @pnDemo3)
BEGIN
    INSERT INTO dbo.LichSuKho (MaNL, ViTri, Loai, LuongBienDong, TonTruoc, TonSau, NguonLoai, NguonMa, NguoiLap, TaoLuc, LyDo)
    SELECT ctn.MaNL, 'KHO', 'NHAP', ctn.SoLuong, 0.000, ctn.SoLuong, 'IMPORT', @pnDemo3, @adminId, DATEADD(DAY, -2, SYSUTCDATETIME()), N'Ghi sổ phiếu nhập ' + pn.MaYeuCau
    FROM dbo.ChiTietNhap ctn
    JOIN dbo.PhieuNhap pn ON pn.MaPhieu = ctn.MaPhieu
    WHERE pn.MaPhieu = @pnDemo3;
END

PRINT N'-- [15/16] Đã nạp lịch sử biến động tiêu hao kho Bếp và lịch sử nhập kho KHO.';
GO

-- ============================================================================
-- 16. YÊU CẦU HỦY & YÊU CẦU HOÀN TIỀN (YeuCauHuy, YeuCauHoanTien)
-- ============================================================================
DECLARE @cashierId AS BIGINT = (SELECT TOP 1 MaTK FROM dbo.TaiKhoan WHERE VaiTro = 'CASHIER');
DECLARE @custDuc AS BIGINT = (SELECT MaTK FROM dbo.TaiKhoan WHERE Email = 'customer2@milktea.vn');
DECLARE @dh9 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_09');
DECLARE @dh10 AS BIGINT = (SELECT MaDH FROM dbo.DonHang WHERE MaYeuCau = 'DH_SEED_10');

-- DH9: Yêu cầu hủy được duyệt
IF @dh9 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.YeuCauHuy WHERE MaDH = @dh9)
BEGIN
    INSERT INTO dbo.YeuCauHuy (MaDH, LyDo, LyDoXuLy, TrangThai, TaoLuc, XuLyLuc, NguoiXuLy)
    VALUES (@dh9, N'Khách có việc bận đột xuất xin hủy đơn', N'Thu ngân chấp thuận hủy đơn trước pha chế', 'CHAP_THUAN', DATEADD(MINUTE, -38, SYSUTCDATETIME()), DATEADD(MINUTE, -35, SYSUTCDATETIME()), @cashierId);
END

-- DH10: Yêu cầu hủy và yêu cầu hoàn tiền (do đã thanh toán chuyển khoản)
IF @dh10 IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.YeuCauHuy WHERE MaDH = @dh10)
    BEGIN
        INSERT INTO dbo.YeuCauHuy (MaDH, LyDo, LyDoXuLy, TrangThai, TaoLuc, XuLyLuc, NguoiXuLy)
        VALUES (@dh10, N'Khách mua mang đi xin hủy đơn trước pha chế, chờ hoàn tiền', N'Thu ngân chấp thuận hủy đơn và lập yêu cầu hoàn tiền', 'CHAP_THUAN', DATEADD(MINUTE, -22, SYSUTCDATETIME()), DATEADD(MINUTE, -20, SYSUTCDATETIME()), @cashierId);
    END

    DECLARE @tt10 AS BIGINT = (SELECT TOP 1 MaTT FROM dbo.ThanhToan WHERE MaHD = (SELECT MaHD FROM dbo.HoaDon WHERE MaDH = @dh10));
    IF @tt10 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.YeuCauHoan WHERE MaDH = @dh10)
    BEGIN
        INSERT INTO dbo.YeuCauHoan (MaDH, MaTT, SoTien, LyDo, TrangThai, MaYeuCau)
        VALUES (@dh10, @tt10, 60000.00, N'Hoàn tiền chuyển khoản cho đơn quầy DH_SEED_10 bị hủy', 'CHO_DUYET', 'REFUND_DH10');
    END
END

PRINT N'-- [16/16] Đã nạp yêu cầu hủy và hoàn tiền liên kết chặt chẽ.';
PRINT N'>>> HOÀN TẤT NẠP TOÀN BỘ DỮ LIỆU MẪU DEMO MILKTEA (100% NHẤT QUÁN & CÓ ĐỦ CÔNG THỨC)!';
GO

```
