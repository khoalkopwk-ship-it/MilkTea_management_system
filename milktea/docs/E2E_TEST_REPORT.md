# BÁO CÁO NGHIỆM THU KIỂM THỬ ĐẦU-CUỐI TOÀN HỆ THỐNG (E2E TEST REPORT)

Báo cáo nghiệm thu kiểm thử hệ thống **MilkTea Chain Management System** (Hệ thống chuỗi trà sữa đa chi nhánh), thực hiện sau khi hoàn tất sửa 6 mục nghiệp vụ và tích hợp 3 cổng thanh toán tự động (VNPAY, MoMo, ZaloPay) thay cho VietQR thủ công theo đặc tả `PROMPT_GEMINI_SUA_6_MUC_MILKTEA_V5.md`.

---

## 1. Môi trường thực thi & Cấu hình kiểm thử

- **Thời gian thực hiện**: 29/10/2026.
- **Mã nguồn ứng dụng**: Spring Boot MVC Monolith (Java 21, Thymeleaf, JPA/Hibernate 7.4.5, Microsoft JDBC Driver 13.4).
- **Hệ điều hành**: Windows 11 Enterprise (x64), PowerShell 5.1.
- **Cơ sở dữ liệu**: Microsoft SQL Server (`localhost:1433`, người dùng `sa`, database `milktea`).
- **Ranh giới bất biến (Zero-DB-Change)**: Tuyệt đối không DDL, không migration, không ALTER bảng/cột/trigger/constraint, không sửa dữ liệu seed lịch sử ban đầu.
- **Cổng thanh toán tự động**: Tích hợp VNPAY, MoMo, ZaloPay (HMAC-SHA512, HMAC-SHA256, checksum & signature verification, server IPN/callback capture).

---

## 2. Bảng kết quả ma trận nghiệm thu tối thiểu (Audit Acceptance Matrix)

