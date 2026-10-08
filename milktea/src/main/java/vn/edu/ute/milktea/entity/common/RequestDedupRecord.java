package vn.edu.ute.milktea.entity.common;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "BanGhiChongLap", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_BanGhiChongLap", columnNames = {"PhamVi", "KhoaYeuCau"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDedupRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaBanGhi")
    private Long id;

    @Column(name = "PhamVi", nullable = false, length = 128)
    private String scope;

    @Column(name = "KhoaYeuCau", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "DauVanPayload", length = 128)
    private String payloadFingerprint;

    @Column(name = "LoaiTaiNguyen", length = 40)
    private String resourceType;

    @Column(name = "MaTaiNguyen")
    private Long resourceId;

    @Column(name = "TaoLuc", nullable = false)
    private Instant createdAt;

    @Column(name = "HetHanLuc", nullable = false)
    private Instant expiresAt;
}
