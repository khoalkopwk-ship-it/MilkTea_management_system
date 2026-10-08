package vn.edu.ute.milktea.entity.account;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "TaiKhoan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTK")
    private Long id;

    @Column(name = "Email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "MatKhauBam", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "HoTen", length = 120)
    private String fullName;

    @Column(name = "DienThoai", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "VaiTro", nullable = false, length = 16)
    private Role role;

    @Column(name = "HoatDong", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "TokenVersion", nullable = false)
    @Builder.Default
    private Long tokenVersion = 1L;

    @Column(name = "TaoLuc", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (active == null) {
            active = true;
        }
        if (tokenVersion == null) {
            tokenVersion = 1L;
        }
    }
}