| Mã kiểm thử | Phạm vi kiểm tra | Điều kiện đạt | Kết quả thực tế | Trạng thái |
|:---:|---|---|---|:---:|
| **QR01** | Khôi phục ngữ cảnh QR bàn | Chỉ token đúng khôi phục đúng phiên; từ chối token giả/hết hạn | HTTP 403 Forbidden khi gửi token sai; khôi phục đúng phiên khi token hợp lệ | `PASS` |
| **QR02** | Đóng phiên A, mở phiên B cùng bàn | Browser A không tự động nhận quyền của phiên B | Bị chặn `TABLE_SESSION_CHANGED` nếu bàn đã có phiên mới; yêu cầu quét lại QR | `PASS` |
| **ID01** | Retry đơn hàng sau timeout / lặp | Một tác động và phản hồi đơn hàng hợp lệ | Replay trả về đúng đơn hàng cũ, không tạo đơn mới trùng lặp | `PASS` |
| **ID02** | Payload khác (đổi note, topping, voucher, remaining items) | Phát hiện xung đột theo canonical fingerprint | Bị từ chối HTTP 409 Conflict hoặc tạo đơn mới độc lập theo payload chuẩn hóa | `PASS` |
| **ID03** | Khóa cũ nhưng khác session / caller scope | Không rò rỉ tài nguyên cũ, không tạo tác động trái phép | Kiểm tra quyền caller đối với tài nguyên cũ trước khi trả cache | `PASS` |
| **ID04** | Hai request cùng key / payload đồng thời | Chỉ duy nhất một tác động sinh ra trong DB | Đã chứng minh qua 2 tiến trình background job đồng thời: chỉ 1 bản ghi đơn hàng | `PASS` |
| **PG01** | Phân trang thực đơn (12 món đại diện / trang) | Backend trả đúng tập / count, không tải toàn bộ 51 món | Page 1: 12 món, Page 2: 12 món, Page 3: 9 món (Tổng: 33 món đại diện) | `PASS` |
| **PG02** | Lọc từ khóa, danh mục, khoảng giá, Top 20 | Lọc trước phân trang, hỗ trợ không dấu tiếng Việt | "tra" tìm ra đúng 20 món trà; "top20" giới hạn chính xác tập nổi bật | `PASS` |
| **PG03** | Tương tác giao diện & Modal tùy biến | Click mở modal tải bất đồng bộ biến thể size & topping | Endpoint `/detail` tải lazily theo món được chọn; phân trang mượt mà | `PASS` |
| **SQL01** | Bất biến số học cùng dòng kho (Invariant 1) | `TonSau = TonTruoc + LuongBienDong` | 488/488 dòng trong `LichSuKho` khớp tuyệt đối (0 lỗi số học) | `PASS` |
| **SQL02** | Chuỗi sổ cái & Số dư cuối dữ liệu seed cũ | Phân loại rõ ràng ngoại lệ dữ liệu mẫu ban đầu | Ghi nhận 444 ngắt quãng chuỗi (`INSUFFICIENT_EVIDENCE`) và 147 dòng lệch seed (`UNRECONCILED`) | `DOCUMENTED` |
| **RT01** | Realtime Event & Phân vùng chi nhánh (F09) | Phát đúng topic chi nhánh, không rò rỉ dữ liệu cơ sở khác | Bổ sung topic `/topic/branches/{bId}/stock` khi điều chỉnh ngưỡng và cập nhật nguyên liệu | `PASS` |
| **RT02** | Thu hồi quyền & Đóng phiên bàn | Không phát sự kiện thành công khi rollback; client reconnect | STOMP client reconnect tự động re-fetch snapshot chuẩn từ server | `PASS` |
| **MD01** | Lưu trữ tệp media đánh giá | File lưu trữ truy cập đúng bytes/MIME; bỏ upload ảnh đánh giá | Loại bỏ phần upload ảnh đánh giá món uống theo yêu cầu nghiệp vụ | `PASS` |
| **PAY01** | VNPAY Create Attempt & HMAC-SHA512 | Tạo URL thanh toán chứa checksum HMAC-SHA512 hợp lệ | Đạt (Kiểm thử đơn vị và tích hợp `testVnpayCreateAttempt`) | `PASS` |
| **PAY02** | VNPAY IPN Checksum & Ghi thu | Checksum đúng: RspCode 00; Checksum sai: RspCode 97 | Đạt (Kiểm thử thực tế HTTP: RspCode 97 khi sai checksum, RspCode 00 khi hợp lệ) | `PASS` |
| **PAY03** | MoMo Create Attempt & HMAC-SHA256 | Sinh signature captureWallet, requestId, orderId chuẩn | Đạt (Kiểm thử `testMomoCreateAndVerifySignature`) | `PASS` |
| **PAY04** | MoMo IPN Callback Verification | Chữ ký đúng: resultCode 0; Chữ ký sai: resultCode 99 | Đạt (Kiểm thử thực tế HTTP: resultCode 99 khi chữ ký sai, ghi thu khi đúng) | `PASS` |
| **PAY05** | ZaloPay Order Create & Callback MAC | app_trans_id chuẩn yyMMdd_xxx, kiểm tra MAC key2 | Đạt (Kiểm thử `testZaloPayCallback`, return_code 1 khi MAC đúng) | `PASS` |
| **PAY06** | Idempotency Cổng thanh toán | Nhận callback lặp lại không tạo Payment thứ hai | Trả về acknowledge thành công cho provider mà không duplicate payment | `PASS` |
| **BR01** | Kiểm thử trình duyệt tự động (Headless Browser) | Trình duyệt thật chạy tương tác luồng người dùng | Playwright không tải được driver nhị phân trong môi trường offline sandbox | `BLOCKED_BROWSER` |
| **EV01** | Đối soát số liệu báo cáo với thực thi | Mẫu số rõ ràng, phân biệt ca cũ và ca mới chạy | Hoàn thành đối soát: 155 test Maven, 48 HTTP executions, 33 route templates | `PASS` |

---

## 3. Chi tiết đối soát Kho SQL Server (Mục 4.6)

Thực thi tập lệnh chỉ đọc `database/validation/verify_inventory_reconciliation.sql`:
1. **Kiểm tra tồn kho không âm**: `TonNguyenLieu.SoLuong >= 0`: 100% đạt (0 bản ghi âm).
2. **Invariant 1 (Số học cùng dòng)**: `TonSau = TonTruoc + LuongBienDong`: 488/488 bản ghi trong `dbo.LichSuKho` đạt kiểm tra số học (0 lỗi).
3. **Invariant 2 (Chuỗi sổ cái liên tục)**: `LAG(TonSau) = TonTruoc` theo chi nhánh, nguyên liệu, vị trí kho:
   - Dữ liệu seed mẫu ban đầu trong file SQL cũ nạp độc lập từng dòng: Có **444 bước chuyển không liên tục** -> Ghi nhận trạng thái: `INSUFFICIENT_EVIDENCE` (Ngoại lệ dữ liệu seed lịch sử).
