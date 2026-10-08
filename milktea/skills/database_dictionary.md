# MilkTea — từ điển dữ liệu theo báo cáo

Giữ tên SQL không dấu trong Chương 4; map tên lớp Java tiếng Anh bằng @Table/@Column. Áp dụng quan hệ/invariant/migration của [database_rule.md](database_rule.md). Không tạo các bảng chi nhánh, trạng thái món hoặc chương trình khuyến mãi.

## Danh mục

- TaiKhoan
- TokenKhoiPhuc
- DanhMuc
- MonUong
- Ban
- PhienBan
- DonHang
- ChiTietDonHang
- DinhMucDonHang
- HoaDon
- ThanhToan
- YeuCauHuy
- YeuCauHoan
- NguyenLieu
- TonNguyenLieu
- CongThucMon
- CongThucSoChe
- ChiTietCTSoChe
- PhieuNhap
- ChiTietNhap
- PhieuXuat
- ChiTietXuat
- MeSoChe
- ChiTietMeSoChe
- LichSuKho
- NhatKyNghiepVu
- CauHinhChung
- GioPhien
- ChiTietGioPhien
- TruyCapPhienBan
- TruyCapDonQuay
- ThongBaoThanhToan
- BanGhiChongLap

## TaiKhoan

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaTK | BIGINT | PK |
| Email | VARCHAR(254) | UNIQUE NOT NULL |
| MatKhauBam | VARCHAR(255) | BCrypt; không mật khẩu rõ |
| HoTen / DienThoai | NVARCHAR(120) / VARCHAR(20) | Hồ sơ |
| VaiTro | VARCHAR(16) | ADMIN / CASHIER / KITCHEN / CUSTOMER |
| HoatDong / TokenVersion | BIT / BIGINT | Token cũ bị vô hiệu khi version thay đổi |
| TaoLuc | DATETIME2 | UTC; NOT NULL |

## TokenKhoiPhuc

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaToken | BIGINT | PK |
| MaTK | BIGINT | FK TaiKhoan |
| TokenHash | VARCHAR(128) | UNIQUE; không lưu token rõ |
| HetHanLuc / DaDungLuc | DATETIME2 | Hạn dùng bắt buộc; thời điểm dùng nullable |

## DanhMuc

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaDM | BIGINT | PK |
| TenDM | NVARCHAR(100) | UNIQUE NOT NULL |
| HoatDong | BIT | Ngừng hoạt động thay vì xóa lịch sử |

## MonUong

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaMon | BIGINT | PK |
| MaDM | BIGINT | FK DanhMuc |
| TenMon / Size | NVARCHAR(120) / VARCHAR(12) | UNIQUE(TenMon, Size) |
| MoTa | NVARCHAR(1000) | Nullable |
| Gia | DECIMAL(18,2) | CHECK >= 0 |
| AnhUrl / AnhPublicId | NVARCHAR(1000) / VARCHAR(255) | Thông tin Cloudinary |
| HoatDong | BIT | NOT NULL |

## Ban

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaBan | BIGINT | PK |
| TenBan | NVARCHAR(40) | UNIQUE NOT NULL trong quán |
| MaQR | VARCHAR(64) | UNIQUE; mã opaque định danh bàn |
| TrangThai | VARCHAR(16) | TRONG / CO_KHACH |
| MaPhienDangMo | BIGINT | FK PhienBan nullable; phiên thuộc chính bàn |
| HoatDong / Version | BIT / BIGINT | @Version cho cạnh tranh khi cần |

## PhienBan

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaPhien | BIGINT | PK |
| MaBan | BIGINT | FK Ban |
| TrangThai | VARCHAR(8) | OPEN / CLOSED |
| MoLuc / DongLuc | DATETIME2 | UTC; DongLuc nullable khi OPEN |
| NguoiMo / NguoiDong | BIGINT | FK TaiKhoan nullable; NguoiMo NULL nếu khách mở bằng order |
| Version | BIGINT | JPA @Version; đóng phiên kiểm phiên hiện tại |

