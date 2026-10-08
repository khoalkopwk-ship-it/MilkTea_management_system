# Quy Trình Khởi Tạo, Sao Lưu và Phục Hồi Database MilkTea

Tài liệu hướng dẫn quản trị cơ sở dữ liệu Microsoft SQL Server cho hệ thống MilkTea, tuân thủ `database_rule.md` và `docker_reverse_proxy_rule.md`.

---

## 1. Khởi Tạo Cơ Sở Dữ Liệu Ban Đầu (Fresh Deployment)

Khi triển khai trên môi trường Production lần đầu:

### Bước 1.1: Tạo Database và Schema DDL
Chạy file script DDL để tạo toàn bộ 14 bảng, ràng buộc khóa ngoại, unique constraints và index:
```bash
# Trong môi trường Docker Container
docker exec -i milktea_db /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P "$DB_PASSWORD" -C \
  -i /docker-entrypoint-initdb.d/schema.sql
```
*(Nếu trên Azure SQL Database: Chạy trực tiếp nội dung `database/sqlserver/schema.sql` qua Azure Portal Query Editor hoặc Azure Data Studio).*

### Bước 1.2: Nạp Dữ Liệu Khởi Tạo Production (Seed)
Chỉ tạo tài khoản Quản Trị Viên (Admin) và bản ghi cấu hình quán mặc định, **không** tạo dữ liệu mẫu/đơn hàng demo:
```bash
docker exec -i milktea_db /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P "$DB_PASSWORD" -C \
  -i /docker-entrypoint-initdb.d/seed_prod_admin.sql
```

---

## 2. Quy Trình Sao Lưu Cơ Sở Dữ Liệu (Backup)

### 2.1. Sao Lưu Thủ Công (Docker Environment)
Chạy script tự động:
```bash
chmod +x ./database/backup_database.sh
./database/backup_database.sh
```
File sao lưu sẽ được lưu tại: `./database/backups/milktea_backup_YYYYMMDD_HHMMSS.bak`.

### 2.2. Lên Lịch Sao Lưu Tự Động Hàng Ngày (Cronjob trên Linux VPS)
Mở crontab bằng `crontab -e` và thêm lệnh sao lưu mỗi đêm vào lúc 02:00 sáng:
```cron
0 2 * * * cd /opt/milktea && ./database/backup_database.sh >> /var/log/milktea_backup.log 2>&1
```

### 2.3. Sao Lưu Trên Microsoft Azure SQL Database
Azure SQL Database tự động sao lưu dự phòng (Point-in-time Restore). Nếu muốn xuất file sao lưu độc lập (BACPAC) về máy:
```bash
az sql db export \
  --resource-group rg-milktea \
  --server sql-milktea-server \
  --name milktea \
  --storage-key-type StorageAccessKey \
  --storage-key "<STORAGE_KEY>" \
  --storage-uri "https://<STORAGE_ACCOUNT>.blob.core.windows.net/backups/milktea_backup.bacpac" \
  --admin-user sa_admin \
  --admin-password "$DB_PASSWORD"
```

---

## 3. Quy Trình Phục Hồi Dữ Liệu (Restore & Disaster Recovery)

### 3.1. Phục Hồi Trong Môi Trường Docker
Khi cần phục hồi từ một file `.bak` đã có:
```bash
chmod +x ./database/restore_database.sh
./database/restore_database.sh ./database/backups/milktea_backup_20261006_020000.bak
```

### 3.2. Phục Hồi Trên Azure SQL Database
Nhập file BACPAC từ Azure Storage Blob vào database mới:
```bash
az sql db import \
  --resource-group rg-milktea \
  --server sql-milktea-server \
  --name milktea_restored \
  --storage-key-type StorageAccessKey \
  --storage-key "<STORAGE_KEY>" \
  --storage-uri "https://<STORAGE_ACCOUNT>.blob.core.windows.net/backups/milktea_backup.bacpac" \
  --admin-user sa_admin \
  --admin-password "$DB_PASSWORD"
```

---

## 4. Nguyên Tắc An Toàn Dữ Liệu
1. **Không tắt cờ kiểm tra schema**: Trên production luôn giữ `spring.jpa.hibernate.ddl-auto=validate` để Hibernate không tự ý thay đổi cấu trúc bảng.
2. **Bảo mật file backup**: Các file `.bak` chứa dữ liệu nhạy cảm (thông tin tài khoản, doanh thu) phải được phân quyền `chmod 600` và không lưu trữ trên web server public.
