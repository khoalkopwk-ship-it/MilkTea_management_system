package vn.edu.ute.milktea.entity.settings;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "CauHinhChung")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalSettings {

    @Id
    @Column(name = "MaCauHinh")
    private Integer id; // Cố định 1

    @Column(name = "TenQuan", nullable = false, length = 120)
    private String shopName;

    @Column(name = "DiaChi", nullable = false, length = 255)
    private String address;

    @Column(name = "NganHang", length = 120)
    private String bankName;

    @Column(name = "SoTaiKhoan", length = 120)
    private String bankAccountNumber;

    @Column(name = "ChuTK", length = 120)
    private String bankAccountHolder;

    @Column(name = "TyLeGiam", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiSua")
    private Account modifiedBy;

    @Column(name = "SuaLuc", nullable = false)
    private Instant modifiedAt;
}
