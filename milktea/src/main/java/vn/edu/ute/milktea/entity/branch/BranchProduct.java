package vn.edu.ute.milktea.entity.branch;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.catalog.Product;

import java.time.Instant;

@Entity
@Table(name = "MonUongChiNhanh", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_MonUongChiNhanh_CN_Mon", columnNames = {"MaCN", "MaMon"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaMonChiNhanh")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaCN", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaMon", nullable = false)
    private Product product;

    @Column(name = "KinhDoanh", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "ConPhucVu", nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "CapNhatLuc", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
