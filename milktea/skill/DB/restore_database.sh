#!/usr/bin/env bash
# ========================================================
# MilkTea Management System - Database Restore Script
# Phục hồi cơ sở dữ liệu từ file .bak trong Docker Container
# ========================================================
set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Sử dụng: $0 <path_to_backup_file.bak>"
    exit 1
fi

BACKUP_SRC="$1"

if [ ! -f "${BACKUP_SRC}" ]; then
    echo "[ERROR] Không tìm thấy file sao lưu: ${BACKUP_SRC}"
    exit 1
fi

FILENAME=$(basename "${BACKUP_SRC}")

echo "[INFO] Đang tải file sao lưu '${FILENAME}' vào container..."
docker cp "${BACKUP_SRC}" "milktea_db:/var/opt/mssql/${FILENAME}"

echo "[INFO] Đang phục hồi cơ sở dữ liệu 'milktea'..."
# Đóng kết nối cũ và phục hồi
docker exec -i milktea_db /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P "${DB_PASSWORD:-YourStrongPassword123!}" -C \
  -Q "
    ALTER DATABASE [milktea] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    RESTORE DATABASE [milktea] FROM DISK = N'/var/opt/mssql/${FILENAME}' WITH REPLACE;
    ALTER DATABASE [milktea] SET MULTI_USER;
  "

docker exec -i milktea_db rm -f "/var/opt/mssql/${FILENAME}"

echo "[SUCCESS] Phục hồi cơ sở dữ liệu hoàn tất thành công!"
