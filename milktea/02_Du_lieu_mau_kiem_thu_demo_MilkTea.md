# DỮ LIỆU MẪU KIỂM THỬ VÀ DEMO TOÀN HỆ THỐNG MILKTEA

Phiên bản **D26-1**, ngày thiết kế **28/10/2026**. Đi kèm `01_Danh_gia_toan_dien_MilkTea.md`.

## 1. Mục đích và cách sử dụng

Đây là **bộ fixture nghiệp vụ đầy đủ với số liệu và kết quả mong đợi**, để nhập qua UI/API hoặc hiện thực thành seed theo schema đã migration. Đây không phải SQL đã chạy trên database của nhóm. Không có thao tác thay đổi source hoặc dữ liệu thật trong lần đánh giá này.

Bộ cơ sở gồm kho tổng, hai chi nhánh bán lẻ, 15 tài khoản, 18 nguyên liệu, 4 định mức sơ chế, 24 món phân biệt + 4 biến thể L + 2 topping, 44 đơn, sổ kho đầy đủ, chứng từ nhập/chuyển/sơ chế/kết ca, review và yêu thích. Các chức năng chưa có như sổ địa chỉ/Shipper/cổng thanh toán được ghi thành ca nghiệm thu mở rộng riêng, không giả làm chức năng đã hoạt động.

**Phải sửa blocker quyền/MaCN/sơ chế trước khi chạy toàn bộ bằng ứng dụng.** Dữ liệu chuẩn không tự chữa lỗi source. Có thể dùng fixture này để chứng minh lỗi hiện tại rồi chạy lại sau sửa.

### Trình tự an toàn

1. Sao lưu database đang dùng; tạo database demo riêng, cấu hình DB_URL trỏ đúng database demo. Không chạy `1_schema.sql` nguyên bản vì đầu file xóa database `milktea`.
2. Chuẩn hóa baseline DDL tạo mới và áp dụng migration V1–V4 đã review. Không dùng tồn tự sinh 50/1000 của V1/V4 cho bộ này. Trên database demo trống, tạo danh mục/nguyên liệu **sau** các migration có seed tồn; không nạp `full_demo_data.sql` cũ vào fixture mới.
3. Nếu demo DB đã có tồn/đơn, không chồng fixture. Tạo bản demo mới hoặc reset **chỉ bản demo được chỉ định**, giữ dữ liệu thật nguyên vẹn.
4. Ánh xạ mã tượng trưng dưới đây sang ID thật bằng khóa tự nhiên. Ghi lại ID trả về từ API/OUTPUT INSERTED; không giả định CN01 có ID1 hoặc orderId trùng số thứ tự Hxx.
5. Tạo account/branch/assignment → nguyên liệu và công thức → tồn0 → nhập tổng → cấp cơ sở → sơ chế → catalog branch → đơn → thu/hủy/hoàn → review/favorite → kết ca.
6. Lưu ID sinh ra và chứng từ; chạy truy vấn đối soát ở cuối. Nạp lại phải không nhân đôi giao dịch: seed có manifest/version và unique key, hoặc từ chối nếu fixture đã có. Không cập nhật đè lịch sử để giả idempotent.

Các email `.test` chỉ dùng đăng nhập demo đã xác thực sẵn. Test OTP thật cần một hộp thư thử nghiệm người kiểm thử sở hữu và SMTP đã cấu hình; không kỳ vọng email `.test` nhận được thư. Dữ liệu tên/địa chỉ/người nhận trong tài liệu là giả lập.

## 2. Nguyên tắc dữ liệu không được phá

- Tồn đầu mọi `(chi nhánh, nguyên liệu, vị trí)` bằng0. Chỉ tăng qua nhập/nhận chuyển/mẻ sơ chế; mọi biến động có nguồn và người lập.
- Nếu món A dùng B,C: B,C phải tồn tại; có row tồn đúng cơ sở và location; số lượng đáp ứng khi hoàn tất; công thức/snapshot đủ tất cả thành phần. Không bỏ qua nguyên liệu bị thiếu.
- Nguyên liệu THO có phiếu nhập NCC và chi tiết nhập; nguyên liệu SOCHE có mẻ + chi tiết nguyên liệu đầu vào, truy ngược được đến phiếu nhập THO. Không tạo “phiếu nhập NCC thành phẩm” giả cho SOCHE được sản xuất tại bếp.
- Raw ở KHO; SOCHE ở BEP trong fixture này. Tạo row vị trí còn lại bằng0 để thử đúng invariant location, không lấy tồn nhầm cơ sở. KHOTONG chỉ có raw/KHO và không được nhận đơn bán lẻ.
- Không cộng/trừ trực tiếp số dư cuối. `tồn cuối = 0 + tổng delta`; mỗi dòng `sau = trước + delta`; lần trước của dòng tiếp theo phải khớp.
- Snapshot định mức/giá được chốt lúc đặt. Pending/cancel không tiêu hao; HOAN_THANH mới tiêu hao. PaymentNotice không tạo Payment.
- Giá dùng chung toàn chuỗi; bán/không bán/tạm hết theo branch. Bán chạy = số lượng dòng món của đơn HOAN_THANH và đã tất toán; không lấy mọi đơn chưa hủy.
- Refund là tiền chi trả, không hoàn nguyên liệu hay xóa khoản thu. Review gắn đúng chi tiết đơn có chủ tài khoản, HOAN_THANH và đã thanh toán.
- Không sinh JWT/OTP/token phiên đoán được. Phần token dùng chức năng ứng dụng để cấp/hash/thu hồi; không chép raw token fixture vào DB.

## 3. Mốc thời gian

D là ngày demo; T0 là00:00 UTC ngày D, tương ứng07:00 Việt Nam. Có thể dùng D=28/10/2026 để tái hiện số liệu này. Nhập NCC: D−3; cấp cơ sở: D−2; sơ chế: D−1 lúc00:00 UTC; H01–H36 phát sinh D−1 sau sơ chế; review/favorite sau thanh toán; L01–L08 tại D. Kết ca sau các đơn H hoàn thành. Cờ tạm hết P23/ngừng kinh doanh P24 tại CN01 được đổi **sau** các đơn H, có audit.

Nếu chạy tuần khác, dịch tất cả mốc thời gian cùng một khoảng và đặt thời hạn voucher tương đối D. Không sửa ngày máy chủ sản xuất chỉ để làm demo.

## 4. Chi nhánh, nhân sự và bàn

| Mã | Loại | Tên giả lập | Tình trạng |
| --- | --- | --- | --- |
| KHOTONG | Kho trung tâm | Kho tổng MilkTea Demo | HOAT_DONG, không bán lẻ |
| CN01 | Cơ sở bán lẻ | MilkTea Demo Võ Văn Ngân | HOAT_DONG |
| CN02 | Cơ sở bán lẻ | MilkTea Demo Nguyễn Gia Trí | HOAT_DONG |

Các mã CN01/CN02/KHOTONG là mã nghiệp vụ, không phải giá trị IDENTITY. Giữ KHOTONG vì code hiện nhận diện kho tổng bằng mã này. Bộ fixture yêu cầu không lẫn dữ liệu seed cũ. Mật khẩu demo chung đề xuất `DemoMilkTea@2026`; tạo qua PasswordEncoder/API, không lưu dạng rõ trong cột MatKhauBam. Mỗi staff có một assignment HOAT_DONG; ADMIN toàn chuỗi, CUSTOMER không có assignment nhân sự.

| Mã | Email demo | Vai trò | Scope | EmailVerified/Active |
| --- | --- | --- | --- | --- |
| A01 | admin.demo@milktea.test | ADMIN | ALL | True/True |
| M_CN01 | m.cn01.demo@milktea.test | MANAGER | CN01 | True/True |
| TN_CN01 | tn.cn01.demo@milktea.test | CASHIER | CN01 | True/True |
| BEP_CN01 | bep.cn01.demo@milktea.test | KITCHEN | CN01 | True/True |
| M_CN02 | m.cn02.demo@milktea.test | MANAGER | CN02 | True/True |
| TN_CN02 | tn.cn02.demo@milktea.test | CASHIER | CN02 | True/True |
| BEP_CN02 | bep.cn02.demo@milktea.test | KITCHEN | CN02 | True/True |
| C01 | kh01.demo@milktea.test | CUSTOMER | PERSONAL | True/True |
| C02 | kh02.demo@milktea.test | CUSTOMER | PERSONAL | True/True |
| C03 | kh03.demo@milktea.test | CUSTOMER | PERSONAL | True/True |
| C04 | kh04.demo@milktea.test | CUSTOMER | PERSONAL | True/True |
| C05 | kh05.demo@milktea.test | CUSTOMER | PERSONAL | True/True |
| C06 | kh06.demo@milktea.test | CUSTOMER | PERSONAL | True/True |
| C07 | unverified.demo@milktea.test | CUSTOMER | PERSONAL | False/True |
| C08 | locked.demo@milktea.test | CUSTOMER | PERSONAL | True/False |

Mỗi cơ sở có Bàn01–04, QR khác nhau toàn hệ thống: `D26_CN01_B01`…`D26_CN02_B04`. Hai cơ sở được trùng TenBan. S_A_OPEN ở CN01/Bàn01 và S_B_OPEN ở CN02/Bàn01; S_A_OLD ở CN01/Bàn02 và S_B_OLD ở CN02/Bàn02 đã CLOSED. Bàn03/04 TRONG dùng thử mở phiên/đặt quầy/gắn bàn. Không sinh token cố định từ mã QR này.

| Phiên | Bàn | Trạng thái | Đơn | Kỳ vọng |
| --- | --- | --- | --- | --- |
| S_A_OLD | CN01/Bàn02 | CLOSED | H01,H02 | Giữ lịch sử, không còn giỏ/token |
| S_B_OLD | CN02/Bàn02 | CLOSED | H25 | Giữ lịch sử, không còn giỏ/token |
| S_A_OPEN | CN01/Bàn01 | OPEN | L01,L02 | Không đóng được: còn đơn chưa xong/tiền chưa thu |
| S_B_OPEN | CN02/Bàn01 | OPEN | L03 | Không đóng được: đang pha, chưa thu |

## 5. Nguyên liệu, đơn vị và phả hệ nhập

Số lượng/giá vốn dưới đây theo **đơn vị chuẩn của từng nguyên liệu**. Ví dụ0.030kg=30g; không dùng30kg thay cho30g. Nước theo ml, thành phẩm trà theo ml, trân châu chín theo g. Một bộ ly bao gồm nắp/ống hút để demo không thiếu bao bì; có thể tách SKU bao bì trong dự án thật.

| Mã | Tên | Loại | Đơn vị | Nhập tổng | Cấp mỗi CN | Giá vốn/đv |
| --- | --- | --- | --- | --- | --- | --- |
| R01 | Trà đen khô | THO | kg | 10 | 1 | 120000 |
| R02 | Trà lài khô | THO | kg | 10 | 1 | 140000 |
| R03 | Trà ô long khô | THO | kg | 10 | 1 | 160000 |
| R04 | Sữa đặc | THO | kg | 40 | 10 | 45000 |
| R05 | Đường cát | THO | kg | 50 | 10 | 22000 |
| R06 | Trân châu khô | THO | kg | 20 | 3 | 50000 |
| R07 | Nước lọc | THO | ml | 500000 | 50000 | 0.01 |
| R08 | Ly kèm nắp và ống hút | THO | cai | 2000 | 300 | 1200 |
| R09 | Syrup đào | THO | ml | 30000 | 3000 | 40 |
| R10 | Syrup xoài | THO | ml | 30000 | 3000 | 45 |
| R11 | Syrup dâu | THO | ml | 30000 | 3000 | 45 |
| R12 | Bột matcha | THO | kg | 5 | 0.5 | 700000 |
| R13 | Sữa tươi | THO | ml | 100000 | 20000 | 32 |
| R14 | Bột cacao | THO | kg | 5 | 0.5 | 300000 |
| B01 | Cốt trà đen | SOCHE | ml | Từ mẻ | Từ mẻ | 2.677778 |
| B02 | Cốt trà lài | SOCHE | ml | Từ mẻ | Từ mẻ | 3.122222 |
| B03 | Cốt trà ô long | SOCHE | ml | Từ mẻ | Từ mẻ | 3.566667 |
| B04 | Trân châu chín | SOCHE | g | Từ mẻ | Từ mẻ | 20.008 |

### 5.1. Phiếu nhập và cấp phát

`PN_TONG_01` có14 chi tiết R01–R14, lấy đúng lượng “Nhập tổng” và đơn giá trên. NCC_DEMO_01 — Nhà cung cấp nguyên liệu Demo; người lập A01, trạng thái DA_GHI_SO. Từng dòng tạo NHAP ở KHOTONG/KHO.

`PN_CN01_01`, `PN_CN02_01` có14 chi tiết mỗi phiếu, lượng “Cấp mỗi CN”, nguồn KHOTONG, người lập M_CN01/M_CN02. Mỗi dòng tạo hai bút toán bằng lượng và trái dấu: KHOTONG/KHO `XUAT_CHUYEN`, sourceType `CHUYEN_KHO`; CNxx/KHO `NHAP`, sourceType `NHAP_TONG`. Giá nhận bằng giá vốn lô tổng trong fixture một lô này. Không cộng chi phí chuyển nội bộ thành doanh thu chuỗi.

Cả14 nguyên liệu thô đều có lịch sử nhập, kể cả nước và bộ ly. Không chỉ nạp các nguyên liệu dùng cho một món nổi bật.

### 5.2. Bốn định mức sơ chế dùng chung

| MãCT | Thành phẩm | Đầu ra chuẩn | Đầu vào chuẩn | Sản lượng mỗi CN | Chi phí/mẻ tại mỗi CN |
| --- | --- | --- | --- | --- | --- |
| SC01 | B01 | 4500 ml | R01=0.1 kg; R07=5000 ml | 9000 | 24100 |
| SC02 | B02 | 4500 ml | R02=0.1 kg; R07=5000 ml | 4500 | 14050 |
| SC03 | B03 | 4500 ml | R03=0.1 kg; R07=5000 ml | 9000 | 32100 |
| SC04 | B04 | 2500 g | R06=1 kg; R07=2000 ml | 5000 | 100040 |

Mỗi cơ sở tạo ME_CNxx_SC01…SC04, tổng8 mẻ DA_GHI_SO, người lập BEP_CNxx. Hệ số mẻ = sản lượng yêu cầu/đầu ra chuẩn; từng nguyên liệu thô bị trừ theo hệ số, thành phẩm tăng tại BEP. Chi phí = tổng lượng đầu vào×giá vốn; giá vốn thành phẩm = chi phí/sản lượng. Tỷ lệ khối lượng–thể tích là định mức sản xuất riêng, không phải phép đổi kg→ml chung. Ghi LuongDuKien bằng sản lượng dự kiến của **mẻ thực hiện**, không nhầm với đầu ra của một định mức chuẩn.

## 6. Danh mục, món và toàn bộ công thức

Danh mục: DM01 Trà sữa; DM02 Trà nguyên vị; DM03 Trà trái cây; DM04 Sữa tươi; DM05 Topping. DM01–04 có ChoPhepTopping=1; DM05=0, tất cả HoatDong=1.

Có24 tên đồ uống khác nhau để demo >10 món và top20, không đếm trùng size là24 tên. Thêm4 variant L và2 topping để kiểm giá/công thức size/topping. Tất cả có mô tả và ảnh hợp lệ do nhóm tự chuẩn bị; không dùng ảnh rỗng để tự nhận đã demo upload.

