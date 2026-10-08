package vn.edu.ute.milktea.entity.table;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Ban")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaBan")
    private Long id;

    @Column(name = "TenBan", nullable = false, unique = true, length = 40)
    private String name;

    @Column(name = "MaQR", nullable = false, unique = true, length = 64)
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 16)
    @Builder.Default
    private TableStatus status = TableStatus.TRONG;

    @Column(name = "MaPhienDangMo")
    private Long activeSessionId;

    @Column(name = "HoatDong", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Version
    @Column(name = "Version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
