package com.example.grapefield2.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetailResponse {
    // Performance 기본 정보
    private Long idx;
    private String performanceId;
    private String title;
    private String startDate;
    private String endDate;
    private String venue;
    private String poster;
    private String state;
    private String area;
    private String genre;
    private String price;

    // PerformanceDetail 정보
    private String castInfo;
    private String crewInfo;
    private String runtime;
    private String ageLimit;
    private String showtimes;
    private String producer;
    private String agency;
    private String story;
    private String venueId;
    private String isDaehakro;

    // 파싱된 데이터
    private List<TicketSiteInfo> ticketSites;
    private List<String> introImages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketSiteInfo {
        private String siteName;
        private String url;
    }
}
