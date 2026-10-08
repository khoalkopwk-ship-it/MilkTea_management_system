package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietNhap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportReceiptItem {

    @EmbeddedId
    private ImportReceiptItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("receiptId")
    @JoinColumn(name = "MaPhieu", nullable = false)
    private ImportReceipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("materialId")
    @JoinColumn(name = "MaNL", nullable = false)
    private Material material;

    @Column(name = "SoLuong", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "DonGiaNhap", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;
}
