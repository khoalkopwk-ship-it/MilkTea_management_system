package vn.edu.ute.milktea.entity.catalog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DanhMuc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDM")
    private Long id;

    @Column(name = "TenDM", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "HoatDong", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