| Mã | Tên | DM | Size | Giá VND | Công thức/1 đơn vị |
| --- | --- | --- | --- | --- | --- |
| P01 | Trà sữa truyền thống | DM01 | M | 32000 | B01=150; R04=0.03; R05=0.02; R08=1 |
| P02 | Trà sữa lài | DM01 | M | 34000 | B02=150; R04=0.03; R05=0.02; R08=1 |
| P03 | Trà sữa ô long | DM01 | M | 36000 | B03=150; R04=0.03; R05=0.02; R08=1 |
| P04 | Trà sữa matcha | DM01 | M | 39000 | R12=0.006; R13=150; R04=0.02; R05=0.02; R08=1 |
| P05 | Trà sữa cacao | DM01 | M | 38000 | B01=100; R14=0.008; R04=0.03; R05=0.02; R08=1 |
| P06 | Trà sữa đào | DM01 | M | 35000 | B01=120; R04=0.03; R09=20; R05=0.01; R08=1 |
| P07 | Trà sữa xoài | DM01 | M | 36000 | B01=120; R04=0.03; R10=20; R05=0.01; R08=1 |
| P08 | Trà sữa dâu | DM01 | M | 36000 | B02=120; R04=0.03; R11=20; R05=0.01; R08=1 |
| P09 | Trà đen nguyên vị | DM02 | M | 25000 | B01=180; R05=0.02; R08=1 |
| P10 | Trà lài nguyên vị | DM02 | M | 25000 | B02=180; R05=0.02; R08=1 |
| P11 | Trà ô long nguyên vị | DM02 | M | 28000 | B03=180; R05=0.02; R08=1 |
| P12 | Trà đào | DM03 | M | 30000 | B01=150; R09=25; R05=0.01; R08=1 |
| P13 | Trà xoài | DM03 | M | 30000 | B01=150; R10=25; R05=0.01; R08=1 |
| P14 | Trà dâu | DM03 | M | 30000 | B02=150; R11=25; R05=0.01; R08=1 |
| P15 | Sữa tươi đường đen trân châu | DM04 | M | 38000 | R13=180; R05=0.025; B04=40; R08=1 |
| P16 | Sữa tươi matcha | DM04 | M | 40000 | R12=0.008; R13=180; R05=0.02; R08=1 |
| P17 | Sữa tươi cacao | DM04 | M | 40000 | R14=0.01; R13=180; R05=0.02; R08=1 |
| P18 | Sữa tươi dâu | DM04 | M | 38000 | R13=180; R11=25; R05=0.01; R08=1 |
| P19 | Sữa tươi xoài | DM04 | M | 38000 | R13=180; R10=25; R05=0.01; R08=1 |
| P20 | Ô long sữa tươi | DM04 | M | 39000 | B03=100; R13=120; R05=0.02; R08=1 |
| P21 | Trà đào lài | DM03 | M | 32000 | B02=150; R09=25; R05=0.01; R08=1 |
| P22 | Trà đào ô long | DM03 | M | 34000 | B03=150; R09=25; R05=0.01; R08=1 |
| P23 | Trà dâu ô long | DM03 | M | 34000 | B03=150; R11=25; R05=0.01; R08=1 |
| P24 | Trà xoài lài | DM03 | M | 32000 | B02=150; R10=25; R05=0.01; R08=1 |
| P01L | Trà sữa truyền thống | DM01 | L | 40000 | B01=195; R04=0.039; R05=0.026; R08=1 |
| P02L | Trà sữa lài | DM01 | L | 42000 | B02=195; R04=0.039; R05=0.026; R08=1 |
| P03L | Trà sữa ô long | DM01 | L | 44000 | B03=195; R04=0.039; R05=0.026; R08=1 |
| P12L | Trà đào | DM03 | L | 38000 | B01=195; R09=32.5; R05=0.013; R08=1 |
| T01 | Thêm trân châu | DM05 | M | 5000 | B04=50 |
| T02 | Thêm sữa đặc | DM05 | M | 4000 | R04=0.02 |

Công thức ghi bằng mã NL; đơn vị tra bảng nguyên liệu. Với size L chỉ thành phần đồ uống tăng1.3 lần, **ly vẫn1 bộ**. Giá L=M+8.000đ, không suy giá từ hệ số công thức. Topping T01/T02 không thêm ly riêng; mỗi topping được chọn tính cho từng ly, nhân quantity. Không thêm T01 hai lần cùng ID nếu chưa có mô hình số lượng topping rõ ràng.

### 6.1. Menu theo chi nhánh tại thời điểm cuối fixture

CN02 bán và phục vụ tất cả30 variant/product. CN01 bán mọi món trừ P24 (`KinhDoanh=0`); P23 (`KinhDoanh=1, ConPhucVu=0`) tạm hết. Các variant còn lại KinhDoanh=1, ConPhucVu=1. Đặt P24 ở CN01 phải bị từ chối; P23 có thể hiển thị nhãn tạm hết nếu policy cho phép nhưng không được thêm/checkout. Các đơn H23/H24 có trước thời điểm đổi cờ nên giữ lịch sử hợp lệ; không xóa chúng để “làm sạch” menu hiện tại.

Khóa truy vấn theo cơ sở phải lấy từ bàn khi QR; người dùng không đổi branchId để thoát quy tắc. KHOTONG không xuất hiện như cửa hàng nhận đơn, và POST trực tiếp đến kho tổng phải bị từ chối.

### 6.2. Mới nhất

Thứ tự tạo đích của P01…P24 tăng dần;4 variant L tạo sau đó. Nếu dùng product family,24 tên mới nhất là P24…P01 và tập top20 là P24…P05 trước lọc cơ sở; phải lọc branch **trước** lấy top20. Nếu dùng variant, dùng createdAt thực của từng variant và tie-break ID. Schema hiện chưa có ngày tạo món: không chèn cột tưởng tượng vào DB cũ; thêm migration trước hoặc trình bày rõ ID chỉ là xấp xỉ.

## 7. Khuyến mãi/voucher

Cấu hình giảm toàn chuỗi bằng0 trong baseline để tách các phép thử. D26_10 tính trên subtotal, cap20.000; mỗi đơn một voucher. Case giảm cửa hàng10% + voucher là bài thử riêng bên dưới, không cộng vào báo cáo baseline.

| Mã | Loại/giá trị | Tối thiểu | Tổng lượt | Mỗi khách | Scope | Thời hạn/trạng thái |
| --- | --- | --- | --- | --- | --- | --- |
| D26_10 | PERCENT10, cap20000 | 50000 | 50 | 2 | ALL | D−7 đến D+7; bật |
| D26_CN01 | FIXED10000 | 40000 | 20 | 1 | CN01 | D−7 đến D+7; bật |
| D26_LAST | FIXED5000 | 10000 | 1 | 1 | ALL | D−7 đến D+7; bật, chưa dùng |
| D26_USED | FIXED5000 | 10000 | 1 | 1 | ALL | H30 đã dùng hết |
| D26_ZERO | FIXED200000 | 0 | 10 | 1 | ALL | D−7 đến D+7; chỉ case mở rộng |
| D26_EXPIRED | FIXED5000 | 10000 | 10 | 1 | ALL | D−7 đến D−1; hết hạn |
| D26_FUTURE | FIXED5000 | 10000 | 10 | 1 | ALL | D+1 đến D+7; chưa bắt đầu |
| D26_LOCKED | FIXED5000 | 10000 | 10 | 1 | ALL | Còn hạn nhưng tắt |
| D26_BIG | FIXED10000 | 500000 | 10 | 1 | ALL | Không đủ min trên giỏ nhỏ |

Lịch sử baseline: H32 và L06 dùng D26_10, SoLuotDaDung=2; H33 dùng D26_CN01 sau đó hủy và DA_HOAN_TRA, SoLuotDaDung=0; H30 dùng D26_USED, SoLuotDaDung=1. Mã còn lại usedCount=0. Invoice giữ MaVoucher, MaCodeVoucher, TienGiamVoucher; lịch sử giữ order/invoice/account/branch. Bổ sung snapshot loại/tỷ lệ/cap nếu sửa schema theo audit. Không update usedCount tùy ý tách khỏi history.

## 8. Toàn bộ44 đơn và kết quả tiền mong đợi

Mỗi đơn baseline có một dòng món chính; toppings trong cột đi kèm. Tất cả đơn H hoàn thành là đơn của CUSTOMER; đơn quầy L05 do TN_CN01 tạo. H01/H02/H25 gắn phiên cũ; L01/L02 cùng phiên A đang mở, L03 phiên B đang mở. Các đơn ONLINE của baseline hiểu là khách đặt để nhận tại quầy; **không giả lập đã giao hàng khi source chưa có lifecycle giao**.

`Paid=Y` là có Payment đúng bằng PhaiTra. Hủy đã thu vẫn giữ khoản thu và hóa đơn DA_HUY; hủy chưa thu không có Payment. HOAN_THANH nhưng chưa paid vẫn tiêu hao, nhưng không được tính best seller hay review.

| Đơn | CN | Khách/Staff | Nguồn | Món×SL + topping | Trạng thái | Gốc | Voucher/giảm | Phải trả | Paid/method | Hoàn |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| H01 | CN01 | C01 | TABLE | P01×2 | HOAN_THANH | 64000 | —/0 | 64000 | Y/CASH | — |
| H02 | CN01 | C02 | TABLE | P02×3 | HOAN_THANH | 102000 | —/0 | 102000 | Y/BANK_TRANSFER | — |
| H03 | CN01 | C03 | ONLINE | P03×1 | HOAN_THANH | 36000 | —/0 | 36000 | Y/CASH | — |
| H04 | CN01 | C04 | ONLINE | P04×2 | HOAN_THANH | 78000 | —/0 | 78000 | Y/BANK_TRANSFER | — |
| H05 | CN01 | C05 | ONLINE | P05×3 | HOAN_THANH | 114000 | —/0 | 114000 | Y/CASH | — |
| H06 | CN01 | C06 | ONLINE | P06×1 | HOAN_THANH | 35000 | —/0 | 35000 | Y/BANK_TRANSFER | — |
| H07 | CN01 | C01 | ONLINE | P07×2 | HOAN_THANH | 72000 | —/0 | 72000 | Y/CASH | — |
| H08 | CN01 | C02 | ONLINE | P08×3 | HOAN_THANH | 108000 | —/0 | 108000 | Y/BANK_TRANSFER | — |
| H09 | CN01 | C03 | ONLINE | P09×1 | HOAN_THANH | 25000 | —/0 | 25000 | Y/CASH | — |
| H10 | CN01 | C04 | ONLINE | P10×2 | HOAN_THANH | 50000 | —/0 | 50000 | Y/BANK_TRANSFER | — |
| H11 | CN01 | C05 | ONLINE | P11×3 | HOAN_THANH | 84000 | —/0 | 84000 | Y/CASH | — |
| H12 | CN01 | C06 | ONLINE | P12×1 | HOAN_THANH | 30000 | —/0 | 30000 | Y/BANK_TRANSFER | — |
| H13 | CN01 | C01 | ONLINE | P13×2 | HOAN_THANH | 60000 | —/0 | 60000 | Y/CASH | — |
| H14 | CN01 | C02 | ONLINE | P14×3 | HOAN_THANH | 90000 | —/0 | 90000 | Y/BANK_TRANSFER | — |
| H15 | CN01 | C03 | ONLINE | P15×1 | HOAN_THANH | 38000 | —/0 | 38000 | Y/CASH | — |
| H16 | CN01 | C04 | ONLINE | P16×2 | HOAN_THANH | 80000 | —/0 | 80000 | Y/BANK_TRANSFER | — |
| H17 | CN01 | C05 | ONLINE | P17×3 | HOAN_THANH | 120000 | —/0 | 120000 | Y/CASH | — |
| H18 | CN01 | C06 | ONLINE | P18×1 | HOAN_THANH | 38000 | —/0 | 38000 | Y/BANK_TRANSFER | — |
| H19 | CN01 | C01 | ONLINE | P19×2 | HOAN_THANH | 76000 | —/0 | 76000 | Y/CASH | — |
| H20 | CN01 | C02 | ONLINE | P20×3 | HOAN_THANH | 117000 | —/0 | 117000 | Y/BANK_TRANSFER | — |
| H21 | CN01 | C03 | ONLINE | P21×1 | HOAN_THANH | 32000 | —/0 | 32000 | Y/CASH | — |
| H22 | CN01 | C04 | ONLINE | P22×2 | HOAN_THANH | 68000 | —/0 | 68000 | Y/BANK_TRANSFER | — |
| H23 | CN01 | C05 | ONLINE | P23×3 | HOAN_THANH | 102000 | —/0 | 102000 | Y/CASH | — |
| H24 | CN01 | C06 | ONLINE | P24×1 | HOAN_THANH | 32000 | —/0 | 32000 | Y/BANK_TRANSFER | — |
| H25 | CN02 | C01 | TABLE | P01×4 | HOAN_THANH | 128000 | —/0 | 128000 | Y/CASH | — |
| H26 | CN02 | C02 | ONLINE | P02×3 | HOAN_THANH | 102000 | —/0 | 102000 | Y/BANK_TRANSFER | — |
| H27 | CN02 | C03 | ONLINE | P03×2 | HOAN_THANH | 72000 | —/0 | 72000 | Y/CASH | — |
| H28 | CN02 | C04 | ONLINE | P04×1 | HOAN_THANH | 39000 | —/0 | 39000 | Y/BANK_TRANSFER | — |
| H29 | CN02 | C05 | ONLINE | P05×2 | HOAN_THANH | 76000 | —/0 | 76000 | Y/CASH | — |
| H30 | CN02 | C06 | ONLINE | P06×3 | HOAN_THANH | 105000 | D26_USED/5000 | 100000 | Y/BANK_TRANSFER | — |
| H31 | CN02 | C01 | ONLINE | P12×2 | HOAN_THANH | 60000 | —/0 | 60000 | N | — |
| H32 | CN01 | C02 | ONLINE | P01L×2 + T01 | HOAN_THANH | 90000 | D26_10/9000 | 81000 | Y/BANK_TRANSFER | — |
| H33 | CN01 | C03 | ONLINE | P02×2 | DA_HUY | 68000 | D26_CN01/10000 | 58000 | Y/BANK_TRANSFER | DA_HOAN |
| H34 | CN02 | C04 | ONLINE | P03×2 | DA_HUY | 72000 | —/0 | 72000 | Y/CASH | DA_HOAN |
| H35 | CN01 | C05 | ONLINE | P04×2 | DA_HUY | 78000 | —/0 | 78000 | Y/BANK_TRANSFER | CHO_DUYET |
| H36 | CN02 | C06 | ONLINE | P05×1 | DA_HUY | 38000 | —/0 | 38000 | N | — |
| L01 | CN01 | C01 | TABLE | P01×2 + T01 | CHO_XAC_NHAN | 74000 | —/0 | 74000 | N | — |
| L02 | CN01 | C02 | TABLE | P12×1 | CHO_CHE_BIEN | 30000 | —/0 | 30000 | Y/CASH | — |
| L03 | CN02 | C03 | TABLE | P02×2 + T01 | DANG_CHE_BIEN | 78000 | —/0 | 78000 | N | — |
| L04 | CN02 | C04 | ONLINE | P03×1 | CHO_XAC_NHAN | 36000 | —/0 | 36000 | N | — |
| L05 | CN01 | TN_CN01 | COUNTER | P01×1 | CHO_THANH_TOAN | 32000 | —/0 | 32000 | N | — |
| L06 | CN02 | C06 | ONLINE | P04×2 | CHO_CHE_BIEN | 78000 | D26_10/7800 | 70200 | Y/BANK_TRANSFER | — |
| L07 | CN01 | C05 | ONLINE | P22×1 | CHO_XAC_NHAN | 34000 | —/0 | 34000 | N | — |
| L08 | CN02 | C06 | ONLINE | P01×1 | CHO_XAC_NHAN | 32000 | —/0 | 32000 | N | — |

L04 có PaymentNotice BANK_TRANSFER nhưng chưa có Payment; không gọi đây là đã thanh toán. L05 dùng CHO_THANH_TOAN phản ánh source hiện tại; nếu nhóm thống nhất bỏ paid gate cho đơn staff thì thay trạng thái và cập nhật acceptance đồng bộ, không tự coi nó giống CHO_XAC_NHAN.

### 8.1. Sổ hoàn tiền

H33: hủy trước pha chế, giữ Payment BANK_TRANSFER58.000; request và xác nhận hoàn58.000, trạng thái DA_HOAN, có tham chiếu demo `D26_BANK_H33`.

H34: hủy trước pha chế, giữ Payment CASH72.000; thu ngân trả72.000 và ghi DA_HOAN, tham chiếu `D26_CASH_H34`.

H35: hủy trước pha chế, đã thu BANK_TRANSFER78.000; request CHO_DUYET, chưa thực chi. Khoản78.000 vẫn nằm trong dòng tiền ròng cho đến khi ghi hoàn thực tế, đồng thời là nghĩa vụ hoàn còn mở.

H36: chưa thu, hủy; không tạo nghĩa vụ hoàn. Không tạo sổ tiêu hao ORDER cho H33–H36.

### 8.2. Đối soát tiền toàn khoảng dữ liệu

Chọn khoảng thời gian chứa toàn bộ các khoản thu/hoàn trên, gồm H và L. “Thu ròng” là dòng tiền, chưa phải lợi nhuận và chưa đồng nghĩa doanh thu được ghi nhận theo hoàn thành bán hàng.

| Phạm vi | Số đơn | Thực thu | Thực hoàn | Thu ròng | Nghĩa vụ hoàn chờ | Phải thu HĐ hiệu lực |
| --- | --- | --- | --- | --- | --- | --- |
| CN01 | 31 | 1898000 | 58000 | 1840000 | 78000 | 140000 |
| CN02 | 13 | 659200 | 72000 | 587200 | 0 | 206000 |
| ALL | 44 | 2557200 | 130000 | 2427200 | 78000 | 346000 |

