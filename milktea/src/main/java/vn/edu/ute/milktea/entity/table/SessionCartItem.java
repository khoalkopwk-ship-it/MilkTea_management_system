package vn.edu.ute.milktea.entity.table;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.catalog.Product;

@Entity
@Table(name = "ChiTietGioPhien", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_ChiTietGioPhien_Mon", columnNames = {"MaGio", "MaMon"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDong")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaGio", nullable = false)
    private SessionCart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaMon", nullable = false)
    private Product product;

    @Column(name = "SoLuong", nullable = false)
    private Integer quantity;

    @Column(name = "GhiChu", length = 500)
    private String note;
}
