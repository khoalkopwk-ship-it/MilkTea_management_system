# MilkTea Database

## Database
SQL Server

## Branch
Hệ thống demo sử dụng 1 chi nhánh:

- CN1 - Chi nhánh chính

## Inventory

### Material types

- THO: nguyên liệu thô
- SOCHE: nguyên liệu đã sơ chế

### Stock locations

- KHO: kho tổng
- BEP: tồn tại bếp

### Inventory flow

Nhập hàng
→ KHO
→ Xuất KHO sang BẾP
→ Ghi mẻ sơ chế
→ THO tại BẾP giảm
→ SOCHE tại BẾP tăng
→ Hoàn thành đơn
→ Trừ tồn BẾP

### Scripts

1. `001-foundation.sql`
   - Schema nền
   - Branch

2. `002-inventory.sql`
   - Material
   - Stock
   - Import receipt
   - Stock issue
   - Preparation batch
   - Stock movement

3. `003-inventory-seed.sql`
   - Dữ liệu Inventory mẫu