Chi phí hao hụt kết ca CN01 là267.78đ; đây là chi phí, không tự trừ vào “thực thu−thực hoàn”. Giá vốn đơn hoàn thành tính từ snapshot/cost lô, không lấy giá bán làm chi phí. Từng báo cáo phải ghi rõ dùng ngày tạo đơn, ngày hoàn tất, ngày thu hay ngày thực hoàn.

## 9. Snapshot định mức cho từng đơn

Tất cả44 đơn đều phải có snapshot đầy đủ. Bảng này là DinhMucDonHang đích, theo mã đơn + nguyên liệu. Đơn chưa hoàn thành/hủy vẫn giữ snapshot lịch sử nhưng không có tiêu hao. H32 có2 ly L và T01 định mức50g/ly nên tổng trân châu là100g; hệ số size L chỉ áp công thức đồ uống, không tự tăng topping.

| Đơn | Định mức toàn đơn |
| --- | --- |
| H01 | B01=300 ml; R04=0.06 kg; R05=0.04 kg; R08=2 cai |
| H02 | B02=450 ml; R04=0.09 kg; R05=0.06 kg; R08=3 cai |
| H03 | B03=150 ml; R04=0.03 kg; R05=0.02 kg; R08=1 cai |
| H04 | R04=0.04 kg; R05=0.04 kg; R08=2 cai; R12=0.012 kg; R13=300 ml |
| H05 | B01=300 ml; R04=0.09 kg; R05=0.06 kg; R08=3 cai; R14=0.024 kg |
| H06 | B01=120 ml; R04=0.03 kg; R05=0.01 kg; R08=1 cai; R09=20 ml |
| H07 | B01=240 ml; R04=0.06 kg; R05=0.02 kg; R08=2 cai; R10=40 ml |
| H08 | B02=360 ml; R04=0.09 kg; R05=0.03 kg; R08=3 cai; R11=60 ml |
| H09 | B01=180 ml; R05=0.02 kg; R08=1 cai |
| H10 | B02=360 ml; R05=0.04 kg; R08=2 cai |
| H11 | B03=540 ml; R05=0.06 kg; R08=3 cai |
| H12 | B01=150 ml; R05=0.01 kg; R08=1 cai; R09=25 ml |
| H13 | B01=300 ml; R05=0.02 kg; R08=2 cai; R10=50 ml |
| H14 | B02=450 ml; R05=0.03 kg; R08=3 cai; R11=75 ml |
| H15 | B04=40 g; R05=0.025 kg; R08=1 cai; R13=180 ml |
| H16 | R05=0.04 kg; R08=2 cai; R12=0.016 kg; R13=360 ml |
| H17 | R05=0.06 kg; R08=3 cai; R13=540 ml; R14=0.03 kg |
| H18 | R05=0.01 kg; R08=1 cai; R11=25 ml; R13=180 ml |
| H19 | R05=0.02 kg; R08=2 cai; R10=50 ml; R13=360 ml |
| H20 | B03=300 ml; R05=0.06 kg; R08=3 cai; R13=360 ml |
| H21 | B02=150 ml; R05=0.01 kg; R08=1 cai; R09=25 ml |
| H22 | B03=300 ml; R05=0.02 kg; R08=2 cai; R09=50 ml |
| H23 | B03=450 ml; R05=0.03 kg; R08=3 cai; R11=75 ml |
| H24 | B02=150 ml; R05=0.01 kg; R08=1 cai; R10=25 ml |
| H25 | B01=600 ml; R04=0.12 kg; R05=0.08 kg; R08=4 cai |
| H26 | B02=450 ml; R04=0.09 kg; R05=0.06 kg; R08=3 cai |
| H27 | B03=300 ml; R04=0.06 kg; R05=0.04 kg; R08=2 cai |
| H28 | R04=0.02 kg; R05=0.02 kg; R08=1 cai; R12=0.006 kg; R13=150 ml |
| H29 | B01=200 ml; R04=0.06 kg; R05=0.04 kg; R08=2 cai; R14=0.016 kg |
| H30 | B01=360 ml; R04=0.09 kg; R05=0.03 kg; R08=3 cai; R09=60 ml |
| H31 | B01=300 ml; R05=0.02 kg; R08=2 cai; R09=50 ml |
| H32 | B01=390 ml; B04=100 g; R04=0.078 kg; R05=0.052 kg; R08=2 cai |
| H33 | B02=300 ml; R04=0.06 kg; R05=0.04 kg; R08=2 cai |
| H34 | B03=300 ml; R04=0.06 kg; R05=0.04 kg; R08=2 cai |
| H35 | R04=0.04 kg; R05=0.04 kg; R08=2 cai; R12=0.012 kg; R13=300 ml |
| H36 | B01=100 ml; R04=0.03 kg; R05=0.02 kg; R08=1 cai; R14=0.008 kg |
| L01 | B01=300 ml; B04=100 g; R04=0.06 kg; R05=0.04 kg; R08=2 cai |
| L02 | B01=150 ml; R05=0.01 kg; R08=1 cai; R09=25 ml |
| L03 | B02=300 ml; B04=100 g; R04=0.06 kg; R05=0.04 kg; R08=2 cai |
| L04 | B03=150 ml; R04=0.03 kg; R05=0.02 kg; R08=1 cai |
| L05 | B01=150 ml; R04=0.03 kg; R05=0.02 kg; R08=1 cai |
| L06 | R04=0.04 kg; R05=0.04 kg; R08=2 cai; R12=0.012 kg; R13=300 ml |
| L07 | B03=150 ml; R05=0.01 kg; R08=1 cai; R09=25 ml |
| L08 | B01=150 ml; R04=0.03 kg; R05=0.02 kg; R08=1 cai |

## 10. Toàn bộ sổ kho có thể đối soát

Mã Kxxxx là thứ tự bút toán fixture, không giả làm MaBienDong thực của SQL Server. Mỗi dòng phải có MaCN, material, location, nguồn/ID nguồn thật sau ánh xạ. Người lập: IMPORT tổng A01; CHUYEN_KHO/NHAP_TONG M_CNxx; BATCH/ORDER/KC BEP_CNxx đúng cơ sở. Các nguồn đa hình phải được đối soát về bảng chứng từ tương ứng.

