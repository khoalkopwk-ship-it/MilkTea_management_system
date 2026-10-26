package vn.edu.ute.milktea.entity.catalog;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;

import java.time.Instant;

/**
 * Thực thể Sản phẩm yêu thích (Wishlist - CUS-05).
 * Tài khoản khách hàng chung toàn chuỗi, không trùng cặp (MaTK, MaMon).
 */
@Entity
@Table(name = "SanPhamYeuThich", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_SanPhamYeuThich_TaiKhoan_Mon", columnNames = {"MaTK", "MaMon"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaYeuThich")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTK", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaMon", nullable = false)
    private Product product;

    @Column(name = "TaoLuc", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