## DonHang

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaDH | BIGINT | PK |
| MaPhien / MaTK | BIGINT | FK PhienBan / TaiKhoan nullable |
| Nguon | VARCHAR(10) | TABLE / COUNTER |
| TrangThai | VARCHAR(24) | CHO_XAC_NHAN / CHO_CHE_BIEN / DANG_CHE_BIEN / HOAN_THANH / DA_HUY |
| ThayTheMaDH | BIGINT | FK DonHang nullable |
| MaYeuCau | VARCHAR(64) | Chống tạo lặp theo phạm vi và fingerprint |
| GhiChu | NVARCHAR(500) | Nullable |
| TaoLuc / BatDauLuc / HoanThanhLuc | DATETIME2 | UTC; hai mốc sau nullable |
| Version | BIGINT | JPA @Version, enum STRING |

## ChiTietDonHang

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaChiTiet | BIGINT | PK riêng |
| MaDH / MaMon | BIGINT | FK DonHang / MonUong |
| TenMonChot / SizeChot | NVARCHAR(120) / VARCHAR(12) | Tên/size đã chốt |
| SoLuong | INT | CHECK > 0 |
| DonGiaChot | DECIMAL(18,2) | CHECK >= 0 |
| GhiChu | NVARCHAR(500) | Nullable |

## DinhMucDonHang

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaDH / MaNL | BIGINT | PK kép; FK DonHang / NguyenLieu |
| TongLuong | DECIMAL(18,3) | > 0; tổng định mức toàn đơn |
| LoaiChot / DonViChot | VARCHAR(8) / VARCHAR(16) | Snapshot THO/SOCHE và đơn vị tại lúc đặt |

## HoaDon

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaHD | BIGINT | PK |
| MaDH | BIGINT | FK UNIQUE DonHang |
| TongTien / TienGiam / PhaiTra | DECIMAL(18,2) | >= 0; PhaiTra = TongTien − TienGiam |
| TyLeGiamChot | DECIMAL(5,2) | 0..100 |
| TrangThai | VARCHAR(12) | HIEU_LUC / DA_HUY |
| LapLuc / HuyLuc | DATETIME2 | UTC; HuyLuc nullable |

## ThanhToan

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaTT | BIGINT | PK |
| MaHD | BIGINT | FK UNIQUE HoaDon |
| PhuongThuc | VARCHAR(16) | CASH / BANK_TRANSFER |
| SoTien | DECIMAL(18,2) | Bằng PhaiTra; khoản đã đối chiếu |
| ThamChieu | NVARCHAR(120) | Nullable |
| NguoiThu / ThuLuc | BIGINT / DATETIME2 | CASHIER và UTC thực nhận |
| MaYeuCau | VARCHAR(64) | Chống ghi trùng |

## YeuCauHuy

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaHuy | BIGINT | PK |
| MaDH | BIGINT | FK DonHang |
| LyDo / LyDoXuLy | NVARCHAR(500) | Lý do khách bắt buộc |
| TrangThai | VARCHAR(16) | CHO / CHAP_THUAN / TU_CHOI |
| TaoLuc / XuLyLuc | DATETIME2 | XuLyLuc nullable |
| NguoiXuLy | BIGINT | FK TaiKhoan nullable |

## YeuCauHoan

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaHoan | BIGINT | PK |
| MaDH / MaTT | BIGINT | FK UNIQUE DonHang / ThanhToan |
| SoTien | DECIMAL(18,2) | Bằng toàn khoản đã thu; không hoàn riêng món |
| LyDo / LyDoDuyet | NVARCHAR(500) | Lưu căn cứ |
| TrangThai | VARCHAR(16) | CHO_DUYET / DA_DUYET / TU_CHOI / DA_HOAN |
| NguoiDuyet / NguoiHoan | BIGINT | FK TaiKhoan nullable |
| DuyetLuc / HoanLuc | DATETIME2 | Nullable; HoanLuc là thời điểm thực trả |
| ThamChieuHoan / MaYeuCau | NVARCHAR(120) / VARCHAR(64) | MaYeuCau UNIQUE |