| Sổ | CN | NL | Vị trí | Loại | Nguồn / chứng từ | Trước | Delta | Sau |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| K0001 | KHOTONG | R01 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 10 | 10 |
| K0002 | KHOTONG | R02 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 10 | 10 |
| K0003 | KHOTONG | R03 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 10 | 10 |
| K0004 | KHOTONG | R04 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 40 | 40 |
| K0005 | KHOTONG | R05 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 50 | 50 |
| K0006 | KHOTONG | R06 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 20 | 20 |
| K0007 | KHOTONG | R07 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 500000 | 500000 |
| K0008 | KHOTONG | R08 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 2000 | 2000 |
| K0009 | KHOTONG | R09 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 30000 | 30000 |
| K0010 | KHOTONG | R10 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 30000 | 30000 |
| K0011 | KHOTONG | R11 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 30000 | 30000 |
| K0012 | KHOTONG | R12 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 5 | 5 |
| K0013 | KHOTONG | R13 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 100000 | 100000 |
| K0014 | KHOTONG | R14 | KHO | NHAP | IMPORT/PN_TONG_01 | 0 | 5 | 5 |
| K0015 | KHOTONG | R01 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 10 | -1 | 9 |
| K0016 | CN01 | R01 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 1 | 1 |
| K0017 | KHOTONG | R02 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 10 | -1 | 9 |
| K0018 | CN01 | R02 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 1 | 1 |
| K0019 | KHOTONG | R03 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 10 | -1 | 9 |
| K0020 | CN01 | R03 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 1 | 1 |
| K0021 | KHOTONG | R04 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 40 | -10 | 30 |
| K0022 | CN01 | R04 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 10 | 10 |
| K0023 | KHOTONG | R05 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 50 | -10 | 40 |
| K0024 | CN01 | R05 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 10 | 10 |
| K0025 | KHOTONG | R06 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 20 | -3 | 17 |
| K0026 | CN01 | R06 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 3 | 3 |
| K0027 | KHOTONG | R07 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 500000 | -50000 | 450000 |
| K0028 | CN01 | R07 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 50000 | 50000 |
| K0029 | KHOTONG | R08 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 2000 | -300 | 1700 |
| K0030 | CN01 | R08 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 300 | 300 |
| K0031 | KHOTONG | R09 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 30000 | -3000 | 27000 |
| K0032 | CN01 | R09 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 3000 | 3000 |
| K0033 | KHOTONG | R10 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 30000 | -3000 | 27000 |
| K0034 | CN01 | R10 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 3000 | 3000 |
| K0035 | KHOTONG | R11 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 30000 | -3000 | 27000 |
| K0036 | CN01 | R11 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 3000 | 3000 |
| K0037 | KHOTONG | R12 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 5 | -0.5 | 4.5 |
| K0038 | CN01 | R12 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 0.5 | 0.5 |
| K0039 | KHOTONG | R13 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 100000 | -20000 | 80000 |
| K0040 | CN01 | R13 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 20000 | 20000 |
| K0041 | KHOTONG | R14 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN01_01 | 5 | -0.5 | 4.5 |
| K0042 | CN01 | R14 | KHO | NHAP | NHAP_TONG/PN_CN01_01 | 0 | 0.5 | 0.5 |
| K0043 | CN01 | R01 | KHO | SOCHE_RA | BATCH/ME_CN01_SC01 | 1 | -0.2 | 0.8 |
| K0044 | CN01 | R07 | KHO | SOCHE_RA | BATCH/ME_CN01_SC01 | 50000 | -10000 | 40000 |
| K0045 | CN01 | B01 | BEP | SOCHE_VAO | BATCH/ME_CN01_SC01 | 0 | 9000 | 9000 |
| K0046 | CN01 | R02 | KHO | SOCHE_RA | BATCH/ME_CN01_SC02 | 1 | -0.1 | 0.9 |
| K0047 | CN01 | R07 | KHO | SOCHE_RA | BATCH/ME_CN01_SC02 | 40000 | -5000 | 35000 |
| K0048 | CN01 | B02 | BEP | SOCHE_VAO | BATCH/ME_CN01_SC02 | 0 | 4500 | 4500 |
| K0049 | CN01 | R03 | KHO | SOCHE_RA | BATCH/ME_CN01_SC03 | 1 | -0.2 | 0.8 |
| K0050 | CN01 | R07 | KHO | SOCHE_RA | BATCH/ME_CN01_SC03 | 35000 | -10000 | 25000 |
| K0051 | CN01 | B03 | BEP | SOCHE_VAO | BATCH/ME_CN01_SC03 | 0 | 9000 | 9000 |
| K0052 | CN01 | R06 | KHO | SOCHE_RA | BATCH/ME_CN01_SC04 | 3 | -2 | 1 |
| K0053 | CN01 | R07 | KHO | SOCHE_RA | BATCH/ME_CN01_SC04 | 25000 | -4000 | 21000 |
| K0054 | CN01 | B04 | BEP | SOCHE_VAO | BATCH/ME_CN01_SC04 | 0 | 5000 | 5000 |
| K0055 | KHOTONG | R01 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 9 | -1 | 8 |
| K0056 | CN02 | R01 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 1 | 1 |
| K0057 | KHOTONG | R02 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 9 | -1 | 8 |
| K0058 | CN02 | R02 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 1 | 1 |
| K0059 | KHOTONG | R03 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 9 | -1 | 8 |
| K0060 | CN02 | R03 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 1 | 1 |
| K0061 | KHOTONG | R04 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 30 | -10 | 20 |
| K0062 | CN02 | R04 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 10 | 10 |
| K0063 | KHOTONG | R05 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 40 | -10 | 30 |
| K0064 | CN02 | R05 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 10 | 10 |
| K0065 | KHOTONG | R06 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 17 | -3 | 14 |
| K0066 | CN02 | R06 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 3 | 3 |
| K0067 | KHOTONG | R07 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 450000 | -50000 | 400000 |
| K0068 | CN02 | R07 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 50000 | 50000 |
| K0069 | KHOTONG | R08 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 1700 | -300 | 1400 |
| K0070 | CN02 | R08 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 300 | 300 |
| K0071 | KHOTONG | R09 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 27000 | -3000 | 24000 |
| K0072 | CN02 | R09 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 3000 | 3000 |
| K0073 | KHOTONG | R10 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 27000 | -3000 | 24000 |
| K0074 | CN02 | R10 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 3000 | 3000 |
| K0075 | KHOTONG | R11 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 27000 | -3000 | 24000 |
| K0076 | CN02 | R11 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 3000 | 3000 |
| K0077 | KHOTONG | R12 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 4.5 | -0.5 | 4 |
| K0078 | CN02 | R12 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 0.5 | 0.5 |
| K0079 | KHOTONG | R13 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 80000 | -20000 | 60000 |
| K0080 | CN02 | R13 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 20000 | 20000 |
| K0081 | KHOTONG | R14 | KHO | XUAT_CHUYEN | CHUYEN_KHO/PN_CN02_01 | 4.5 | -0.5 | 4 |
| K0082 | CN02 | R14 | KHO | NHAP | NHAP_TONG/PN_CN02_01 | 0 | 0.5 | 0.5 |
| K0083 | CN02 | R01 | KHO | SOCHE_RA | BATCH/ME_CN02_SC01 | 1 | -0.2 | 0.8 |
| K0084 | CN02 | R07 | KHO | SOCHE_RA | BATCH/ME_CN02_SC01 | 50000 | -10000 | 40000 |
| K0085 | CN02 | B01 | BEP | SOCHE_VAO | BATCH/ME_CN02_SC01 | 0 | 9000 | 9000 |
| K0086 | CN02 | R02 | KHO | SOCHE_RA | BATCH/ME_CN02_SC02 | 1 | -0.1 | 0.9 |
| K0087 | CN02 | R07 | KHO | SOCHE_RA | BATCH/ME_CN02_SC02 | 40000 | -5000 | 35000 |
| K0088 | CN02 | B02 | BEP | SOCHE_VAO | BATCH/ME_CN02_SC02 | 0 | 4500 | 4500 |
| K0089 | CN02 | R03 | KHO | SOCHE_RA | BATCH/ME_CN02_SC03 | 1 | -0.2 | 0.8 |
| K0090 | CN02 | R07 | KHO | SOCHE_RA | BATCH/ME_CN02_SC03 | 35000 | -10000 | 25000 |
| K0091 | CN02 | B03 | BEP | SOCHE_VAO | BATCH/ME_CN02_SC03 | 0 | 9000 | 9000 |
| K0092 | CN02 | R06 | KHO | SOCHE_RA | BATCH/ME_CN02_SC04 | 3 | -2 | 1 |
| K0093 | CN02 | R07 | KHO | SOCHE_RA | BATCH/ME_CN02_SC04 | 25000 | -4000 | 21000 |
| K0094 | CN02 | B04 | BEP | SOCHE_VAO | BATCH/ME_CN02_SC04 | 0 | 5000 | 5000 |
| K0095 | CN01 | B01 | BEP | TIEU_HAO | ORDER/H01 | 9000 | -300 | 8700 |
| K0096 | CN01 | R04 | KHO | TIEU_HAO | ORDER/H01 | 10 | -0.06 | 9.94 |
| K0097 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H01 | 10 | -0.04 | 9.96 |
| K0098 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H01 | 300 | -2 | 298 |
| K0099 | CN01 | B02 | BEP | TIEU_HAO | ORDER/H02 | 4500 | -450 | 4050 |
| K0100 | CN01 | R04 | KHO | TIEU_HAO | ORDER/H02 | 9.94 | -0.09 | 9.85 |
| K0101 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H02 | 9.96 | -0.06 | 9.9 |
| K0102 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H02 | 298 | -3 | 295 |
| K0103 | CN01 | B03 | BEP | TIEU_HAO | ORDER/H03 | 9000 | -150 | 8850 |
| K0104 | CN01 | R04 | KHO | TIEU_HAO | ORDER/H03 | 9.85 | -0.03 | 9.82 |
| K0105 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H03 | 9.9 | -0.02 | 9.88 |
| K0106 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H03 | 295 | -1 | 294 |
| K0107 | CN01 | R04 | KHO | TIEU_HAO | ORDER/H04 | 9.82 | -0.04 | 9.78 |
| K0108 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H04 | 9.88 | -0.04 | 9.84 |
| K0109 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H04 | 294 | -2 | 292 |
| K0110 | CN01 | R12 | KHO | TIEU_HAO | ORDER/H04 | 0.5 | -0.012 | 0.488 |
| K0111 | CN01 | R13 | KHO | TIEU_HAO | ORDER/H04 | 20000 | -300 | 19700 |
| K0112 | CN01 | B01 | BEP | TIEU_HAO | ORDER/H05 | 8700 | -300 | 8400 |
| K0113 | CN01 | R04 | KHO | TIEU_HAO | ORDER/H05 | 9.78 | -0.09 | 9.69 |
| K0114 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H05 | 9.84 | -0.06 | 9.78 |
| K0115 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H05 | 292 | -3 | 289 |
| K0116 | CN01 | R14 | KHO | TIEU_HAO | ORDER/H05 | 0.5 | -0.024 | 0.476 |
| K0117 | CN01 | B01 | BEP | TIEU_HAO | ORDER/H06 | 8400 | -120 | 8280 |
| K0118 | CN01 | R04 | KHO | TIEU_HAO | ORDER/H06 | 9.69 | -0.03 | 9.66 |
| K0119 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H06 | 9.78 | -0.01 | 9.77 |
| K0120 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H06 | 289 | -1 | 288 |
| K0121 | CN01 | R09 | KHO | TIEU_HAO | ORDER/H06 | 3000 | -20 | 2980 |
| K0122 | CN01 | B01 | BEP | TIEU_HAO | ORDER/H07 | 8280 | -240 | 8040 |
| K0123 | CN01 | R04 | KHO | TIEU_HAO | ORDER/H07 | 9.66 | -0.06 | 9.6 |
| K0124 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H07 | 9.77 | -0.02 | 9.75 |
| K0125 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H07 | 288 | -2 | 286 |
| K0126 | CN01 | R10 | KHO | TIEU_HAO | ORDER/H07 | 3000 | -40 | 2960 |
| K0127 | CN01 | B02 | BEP | TIEU_HAO | ORDER/H08 | 4050 | -360 | 3690 |
| K0128 | CN01 | R04 | KHO | TIEU_HAO | ORDER/H08 | 9.6 | -0.09 | 9.51 |
| K0129 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H08 | 9.75 | -0.03 | 9.72 |
| K0130 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H08 | 286 | -3 | 283 |
| K0131 | CN01 | R11 | KHO | TIEU_HAO | ORDER/H08 | 3000 | -60 | 2940 |
| K0132 | CN01 | B01 | BEP | TIEU_HAO | ORDER/H09 | 8040 | -180 | 7860 |
| K0133 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H09 | 9.72 | -0.02 | 9.7 |
| K0134 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H09 | 283 | -1 | 282 |
| K0135 | CN01 | B02 | BEP | TIEU_HAO | ORDER/H10 | 3690 | -360 | 3330 |
| K0136 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H10 | 9.7 | -0.04 | 9.66 |
| K0137 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H10 | 282 | -2 | 280 |
| K0138 | CN01 | B03 | BEP | TIEU_HAO | ORDER/H11 | 8850 | -540 | 8310 |
| K0139 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H11 | 9.66 | -0.06 | 9.6 |
| K0140 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H11 | 280 | -3 | 277 |
| K0141 | CN01 | B01 | BEP | TIEU_HAO | ORDER/H12 | 7860 | -150 | 7710 |
| K0142 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H12 | 9.6 | -0.01 | 9.59 |
| K0143 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H12 | 277 | -1 | 276 |
| K0144 | CN01 | R09 | KHO | TIEU_HAO | ORDER/H12 | 2980 | -25 | 2955 |
| K0145 | CN01 | B01 | BEP | TIEU_HAO | ORDER/H13 | 7710 | -300 | 7410 |
| K0146 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H13 | 9.59 | -0.02 | 9.57 |
| K0147 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H13 | 276 | -2 | 274 |
| K0148 | CN01 | R10 | KHO | TIEU_HAO | ORDER/H13 | 2960 | -50 | 2910 |
| K0149 | CN01 | B02 | BEP | TIEU_HAO | ORDER/H14 | 3330 | -450 | 2880 |
| K0150 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H14 | 9.57 | -0.03 | 9.54 |
| K0151 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H14 | 274 | -3 | 271 |
| K0152 | CN01 | R11 | KHO | TIEU_HAO | ORDER/H14 | 2940 | -75 | 2865 |
| K0153 | CN01 | B04 | BEP | TIEU_HAO | ORDER/H15 | 5000 | -40 | 4960 |
| K0154 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H15 | 9.54 | -0.025 | 9.515 |
| K0155 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H15 | 271 | -1 | 270 |
| K0156 | CN01 | R13 | KHO | TIEU_HAO | ORDER/H15 | 19700 | -180 | 19520 |
| K0157 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H16 | 9.515 | -0.04 | 9.475 |
| K0158 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H16 | 270 | -2 | 268 |
| K0159 | CN01 | R12 | KHO | TIEU_HAO | ORDER/H16 | 0.488 | -0.016 | 0.472 |
| K0160 | CN01 | R13 | KHO | TIEU_HAO | ORDER/H16 | 19520 | -360 | 19160 |
| K0161 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H17 | 9.475 | -0.06 | 9.415 |
| K0162 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H17 | 268 | -3 | 265 |
| K0163 | CN01 | R13 | KHO | TIEU_HAO | ORDER/H17 | 19160 | -540 | 18620 |
| K0164 | CN01 | R14 | KHO | TIEU_HAO | ORDER/H17 | 0.476 | -0.03 | 0.446 |
| K0165 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H18 | 9.415 | -0.01 | 9.405 |
| K0166 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H18 | 265 | -1 | 264 |
| K0167 | CN01 | R11 | KHO | TIEU_HAO | ORDER/H18 | 2865 | -25 | 2840 |
| K0168 | CN01 | R13 | KHO | TIEU_HAO | ORDER/H18 | 18620 | -180 | 18440 |
| K0169 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H19 | 9.405 | -0.02 | 9.385 |
| K0170 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H19 | 264 | -2 | 262 |
| K0171 | CN01 | R10 | KHO | TIEU_HAO | ORDER/H19 | 2910 | -50 | 2860 |
| K0172 | CN01 | R13 | KHO | TIEU_HAO | ORDER/H19 | 18440 | -360 | 18080 |
| K0173 | CN01 | B03 | BEP | TIEU_HAO | ORDER/H20 | 8310 | -300 | 8010 |
| K0174 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H20 | 9.385 | -0.06 | 9.325 |
| K0175 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H20 | 262 | -3 | 259 |
| K0176 | CN01 | R13 | KHO | TIEU_HAO | ORDER/H20 | 18080 | -360 | 17720 |
| K0177 | CN01 | B02 | BEP | TIEU_HAO | ORDER/H21 | 2880 | -150 | 2730 |
| K0178 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H21 | 9.325 | -0.01 | 9.315 |
| K0179 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H21 | 259 | -1 | 258 |
| K0180 | CN01 | R09 | KHO | TIEU_HAO | ORDER/H21 | 2955 | -25 | 2930 |
| K0181 | CN01 | B03 | BEP | TIEU_HAO | ORDER/H22 | 8010 | -300 | 7710 |
| K0182 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H22 | 9.315 | -0.02 | 9.295 |
| K0183 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H22 | 258 | -2 | 256 |
| K0184 | CN01 | R09 | KHO | TIEU_HAO | ORDER/H22 | 2930 | -50 | 2880 |
| K0185 | CN01 | B03 | BEP | TIEU_HAO | ORDER/H23 | 7710 | -450 | 7260 |
| K0186 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H23 | 9.295 | -0.03 | 9.265 |
| K0187 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H23 | 256 | -3 | 253 |
| K0188 | CN01 | R11 | KHO | TIEU_HAO | ORDER/H23 | 2840 | -75 | 2765 |
| K0189 | CN01 | B02 | BEP | TIEU_HAO | ORDER/H24 | 2730 | -150 | 2580 |
| K0190 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H24 | 9.265 | -0.01 | 9.255 |
| K0191 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H24 | 253 | -1 | 252 |
| K0192 | CN01 | R10 | KHO | TIEU_HAO | ORDER/H24 | 2860 | -25 | 2835 |
| K0193 | CN02 | B01 | BEP | TIEU_HAO | ORDER/H25 | 9000 | -600 | 8400 |
| K0194 | CN02 | R04 | KHO | TIEU_HAO | ORDER/H25 | 10 | -0.12 | 9.88 |
| K0195 | CN02 | R05 | KHO | TIEU_HAO | ORDER/H25 | 10 | -0.08 | 9.92 |
| K0196 | CN02 | R08 | KHO | TIEU_HAO | ORDER/H25 | 300 | -4 | 296 |
| K0197 | CN02 | B02 | BEP | TIEU_HAO | ORDER/H26 | 4500 | -450 | 4050 |
| K0198 | CN02 | R04 | KHO | TIEU_HAO | ORDER/H26 | 9.88 | -0.09 | 9.79 |
| K0199 | CN02 | R05 | KHO | TIEU_HAO | ORDER/H26 | 9.92 | -0.06 | 9.86 |
| K0200 | CN02 | R08 | KHO | TIEU_HAO | ORDER/H26 | 296 | -3 | 293 |
| K0201 | CN02 | B03 | BEP | TIEU_HAO | ORDER/H27 | 9000 | -300 | 8700 |
| K0202 | CN02 | R04 | KHO | TIEU_HAO | ORDER/H27 | 9.79 | -0.06 | 9.73 |
| K0203 | CN02 | R05 | KHO | TIEU_HAO | ORDER/H27 | 9.86 | -0.04 | 9.82 |
| K0204 | CN02 | R08 | KHO | TIEU_HAO | ORDER/H27 | 293 | -2 | 291 |
| K0205 | CN02 | R04 | KHO | TIEU_HAO | ORDER/H28 | 9.73 | -0.02 | 9.71 |
| K0206 | CN02 | R05 | KHO | TIEU_HAO | ORDER/H28 | 9.82 | -0.02 | 9.8 |
| K0207 | CN02 | R08 | KHO | TIEU_HAO | ORDER/H28 | 291 | -1 | 290 |
| K0208 | CN02 | R12 | KHO | TIEU_HAO | ORDER/H28 | 0.5 | -0.006 | 0.494 |
| K0209 | CN02 | R13 | KHO | TIEU_HAO | ORDER/H28 | 20000 | -150 | 19850 |
| K0210 | CN02 | B01 | BEP | TIEU_HAO | ORDER/H29 | 8400 | -200 | 8200 |
| K0211 | CN02 | R04 | KHO | TIEU_HAO | ORDER/H29 | 9.71 | -0.06 | 9.65 |
| K0212 | CN02 | R05 | KHO | TIEU_HAO | ORDER/H29 | 9.8 | -0.04 | 9.76 |
| K0213 | CN02 | R08 | KHO | TIEU_HAO | ORDER/H29 | 290 | -2 | 288 |
| K0214 | CN02 | R14 | KHO | TIEU_HAO | ORDER/H29 | 0.5 | -0.016 | 0.484 |
| K0215 | CN02 | B01 | BEP | TIEU_HAO | ORDER/H30 | 8200 | -360 | 7840 |
| K0216 | CN02 | R04 | KHO | TIEU_HAO | ORDER/H30 | 9.65 | -0.09 | 9.56 |
| K0217 | CN02 | R05 | KHO | TIEU_HAO | ORDER/H30 | 9.76 | -0.03 | 9.73 |
| K0218 | CN02 | R08 | KHO | TIEU_HAO | ORDER/H30 | 288 | -3 | 285 |
| K0219 | CN02 | R09 | KHO | TIEU_HAO | ORDER/H30 | 3000 | -60 | 2940 |
| K0220 | CN02 | B01 | BEP | TIEU_HAO | ORDER/H31 | 7840 | -300 | 7540 |
| K0221 | CN02 | R05 | KHO | TIEU_HAO | ORDER/H31 | 9.73 | -0.02 | 9.71 |
| K0222 | CN02 | R08 | KHO | TIEU_HAO | ORDER/H31 | 285 | -2 | 283 |
| K0223 | CN02 | R09 | KHO | TIEU_HAO | ORDER/H31 | 2940 | -50 | 2890 |
| K0224 | CN01 | B01 | BEP | TIEU_HAO | ORDER/H32 | 7410 | -390 | 7020 |
| K0225 | CN01 | B04 | BEP | TIEU_HAO | ORDER/H32 | 4960 | -100 | 4860 |
| K0226 | CN01 | R04 | KHO | TIEU_HAO | ORDER/H32 | 9.51 | -0.078 | 9.432 |
| K0227 | CN01 | R05 | KHO | TIEU_HAO | ORDER/H32 | 9.255 | -0.052 | 9.203 |
| K0228 | CN01 | R08 | KHO | TIEU_HAO | ORDER/H32 | 252 | -2 | 250 |
| K0229 | CN01 | B01 | BEP | HAO_HUT | SHIFT_SETTLEMENT/KC_CN01_01 | 7020 | -100 | 6920 |

### 10.1. Số dư cuối kỳ

Các row vị trí không sử dụng vẫn bằng0. Bảng dưới thể hiện kho tổng/KHO, cơ sở/KHO và cơ sở/BEP theo từng nguyên liệu; không gộp kg với ml hoặc lượng của các nguyên liệu khác nhau.

| NL | Đơn vị | Tổng/KHO | CN01/KHO | CN01/BEP | CN02/KHO | CN02/BEP |
| --- | --- | --- | --- | --- | --- | --- |
| R01 | kg | 8 | 0.8 | 0 | 0.8 | 0 |
| R02 | kg | 8 | 0.9 | 0 | 0.9 | 0 |
| R03 | kg | 8 | 0.8 | 0 | 0.8 | 0 |
| R04 | kg | 20 | 9.432 | 0 | 9.56 | 0 |
| R05 | kg | 30 | 9.203 | 0 | 9.71 | 0 |
| R06 | kg | 14 | 1 | 0 | 1 | 0 |
| R07 | ml | 400000 | 21000 | 0 | 21000 | 0 |
| R08 | cai | 1400 | 250 | 0 | 283 | 0 |
| R09 | ml | 24000 | 2880 | 0 | 2890 | 0 |
| R10 | ml | 24000 | 2835 | 0 | 3000 | 0 |
| R11 | ml | 24000 | 2765 | 0 | 3000 | 0 |
| R12 | kg | 4 | 0.472 | 0 | 0.494 | 0 |
| R13 | ml | 60000 | 17720 | 0 | 19850 | 0 |
| R14 | kg | 4 | 0.446 | 0 | 0.484 | 0 |
| B01 | ml | Không dùng | 0 | 6920 | 0 | 7540 |
| B02 | ml | Không dùng | 0 | 2580 | 0 | 4050 |
| B03 | ml | Không dùng | 0 | 7260 | 0 | 8700 |
| B04 | g | Không dùng | 0 | 4860 | 0 | 5000 |

### 10.2. Kết ca mẫu hợp lệ

KC_CN01_01 chỉ kiểm B01 ở BEP. Tồn sổ=7020ml; đếm thực=7020ml; loại bỏ100ml; giữ lại=6920ml. Giá vốn=2.677778đ/ml; chi phí hao hụt=267.78đ. Có một dòng `HAO_HUT`, sourceType `SHIFT_SETTLEMENT`. Không có chênh lệch không giải thích trong baseline. Ca lệch kiểm kê90/100 được thử riêng để bắt F19, không trộn vào bộ cân đối này.

## 11. Review, yêu thích và thứ hạng đích

Mỗi review gắn đúng ID chi tiết món được sinh ra của đơn tham chiếu, không lấy orderId làm orderItemId. Nội dung mẫu dùng chung có thể thay bằng nội dung thật của người kiểm thử, nhưng phải>=50 ký tự sau trim. Nội dung baseline:

> Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này.

| Review | Đơn | Khách | Món | CN | Sao | Hiển thị |
| --- | --- | --- | --- | --- | --- | --- |
| DG01 | H01 | C01 | P01 | CN01 | 5 | Có |
| DG02 | H02 | C02 | P02 | CN01 | 4 | Có |
| DG03 | H03 | C03 | P03 | CN01 | 5 | Có |
| DG04 | H04 | C04 | P04 | CN01 | 4 | Có |
| DG05 | H05 | C05 | P05 | CN01 | 4 | Ẩn, M_CN01; lý do: kiểm duyệt demo |
| DG06 | H06 | C06 | P06 | CN01 | 5 | Có |
| DG07 | H07 | C01 | P07 | CN01 | 4 | Có |
| DG08 | H08 | C02 | P08 | CN01 | 4 | Có |
| DG09 | H25 | C01 | P01 | CN02 | 5 | Có |
| DG10 | H26 | C02 | P02 | CN02 | 4 | Có |
| DG11 | H27 | C03 | P03 | CN02 | 5 | Có |
| DG12 | H28 | C04 | P04 | CN02 | 4 | Có |
| DG13 | H29 | C05 | P05 | CN02 | 4 | Có |
| DG14 | H30 | C06 | P06 | CN02 | 5 | Có |
| DG15 | H32 | C02 | P01L | CN01 | 5 | Có |