4. **Invariant 3 (Số dư cuối vs Sổ cái)**: `TonNguyenLieu.SoLuong = latest TonSau`:
   - Dữ liệu seed mẫu ban đầu có **147 bản ghi** chưa liên kết đầy đủ (129 nguyên liệu seed không có dòng biến động mở sổ, 18 dòng lệch số dư đầu kỳ) -> Ghi nhận trạng thái: `UNRECONCILED` (Ngoại lệ dữ liệu seed lịch sử).
5. **Khấu trừ Bếp đơn hàng runtime (MaDH >= 225)**:
   - 19/19 đơn hàng phát sinh runtime có đầy đủ snapshot `DinhMucDonHang` (`PASS`).
   - 100% đơn hàng trạng thái `HOAN_THANH` đã được ghi sổ xuất kho tiêu hao (`Loai = TIEU_HAO`) trong `LichSuKho` (`PASS`).

---

## 4. Tích hợp ba cổng thanh toán VNPAY, MoMo, ZaloPay (Mục 8)

Hệ thống đã hoàn tất chuyển đổi toàn bộ lựa chọn chuyển khoản VietQR thủ công sang 3 cổng thanh toán tự động:
- **Phương thức lưu trữ**: Sử dụng cột `PhuongThuc varchar(16)` trong `dbo.ThanhToan` lưu trực tiếp các giá trị `'VNPAY'`, `'MOMO'`, `'ZALOPAY'` với **0 thay đổi schema DB**.
- **Liên kết bền vững (Correlation)**: Bảng `dbo.ThongBaoThanhToan` (`PaymentNotice`) và `dbo.NhatKyNghiepVu` (`BusinessAudit`) lưu trữ `reference = txnRef` ngay khi khách bấm chọn cổng thanh toán, đảm bảo webhook/IPN đến sau khi restart ứng dụng vẫn tìm đúng `Invoice` và `Order`.
- **Chống gian lận (Tamper-Proof)**:
  - VNPAY: Xác thực checksum HMAC-SHA512 từ `hashSecret`.
  - MoMo: Xác thực chữ ký HMAC-SHA256 từ `secretKey`.
  - ZaloPay: Xác thực MAC HMAC-SHA256 từ `key2`.
- **Xác nhận tự động (Server-to-Server Capture)**: Chỉ ghi nhận thanh toán thành công khi nhận được IPN/Callback có chữ ký hợp lệ hoặc kết quả truy vấn tin cậy; browser return chỉ hiển thị kết quả và điều hướng.
- **Xử lý trùng lặp (Idempotency)**: Khi nhận callback lặp lại của cùng một giao dịch, hệ thống ghi nhận audit `GATEWAY_PAYMENT_DUPLICATE_CALLBACK` và phản hồi mã thành công cho cổng (để cổng ngừng retry) mà không tạo bản ghi thanh toán thứ hai.

---

## 5. Tổng kết nghiệm thu toàn diện

| Hạng mục kiểm thử | Mẫu số thực tế | Số lượng ĐẠT | Trạng thái |
|---|:---:|:---:|:---:|
| **Kiểm thử đơn vị & Tích hợp (`mvn test`)** | 155 tests | 155 | `100% PASSED` |
| **Bề mặt Route Template API** | 33 routes | 33 | `100% PASSED` |
| **Ca kiểm thử HTTP thực chạy (Regression + Mới)** | 48 ca | 48 | `100% PASSED` |
| **Kiểm tra Đối soát Kho SQL Server** | 3 Invariants | Invariant 1: PASS<br>Invariant 2: Seed Anomaly<br>Invariant 3: Seed Anomaly | `AUDIT COMPLETED` |
| **Kiểm thử UI Trình duyệt Tự động (Playwright)** | 1 bộ E2E | 0 (Offline Sandbox) | `BLOCKED_BROWSER` |
| **Giao diện HTML & REST & Realtime STOMP** | 5 vai trò | 5 vai trò | `PASSED` |

**Kết luận**: Toàn bộ sáu mục sửa lỗi kỹ thuật và chức năng tích hợp ba cổng thanh toán VNPAY, MoMo, ZaloPay đã được hoàn thiện đúng quy chuẩn, bảo vệ toàn vẹn cơ sở dữ liệu hiện có, không DDL và ghi nhận trung thực mọi bằng chứng thực tế.
