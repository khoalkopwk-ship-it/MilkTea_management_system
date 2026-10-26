# MilkTea — ảnh sản phẩm Cloudinary

## 1. Ranh giới

Cloudinary là dịch vụ ảnh bắt buộc. Backend gọi Java SDK từ ProductImageService; CloudinaryConfig chỉ tạo bean/Properties. DB Product giữ `imageUrl` tương ứng secure_url và `imagePublicId` tương ứng public_id; ảnh tĩnh logo/icon có thể nằm trong static.

Không mang Firebase/quota video/media mạng xã hội sang. Không lưu đơn, tiền hay quyền trên Cloudinary. Không cho client chọn public_id của sản phẩm khác hoặc upload trực tiếp bằng API secret.

## 2. Upload/thay ảnh

ADMIN dùng multipart API theo OpenAPI. Kiểm file không rỗng, giới hạn `APP_IMAGE_MAX_BYTES` (đề xuất 5 MiB cấu hình, không là hạn mức dịch vụ), allowlist JPEG/PNG/WebP và kiểm nội dung thay vì chỉ extension. Tên lưu do server sinh trong folder milktea/products, tránh ghi đè ảnh người khác.

Upload mới → lấy secure_url/public_id → lưu Product thành công → xóa ảnh cũ best effort. Nếu upload lỗi, giữ ảnh cũ. Nếu DB save lỗi sau upload, xóa ảnh mới best effort hoặc ghi cleanup task/nhật ký an toàn để làm lại; SQL rollback không rollback file Cloudinary. Không giữ lock DB lâu trong upload mạng.

Ngừng bán Product không bắt buộc xóa ảnh đã dùng trong snapshot lịch sử. Không delete ảnh cũ trước upload và lưu ảnh mới thành công. Khi hai upload cùng Product, dùng version/conditional update khi ghi URL/public_id: thao tác stale không ghi đè ảnh mới của thao tác khác; cleanup chỉ ảnh do thao tác đó tạo/sở hữu. Xác định ảnh cũ thực sự thay thế tại thời điểm ghi, không dựa một snapshot đọc từ lâu.

## 3. Cấu hình và kiểm chứng

`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` qua environment; không trong Git/templates/log. Timeout có cấu hình; API lỗi theo Architecture. Không tự dùng dịch vụ khác khi SDK lỗi.

Kiểm upload thật, DB có URL/public_id, web hiển thị ảnh; kiểm replace lỗi vẫn giữ cũ và fake extension/quá lớn bị chặn. [Tài liệu Java chính thức](https://cloudinary.com/documentation/java_image_and_video_upload); chọn SDK version tương thích project thực dùng.