DG05 bị ẩn nên không tính rating trung bình. Mỗi review media là tùy chọn; để demo upload cần dùng ảnh/video fixture hợp lệ do nhóm chuẩn bị và nhận URL từ storage, không chèn một link giả rồi kết luận upload thành công.

| Món | Tài khoản yêu thích | Số người phân biệt |
| --- | --- | --- |
| P01 | C01,C02,C03,C04,C05,C06 | 6 |
| P02 | C01,C02,C03,C04,C05 | 5 |
| P03 | C01,C02,C03,C04 | 4 |
| P12 | C01,C02,C03 | 3 |
| P04 | C01,C02 | 2 |
| P22 | C01 | 1 |

Yêu thích giảm dần đích: P01(6), P02(5), P03(4), P12(3), P04(2), P22(1). Các size cùng family cần policy nhất quán; fixture không cố nhân số lượt bằng cách thêm cả P01 và P01L cho cùng khách.

### 11.1 Bán chạy — CN01

| Hạng | Món | SL hoàn thành+đã thu |
| --- | --- | --- |
| 1 | P02 | 3 |
| 2 | P05 | 3 |
| 3 | P08 | 3 |
| 4 | P11 | 3 |
| 5 | P14 | 3 |
| 6 | P17 | 3 |
| 7 | P20 | 3 |
| 8 | P01 | 2 |
| 9 | P01L | 2 |
| 10 | P04 | 2 |
| 11 | P07 | 2 |
| 12 | P10 | 2 |
| 13 | P13 | 2 |
| 14 | P16 | 2 |
| 15 | P19 | 2 |
| 16 | P22 | 2 |
| 17 | P03 | 1 |
| 18 | P06 | 1 |
| 19 | P09 | 1 |
| 20 | P12 | 1 |

### 11.2 Bán chạy — CN02

| Hạng | Món | SL hoàn thành+đã thu |
| --- | --- | --- |
| 1 | P01 | 4 |
| 2 | P02 | 3 |
| 3 | P06 | 3 |
| 4 | P03 | 2 |
| 5 | P05 | 2 |
| 6 | P04 | 1 |

### 11.3 Bán chạy — ALL

| Hạng | Món | SL hoàn thành+đã thu |
| --- | --- | --- |
| 1 | P01 | 6 |
| 2 | P02 | 6 |
| 3 | P05 | 5 |
| 4 | P06 | 4 |
| 5 | P03 | 3 |
| 6 | P04 | 3 |
| 7 | P08 | 3 |
| 8 | P11 | 3 |
| 9 | P14 | 3 |
| 10 | P17 | 3 |
| 11 | P20 | 3 |
| 12 | P23 | 3 |
| 13 | P01L | 2 |
| 14 | P07 | 2 |
| 15 | P10 | 2 |
| 16 | P13 | 2 |
| 17 | P16 | 2 |
| 18 | P19 | 2 |
| 19 | P22 | 2 |
| 20 | P09 | 1 |

Thứ tự hòa dùng mã tượng trưng tăng dần để kết quả tái hiện được; khi ánh xạ sang DB dùng tie-break MaMon thực ổn định. Thống kê trên từng variant như query backend hiện tại, không tự gộp topping vào món chính. CN01 loại P23/P24 hiện không phục vụ trước khi LIMIT20. H31 đã hoàn thành nhưng chưa thu nên không có trong ranking; L02/L06 đã thu nhưng chưa hoàn thành cũng không được tính; H33/H34 đã thu rồi hủy không được tính.

## 12. Dữ liệu máy đọc để viết seed tự động

JSON dưới đây là fixture input; các mã là alias cần ánh xạ ID, không phải cột SQL phát sinh. Trường `created_rank` là thứ tự tạo theo thiết kế, chưa phải cột trong entity. `snapshot` là kết quả tính được để assert, không cho client tự quyết định tiêu hao. `cost` có thể nhiều chữ số; seed SQL làm tròn6 chữ số giá vốn và3 chữ số số lượng theo schema, tiền2 chữ số.

Không insert Order.status=HOAN_THANH rồi quên tiêu hao. Với bootstrap lịch sử phải ghi snapshot, Payment/Refund và toàn bộ ledger cùng transaction; khi chạy acceptance nên ưu tiên đi qua service/API thật. Không vừa nạp số dư cuối ở mục10.1 vừa phát sinh các bút toán mục10 (sẽ cộng hai lần).

