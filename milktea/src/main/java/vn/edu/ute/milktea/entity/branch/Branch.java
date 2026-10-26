package vn.edu.ute.milktea.entity.branch;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ChiNhanh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaCN")
    private Long id;

    @Column(name = "MaDinhDanh", nullable = false, unique = true, length = 20)
    private String code; // e.g. CN01, CN02

    @Column(name = "TenChiNhanh", nullable = false, length = 150)
    private String name;

    @Column(name = "DiaChi", nullable = false, length = 255)
    private String address;

    @Column(name = "SoDienThoai", length = 20)
    private String phone;

    @Column(name = "TrangThai", nullable = false, length = 30)
    @Builder.Default
    private String status = "HOAT_DONG";

    @Column(name = "ThoiGianTao", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public boolean isActive() {
        return "HOAT_DONG".equalsIgnoreCase(status);
    }
}
