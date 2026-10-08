package vn.edu.ute.milktea.entity.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "MonUong", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_MonUong_Ten_Size", columnNames = {"TenMon", "Size"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaMon")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDM", nullable = false)
    private Category category;

    @Column(name = "TenMon", nullable = false, length = 120)
    private String name;

    @Column(name = "Size", nullable = false, length = 12)
    private String size; // S, M, L

    @Column(name = "MoTa", length = 1000)
    private String description;

    @Column(name = "Gia", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "AnhUrl", length = 1000)
    private String imageUrl;

    @Column(name = "AnhPublicId", length = 255)
    private String imagePublicId;

    @Column(name = "HoatDong", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