```json
{"version":"D26-1","branches":["KHOTONG","CN01","CN02"],"accounts":[{"code":"A01","email":"admin.demo@milktea.test","role":"ADMIN","branch":"ALL"},{"code":"M_CN01","email":"m.cn01.demo@milktea.test","role":"MANAGER","branch":"CN01"},{"code":"TN_CN01","email":"tn.cn01.demo@milktea.test","role":"CASHIER","branch":"CN01"},{"code":"BEP_CN01","email":"bep.cn01.demo@milktea.test","role":"KITCHEN","branch":"CN01"},{"code":"M_CN02","email":"m.cn02.demo@milktea.test","role":"MANAGER","branch":"CN02"},{"code":"TN_CN02","email":"tn.cn02.demo@milktea.test","role":"CASHIER","branch":"CN02"},{"code":"BEP_CN02","email":"bep.cn02.demo@milktea.test","role":"KITCHEN","branch":"CN02"},{"code":"C01","email":"kh01.demo@milktea.test","role":"CUSTOMER","branch":"PERSONAL"},{"code":"C02","email":"kh02.demo@milktea.test","role":"CUSTOMER","branch":"PERSONAL"},{"code":"C03","email":"kh03.demo@milktea.test","role":"CUSTOMER","branch":"PERSONAL"},{"code":"C04","email":"kh04.demo@milktea.test","role":"CUSTOMER","branch":"PERSONAL"},{"code":"C05","email":"kh05.demo@milktea.test","role":"CUSTOMER","branch":"PERSONAL"},{"code":"C06","email":"kh06.demo@milktea.test","role":"CUSTOMER","branch":"PERSONAL"},{"code":"C07","email":"unverified.demo@milktea.test","role":"CUSTOMER","branch":"PERSONAL","verified":false},{"code":"C08","email":"locked.demo@milktea.test","role":"CUSTOMER","branch":"PERSONAL","active":false}],"materials":[{"code":"R01","name":"Trà đen khô","type":"THO","unit":"kg","central_import":"10","branch_transfer":"1","cost":"120000"},{"code":"R02","name":"Trà lài khô","type":"THO","unit":"kg","central_import":"10","branch_transfer":"1","cost":"140000"},{"code":"R03","name":"Trà ô long khô","type":"THO","unit":"kg","central_import":"10","branch_transfer":"1","cost":"160000"},{"code":"R04","name":"Sữa đặc","type":"THO","unit":"kg","central_import":"40","branch_transfer":"10","cost":"45000"},{"code":"R05","name":"Đường cát","type":"THO","unit":"kg","central_import":"50","branch_transfer":"10","cost":"22000"},{"code":"R06","name":"Trân châu khô","type":"THO","unit":"kg","central_import":"20","branch_transfer":"3","cost":"50000"},{"code":"R07","name":"Nước lọc","type":"THO","unit":"ml","central_import":"500000","branch_transfer":"50000","cost":"0.01"},{"code":"R08","name":"Ly kèm nắp và ống hút","type":"THO","unit":"cai","central_import":"2000","branch_transfer":"300","cost":"1200"},{"code":"R09","name":"Syrup đào","type":"THO","unit":"ml","central_import":"30000","branch_transfer":"3000","cost":"40"},{"code":"R10","name":"Syrup xoài","type":"THO","unit":"ml","central_import":"30000","branch_transfer":"3000","cost":"45"},{"code":"R11","name":"Syrup dâu","type":"THO","unit":"ml","central_import":"30000","branch_transfer":"3000","cost":"45"},{"code":"R12","name":"Bột matcha","type":"THO","unit":"kg","central_import":"5","branch_transfer":"0.5","cost":"700000"},{"code":"R13","name":"Sữa tươi","type":"THO","unit":"ml","central_import":"100000","branch_transfer":"20000","cost":"32"},{"code":"R14","name":"Bột cacao","type":"THO","unit":"kg","central_import":"5","branch_transfer":"0.5","cost":"300000"},{"code":"B01","name":"Cốt trà đen","type":"SOCHE","unit":"ml","cost":"2.677777777777777777777777778"},{"code":"B02","name":"Cốt trà lài","type":"SOCHE","unit":"ml","cost":"3.122222222222222222222222222"},{"code":"B03","name":"Cốt trà ô long","type":"SOCHE","unit":"ml","cost":"3.566666666666666666666666667"},{"code":"B04","name":"Trân châu chín","type":"SOCHE","unit":"g","cost":"20.008"}],"preparation_recipes":[{"code":"SC01","output":"B01","standard_output":"4500","inputs":{"R01":"0.1","R07":"5000"},"production_each_branch":"9000"},{"code":"SC02","output":"B02","standard_output":"4500","inputs":{"R02":"0.1","R07":"5000"},"production_each_branch":"4500"},{"code":"SC03","output":"B03","standard_output":"4500","inputs":{"R03":"0.1","R07":"5000"},"production_each_branch":"9000"},{"code":"SC04","output":"B04","standard_output":"2500","inputs":{"R06":"1","R07":"2000"},"production_each_branch":"5000"}],"products":[{"code":"P01","name":"Trà sữa truyền thống","category":"DM01","size":"M","price":"32000","recipe":{"B01":"150","R04":"0.03","R05":"0.02","R08":"1"},"created_rank":1,"type":"DRINK"},{"code":"P02","name":"Trà sữa lài","category":"DM01","size":"M","price":"34000","recipe":{"B02":"150","R04":"0.03","R05":"0.02","R08":"1"},"created_rank":2,"type":"DRINK"},{"code":"P03","name":"Trà sữa ô long","category":"DM01","size":"M","price":"36000","recipe":{"B03":"150","R04":"0.03","R05":"0.02","R08":"1"},"created_rank":3,"type":"DRINK"},{"code":"P04","name":"Trà sữa matcha","category":"DM01","size":"M","price":"39000","recipe":{"R12":"0.006","R13":"150","R04":"0.02","R05":"0.02","R08":"1"},"created_rank":4,"type":"DRINK"},{"code":"P05","name":"Trà sữa cacao","category":"DM01","size":"M","price":"38000","recipe":{"B01":"100","R14":"0.008","R04":"0.03","R05":"0.02","R08":"1"},"created_rank":5,"type":"DRINK"},{"code":"P06","name":"Trà sữa đào","category":"DM01","size":"M","price":"35000","recipe":{"B01":"120","R04":"0.03","R09":"20","R05":"0.01","R08":"1"},"created_rank":6,"type":"DRINK"},{"code":"P07","name":"Trà sữa xoài","category":"DM01","size":"M","price":"36000","recipe":{"B01":"120","R04":"0.03","R10":"20","R05":"0.01","R08":"1"},"created_rank":7,"type":"DRINK"},{"code":"P08","name":"Trà sữa dâu","category":"DM01","size":"M","price":"36000","recipe":{"B02":"120","R04":"0.03","R11":"20","R05":"0.01","R08":"1"},"created_rank":8,"type":"DRINK"},{"code":"P09","name":"Trà đen nguyên vị","category":"DM02","size":"M","price":"25000","recipe":{"B01":"180","R05":"0.02","R08":"1"},"created_rank":9,"type":"DRINK"},{"code":"P10","name":"Trà lài nguyên vị","category":"DM02","size":"M","price":"25000","recipe":{"B02":"180","R05":"0.02","R08":"1"},"created_rank":10,"type":"DRINK"},{"code":"P11","name":"Trà ô long nguyên vị","category":"DM02","size":"M","price":"28000","recipe":{"B03":"180","R05":"0.02","R08":"1"},"created_rank":11,"type":"DRINK"},{"code":"P12","name":"Trà đào","category":"DM03","size":"M","price":"30000","recipe":{"B01":"150","R09":"25","R05":"0.01","R08":"1"},"created_rank":12,"type":"DRINK"},{"code":"P13","name":"Trà xoài","category":"DM03","size":"M","price":"30000","recipe":{"B01":"150","R10":"25","R05":"0.01","R08":"1"},"created_rank":13,"type":"DRINK"},{"code":"P14","name":"Trà dâu","category":"DM03","size":"M","price":"30000","recipe":{"B02":"150","R11":"25","R05":"0.01","R08":"1"},"created_rank":14,"type":"DRINK"},{"code":"P15","name":"Sữa tươi đường đen trân châu","category":"DM04","size":"M","price":"38000","recipe":{"R13":"180","R05":"0.025","B04":"40","R08":"1"},"created_rank":15,"type":"DRINK"},{"code":"P16","name":"Sữa tươi matcha","category":"DM04","size":"M","price":"40000","recipe":{"R12":"0.008","R13":"180","R05":"0.02","R08":"1"},"created_rank":16,"type":"DRINK"},{"code":"P17","name":"Sữa tươi cacao","category":"DM04","size":"M","price":"40000","recipe":{"R14":"0.01","R13":"180","R05":"0.02","R08":"1"},"created_rank":17,"type":"DRINK"},{"code":"P18","name":"Sữa tươi dâu","category":"DM04","size":"M","price":"38000","recipe":{"R13":"180","R11":"25","R05":"0.01","R08":"1"},"created_rank":18,"type":"DRINK"},{"code":"P19","name":"Sữa tươi xoài","category":"DM04","size":"M","price":"38000","recipe":{"R13":"180","R10":"25","R05":"0.01","R08":"1"},"created_rank":19,"type":"DRINK"},{"code":"P20","name":"Ô long sữa tươi","category":"DM04","size":"M","price":"39000","recipe":{"B03":"100","R13":"120","R05":"0.02","R08":"1"},"created_rank":20,"type":"DRINK"},{"code":"P21","name":"Trà đào lài","category":"DM03","size":"M","price":"32000","recipe":{"B02":"150","R09":"25","R05":"0.01","R08":"1"},"created_rank":21,"type":"DRINK"},{"code":"P22","name":"Trà đào ô long","category":"DM03","size":"M","price":"34000","recipe":{"B03":"150","R09":"25","R05":"0.01","R08":"1"},"created_rank":22,"type":"DRINK"},{"code":"P23","name":"Trà dâu ô long","category":"DM03","size":"M","price":"34000","recipe":{"B03":"150","R11":"25","R05":"0.01","R08":"1"},"created_rank":23,"type":"DRINK"},{"code":"P24","name":"Trà xoài lài","category":"DM03","size":"M","price":"32000","recipe":{"B02":"150","R10":"25","R05":"0.01","R08":"1"},"created_rank":24,"type":"DRINK"},{"code":"P01L","name":"Trà sữa truyền thống","category":"DM01","size":"L","price":"40000","recipe":{"B01":"195","R04":"0.039","R05":"0.026","R08":"1"},"created_rank":25,"type":"DRINK"},{"code":"P02L","name":"Trà sữa lài","category":"DM01","size":"L","price":"42000","recipe":{"B02":"195","R04":"0.039","R05":"0.026","R08":"1"},"created_rank":26,"type":"DRINK"},{"code":"P03L","name":"Trà sữa ô long","category":"DM01","size":"L","price":"44000","recipe":{"B03":"195","R04":"0.039","R05":"0.026","R08":"1"},"created_rank":27,"type":"DRINK"},{"code":"P12L","name":"Trà đào","category":"DM03","size":"L","price":"38000","recipe":{"B01":"195","R09":"32.5","R05":"0.013","R08":"1"},"created_rank":36,"type":"DRINK"},{"code":"T01","name":"Thêm trân châu","category":"DM05","size":"M","price":"5000","recipe":{"B04":"50"},"created_rank":40,"type":"TOPPING"},{"code":"T02","name":"Thêm sữa đặc","category":"DM05","size":"M","price":"4000","recipe":{"R04":"0.02"},"created_rank":41,"type":"TOPPING"}],"documents":[{"code":"PN_TONG_01","branch":"KHOTONG","type":"IMPORT","supplier":"NCC_DEMO_01"},{"code":"PN_CN01_01","branch":"CN01","type":"TRANSFER_FROM_CENTRAL","supplier":"KHOTONG"},{"code":"ME_CN01_SC01","branch":"CN01","type":"PREPARATION","recipe":"SC01"},{"code":"ME_CN01_SC02","branch":"CN01","type":"PREPARATION","recipe":"SC02"},{"code":"ME_CN01_SC03","branch":"CN01","type":"PREPARATION","recipe":"SC03"},{"code":"ME_CN01_SC04","branch":"CN01","type":"PREPARATION","recipe":"SC04"},{"code":"PN_CN02_01","branch":"CN02","type":"TRANSFER_FROM_CENTRAL","supplier":"KHOTONG"},{"code":"ME_CN02_SC01","branch":"CN02","type":"PREPARATION","recipe":"SC01"},{"code":"ME_CN02_SC02","branch":"CN02","type":"PREPARATION","recipe":"SC02"},{"code":"ME_CN02_SC03","branch":"CN02","type":"PREPARATION","recipe":"SC03"},{"code":"ME_CN02_SC04","branch":"CN02","type":"PREPARATION","recipe":"SC04"}],"orders":[{"code":"H01","branch":"CN01","account":"C01","source":"TABLE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P01","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":"S_A_OLD","subtotal":"64000","discount":"0","payable":"64000","snapshot":{"B01":"300","R04":"0.06","R05":"0.04","R08":"2"}},{"code":"H02","branch":"CN01","account":"C02","source":"TABLE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P02","quantity":3,"toppings":[],"refund":null,"voucher":null,"session":"S_A_OLD","subtotal":"102000","discount":"0","payable":"102000","snapshot":{"B02":"450","R04":"0.09","R05":"0.06","R08":"3"}},{"code":"H03","branch":"CN01","account":"C03","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P03","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"36000","discount":"0","payable":"36000","snapshot":{"B03":"150","R04":"0.03","R05":"0.02","R08":"1"}},{"code":"H04","branch":"CN01","account":"C04","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P04","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"78000","discount":"0","payable":"78000","snapshot":{"R12":"0.012","R13":"300","R04":"0.04","R05":"0.04","R08":"2"}},{"code":"H05","branch":"CN01","account":"C05","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P05","quantity":3,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"114000","discount":"0","payable":"114000","snapshot":{"B01":"300","R14":"0.024","R04":"0.09","R05":"0.06","R08":"3"}},{"code":"H06","branch":"CN01","account":"C06","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P06","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"35000","discount":"0","payable":"35000","snapshot":{"B01":"120","R04":"0.03","R09":"20","R05":"0.01","R08":"1"}},{"code":"H07","branch":"CN01","account":"C01","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P07","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"72000","discount":"0","payable":"72000","snapshot":{"B01":"240","R04":"0.06","R10":"40","R05":"0.02","R08":"2"}},{"code":"H08","branch":"CN01","account":"C02","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P08","quantity":3,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"108000","discount":"0","payable":"108000","snapshot":{"B02":"360","R04":"0.09","R11":"60","R05":"0.03","R08":"3"}},{"code":"H09","branch":"CN01","account":"C03","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P09","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"25000","discount":"0","payable":"25000","snapshot":{"B01":"180","R05":"0.02","R08":"1"}},{"code":"H10","branch":"CN01","account":"C04","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P10","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"50000","discount":"0","payable":"50000","snapshot":{"B02":"360","R05":"0.04","R08":"2"}},{"code":"H11","branch":"CN01","account":"C05","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P11","quantity":3,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"84000","discount":"0","payable":"84000","snapshot":{"B03":"540","R05":"0.06","R08":"3"}},{"code":"H12","branch":"CN01","account":"C06","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P12","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"30000","discount":"0","payable":"30000","snapshot":{"B01":"150","R09":"25","R05":"0.01","R08":"1"}},{"code":"H13","branch":"CN01","account":"C01","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P13","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"60000","discount":"0","payable":"60000","snapshot":{"B01":"300","R10":"50","R05":"0.02","R08":"2"}},{"code":"H14","branch":"CN01","account":"C02","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P14","quantity":3,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"90000","discount":"0","payable":"90000","snapshot":{"B02":"450","R11":"75","R05":"0.03","R08":"3"}},{"code":"H15","branch":"CN01","account":"C03","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P15","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"38000","discount":"0","payable":"38000","snapshot":{"R13":"180","R05":"0.025","B04":"40","R08":"1"}},{"code":"H16","branch":"CN01","account":"C04","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P16","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"80000","discount":"0","payable":"80000","snapshot":{"R12":"0.016","R13":"360","R05":"0.04","R08":"2"}},{"code":"H17","branch":"CN01","account":"C05","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P17","quantity":3,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"120000","discount":"0","payable":"120000","snapshot":{"R14":"0.03","R13":"540","R05":"0.06","R08":"3"}},{"code":"H18","branch":"CN01","account":"C06","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P18","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"38000","discount":"0","payable":"38000","snapshot":{"R13":"180","R11":"25","R05":"0.01","R08":"1"}},{"code":"H19","branch":"CN01","account":"C01","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P19","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"76000","discount":"0","payable":"76000","snapshot":{"R13":"360","R10":"50","R05":"0.02","R08":"2"}},{"code":"H20","branch":"CN01","account":"C02","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P20","quantity":3,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"117000","discount":"0","payable":"117000","snapshot":{"B03":"300","R13":"360","R05":"0.06","R08":"3"}},{"code":"H21","branch":"CN01","account":"C03","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P21","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"32000","discount":"0","payable":"32000","snapshot":{"B02":"150","R09":"25","R05":"0.01","R08":"1"}},{"code":"H22","branch":"CN01","account":"C04","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P22","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"68000","discount":"0","payable":"68000","snapshot":{"B03":"300","R09":"50","R05":"0.02","R08":"2"}},{"code":"H23","branch":"CN01","account":"C05","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P23","quantity":3,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"102000","discount":"0","payable":"102000","snapshot":{"B03":"450","R11":"75","R05":"0.03","R08":"3"}},{"code":"H24","branch":"CN01","account":"C06","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P24","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"32000","discount":"0","payable":"32000","snapshot":{"B02":"150","R10":"25","R05":"0.01","R08":"1"}},{"code":"H25","branch":"CN02","account":"C01","source":"TABLE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P01","quantity":4,"toppings":[],"refund":null,"voucher":null,"session":"S_B_OLD","subtotal":"128000","discount":"0","payable":"128000","snapshot":{"B01":"600","R04":"0.12","R05":"0.08","R08":"4"}},{"code":"H26","branch":"CN02","account":"C02","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P02","quantity":3,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"102000","discount":"0","payable":"102000","snapshot":{"B02":"450","R04":"0.09","R05":"0.06","R08":"3"}},{"code":"H27","branch":"CN02","account":"C03","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P03","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"72000","discount":"0","payable":"72000","snapshot":{"B03":"300","R04":"0.06","R05":"0.04","R08":"2"}},{"code":"H28","branch":"CN02","account":"C04","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P04","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"39000","discount":"0","payable":"39000","snapshot":{"R12":"0.006","R13":"150","R04":"0.02","R05":"0.02","R08":"1"}},{"code":"H29","branch":"CN02","account":"C05","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"CASH","product":"P05","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"76000","discount":"0","payable":"76000","snapshot":{"B01":"200","R14":"0.016","R04":"0.06","R05":"0.04","R08":"2"}},{"code":"H30","branch":"CN02","account":"C06","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P06","quantity":3,"toppings":[],"refund":null,"voucher":"D26_USED","session":null,"subtotal":"105000","discount":"5000","payable":"100000","snapshot":{"B01":"360","R04":"0.09","R09":"60","R05":"0.03","R08":"3"}},{"code":"H31","branch":"CN02","account":"C01","source":"ONLINE","status":"HOAN_THANH","paid":false,"method":null,"product":"P12","quantity":2,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"60000","discount":"0","payable":"60000","snapshot":{"B01":"300","R09":"50","R05":"0.02","R08":"2"}},{"code":"H32","branch":"CN01","account":"C02","source":"ONLINE","status":"HOAN_THANH","paid":true,"method":"BANK_TRANSFER","product":"P01L","quantity":2,"toppings":["T01"],"refund":null,"voucher":"D26_10","session":null,"subtotal":"90000","discount":"9000","payable":"81000","snapshot":{"B01":"390","R04":"0.078","R05":"0.052","R08":"2","B04":"100"}},{"code":"H33","branch":"CN01","account":"C03","source":"ONLINE","status":"DA_HUY","paid":true,"method":"BANK_TRANSFER","product":"P02","quantity":2,"toppings":[],"refund":"DA_HOAN","voucher":"D26_CN01","session":null,"subtotal":"68000","discount":"10000","payable":"58000","snapshot":{"B02":"300","R04":"0.06","R05":"0.04","R08":"2"}},{"code":"H34","branch":"CN02","account":"C04","source":"ONLINE","status":"DA_HUY","paid":true,"method":"CASH","product":"P03","quantity":2,"toppings":[],"refund":"DA_HOAN","voucher":null,"session":null,"subtotal":"72000","discount":"0","payable":"72000","snapshot":{"B03":"300","R04":"0.06","R05":"0.04","R08":"2"}},{"code":"H35","branch":"CN01","account":"C05","source":"ONLINE","status":"DA_HUY","paid":true,"method":"BANK_TRANSFER","product":"P04","quantity":2,"toppings":[],"refund":"CHO_DUYET","voucher":null,"session":null,"subtotal":"78000","discount":"0","payable":"78000","snapshot":{"R12":"0.012","R13":"300","R04":"0.04","R05":"0.04","R08":"2"}},{"code":"H36","branch":"CN02","account":"C06","source":"ONLINE","status":"DA_HUY","paid":false,"method":null,"product":"P05","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"38000","discount":"0","payable":"38000","snapshot":{"B01":"100","R14":"0.008","R04":"0.03","R05":"0.02","R08":"1"}},{"code":"L01","branch":"CN01","account":"C01","source":"TABLE","status":"CHO_XAC_NHAN","paid":false,"method":null,"product":"P01","quantity":2,"toppings":["T01"],"refund":null,"voucher":null,"session":"S_A_OPEN","subtotal":"74000","discount":"0","payable":"74000","snapshot":{"B01":"300","R04":"0.06","R05":"0.04","R08":"2","B04":"100"}},{"code":"L02","branch":"CN01","account":"C02","source":"TABLE","status":"CHO_CHE_BIEN","paid":true,"method":"CASH","product":"P12","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":"S_A_OPEN","subtotal":"30000","discount":"0","payable":"30000","snapshot":{"B01":"150","R09":"25","R05":"0.01","R08":"1"}},{"code":"L03","branch":"CN02","account":"C03","source":"TABLE","status":"DANG_CHE_BIEN","paid":false,"method":null,"product":"P02","quantity":2,"toppings":["T01"],"refund":null,"voucher":null,"session":"S_B_OPEN","subtotal":"78000","discount":"0","payable":"78000","snapshot":{"B02":"300","R04":"0.06","R05":"0.04","R08":"2","B04":"100"}},{"code":"L04","branch":"CN02","account":"C04","source":"ONLINE","status":"CHO_XAC_NHAN","paid":false,"method":null,"product":"P03","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"payment_notice":"BANK_TRANSFER","subtotal":"36000","discount":"0","payable":"36000","snapshot":{"B03":"150","R04":"0.03","R05":"0.02","R08":"1"}},{"code":"L05","branch":"CN01","account":"TN_CN01","source":"COUNTER","status":"CHO_THANH_TOAN","paid":false,"method":null,"product":"P01","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"32000","discount":"0","payable":"32000","snapshot":{"B01":"150","R04":"0.03","R05":"0.02","R08":"1"}},{"code":"L06","branch":"CN02","account":"C06","source":"ONLINE","status":"CHO_CHE_BIEN","paid":true,"method":"BANK_TRANSFER","product":"P04","quantity":2,"toppings":[],"refund":null,"voucher":"D26_10","session":null,"subtotal":"78000","discount":"7800","payable":"70200","snapshot":{"R12":"0.012","R13":"300","R04":"0.04","R05":"0.04","R08":"2"}},{"code":"L07","branch":"CN01","account":"C05","source":"ONLINE","status":"CHO_XAC_NHAN","paid":false,"method":null,"product":"P22","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"34000","discount":"0","payable":"34000","snapshot":{"B03":"150","R09":"25","R05":"0.01","R08":"1"}},{"code":"L08","branch":"CN02","account":"C06","source":"ONLINE","status":"CHO_XAC_NHAN","paid":false,"method":null,"product":"P01","quantity":1,"toppings":[],"refund":null,"voucher":null,"session":null,"subtotal":"32000","discount":"0","payable":"32000","snapshot":{"B01":"150","R04":"0.03","R05":"0.02","R08":"1"}}],"shift":{"code":"KC_CN01_01","branch":"CN01","material":"B01","book":"7020","actual":"7020","discarded":"100","retained":"6920","cost":"267.78"},"reviews":[{"code":"DG01","order":"H01","account":"C01","product":"P01","branch":"CN01","rating":5,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG02","order":"H02","account":"C02","product":"P02","branch":"CN01","rating":4,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG03","order":"H03","account":"C03","product":"P03","branch":"CN01","rating":5,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG04","order":"H04","account":"C04","product":"P04","branch":"CN01","rating":4,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG05","order":"H05","account":"C05","product":"P05","branch":"CN01","rating":4,"hidden":true,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG06","order":"H06","account":"C06","product":"P06","branch":"CN01","rating":5,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG07","order":"H07","account":"C01","product":"P07","branch":"CN01","rating":4,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG08","order":"H08","account":"C02","product":"P08","branch":"CN01","rating":4,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG09","order":"H25","account":"C01","product":"P01","branch":"CN02","rating":5,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG10","order":"H26","account":"C02","product":"P02","branch":"CN02","rating":4,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG11","order":"H27","account":"C03","product":"P03","branch":"CN02","rating":5,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG12","order":"H28","account":"C04","product":"P04","branch":"CN02","rating":4,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG13","order":"H29","account":"C05","product":"P05","branch":"CN02","rating":4,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG14","order":"H30","account":"C06","product":"P06","branch":"CN02","rating":5,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."},{"code":"DG15","order":"H32","account":"C02","product":"P01L","branch":"CN01","rating":5,"hidden":false,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này."}],"favorites":[{"account":"C01","product":"P01"},{"account":"C02","product":"P01"},{"account":"C03","product":"P01"},{"account":"C04","product":"P01"},{"account":"C05","product":"P01"},{"account":"C06","product":"P01"},{"account":"C01","product":"P02"},{"account":"C02","product":"P02"},{"account":"C03","product":"P02"},{"account":"C04","product":"P02"},{"account":"C05","product":"P02"},{"account":"C01","product":"P03"},{"account":"C02","product":"P03"},{"account":"C03","product":"P03"},{"account":"C04","product":"P03"},{"account":"C01","product":"P12"},{"account":"C02","product":"P12"},{"account":"C03","product":"P12"},{"account":"C01","product":"P04"},{"account":"C02","product":"P04"},{"account":"C01","product":"P22"}]}
```

## 13. Ánh xạ fixture sang database hiện có

| Fixture | Bảng/cột hiện có | Quy tắc |
|---|---|---|
| branches.code | ChiNhanh.MaDinhDanh | Lấy MaCN trả về, không hardcode |
| accounts | TaiKhoan + NhanVienChiNhanh | Role, active, verified, TokenVersion; BCrypt |
| categories/products | DanhMuc, MonUong, MonUongChiNhanh | TenMon+Size unique; mapping branch đủ |
| materials | NguyenLieu | TenNL unique, Loai, DonVi; giữ tên gắn mã alias trong manifest |
| product.recipe | CongThucMon | Một dòng/Mon–NL, định lượng dương |
| preparation_recipes | CongThucSoChe + ChiTietCTSoChe | Sản lượng chuẩn và đầu vào chuẩn |
| stocks0 | TonNguyenLieu | PK MaCN–MaNL–ViTri, số dư0, version0 |
| PN_TONG_01/PN_CNxx_01 | PhieuNhap + ChiTietNhap | MaCN, nguồn, trạng thái, key, thời điểm; chuyển hai vế |
| ME_* | MeSoChe + ChiTietMeSoChe | MaCN bắt buộc, cost/quantity, định mức/thực dùng |
| sessions/tables | Ban + PhienBan | MaPhienDangMo phải trỏ đúng phiên OPEN của chính bàn |
| open session cart | GioPhien + ChiTietGioPhien | Chỉ phiên OPEN; version1; quyền phiên |
| orders | DonHang + ChiTietDonHang + ChiTietDonHangTopping | Nguồn, owner, MaCN, trạng thái; giá snapshot |
| orders.snapshot | DinhMucDonHang | Đủ raw và SOCHE, LoaiChot/DonViChot |
| invoice/payment | HoaDon + ThanhToan | PhaiTra=TongTien−TienGiam−TienGiamVoucher trong baseline |
| payment_notice L04 | ThongBaoThanhToan | Chưa tạo ThanhToan; giữ bằng chứng thông báo riêng |
| refund/cancellation | YeuCauHuy + YeuCauHoan | Chính xác order/payment, lý do và thực chi; không hoàn kho |
| voucher/history | Voucher, VoucherChiNhanh, LichSuSuDungVoucher | UsedCount khớp trạng thái lịch sử |
| reviews/media | DanhGia + DanhGiaMedia | Customer/Mon/CN cùng nguồn orderItem; unique dòng đơn |
| favorites | SanPhamYeuThich | Một khách/món theo policy family/variant |
| ledger | LichSuKho | MaCN, trước/delta/sau, loại nguồn và ID thật |
| shift | KetCaKho + ChiTietKetCaKho | MaCN, lượng/chi phí; có bút toán hao hụt |
| business events | NhatKyNghiepVu | Người/thời điểm/đối tượng/trước/sau/lý do; không giả chứng từ thật |
| retry key | BanGhiChongLap | Ghi khi gọi mutation thực; seed không tự giả trạng thái request đang xử lý |

