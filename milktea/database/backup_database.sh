#!/usr/bin/env bash
# ========================================================
# MilkTea Management System - Database Backup Script
# Tạo file sao lưu .bak cho SQL Server trong Docker Container
# ========================================================
set -euo pipefail

BACKUP_DIR="./database/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="milktea_backup_${TIMESTAMP}.bak"

mkdir -p "${BACKUP_DIR}"

echo "[INFO] Đang thực hiện sao lưu cơ sở dữ liệu 'milktea'..."

# Thực thi lệnh BACKUP DATABASE bên trong container milktea_db
docker exec -i milktea_db /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P "${DB_PASSWORD:-YourStrongPassword123!}" -C \
  -Q "BACKUP DATABASE [milktea] TO DISK = N'/var/opt/mssql/${BACKUP_FILE}' WITH FORMAT, MEDIANAME = 'MilkTeaBackups', NAME = 'Full Backup of milktea';"

# Copy file .bak từ container ra host
docker cp "milktea_db:/var/opt/mssql/${BACKUP_FILE}" "${BACKUP_DIR}/${BACKUP_FILE}"

# Xóa file tạm trong container
docker exec -i milktea_db rm -f "/var/opt/mssql/${BACKUP_FILE}"

echo "[SUCCESS] Sao lưu thành công: ${BACKUP_DIR}/${BACKUP_FILE}"