## NguyenLieu

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaNL | BIGINT | PK |
| TenNL | NVARCHAR(120) | UNIQUE NOT NULL |
| Loai | VARCHAR(8) | THO / SOCHE |
| DonVi | VARCHAR(16) | Đơn vị chuẩn: kg, g, cai, suat... |
| HoatDong | BIT | NOT NULL |

## TonNguyenLieu

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaNL | BIGINT | PK kép cùng ViTri; FK NguyenLieu |
| ViTri | VARCHAR(8) | KHO / BEP |
| SoLuong / NguongCanhBao | DECIMAL(18,3) | CHECK >= 0 |
| Version | BIGINT | JPA @Version hoặc khóa pessimistic khi ghi |

## CongThucMon

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaMon / MaNL | BIGINT | PK kép; FK MonUong / NguyenLieu |
| DinhLuong | DECIMAL(18,3) | CHECK > 0 |

## CongThucSoChe

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaCTSC | BIGINT | PK |
| TenCT | NVARCHAR(120) | NOT NULL |
| MaNLDauRa | BIGINT | FK NguyenLieu loại SOCHE |
| LuongDauRaChuan | DECIMAL(18,3) | CHECK > 0 |
| HoatDong | BIT | NOT NULL |

## ChiTietCTSoChe

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaCTSC / MaNL | BIGINT | PK kép; FK CongThucSoChe / NguyenLieu loại THO |
| LuongDauVaoChuan | DECIMAL(18,3) | CHECK > 0 |

## PhieuNhap

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaPhieu | BIGINT | PK |
| NguoiLap | BIGINT | FK TaiKhoan |
| LyDo / NguonNhap | NVARCHAR(500) | NguonNhap chỉ áp dụng phiếu nhập |
| TrangThai | VARCHAR(12) | NHAP / DA_GHI_SO; trạng thái chứng từ |
| LapLuc / GhiSoLuc | DATETIME2 | GhiSoLuc nullable khi nháp |
| MaYeuCau | VARCHAR(64) | UNIQUE |

## ChiTietNhap

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaPhieu / MaNL | BIGINT | PK kép; FK phiếu tương ứng / NguyenLieu |
| SoLuong | DECIMAL(18,3) | CHECK > 0 |
| DonGiaNhap | DECIMAL(18,2) | CHECK >= 0 |

## PhieuXuat

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaPhieu | BIGINT | PK |
| NguoiLap | BIGINT | FK TaiKhoan |
| LyDo | NVARCHAR(500) | Bắt buộc lý do xuất |
| TrangThai | VARCHAR(12) | NHAP / DA_GHI_SO; trạng thái chứng từ |
| LapLuc / GhiSoLuc | DATETIME2 | GhiSoLuc nullable khi nháp |
| MaYeuCau | VARCHAR(64) | UNIQUE |

## ChiTietXuat

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaPhieu / MaNL | BIGINT | PK kép; FK phiếu tương ứng / NguyenLieu |
| SoLuong | DECIMAL(18,3) | CHECK > 0 |

## MeSoChe

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaMe | BIGINT | PK |
| MaCTSC / MaNLDauRa | BIGINT | FK; đầu ra chốt |
| LuongDuKien / LuongThucThu | DECIMAL(18,3) | CHECK >= 0; thực thu nhập khi ghi sổ |
| TrangThai | VARCHAR(12) | NHAP / DA_GHI_SO |
| LyDoChenhLech | NVARCHAR(500) | Bắt buộc nếu khác dự kiến |
| NguoiLap / GhiSoLuc | BIGINT / DATETIME2 | FK TaiKhoan; thời điểm ghi sổ |
| MaYeuCau | VARCHAR(64) | UNIQUE |

