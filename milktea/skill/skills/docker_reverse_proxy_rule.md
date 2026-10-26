# MilkTea — chạy ứng dụng và triển khai

## 1. Đường chạy tối thiểu

Một Spring Boot app + một DB thực + Cloudinary + kênh mail reset. Chạy từ IDE/Maven wrapper/JAR là đường demo hợp lệ; Docker và Nginx chỉ triển khai khi nhóm cần, không trì hoãn nghiệp vụ để dựng VPS.

README của project ứng dụng cần: JDK/pom versions thực dùng, URL/port, DB/schema/seed theo thứ tự, biến môi trường, tài khoản demo, lệnh wrapper thực tế, cách kiểm JWT/WebSocket/ảnh và restore DB. Bộ rules này không thay README chạy source.

## 2. Biến môi trường

Giữ placeholder thực tế của project. Baseline MilkTea: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `APP_BASE_URL`, khóa JWT đúng thuật toán, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, cấu hình mail. Nếu dùng `SPRING_DATASOURCE_*` của Spring thì ánh xạ duy nhất tương đương; không trộn hai bộ biến mà không ghi ưu tiên.

Ví dụ `application.properties` tối thiểu (phải ghép với cấu hình JWT/WebSocket/Cloudinary/mail đúng source):
```properties
spring.application.name=milktea
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
spring.thymeleaf.cache=false
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB
server.port=${SERVER_PORT:8080}
app.base-url=${APP_BASE_URL:http://localhost:8080}
```
Chỉ dùng driver SQL Server này khi DB được chọn là SQL Server. Khi đổi vendor phải đổi driver/dependency/DDL tương ứng; không đặt YAML block trong `.properties`. Có env.example không secret. Spring Boot không mặc định tự đọc mọi `.env`; chọn IDE env, shell loader hoặc Compose env_file và ghi cách nạp có thật. Không hứa `.env` hoạt động khi chưa cấu hình.

Driver/URL cho SQL Server/MySQL/PostgreSQL theo DB đã chọn. Dùng đúng port thực tế, không giả định mọi SQL Server đều 1433/SQLEXPRESS đều 1434. TLS/trust settings cho local được ghi rõ, không mang bỏ certificate validation vào production mặc định.

## 3. Docker khi được yêu cầu

Dockerfile build/runtime theo Java project, multi-stage nếu cần; service `app` và một `db`; thêm `nginx` khi có HTTPS/domain. Không hardcode image tag latest; khóa tag đã kiểm. DB volume bền; không public DB ra Internet. Container app gọi db bằng service hostname, không localhost. Kiểm healthcheck và DB-ready chứ không chỉ depends_on started. Một instance app/simple STOMP broker đủ; không load balance nhiều node rồi coi giỏ/socket đồng bộ sẵn.

## 4. Reverse proxy khi dùng

Forward Host, X-Forwarded-For, X-Forwarded-Proto và WebSocket Upgrade ở đúng `/ws`; cấu hình forwarded headers của Spring phù hợp trusted proxy. HTTPS/WSS và Secure JWT cookie nhất quán; không tắt CSRF vì dùng proxy. Body limit khớp upload và giữ nguyên HTTP error/body từ app. GET QR không cache response có session token/payment view trên proxy công khai.

Ví dụ đoạn Nginx (phải ghép vào cấu hình thực, không phải file deploy hoàn chỉnh):
```nginx
location /ws {
    proxy_pass http://app:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## 5. Dữ liệu và nghiệm thu

Backup DB và kiểm restore trước demo; migration giữ lịch sử. Không mount source/secret vào image production không cần thiết. Lỗi mail/ảnh/socket được tái hiện và xử lý, không đổi sang mock rồi tuyên bố tích hợp thật. Kiểm trên máy demo bằng một DB sạch có seed, thao tác QR/read-only, tiền trước/sau, close thủ công và reload/tab mới.
