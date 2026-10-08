package vn.edu.ute.milktea.entity.recipe;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.inventory.Material;

import java.math.BigDecimal;

@Entity
@Table(name = "CongThucSoChe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreparationRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaCTSC")
    private Long id;

    @Column(name = "TenCT", nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNLDauRa", nullable = false)
    private Material outputMaterial;

    @Column(name = "LuongDauRaChuan", nullable = false, precision = 18, scale = 3)
    private BigDecimal standardOutputQuantity;

    @Column(name = "HoatDong", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
