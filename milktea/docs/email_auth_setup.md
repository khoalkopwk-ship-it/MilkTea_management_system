# Hướng Dẫn Thiết Lập & Vận Hành Xác Thực Email và Khôi Phục Mật Khẩu (MilkTea)

Tài liệu này hướng dẫn chi tiết cách cấu hình dịch vụ gửi email Gmail SMTP, thiết lập biến môi trường, khởi chạy ứng dụng và vận hành 3 luồng: **Đăng ký có xác thực email qua OTP 6 số**, **Đăng nhập với kiểm tra xác thực**, và **Quên/Đặt lại mật khẩu**.

---

## 1. Thiết Lập Mật Khẩu Ứng Dụng (App Password) Trên Tài Khoản Gmail

Theo chính sách bảo mật hiện hành của Google (loại bỏ xác thực cơ bản "Less Secure Apps" từ năm 2022), việc gửi email qua SMTP yêu cầu sử dụng **App Password (Mật khẩu ứng dụng)** kết hợp với **Xác minh 2 bước (2-Step Verification)**:

1. **Kiểm tra điều kiện tài khoản:**
   - Đăng nhập vào tài khoản Google cần dùng gửi mail (ví dụ: `khoalkopwk@gmail.com`).
   - Truy cập trang quản lý: [Google Account Security](https://myaccount.google.com/security).
   - Đảm bảo tính năng **Xác minh 2 bước (2-Step Verification)** đã được bật (`Bật / On`).
2. **Tạo Mật khẩu ứng dụng (App Password):**
   - Vào mục **Mật khẩu ứng dụng (App passwords)** hoặc tìm kiếm "App passwords" trên thanh tìm kiếm của trang tài khoản Google (URL trực tiếp: `https://myaccount.google.com/apppasswords`).
   - Nhập tên ứng dụng nhận diện: ví dụ `MilkTea System`.
   - Bấm **Tạo (Create)**. Google sẽ hiển thị một chuỗi ký tự ngẫu nhiên gồm 16 chữ cái (dạng `xxxx xxxx xxxx xxxx`).
   - **Lưu ý:** Loại bỏ khoảng trắng khi dùng làm giá trị biến môi trường (ví dụ: 16 ký tự liền nhau).
   - Tuyệt đối không chia sẻ mật khẩu ứng dụng này hoặc commit vào repository hay tài liệu.

---

## 2. File Cấu Hình & Biến Môi Trường Ứng Dụng Đọc

Ứng dụng MilkTea đọc cấu hình qua file `src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

app:
  mail:
    from: ${MAIL_FROM:${MAIL_USERNAME:khoalkopwk@gmail.com}}
    sender-name: ${MAIL_SENDER_NAME:MilkTea Cafe}
  otp:
    secret: ${OTP_SECRET:milktea_secure_otp_secret_key_2026_very_long_and_safe}
    expiry-minutes: 10
    cooldown-seconds: 60
    max-attempts: 5
```

### Danh sách các biến môi trường:
| Tên biến | Giá trị mặc định | Mô tả |
|---|---|---|
| `MAIL_HOST` | `smtp.gmail.com` | Máy chủ SMTP Gmail |
| `MAIL_PORT` | `587` | Cổng STARTTLS chuẩn (port 587) |
| `MAIL_USERNAME` | *(Trống)* | Địa chỉ Gmail gửi thư (ví dụ: `khoalkopwk@gmail.com`) |
| `MAIL_PASSWORD` | *(Trống)* | Mật khẩu ứng dụng Gmail (16 ký tự, KHÔNG phải mật khẩu tài khoản) |
| `MAIL_FROM` | `${MAIL_USERNAME}` | Địa chỉ email hiển thị người gửi |
| `MAIL_SENDER_NAME` | `MilkTea Cafe` | Tên thương hiệu hiển thị trong hộp thư đến |
| `OTP_SECRET` | *(Mặc định an toàn)* | Khóa bí mật máy chủ dùng băm HMAC-SHA256 bảo vệ OTP |

---

## 3. Khởi Chạy Ứng Dụng Từ Terminal (PowerShell)

Mở PowerShell tại thư mục gốc của dự án (`H:\UTE\Nam_3\v8\MilkTea_management_system\milktea`) và thiết lập các biến môi trường trước khi chạy (sử dụng placeholder cho mật khẩu):

```powershell
# 1. Thiết lập biến môi trường kết nối Mail SMTP
$env:MAIL_HOST = "smtp.gmail.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "khoalkopwk@gmail.com"
$env:MAIL_PASSWORD = "<YOUR_16_CHAR_GMAIL_APP_PASSWORD>"
$env:MAIL_FROM = "khoalkopwk@gmail.com"
$env:MAIL_SENDER_NAME = "MilkTea Cafe"

# 2. Khởi chạy ứng dụng Spring Boot
./mvnw.cmd spring-boot:run
```

*(Lưu ý: Thay thế `<YOUR_16_CHAR_GMAIL_APP_PASSWORD>` bằng mật khẩu ứng dụng 16 ký tự thực tế nhận từ Google).*

---

## 4. Quy Trình Vận Hành 3 Luồng Thực Tế

### Luồng 1: Đăng ký tài khoản có xác thực email bằng OTP 6 số
1. **Truy cập:** Mở trình duyệt vào `http://localhost:8080/login`, chọn tab **Đăng Ký**.
2. **Nhập thông tin:** Điền Họ tên, Email, Số điện thoại (10 chữ số), Mật khẩu (tối thiểu 6 ký tự) và Xác nhận mật khẩu. Bấm **Đăng Ký & Nhận Mã Xác Thực**.
3. **Phía Server:**
   - Kiểm tra email chưa tồn tại (hoặc tài khoản cũ chưa xác thực).
   - Lưu tài khoản với vai trò `CUSTOMER` và `EmailXacThuc = 0` (`false`). Tuyệt đối **không cấp JWT token**.
   - Sinh mã OTP 6 chữ số ngẫu nhiên an toàn (`SecureRandom`), băm HMAC-SHA256 lưu vào bảng `XacThucEmail`.
   - Gửi email kích hoạt thật qua SMTP.
4. **Giao diện OTP:** Màn hình tự động chuyển sang khối nhập mã OTP:
   - Hiển thị email đã che một phần (ví dụ: `kh***wk@gmail.com`).
   - Ô nhập 6 chữ số OTP với kiểu chữ lớn, cách đều.
   - Nút **Gửi lại mã OTP** kích hoạt đồng hồ đếm ngược 60 giây (cooldown bảo vệ cả phía client lẫn server).
5. **Kích hoạt:** Người dùng nhập 6 chữ số từ email và bấm **Xác Thực & Kích Hoạt Tài Khoản**. Server đối chiếu mã băm và hạn sử dụng (10 phút):
   - Nếu đúng: Cập nhật `EmailXacThuc = 1` (`true`), thông báo thành công và chuyển ngay sang tab **Đăng Nhập** có điền sẵn email.
   - Nếu sai: Tăng số lần thử (tối đa 5 lần), hiển thị số lần thử còn lại. Quá 5 lần, mã bị khóa và yêu cầu gửi lại mã mới.

### Luồng 2: Đăng nhập hệ thống
1. **Truy cập:** Màn hình `http://localhost:8080/login`, tab **Đăng Nhập**.
2. **Kiểm tra trạng thái tài khoản:**
   - **Tài khoản chưa xác thực email:** Nếu cố tình đăng nhập khi chưa xác thực OTP, hệ thống từ chối cấp JWT và hiển thị thông báo: `Địa chỉ email chưa được xác thực.` kèm đường dẫn: `👉 Nhập mã OTP kích hoạt tài khoản ngay`. Khi bấm vào, màn hình tự động chuyển sang form nhập OTP của email đó.
   - **Tài khoản bị Admin khóa (`HoatDong = 0`):** Hiển thị lỗi rõ ràng: `Tài khoản đã bị tạm khóa. Vui lòng liên hệ ban quản trị.` (Tách biệt hoàn toàn khỏi lỗi chưa xác thực).
   - **Tài khoản hợp lệ:** Cấp HttpOnly cookie `milktea_access_token` và chuyển hướng đúng vai trò:
     - `ADMIN` $\rightarrow$ `/admin` (Báo cáo doanh thu & Quản trị)
     - `CASHIER` $\rightarrow$ `/cashier` (Giao diện Thu ngân)
     - `KITCHEN` $\rightarrow$ `/kitchen` (Giao diện Bếp)
     - `CUSTOMER` $\rightarrow$ `/` hoặc trang bàn QR trước đó.

### Luồng 3: Quên và đặt lại mật khẩu
1. **Bấm "Quên mật khẩu?":** Tại tab Đăng Nhập, mở modal **Khôi Phục Mật Khẩu**.
2. **Bước 1 (Nhập Email):** Nhập email tài khoản và bấm **Gửi Mã Khôi Phục Qua Email**.
   - Server bảo mật thông tin: Response công khai luôn trả thông báo chung, không tiết lộ email có tồn tại trong hệ thống hay không.
   - Nếu email tồn tại và đang hoạt động: Sinh mã xác thực 6 số có mục đích `PASSWORD_RESET` (hạn 15 phút) và gửi email khôi phục.
3. **Bước 2 (Đặt lại mật khẩu):**
   - Modal hiển thị form nhập: Mã xác thực 6 chữ số từ email + Mật khẩu mới + Xác nhận mật khẩu mới.
   - Bấm **Đặt Lại Mật Khẩu**.
   - Server kiểm tra tính hợp lệ của mã khôi phục `PASSWORD_RESET` cho đúng email đó.
   - Băm mật khẩu mới bằng `BCryptPasswordEncoder`.
   - **Tăng `TokenVersion` của tài khoản:** Toàn bộ phiên đăng nhập cũ, cookie hoặc JWT cũ trên tất cả các thiết bị ngay lập tức bị vô hiệu hóa hoàn toàn.
   - Đóng modal, chuyển về form đăng nhập để người dùng đăng nhập bằng mật khẩu mới.

---

## 5. Hướng Dẫn Kiểm Tra Email Trong Inbox & Spam

- **Hộp thư đến (Inbox):** Thư từ hệ thống có tiêu đề `[MilkTea] Mã xác thực đăng ký tài khoản` hoặc `[MilkTea] Mã xác thực đặt lại mật khẩu` gửi từ tên `MilkTea Cafe`.
- **Thư rác / Quảng cáo (Spam / Junk / Promotions):** Với các tài khoản nhận lần đầu nhận mail tự động từ máy chủ phát triển cục bộ (`localhost`), bộ lọc của Google hoặc Outlook có thể xếp tạm vào thư mục **Spam** hoặc tab **Promotions / Updates**.
  - Mở thư mục **Spam**, tìm email từ `MilkTea`.
  - Bấm chọn **Báo cáo không phải là spam (Not Spam)** để các email xác thực tiếp theo gửi thẳng vào Inbox.

---

## 6. Chẩn Đoán & Khắc Phục Các Sự Cố SMTP Thường Gặp

| Mã lỗi / Hiện tượng | Nguyên nhân chính | Cách xử lý |
|---|---|---|
| `535-5.7.8 Username and Password not accepted` | Dùng sai mật khẩu tài khoản Gmail thông thường hoặc sai App Password | Tạo Mật khẩu ứng dụng 16 ký tự mới tại `myaccount.google.com/apppasswords`. Đảm bảo đã bật Xác minh 2 bước. |
| `MailConnectException: Couldn't connect to host, port: smtp.gmail.com, 587` | Bị chặn cổng outbound 587 bởi tường lửa hoặc mạng nội bộ (công ty/trường học) | Kiểm tra kết nối bằng PowerShell: `Test-NetConnection -ComputerName smtp.gmail.com -Port 587`. Nếu mạng chặn, chuyển sang mạng WiFi cá nhân hoặc 4G. |
| `SocketTimeoutException: connect timed out` | Thời gian chờ kết nối quá ngắn hoặc mạng chậm | Ứng dụng đã cấu hình timeout 5000ms. Kiểm tra lại đường truyền mạng Internet. |
| `AuthenticationFailedException` | Sai username hoặc mật khẩu ứng dụng có chứa ký tự thừa | Đảm bảo biến `$env:MAIL_USERNAME` là địa chỉ email đầy đủ (có `@gmail.com`) và `$env:MAIL_PASSWORD` không chứa khoảng trắng. |
| `Không nhận được email dù server báo thành công` | Thư vào hộp Spam hoặc tài khoản gửi bị Google chặn tạm thời do gửi quá giới hạn cá nhân | Kiểm tra kỹ mục Spam/Thư rác. Tài khoản Gmail cá nhân miễn phí có giới hạn gửi khoảng 500 email/ngày. |

---

## 7. Migration Cho Dữ Liệu SQL Server Hiện Hữu

Để cập nhật cơ sở dữ liệu đang có mà không làm mất dữ liệu của các tài khoản đã tạo:

Chạy file script `database/sqlserver/patch_email_verification.sql` trong SQL Server Management Studio (SSMS) hoặc sqlcmd:

```sql
USE milktea;
GO

-- 1. Thêm cột EmailXacThuc vào bảng TaiKhoan (mặc định 1 cho tài khoản cũ)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.TaiKhoan') AND name = 'EmailXacThuc')
BEGIN
    ALTER TABLE dbo.TaiKhoan ADD EmailXacThuc BIT NOT NULL CONSTRAINT DF_TaiKhoan_EmailXacThuc DEFAULT 1;
    UPDATE dbo.TaiKhoan SET EmailXacThuc = 1 WHERE EmailXacThuc IS NULL;
END
GO

-- 2. Tạo bảng XacThucEmail lưu trữ mã OTP băm an toàn
IF OBJECT_ID('dbo.XacThucEmail', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.XacThucEmail (
        MaOTP BIGINT IDENTITY(1, 1) PRIMARY KEY,
        Email VARCHAR(254) NOT NULL,
        OtpHash VARCHAR(255) NOT NULL,
        Salt VARCHAR(64) NOT NULL,
        MucDich VARCHAR(32) NOT NULL,
        HetHanLuc DATETIME2 NOT NULL,
        SoLanThu INT NOT NULL CONSTRAINT DF_XacThucEmail_SoLanThu DEFAULT 0,
        ToiDaThu INT NOT NULL CONSTRAINT DF_XacThucEmail_ToiDaThu DEFAULT 5,
        DaDungLuc DATETIME2 NULL,
        VoHieuLuc DATETIME2 NULL,
        GuiCuoiLuc DATETIME2 NOT NULL,
        TaoLuc DATETIME2 NOT NULL
    );
    CREATE INDEX IX_XacThucEmail_Email_MucDich ON dbo.XacThucEmail(Email, MucDich);
END
GO
```

Tất cả các tài khoản demo cũ (`admin@milktea.vn`, `cashier@milktea.vn`, `kitchen@milktea.vn`, `customer@milktea.vn`) đều được bảo lưu nguyên vẹn quyền đăng nhập (`EmailXacThuc = 1`), trong khi mọi đăng ký mới đều bắt buộc trải qua bước xác thực email qua mã OTP 6 số.
