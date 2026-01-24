package com.example.grapefield2.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
        import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "performances")
@Data
public class Performance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(name = "performance_id", unique = true, nullable = false)
    private String performanceId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    private String subtitle;

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "end_date")
    private String endDate;

    @Column(name = "venue_name")
    private String venue;

    @Column(name = "genre_name")
    private String genre;

    private String state;

    @Column(name = "area")
    private String area; // 서울특별시, 경기도 등

    @Column(name = "ticket_price")
    private String ticketPrice;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(); // 저장 직전에 현재 시간을 자동으로 설정
    }
}