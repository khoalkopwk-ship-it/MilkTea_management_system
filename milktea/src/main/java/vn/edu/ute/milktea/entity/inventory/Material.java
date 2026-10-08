package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "NguyenLieu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaNL")
    private Long id;

    @Column(name = "TenNL", nullable = false, unique = true, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "Loai", nullable = false, length = 8)
    private MaterialType type; // THO, SOCHE

    @Column(name = "DonVi", nullable = false, length = 16)
    private String unit; // kg, g, ml, ly, cai, suat

    @Column(name = "HoatDong", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
