package com.example.grapefield2.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "performance_detail")
@Data
public class PerformanceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @OneToOne
    @JoinColumn(name = "performance_id", referencedColumnName = "idx")
    @JsonBackReference
    private Performance performance;

    @Column(name = "cast_info", columnDefinition = "TEXT")
    private String castInfo; // prfcast - 출연진

    @Column(name = "crew_info", columnDefinition = "TEXT")
    private String crewInfo;

    @Column(name = "runtime")
    private String runtime;

    @Column(name = "age_limit")
    private String ageLimit;

    @Column(name = "show_times", columnDefinition = "TEXT")
    private String showTimes;

    @Column(name = "producer")
    private String producer;

    @Column(name = "agency")
    private String agency;

    @Column(name = "story", columnDefinition = "TEXT")
    private String story; // sty - 줄거리

    @Column(name = "venue_id")
    private String venueId; // 공연시설 ID

    @Column(name = "is_daehakro")
    private String isDaehakro; // 대학로에서 공연하는지

    @Column(name = "ticket_urls", columnDefinition = "TEXT")
    private String ticketUrls; // 예매 url, JSON 형태로

    @Column(name = "intro_image_urls", columnDefinition = "TEXT")
    private String introImageUrls; // JSON 배열로 여러 URL 저장
}
