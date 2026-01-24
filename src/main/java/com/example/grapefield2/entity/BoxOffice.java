package com.example.grapefield2.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "box_office",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"performance_id", "genre"})
        }
)
@Data
public class BoxOffice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false)
    private Integer rnum; // 순위

    @Column(name = "performance_id", nullable = false)
    private String performanceId;

    @Column(nullable = false)
    private String genre;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
