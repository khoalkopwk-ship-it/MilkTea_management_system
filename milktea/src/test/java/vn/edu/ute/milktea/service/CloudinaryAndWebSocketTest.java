package vn.edu.ute.milktea.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.settings.GlobalSettings;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.settings.GlobalSettingsRepository;
import vn.edu.ute.milktea.service.media.ProductImageService;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryAndWebSocketTest {

        @Mock
        private Cloudinary cloudinary;

        @Mock
        private Uploader uploader;

        @Mock
        private ProductRepository productRepository;

        @Mock
        private GlobalSettingsRepository settingsRepository;

        @Mock
        private AccountRepository accountRepository;

        @Mock
        private RealtimeEventPublisher realtimeEventPublisher;

        private ProductImageService service;

        // Magic bytes mẫu
        private static final byte[] JPEG_BYTES = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00,
                        0x10, 0x4A, 0x46 };
        private static final byte[] PNG_BYTES = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00, 0x00 };
        private static final byte[] FAKE_TEXT_BYTES = "Not an image content".getBytes();

        @BeforeEach
        void setUp() {
                service = new ProductImageService(
                                cloudinary,
                                productRepository,
                                settingsRepository,
                                accountRepository,
                                realtimeEventPublisher);
                ReflectionTestUtils.setField(service, "cloudName", "Milktea");
                ReflectionTestUtils.setField(service, "apiKey", "test-api-key");
                ReflectionTestUtils.setField(service, "apiSecret", "test-api-secret");
        }

        @Test
        @DisplayName("Ném lỗi khi file rỗng hoặc kích thước = 0")
        void testUploadEmptyFile() {
                MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);
                BusinessException ex = assertThrows(BusinessException.class,
                                () -> service.uploadProductImage(1L, emptyFile));
                assertTrue(ex.getMessage().contains("trống"));
        }

        @Test
        @DisplayName("Ném lỗi khi file có extension giả mạo (không đúng magic bytes)")
        void testUploadFakeImage() {
                MockMultipartFile fakeFile = new MockMultipartFile("file", "fake.jpg", "image/jpeg", FAKE_TEXT_BYTES);
                BusinessException ex = assertThrows(BusinessException.class,
                                () -> service.uploadProductImage(1L, fakeFile));
                assertTrue(ex.getMessage().contains("không phải định dạng ảnh hợp lệ"));
        }

        @Test
        @DisplayName("Ném lỗi khi chưa cấu hình biến môi trường Cloudinary")
        void testCloudinaryNotConfigured() {
                ProductImageService unconfiguredService = new ProductImageService(
                                cloudinary,
                                productRepository,
                                settingsRepository,
                                accountRepository,
                                realtimeEventPublisher);
                ReflectionTestUtils.setField(unconfiguredService, "cloudName", "");
                ReflectionTestUtils.setField(unconfiguredService, "apiKey", "");
                ReflectionTestUtils.setField(unconfiguredService, "apiSecret", "");

                MockMultipartFile validFile = new MockMultipartFile("file", "valid.jpg", "image/jpeg", JPEG_BYTES);
                BusinessException ex = assertThrows(BusinessException.class,
                                () -> unconfiguredService.uploadProductImage(1L, validFile));
                assertTrue(ex.getMessage().contains("chưa được cấu hình"));
        }

        @Test
        @DisplayName("Upload ảnh món uống thành công: lưu DB, xóa ảnh cũ nếu không dùng chung, phát CATALOG_CHANGED")
        void testUploadProductImageSuccess() throws IOException {
                Product product = Product.builder()
                                .id(10L)
                                .name("Trà sữa Oolong")
                                .imageUrl("https://res.cloudinary.com/milktea/old.jpg")
                                .imagePublicId("milktea/products/old_id")
                                .build();

                when(productRepository.findById(10L)).thenReturn(Optional.of(product));
                when(cloudinary.uploader()).thenReturn(uploader);
                when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                                "secure_url", "https://res.cloudinary.com/milktea/new.jpg",
                                "public_id", "milktea/products/new_id"));
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(productRepository.countByImagePublicId("milktea/products/old_id")).thenReturn(0L);

                MockMultipartFile file = new MockMultipartFile("file", "oolong.png", "image/png", PNG_BYTES);
                var result = service.uploadProductImage(10L, file);

                assertEquals("https://res.cloudinary.com/milktea/new.jpg", result.getImageUrl());
                assertEquals("milktea/products/new_id", result.getImagePublicId());

                // Kiểm tra xóa ảnh cũ vì countByImagePublicId = 0
                verify(uploader, times(1)).destroy(eq("milktea/products/old_id"), anyMap());
                // Kiểm tra phát realtime
                verify(realtimeEventPublisher, times(1)).publishAfterCommit(eq("/topic/admin"), eq("CATALOG_CHANGED"),
                                anyString(), any(), anyString(), any());
                verify(realtimeEventPublisher, times(1)).publishAfterCommit(eq("/topic/cashier"), eq("CATALOG_CHANGED"),
                                anyString(), any(), anyString(), any());
        }

        @Test
        @DisplayName("Chống ảnh mồ côi: rollback destroy ảnh Cloudinary mới nếu DB save thất bại")
        void testRollbackOrphanImageOnDbFailure() throws IOException {
                Product product = Product.builder()
                                .id(20L)
                                .name("Trà Đào")
                                .build();

                when(productRepository.findById(20L)).thenReturn(Optional.of(product));
                when(cloudinary.uploader()).thenReturn(uploader);
                when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                                "secure_url", "https://res.cloudinary.com/milktea/new_orphan.jpg",
                                "public_id", "milktea/products/orphan_id"));
                when(productRepository.save(any(Product.class))).thenThrow(new RuntimeException("Database error!"));

                MockMultipartFile file = new MockMultipartFile("file", "dao.jpg", "image/jpeg", JPEG_BYTES);
                assertThrows(RuntimeException.class, () -> service.uploadProductImage(20L, file));

                // Phải hủy ảnh vừa upload trên Cloudinary để tránh mồ côi
                verify(uploader, times(1)).destroy(eq("milktea/products/orphan_id"), anyMap());
        }

        @Test
        @DisplayName("Không xóa ảnh cũ nếu còn sản phẩm khác dùng chung publicId")
        void testDoNotDeleteSharedOldImage() throws IOException {
                Product product = Product.builder()
                                .id(30L)
                                .imagePublicId("shared_public_id")
                                .build();

                when(productRepository.findById(30L)).thenReturn(Optional.of(product));
                when(cloudinary.uploader()).thenReturn(uploader);
                when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                                "secure_url", "https://res.cloudinary.com/milktea/brand_new.jpg",
                                "public_id", "milktea/products/brand_new_id"));
                when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
                // Đang có 2 sản phẩm khác dùng chung ảnh này
                when(productRepository.countByImagePublicId("shared_public_id")).thenReturn(2L);

                MockMultipartFile file = new MockMultipartFile("file", "share.jpg", "image/jpeg", JPEG_BYTES);
                service.uploadProductImage(30L, file);

                // KHÔNG ĐƯỢC xóa ảnh cũ shared_public_id
                verify(uploader, never()).destroy(eq("shared_public_id"), anyMap());
        }

        @Test
        @DisplayName("Upload Logo quán thành công: lưu GlobalSettings và phát SETTINGS_CHANGED")
        void testUploadLogoImageSuccess() throws IOException {
                GlobalSettings settings = GlobalSettings.builder().id(1).shopName("MilkTea").build();
                when(settingsRepository.findById(1)).thenReturn(Optional.of(settings));
                when(cloudinary.uploader()).thenReturn(uploader);
                when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                                "secure_url", "https://res.cloudinary.com/milktea/logo.png",
                                "public_id", "milktea/branding/logo_id"));
                when(settingsRepository.save(any(GlobalSettings.class))).thenAnswer(i -> i.getArgument(0));

                MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG_BYTES);
                var res = service.uploadLogoImage(file, 1L);

                assertEquals("https://res.cloudinary.com/milktea/logo.png", res.getLogoUrl());
                assertEquals("milktea/branding/logo_id", res.getLogoPublicId());
                verify(realtimeEventPublisher, times(1)).publishAfterCommit(eq("/topic/admin"), eq("SETTINGS_CHANGED"),
                                anyString(), any(), anyString(), any());
                verify(realtimeEventPublisher, times(1)).publishAfterCommit(eq("/topic/cashier"),
                                eq("SETTINGS_CHANGED"), anyString(), any(), anyString(), any());
        }

        @Test
        @DisplayName("Upload Avatar nhân viên thành công: lưu Account và phát STAFF_CHANGED")
        void testUploadAvatarSuccess() throws IOException {
                Account staff = Account.builder().id(5L).email("staff@milktea.vn").role(Role.CASHIER).build();
                when(accountRepository.findById(5L)).thenReturn(Optional.of(staff));
                when(cloudinary.uploader()).thenReturn(uploader);
                when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                                "secure_url", "https://res.cloudinary.com/milktea/avatar.jpg",
                                "public_id", "milktea/avatars/avatar_id"));
                when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

                MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", JPEG_BYTES);
                String avatarUrl = service.uploadAvatarImage(5L, file);

                assertEquals("https://res.cloudinary.com/milktea/avatar.jpg", avatarUrl);
                verify(realtimeEventPublisher, times(1)).publishAfterCommit(eq("/topic/admin"), eq("STAFF_CHANGED"),
                                eq("5"), any(), anyString(), any());
        }
}
