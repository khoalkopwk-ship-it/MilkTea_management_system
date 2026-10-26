# GEMINI.md — Điểm vào Antigravity & Gemini cho MilkTea Multi-Branch

Chào mừng đến với dự án **MilkTea Chain Management System** (Hệ thống chuỗi trà sữa đa chi nhánh).

## 1. Nguồn chuẩn duy nhất (Single Source of Truth)
Mọi quy tắc nghiệp vụ, ma trận phân quyền, luồng tiền, kho và tính năng khách hàng được quản lý tập trung tại:
- [docs/BUSINESS_RULES.md](docs/BUSINESS_RULES.md): Toàn bộ quy tắc chuỗi, phân quyền, bàn/phiên, đơn hàng, kho, đánh giá và voucher.
- [docs/REQUIREMENTS_TRACEABILITY.md](docs/REQUIREMENTS_TRACEABILITY.md): Ma trận truy vết yêu cầu (BR, SEC, FLOW, CUS, OPS, RT, MEDIA, AT).
- [docs/BRANCH_DEVELOPMENT_PLAN.md](docs/BRANCH_DEVELOPMENT_PLAN.md): Lộ trình 6 giai đoạn phát triển.
- [docs/IMPLEMENTATION_PROGRESS.md](docs/IMPLEMENTATION_PROGRESS.md): Tiến độ, checkpoint và các quyết định.
- [docs/MIGRATION_GUIDE.md](docs/MIGRATION_GUIDE.md): Kịch bản sao lưu, backfill và đối soát dữ liệu SQL Server.

## 2. Nguyên tắc bất biến khi lập trình
1. **Kiến trúc**: Spring Boot MVC Monolith (Java 21, Thymeleaf, Bootstrap, JPA) + 1 database SQL Server. Không chuyển sang Microservices hoặc SPA.
2. **Dữ liệu**: Không drop database hoặc xóa sạch bảng trên database thật `milktea`. Mọi thay đổi schema phải dùng migration có đối soát trước/sau.
3. **Phân quyền Backend**: Backend xác thực cả vai trò và phạm vi chi nhánh (`CurrentActor.branchId`). Không tin tưởng `branchId` hay giá/tiền do client gửi lên.
4. **Luồng nghiệp vụ**: Giữ nguyên luồng giỏ hàng, mở phiên khi có đơn đầu tiên thành công, Bếp hoàn thành đơn thì mới trừ kho Bếp, Thu ngân đóng phiên khi hóa đơn đã thu đủ. Bổ sung phạm vi chi nhánh nhất quán.
5. **CSRF & OTP**: CSRF được bật và kiểm tra; OTP lưu số lần thử sai trong transaction độc lập, không bị mất khi rollback exception.
6. **Idempotency**: Dùng `BanGhiChongLap` (`RequestDedupRecord`) cho tạo đơn, thanh toán, hoàn tiền và ghi sổ kho.