## ChiTietMeSoChe

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaMe / MaNL | BIGINT | PK kép; FK MeSoChe / NguyenLieu THO |
| LuongDinhMuc / LuongThucDung | DECIMAL(18,3) | CHECK > 0 |

## LichSuKho

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaBienDong | BIGINT | PK |
| MaNL / ViTri | BIGINT / VARCHAR(8) | Tham chiếu khóa tồn |
| Loai | VARCHAR(16) | NHAP / XUAT_DI / NHAN_BEP / SOCHE_VAO / SOCHE_RA / TIEU_HAO / HAO_HUT |
| LuongBienDong / TonTruoc / TonSau | DECIMAL(18,3) | Có dấu cho biến động; tồn trước/sau không âm |
| NguonLoai / NguonMa / NguonDong | VARCHAR(16) / BIGINT / BIGINT | Khóa chứng từ nguồn |
| NguoiLap / TaoLuc | BIGINT / DATETIME2 | FK TaiKhoan; NOT NULL |
| LyDo | NVARCHAR(500) | Bắt buộc hao hụt |

## NhatKyNghiepVu

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaNK | BIGINT | PK |
| MaPhien / MaDH / MaTK | BIGINT | FK nullable theo sự kiện |
| HanhDong / Truoc / Sau | VARCHAR(40) / NVARCHAR(500) | Giá trị trạng thái hoặc mô tả quyết định |
| LyDo | NVARCHAR(1000) | Ghi sự cố/hủy/đổi cấu hình |
| TaoLuc | DATETIME2 | NOT NULL |

## CauHinhChung

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaCauHinh | INT | PK; cố định 1 |
| TenQuan / DiaChi | NVARCHAR(120) / NVARCHAR(255) | Thông tin duy nhất tại quán |
| NganHang / SoTaiKhoan / ChuTK | NVARCHAR(120) | Thông tin nhận chuyển khoản chung |
| TyLeGiam | DECIMAL(5,2) | 0..100; áp dụng mọi khách |
| NguoiSua / SuaLuc | BIGINT / DATETIME2 | FK TaiKhoan; UTC |

## GioPhien

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaGio | BIGINT | PK |
| MaPhien | BIGINT | FK UNIQUE PhienBan |
| Version | BIGINT | @Version; dùng If-Match |
| SuaLuc | DATETIME2 | UTC |

## ChiTietGioPhien

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaDong | BIGINT | PK |
| MaGio / MaMon | BIGINT | FK; UNIQUE(MaGio,MaMon) |
| SoLuong | INT | > 0 |
| GhiChu | NVARCHAR(500) | Nullable |

## TruyCapPhienBan

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaTruyCap | BIGINT | PK |
| MaPhien | BIGINT | FK PhienBan |
| TokenHash | VARCHAR(128) | UNIQUE; không token rõ |
| HetHanLuc / ThuHoiLuc | DATETIME2 | UTC; hết hiệu lực khi phiên đóng |

## TruyCapDonQuay

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaTruyCap | BIGINT | PK |
| MaDH | BIGINT | FK DonHang |
| TokenHash | VARCHAR(128) | UNIQUE; raw chỉ RAM trang khách |
| HetHanLuc / ThuHoiLuc | DATETIME2 | UTC; theo chính sách |

## ThongBaoThanhToan

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaTB | BIGINT | PK |
| MaHD | BIGINT | FK HoaDon |
| PhuongThuc | VARCHAR(16) | CASH / BANK_TRANSFER |
| ThamChieu | NVARCHAR(120) | Nullable, dữ liệu cần đối chiếu |
| TaoLuc / MaYeuCau | DATETIME2 / VARCHAR(64) | UTC; chống gửi lặp |

## BanGhiChongLap

