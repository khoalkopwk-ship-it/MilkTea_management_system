package vn.edu.ute.milktea.entity.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "NhatKyNghiepVu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaNK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNKCha")
    private BusinessAudit parent;

    @Column(name = "MaPhien")
    private Long sessionId;

    @Column(name = "MaDH")
    private Long orderId;

    @Column(name = "MaTK")
    private Long accountId;

    @Column(name = "HanhDong", nullable = false, length = 40)
    private String action;

    @Column(name = "Truoc", length = 500)
    private String beforeState;

    @Column(name = "Sau", length = 500)
    private String afterState;

    @Column(name = "LyDo", length = 1000)
    private String reason;

    @Column(name = "DuLieu", columnDefinition = "NVARCHAR(MAX)")
    private String dataJson;

    @Column(name = "TaoLuc", nullable = false)
    private Instant createdAt;
}
