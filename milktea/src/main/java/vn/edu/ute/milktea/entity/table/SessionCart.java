package vn.edu.ute.milktea.entity.table;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "GioPhien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGio")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaPhien", nullable = false, unique = true)
    private TableSession session;

    @Version
    @Column(name = "Version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "SuaLuc", nullable = false)
    private Instant modifiedAt;
}
