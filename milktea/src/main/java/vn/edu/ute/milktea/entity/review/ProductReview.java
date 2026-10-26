package vn.edu.ute.milktea.entity.review;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.order.OrderItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể Đánh giá & Bình luận sản phẩm (CUS-06).
 * Gắn liền với ChiTietDonHang (mỗi dòng chi tiết đơn chỉ được đánh giá 1 lần).
 * Khách hàng phải sở hữu đơn hàng đã HOAN_THANH và đã thanh toán.
 */
@Entity
@Table(name = "DanhGia", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_DanhGia_ChiTietDonHang", columnNames = {"MaChiTiet"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDanhGia")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaChiTiet", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTK", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaMon", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaCN", nullable = false)
    private Branch branch;

    @Column(name = "SoSao", nullable = false)
    private Integer rating; // 1 đến 5 sao

    @Column(name = "NoiDung", nullable = false, length = 1000)
    private String content; // Tối thiểu 50 ký tự

    @Column(name = "An", nullable = false)
    @Builder.Default
    private Boolean hidden = false;

    @Column(name = "LyDoAn", length = 500)
    private String hiddenReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiAn")
    private Account hiddenBy;

    @Column(name = "AnLuc")
    private Instant hiddenAt;

    @Column(name = "TaoLuc", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "CapNhatLuc", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReviewMedia> mediaList = new ArrayList<>();
}