### 13.1. Giỏ mẫu khi còn phiên

S_A_OPEN: giỏ chưa gửi có P03×1 +T02; tổng40.000đ. S_B_OPEN: P12×1, tổng30.000đ. Đây là giỏ tạm, **không** tạo hóa đơn, khoản thu hay tiêu hao; không cộng vào báo cáo44 đơn. Hai phiên CLOSED không còn row giỏ hoặc token còn hiệu lực.

Đơn L01/L02 chia sẻ phiên nhưng có khách sở hữu riêng. Sau khi sửa policy cần xác định khách có quyền xem chung giỏ/bill bàn đến đâu; không mặc định sở hữu mọi đơn cá nhân ngoài phiên. Người C07 chưa verified không login được; C08 bị khóa không login được, kể cả biết mật khẩu.

## 14. Kịch bản demo end-to-end

| Bước | Actor | Thao tác | Quan sát cần có |
|---|---|---|---|
| 1 | Admin | Xem catalog/nguyên liệu/công thức |30 product/variant,18NL,4CT sơ chế, mỗi món đủ NL |
| 2 | Admin | Xem nhập tổng, chứng từ cấp cơ sở |14dòng nhập; hai vế cấp CN; tồn tổng có nguồn |
| 3 | Kitchen CN01/CN02 | Xem8mẻ và kho đúng cơ sở | Không NonUniqueResultException, không MaCN NULL |
| 4 | Guest | Mở trang/QR Bàn03 | >10tên món; tên CN/bàn; chưa tạo phiên |
| 5 | Guest/User | Tìm tên, lọc DM/giá, xem4danh sách | Network đi search API; scope và ranking đúng; tối đa20 |
| 6 | Customer C01 | Đặt1P01+T01 từ Bàn03CN01 | Phiên mới và1đơn, subtotal37.000; snapshot đủ B01/R04/R05/R08/B04 |
| 7 | Cashier CN01 | Xác nhận; ghi thu CASH37.000 | CHO_CHE_BIEN; một Payment; CN02 không thấy thông báo |
| 8 | Kitchen CN01 | Start rồi Complete | Trừ150mlB01,0.03kgR04,0.02kgR05,1lyR08,50gB04;5dòng ledger |
| 9 | C01 | Review >=50ký tự và favorite | Review đúng orderItem, paid+complete; trùng bị chặn |
| 10 | Cashier CN01 | Đóng phiên Bàn03 | Token cũ từ chối, bàn TRONG; lịch sử mua còn |
| 11 | Customer/Cashier | Dùng đơn hủy mẫu H33/H34/H35 | DA_HOAN khác CHO_DUYET; đối soát đúng tiền |
| 12 | Manager/Admin | Báo cáo CN01/CN02/chuỗi | Khớp baseline + delta riêng của đơn demo mới |

Sau bước6–10 dữ liệu đã thay đổi so với baseline: thêm1đơn,1Payment37.000,1review tùy chọn,5dòng tiêu hao,1phiên CLOSED. Ghi riêng delta và đối soát mới; không dùng số dư baseline cũ để báo sai. Để demo lại, dùng một bản sao fixture sạch, không reset DB thật.

Luồng online cần kiểm UI sau sửa F13: chọn CN02 và nhận tại quầy, gọi ONLINE; COUNTER chỉ cashier. Luồng shipper/cổng thanh toán ở mục17, chưa thể demo thật từ source hiện tại.

## 15. Ca kiểm thử thành công, từ chối và tranh chấp

| ID | Dữ liệu/thao tác | Kết quả phải đạt |
|---|---|---|
| AT01 | C07/C08 đăng nhập | Chưa xác thực hoặc bị khóa: từ chối, không cấp JWT |
| AT02 | Đăng ký email mới có SMTP thật → OTP đúng | Xác thực rồi mới login; role ADMIN do client giả bị bỏ qua |
| AT03 | Nhập sai OTP 5 lần; đọc DB từ transaction khác | Attempts=5, revoked; lần 6 không được tiếp tục thử |
| AT04 | OTP hết hạn/đã dùng; gửi lại quá sớm | Từ chối đúng lý do; gửi lại hợp lệ làm mã cũ mất hiệu lực |
| AT05 | Reset mật khẩu rồi dùng JWT cũ | JWT cũ bị từ chối; mật khẩu mới đăng nhập được |
| AT06 | Anonymous đọc/đổi giỏ S_A_OPEN | 401/403; không đổi version/dòng giỏ |
| AT07 | C06 không có quyền phiên A đọc/đổi giỏ | 403; không cho qua chỉ vì mang vai trò CUSTOMER |
| AT08 | Gửi thêm đơn chỉ với tableId/sessionId của người khác | Từ chối vì thiếu quyền phiên |
| AT09 | Hai request đơn đầu Bàn03 đồng thời | Chỉ một phiên OPEN; không tạo hai lượt khách tách biệt |
| AT10 | Cashier CN01 confirm/pay/cancel/refund đơn CN02 | 403; không thay dữ liệu hoặc ghi audit thành công |
| AT11 | Kitchen CN01 start/complete L03 ở CN02 | 403; kho CN02 không đổi |
| AT12 | Kitchen CN01 GET stock của CN02 hoặc bỏ branchId | Chỉ trả CN01 hoặc từ chối; không trả toàn chuỗi |
| AT13 | Manager CN01 sửa category.active/allowTopping toàn chuỗi | 403 theo chính sách cấu hình tập trung |
| AT14 | Assignment bị vô hiệu nhưng JWT/WebSocket còn mở | HTTP/STOMP và subscription cũ mất quyền tương ứng |
| AT15 | Customer subscribe phiên khác; staff subscribe topic global | Bị chặn; không nhận sự kiện cơ sở khác |
| AT16 | POST ONLINE vào KHOTONG/cơ sở đóng | Từ chối; không sinh đơn, invoice hoặc token |
| AT17 | Gửi trực tiếp productId P24/P23 ở CN01 | Từ chối món không bán/tạm hết; CN02 vẫn mua được |
| AT18 | Chọn đồ uống P01 làm topping | Từ chối sai loại; topping trùng hoặc không bán cũng bị chặn |
| AT19 | Món thiếu recipe hoặc recipe thiếu nguyên liệu | Không kích hoạt bán hoặc checkout bị chặn; không lưu snapshot thiếu |
| AT20 | Sửa recipe P01 sau khi tạo đơn | Đơn cũ giữ snapshot 150ml B01; đơn mới dùng định mức mới |
| AT21 | Thiếu một nguyên liệu khi hoàn thành | 409; mọi tồn khác và trạng thái đơn không đổi |
| AT22 | Complete hai lần hoặc đồng thời | Chỉ một lần tiêu hao; không nhân lịch sử hoặc âm kho |
| AT23 | Thu hai lần cùng invoice | Một Payment; cùng key/payload trả lại kết quả; đổi payload trả 409 |
| AT24 | Nhập/chuyển kho retry cùng key | Không ghi hai lần; cùng key khác payload trả 409 |
| AT25 | Hai chi nhánh cùng lấy lượng tồn cuối kho tổng | Tổng xuất không vượt tồn; transaction thiếu hàng bị từ chối |
| AT26 | Cùng key đơn nhưng đổi voucher/topping/địa chỉ | 409; không trả nhầm kết quả cũ hoặc tạo giao dịch mới |
| AT27 | Hai khách tranh D26_LAST còn một lượt | Chỉ một thành công; history và usedCount khớp |
| AT28 | Một khách gửi hai request với limit cá nhân=1 | Không vượt hạn mức, dù tổng lượt voucher còn nhiều |
| AT29 | D26_CN01 ở CN02; mã hết hạn/chưa bắt đầu/tắt/hết lượt/min | Mỗi điều kiện bị chặn backend, không chỉ vô hiệu nút UI |
| AT30 | Guest dùng mã giới hạn theo khách | Yêu cầu đăng nhập hoặc có quota Guest rõ ràng; không bỏ qua âm thầm |
| AT31 | Đơn đã thu ở CHO_XAC_NHAN rồi hủy | Giữ khoản thu và có nghĩa vụ hoàn; không mất tiền đã ghi |
| AT32 | Hủy L03 đang pha chế | Bị chặn theo quy tắc chỉ hủy trước khi pha chế |
| AT33 | Duyệt/thực hoàn H35 rồi retry | Chỉ chi 78.000 một lần; thu ròng giảm đúng kỳ thực chi |
| AT34 | Yêu cầu hoàn ngân hàng lại sau TU_CHOI | Theo chính sách revision/update; không lỗi unique 500 |
| AT35 | Cancellation ID khác Order ID | Không hoàn nhầm đơn; endpoint định danh rõ |
| AT36 | Đóng S_A_OPEN/S_B_OPEN trước khi xong hoặc trả tiền | 409; token/giỏ chưa bị thu hồi |
| AT37 | Đóng phiên đã hoàn tất và tất toán | Token/giỏ hết quyền; giữ order/payment/audit |
| AT38 | Review H31 chưa thu, L06 chưa hoàn thành, đơn người khác | Bị chặn từng trường hợp |
| AT39 | Review 49 ký tự, 6 sao, trùng dòng đơn, hoặc Guest | Bị chặn ở backend và constraint DB tương ứng |
| AT40 | Review hợp lệ rồi ẩn DG05 | Avg/count không tính review ẩn; moderator cơ sở khác bị từ chối |
| AT41 | Media MIME giả/file quá lớn/URL ngoài chính sách | Bị chặn qua endpoint thật; không chỉ gọi validator riêng |
| AT42 | Kiểm kê book=100, actual=90, discarded=10, retained=80 | Tồn cuối 80 với −10 điều chỉnh và −10 hao hụt, có lý do |
| AT43 | Quantity 0/âm/null; dòng nhập trùng; note quá dài | Validation 4xx; không ghi một phần |
| AT44 | Type/branch của featured và API route Admin | Đúng bốn danh sách/scope/ranking; route Admin trả 200 cho role đúng |
| AT45 | Client sửa giá/invoice/payable | Server tính giá từ DB và snapshot; không thu sai theo client |
| AT46 | Đơn 0đ với D26_ZERO | Tất toán 0 hợp lệ; review/ranking theo policy; không mắc kẹt chờ tiền |
| AT47 | Mutation có cookie nhưng thiếu/sai CSRF | 403; CSRF thật hợp lệ thì hoạt động |
| AT48 | Backup và restore database demo | Schema/count/sổ/tiền khớp; app khởi động validate được |

Đây là **kết quả kỳ vọng**, chưa phải kết quả đã chạy. Khi thực thi thêm cột Actual, PASS/FAIL, bằng chứng và commit. Kiểm thử tranh chấp phải dùng các kết nối transaction thật; gọi mock tuần tự không chứng minh khóa SQL Server.

### 15.1. Công thức kiểm tiền riêng

Giỏ 2×P01 + 2×T01 = 74.000đ. Giảm cửa hàng 10% là 7.400đ, còn 66.600đ. Nếu voucher 10% tính trên subtotal gốc, tiền voucher là 7.400đ và cuối cùng 59.200đ. Nếu voucher tính sau giảm cửa hàng, tiền voucher là 6.660đ và cuối cùng 59.940đ. Source hiện tính voucher theo subtotal gốc. Nhóm cần công bố một chính sách và dùng chung ở preview/checkout.

Đơn 0đ: 1×P01 = 32.000đ; D26_ZERO giảm thực tế 32.000đ; phải trả 0đ. Không tạo khoản thu dương giả; đơn không được mắc kẹt chờ tiền hoặc mất quyền đánh giá chỉ vì không có Payment.

### 15.2. Thiếu kho nhưng vẫn giữ lịch sử hợp lệ

Tạo bản sao fixture. Ghi chứng từ loại bỏ toàn bộ B04 còn lại tại CN02 với lý do “nguyên liệu quá hạn — bài test” và chi phí; không UPDATE Stock trực tiếp. Tạo đơn P02+T01, start rồi complete phải trả 409 vì B04=0. Các thành phần khác không được trừ. Đây là fixture âm có chủ đích, tách khỏi bộ số dư baseline.

## 16. SQL Server: truy vấn đối soát chỉ đọc

Chạy trên đúng DB demo đã ánh xạ dữ liệu. Các truy vấn không nạp hay sửa DB. Nếu database có nhiều đợt test, giới hạn theo manifest/fixture trước khi so các tổng baseline; không mặc định toàn DB chỉ có 44 đơn. Các query về MaCN yêu cầu đã áp dụng migration chuỗi.