| Trường | Kiểu | Ràng buộc |
| --- | --- | --- |
| MaBanGhi | BIGINT | PK |
| PhamVi / KhoaYeuCau | VARCHAR(128) / VARCHAR(64) | UNIQUE theo phạm vi + key |
| DauVanPayload | VARCHAR(128) | Hash dữ liệu gửi; khác dữ liệu gửi cùng key → 409 |
| LoaiTaiNguyen / MaTaiNguyen | VARCHAR(40) / BIGINT | Kết quả nghiệp vụ đã commit |
| TaoLuc / HetHanLuc | DATETIME2 | UTC; không lưu plaintext secret |

## Hoàn thiện kỹ thuật sự cố và chống lặp

Các trường dưới đây là bổ sung triển khai cho UC39, được ghi rõ ngoài bộ trường trích từ báo cáo; không đổi luồng đơn/tiền/kho:

| Bảng/trường bổ sung | Kiểu | Mục đích |
| --- | --- | --- |
| NhatKyNghiepVu.MaNKCha | BIGINT nullable | FK tự tham chiếu MaNK; liên kết sự kiện giải quyết hoặc chứng từ pha bù tới sự cố gốc |
| NhatKyNghiepVu.DuLieu | NVARCHAR(MAX) nullable | JSON có schemaVersion, movementIds hoặc danh sách materialId/quantity/location, kết quả xử lý; Service kiểm và dựng DTO, không đưa secret/PII vào payload |

Sự kiện ORDER_INCIDENT_RECORDED có MaDH/MaTK/LyDo/TaoLuc, MaNK là IncidentView.id. ORDER_INCIDENT_RESOLVED trỏ MaNKCha, ghi người/lý do/thời điểm; khóa bản ghi gốc và bảo đảm một sự kiện kết thúc cho mỗi sự cố. IncidentView OPEN/RESOLVED là projection từ các sự kiện này, không phải OrderStatus. Không cần bảng Incident riêng. Chứng từ ORDER_REMAKE_CONSUMED có ID riêng MaNK, MaNKCha trỏ sự cố, DuLieu chứa các dòng thực tiêu hao. Ledger NguonLoai=REMAKE, NguonMa=MaNK chứng từ, NguonDong=MaNL; gộp vật tư trùng trước ghi. Audit/chứng từ và StockMovement cùng transaction, duy trì khi phiên đóng. Chứng từ hao hụt dùng cùng cơ chế: audit WASTE_RECORDED có MaNK riêng, MaNKCha trỏ sự cố nếu có, DuLieu chứa các dòng hao hụt; ledger NguonLoai=WASTE/NguonMa=MaNK/NguonDong=MaNL, Loai=HAO_HUT. WasteRecord/InventoryDocumentView là projection audit và ledger, không giả định có bảng PhieuHaoHut chưa định nghĩa. Chứng từ audit + ledger + stock ghi cùng transaction, chống lặp bằng key/phạm vi như pha bù.

POST /api/v1/kitchen/orders/{orderId}/incidents/{incidentId}/resolve nhận ReasonRequest, chỉ ghi kết quả xử lý. Bếp phải ghi đầy đủ hao hụt/pha bù thực tế bằng các API kho trước khi xác nhận xử lý; resolve không tự debit hay credit. Điều kiện thu ngân đóng phiên vẫn dựa trên đơn xong/hủy và hóa đơn hiệu lực thu đủ; sự cố còn OPEN tiếp tục được staff xử lý qua lịch sử, không cấp lại quyền guest phiên cũ.

Enum API CancellationStatus PENDING/APPROVED/REJECTED ánh xạ lần lượt cột TrangThai của YeuCauHuy thành CHO/CHAP_THUAN/TU_CHOI. Khai báo converter rõ ràng, không trộn EnumType.STRING tự động với cột dùng tên khác. Mọi Idempotency-Key tối đa 64 ký tự như MaYeuCau/KhoaYeuCau; quá độ dài trả lỗi validation, không cắt chuỗi.
