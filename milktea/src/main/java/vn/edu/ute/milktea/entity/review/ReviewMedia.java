package vn.edu.ute.milktea.entity.review;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Thực thể lưu trữ media (ảnh/video) đính kèm của đánh giá (CUS-06).
 */
@Entity
@Table(name = "DanhGiaMedia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaMedia")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDanhGia", nullable = false)
    private ProductReview review;

    @Column(name = "Url", nullable = false, length = 1000)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "LoaiMedia", nullable = false, length = 16)
    private ReviewMediaType mediaType;

    @Column(name = "TaoLuc", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