```sql
-- Q01: Phát hiện tồn âm / thiếu scope (kết quả rỗng).
SELECT * FROM dbo.TonNguyenLieu WHERE SoLuong < 0 OR MaCN IS NULL;
SELECT * FROM dbo.LichSuKho WHERE MaCN IS NULL;
SELECT * FROM dbo.MeSoChe WHERE MaCN IS NULL;
SELECT * FROM dbo.KetCaKho WHERE MaCN IS NULL;

-- Q02: Phương trình mỗi dòng sổ (rỗng).
SELECT * FROM dbo.LichSuKho
WHERE ABS(TonSau - (TonTruoc + LuongBienDong)) > 0.0005;

-- Q03: Tồn = tổng sổ khi fixture bắt đầu0 (rỗng).
WITH s AS (
 SELECT MaCN,MaNL,ViTri,SUM(LuongBienDong) Luong
 FROM dbo.LichSuKho GROUP BY MaCN,MaNL,ViTri
)
SELECT COALESCE(t.MaCN,s.MaCN) MaCN, COALESCE(t.MaNL,s.MaNL) MaNL,
       COALESCE(t.ViTri,s.ViTri) ViTri, t.SoLuong, s.Luong
FROM dbo.TonNguyenLieu t FULL JOIN s
 ON s.MaCN=t.MaCN AND s.MaNL=t.MaNL AND s.ViTri=t.ViTri
WHERE t.MaCN IS NULL OR ABS(COALESCE(t.SoLuong,0)-COALESCE(s.Luong,0))>0.0005;

-- Q04: Sổ liên tục theo ID tăng (fixture yêu cầu thứ tự ghi sổ tuần tự).
WITH x AS (
 SELECT *, LAG(TonSau) OVER(PARTITION BY MaCN,MaNL,ViTri ORDER BY MaBienDong) TruocDo
 FROM dbo.LichSuKho
)
SELECT * FROM x WHERE TruocDo IS NOT NULL AND ABS(TonTruoc-TruocDo)>0.0005;

-- Q05: Món active thiếu công thức (rỗng với30món của fixture).
SELECT m.MaMon,m.TenMon,m.Size FROM dbo.MonUong m
WHERE m.HoatDong=1 AND NOT EXISTS
 (SELECT 1 FROM dbo.CongThucMon c WHERE c.MaMon=m.MaMon);

-- Q06: Thành phần món kinh doanh chưa có row tồn theo policy raw/KHO,soche/BEP.
SELECT cn.MaCN,m.MaMon,c.MaNL,nl.TenNL FROM dbo.MonUongChiNhanh cn
JOIN dbo.MonUong m ON m.MaMon=cn.MaMon
JOIN dbo.CongThucMon c ON c.MaMon=m.MaMon
JOIN dbo.NguyenLieu nl ON nl.MaNL=c.MaNL
WHERE cn.KinhDoanh=1 AND NOT EXISTS (
 SELECT 1 FROM dbo.TonNguyenLieu t WHERE t.MaCN=cn.MaCN AND t.MaNL=c.MaNL
 AND t.ViTri=CASE WHEN nl.Loai='SOCHE' THEN 'BEP' ELSE 'KHO' END
);

-- Q07: Raw thiếu lịch sử nhập NCC tại kho tổng (rỗng).
SELECT nl.* FROM dbo.NguyenLieu nl WHERE nl.Loai='THO' AND NOT EXISTS (
 SELECT 1 FROM dbo.ChiTietNhap ct JOIN dbo.PhieuNhap pn ON pn.MaPhieu=ct.MaPhieu
 JOIN dbo.ChiNhanh cn ON cn.MaCN=pn.MaCN
 WHERE ct.MaNL=nl.MaNL AND pn.TrangThai='DA_GHI_SO' AND cn.MaDinhDanh='KHOTONG'
);

-- Q08: Thành phẩm được sử dụng nhưng không có mẻ tại cơ sở đó (rỗng).
SELECT DISTINCT d.MaCN,s.MaNL FROM dbo.DinhMucDonHang s
JOIN dbo.DonHang d ON d.MaDH=s.MaDH
WHERE s.LoaiChot='SOCHE' AND NOT EXISTS (
 SELECT 1 FROM dbo.MeSoChe me WHERE me.MaCN=d.MaCN AND me.MaNLDauRa=s.MaNL
 AND me.TrangThai='DA_GHI_SO'
);

-- Q09: Tiêu hao khớp snapshot của đơn hoàn tất; phát hiện cả dòng thừa (rỗng).
WITH expected AS (
 SELECT d.MaCN,s.MaDH,s.MaNL,SUM(s.TongLuong) SL FROM dbo.DinhMucDonHang s
 JOIN dbo.DonHang d ON d.MaDH=s.MaDH WHERE d.TrangThai='HOAN_THANH'
 GROUP BY d.MaCN,s.MaDH,s.MaNL
), actual AS (
 SELECT MaCN,NguonMa MaDH,MaNL,-SUM(LuongBienDong) SL FROM dbo.LichSuKho
 WHERE NguonLoai='ORDER' AND Loai='TIEU_HAO' GROUP BY MaCN,NguonMa,MaNL
)
SELECT COALESCE(e.MaCN,a.MaCN) MaCN,COALESCE(e.MaDH,a.MaDH) MaDH,
 COALESCE(e.MaNL,a.MaNL) MaNL,e.SL Can,a.SL DaTru
FROM expected e FULL JOIN actual a ON a.MaCN=e.MaCN AND a.MaDH=e.MaDH AND a.MaNL=e.MaNL
WHERE e.MaDH IS NULL OR a.MaDH IS NULL OR ABS(e.SL-a.SL)>0.0005;

-- Q10: Đơn/phiên khác cơ sở hoặc nhiều OPEN/table (rỗng).
SELECT d.MaDH,d.MaCN,b.MaCN MaCNBan FROM dbo.DonHang d
JOIN dbo.PhienBan p ON p.MaPhien=d.MaPhien JOIN dbo.Ban b ON b.MaBan=p.MaBan
WHERE d.MaCN<>b.MaCN;
SELECT MaBan,COUNT(*) SoPhien FROM dbo.PhienBan WHERE TrangThai='OPEN'
GROUP BY MaBan HAVING COUNT(*)>1;
SELECT b.MaBan,b.MaPhienDangMo,p.MaBan MaBanPhien,p.TrangThai
FROM dbo.Ban b LEFT JOIN dbo.PhienBan p ON p.MaPhien=b.MaPhienDangMo
WHERE b.MaPhienDangMo IS NOT NULL AND (p.MaPhien IS NULL OR p.MaBan<>b.MaBan OR p.TrangThai<>'OPEN');

-- Q11: Đối soát tiền không nhân bản do JOIN nhiều-nhiều.
WITH thu AS (
 SELECT d.MaCN,SUM(t.SoTien) Tien FROM dbo.ThanhToan t
 JOIN dbo.HoaDon h ON h.MaHD=t.MaHD JOIN dbo.DonHang d ON d.MaDH=h.MaDH GROUP BY d.MaCN
), hoan AS (
 SELECT d.MaCN,SUM(r.SoTien) Tien FROM dbo.YeuCauHoan r JOIN dbo.DonHang d ON d.MaDH=r.MaDH
 WHERE r.TrangThai='DA_HOAN' GROUP BY d.MaCN
)
SELECT cn.MaDinhDanh,COALESCE(thu.Tien,0) Thu,COALESCE(hoan.Tien,0) Hoan,
 COALESCE(thu.Tien,0)-COALESCE(hoan.Tien,0) ThuRong
FROM dbo.ChiNhanh cn LEFT JOIN thu ON thu.MaCN=cn.MaCN LEFT JOIN hoan ON hoan.MaCN=cn.MaCN
WHERE cn.MaDinhDanh IN ('CN01','CN02');

-- Q12: Hóa đơn/phải thu không khớp công thức baseline (rỗng).
SELECT * FROM dbo.HoaDon WHERE ABS(PhaiTra-(TongTien-TienGiam-TienGiamVoucher))>0.01;
SELECT t.MaTT,t.SoTien,h.PhaiTra FROM dbo.ThanhToan t JOIN dbo.HoaDon h ON h.MaHD=t.MaHD
WHERE ABS(t.SoTien-h.PhaiTra)>0.01;

-- Q13: Đơn hủy đãthu chưa thực hoàn (phải thấy H35 sau ánh xạ).
SELECT d.MaDH,d.MaCN,t.SoTien,r.TrangThai FROM dbo.DonHang d
JOIN dbo.HoaDon h ON h.MaDH=d.MaDH JOIN dbo.ThanhToan t ON t.MaHD=h.MaHD
LEFT JOIN dbo.YeuCauHoan r ON r.MaDH=d.MaDH
WHERE d.TrangThai='DA_HUY' AND (r.MaHoan IS NULL OR r.TrangThai<>'DA_HOAN');

-- Q14: Review có nguồn sai (rỗng).
SELECT g.MaDanhGia FROM dbo.DanhGia g JOIN dbo.ChiTietDonHang ct ON ct.MaChiTiet=g.MaChiTiet
JOIN dbo.DonHang d ON d.MaDH=ct.MaDH
WHERE d.MaTK IS NULL OR g.MaTK<>d.MaTK OR g.MaMon<>ct.MaMon OR g.MaCN<>d.MaCN
 OR d.TrangThai<>'HOAN_THANH' OR NOT EXISTS (
 SELECT 1 FROM dbo.HoaDon h JOIN dbo.ThanhToan t ON t.MaHD=h.MaHD WHERE h.MaDH=d.MaDH
);

-- Q15: Bộ đếm voucher khớp history DA_DUNG (rỗng).
SELECT v.MaCode,v.SoLuotDaDung,COUNT(h.MaLichSu) TuLichSu
FROM dbo.Voucher v LEFT JOIN dbo.LichSuSuDungVoucher h
 ON h.MaVoucher=v.MaVoucher AND h.TrangThai='DA_DUNG'
GROUP BY v.MaCode,v.SoLuotDaDung HAVING v.SoLuotDaDung<>COUNT(h.MaLichSu);

-- Q16: Nguồn chứng từ không tồn tại (rỗng với các loại nguồn của fixture).
SELECT k.* FROM dbo.LichSuKho k WHERE
 (k.NguonLoai IN ('IMPORT','CHUYEN_KHO','NHAP_TONG') AND NOT EXISTS
  (SELECT 1 FROM dbo.PhieuNhap pn WHERE pn.MaPhieu=k.NguonMa)) OR
 (k.NguonLoai='BATCH' AND NOT EXISTS(SELECT 1 FROM dbo.MeSoChe m WHERE m.MaMe=k.NguonMa)) OR
 (k.NguonLoai='ORDER' AND NOT EXISTS(SELECT 1 FROM dbo.DonHang d WHERE d.MaDH=k.NguonMa)) OR
 (k.NguonLoai='SHIFT_SETTLEMENT' AND NOT EXISTS(SELECT 1 FROM dbo.KetCaKho c WHERE c.MaKetCa=k.NguonMa));
```

Các truy vấn kiểm những bất biến chính, không thay thế FK/CHECK/transaction/API test. Q14 cần nâng cấp khái niệm “tất toán 0đ” khi triển khai đơn miễn phí. Cần kiểm nguồn phát sinh trước tiêu hao và đủ lượng; một phiếu nhập bất kỳ nhỏ hơn nhu cầu chưa phải bằng chứng đủ hàng.

## 17. Dữ liệu mở rộng theo rubric — CHƯA CÓ TRONG SCHEMA HIỆN TẠI

Các mục dưới đây là đề xuất. Cần triển khai migration/service/UI và kiểm quyền trước, sau đó nạp bằng API mới.

| Phần | Dữ liệu đề xuất | Điều kiện nghiệm thu |
|---|---|---|
| Sổ địa chỉ | C01: ADDR01 “Nhà”, ADDR02 “Trường”; C02: ADDR03 | Chỉ chủ sở hữu CRUD; một địa chỉ mặc định/tài khoản; sửa địa chỉ không đổi snapshot đơn |
| Giỏ tài khoản | C01/CN01: CART01=P01×2+T01; C02/CN02: CART02=P12×1 | Đổi thiết bị vẫn có giỏ; đổi chi nhánh kiểm lại; không trộn giỏ bàn với giỏ cá nhân |
| Đã xem | C01 xem P01 → P03 → P12 → P01 | Danh sách gần nhất P01,P12,P03; giới hạn 20; C02 không đọc được |
| Nhà vận chuyển | DV01 “Giao nội bộ”, phí 15.000; DV02 “Đối tác Demo”, phí 20.000 | Tính phí trên server, chốt snapshot; kiểm vùng phục vụ và cơ sở |
| Shipper | SH01 thuộc CN01, SH02 thuộc CN02 | Chỉ đơn được giao; không có quyền quầy/bếp; phân công và đổi người có audit |
| Đơn giao | SHD01 của C01/CN01: P01×2, phí 15.000, COD; giao SH01 | Hàng 64.000 + phí 15.000 = 79.000; READY mới nhận hàng; giao → thu COD → đối soát |
| Miễn phí giao | VC_SHIP giảm 15.000 phí giao của đơn trên | Tiền hàng vẫn 64.000; chỉ giảm phí giao; không âm |
| VNPAY/MOMO | PAY_SANDBOX01 cho đơn 32.000; thử callback lặp/sai chữ ký/sai tiền | Chỉ callback hợp lệ chuyển PAID; return URL không quyết định; không thu hai lần |
| Giao thất bại | SHD02 đã pha xong nhưng giao không thành | Tách trả hàng/hoàn tiền/hao hụt; không trả nguyên liệu đã pha vào kho |
| Family/variant | F01 gồm P01/P01L | Một favorite/family nếu chọn policy đó; có thống kê theo family và variant |
| Giá chi nhánh | Chỉ triển khai khi phạm vi được duyệt | Có lịch hiệu lực, giá server và snapshot; baseline hiện giữ giá chung |

Ví dụ địa chỉ giả lập để test form: ADDR01 “Khách Demo 01, 0900000001, 12 Đường Demo, Phường Demo, TP.HCM”; ADDR02 “Khách Demo 01, 0900000001, 1 Đường Trường Demo, Phường Demo, TP.HCM”. Khi nối nhà vận chuyển thật, thay bằng địa chỉ sandbox hợp lệ theo nhà cung cấp; không gửi đơn đến những địa chỉ giả này.

## 18. Bằng chứng đã kiểm cho bộ dữ liệu này

Trình sinh độc lập dùng Decimal đã kiểm: mọi công thức/snapshot có nguyên liệu; mọi nguyên liệu thô có nhập; thành phẩm có định mức; sổ liên tục và không âm; tổng sổ khớp tồn; chỉ đơn hoàn thành có tiêu hao, lượng khớp snapshot; review đúng người mua đã trả tiền; favorite không trùng; phép tính tiền/voucher không âm.

Đây là kiểm tra chất lượng fixture, **không phải test ứng dụng Java hoặc SQL Server**. Các truy vấn SQL ở mục 16 chưa được thực thi trên SQL Server trong lần đánh giá này.

Khi nhóm nhập dữ liệu, cần lưu manifest alias→ID thật, kết quả Q01–Q16, video E2E, phiên bản code/DDL và ngày giờ môi trường. Chỉ đánh PASS khi kết quả runtime thực tế khớp, kể cả những ca phải bị từ chối.

## 19. Payload mẫu khi nối kiểm thử HTTP

Thay các ký hiệu `$Ma...` bằng ID thật đã ánh xạ; các JSON dưới đây là mẫu payload, không phải giá trị literal gửi nguyên lên server. Lấy JWT, token phiên và CSRF từ ứng dụng sau khi sửa bảo mật. Gắn `Idempotency-Key` ổn định cho cùng một thao tác retry; thao tác mới dùng key mới.

### 19.1. Nhập kho tổng và cấp cơ sở

POST `/api/v1/inventory/imports?branchId=$MaKhoTong`, ADMIN:

```json
{"supplier":"NCC_DEMO_01","reason":"D26 PN_TONG_01 — nhập nguyên liệu demo","items":[{"materialId":"$MaR01","quantity":10,"unitPrice":120000},{"materialId":"$MaR04","quantity":40,"unitPrice":45000}]}
```

Đây là ví dụ hai dòng; khi nạp fixture dùng đủ 14 dòng theo mục 5 trong một phiếu. Cấp CN01 dùng cùng endpoint với branchId cơ sở, MANAGER đúng cơ sở; số lượng R01=1, R04=10 và các dòng còn lại theo bảng; giá vốn lấy server từ kho tổng. Source hiện chưa dùng key client cho nhập kho, nên kiểm AT24 sẽ cần sửa trước.

### 19.2. Mẻ sơ chế

POST `/api/v1/inventory/preparation-batches`, Kitchen CN01, key `D26_ME_CN01_SC01`:

```json
{"recipeId":"$MaSC01","recipeVersion":1,"desiredQuantity":9000,"note":"D26 — sơ chế cốt trà đen tại CN01"}
```

Chi nhánh phải lấy từ actor/ngữ cảnh được kiểm quyền; không cho client tự gửi ID để làm ở cơ sở khác. Source hiện thiếu branch ở đường sơ chế, cần sửa F04–F05.

### 19.3. Đơn bàn đầu tiên

POST `/api/v1/orders/table`, key `D26_LIVE_TABLE_001`:

```json
{"tableId":"$MaBan03CN01","expectedSessionId":null,"items":[{"productId":"$MaP01","quantity":1,"toppingIds":["$MaT01"],"note":"Ít ngọt"}],"remainingCartItems":[]}
```

Kỳ vọng 37.000đ; nhận orderId, invoiceId, sessionId và guestTableToken; giữ token ở client theo chính sách. Đơn tiếp theo cần token cùng phiên và expectedSessionId thật.

### 19.4. Đơn online và vòng trạng thái

POST `/api/v1/orders/online`, C01, key `D26_LIVE_ONLINE_001`:

```json
{"branchId":"$MaCN02","customerName":"Khách Demo 01","customerPhone":"0900000001","deliveryAddress":"Nhận tại quầy CN02","items":[{"productId":"$MaP03","quantity":1,"toppingIds":[]}],"note":"D26 — nhận tại quầy"}
```

Đây là cách dùng API hiện có cho đơn lấy tại quầy; field deliveryAddress đang là chuỗi ghi chú, cần nâng cấp DTO nhận hàng/địa chỉ như audit. Không dùng chuỗi này để thay thế sổ địa chỉ hoặc chứng minh có giao hàng.

Cashier đúng CN02 POST `/api/v1/cashier/orders/$MaDH/confirm`; Kitchen CN02 POST `/api/v1/kitchen/orders/$MaDH/start`, sau đó `/complete`. Thu tiền POST `/api/v1/cashier/orders/$MaDH/payments`:

```json
{"method":"CASH","reference":"D26_CASH_LIVE_001"}
```

Số tiền do server lấy từ hóa đơn; không có field amount do client quyết định. Mỗi request phải đi qua cùng policy chi nhánh; thay ID sang cơ sở khác phải bị chặn.

### 19.5. Review sau mua

POST `/api/v1/reviews`, C01 sau khi đơn ở 19.4 đã hoàn tất và thu tiền:

```json
{"orderItemId":"$MaChiTietP03TrongDonC01","rating":5,"content":"Đồ uống pha đúng vị, độ ngọt vừa phải, nguyên liệu thơm và phục vụ nhanh. Tôi hài lòng với trải nghiệm tại chi nhánh này.","mediaList":[]}
```

Mẫu request chỉ giúp chạy nhanh các thao tác; kết quả đúng phải được kiểm bằng response, query và số dư. Không ghi PASS từ việc request trả 200 nếu số tiền/kho/quyền bị sai.
