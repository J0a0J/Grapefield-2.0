package com.example.grapefield2.controller;

import com.example.grapefield2.dto.EventDetailResponse;
import com.example.grapefield2.entity.BoxOffice;
import com.example.grapefield2.entity.Performance;
import com.example.grapefield2.entity.PerformanceDetail;
import com.example.grapefield2.repository.BoxOfficeRepository;
import com.example.grapefield2.repository.PerformanceDetailRepository;
import com.example.grapefield2.repository.PerformanceRepository;
import com.example.grapefield2.service.KopisApiService;
import com.example.grapefield2.service.PerformanceService;
import com.example.grapefield2.service.SimpleOpenSearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/performances")
@CrossOrigin(origins = "http://localhost:5173") // vue 테스트 연동용
@RequiredArgsConstructor
public class PerformanceController {

    private final KopisApiService kopisApiService;
    private final PerformanceRepository performanceRepository;
    private final PerformanceDetailRepository performanceDetailRepository;
    private final BoxOfficeRepository boxOfficeRepository;
    private final PerformanceService performanceService;
    private final SimpleOpenSearchService simpleOpenSearchService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 공연 목록 조회 (통합)
     * @param category 장르 ("ALL", "연극", "뮤지컬", "클래식", "오페라", "국악")
     * @param array 정렬 ("new": 최신순, "popular": 박스오피스, "deadline": 마감순)
     * @param page 페이지 번호
     * @param size 페이지당 개수
     */
    @GetMapping("/")
    public ResponseEntity<List<Performance>> getPerformances(
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "new") String array,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        List<Performance> performances;

        switch (array) {
            case "new":
                performances = performanceService.getLatestPerformances(category, page, size);
                break;

            case "popular":
                performances = performanceService.getPopularPerformances(category, page, size);
                break;

            case "deadline":
                performances = performanceService.getDeadlinePerformances(category, page, size);
                break;

            default:
                performances = performanceService.getLatestPerformances(category, page, size);
        }

        return ResponseEntity.ok(performances);
    }

    @GetMapping("/contents/main")
    public ResponseEntity<Map<String, Object>> getMainContents(@RequestParam(defaultValue = "ALL") String category) {
        Map<String, Object> result = new java.util.HashMap<>();

        List<Performance> popular = performanceService.getPopularPerformances(category, 0, 10);

        List<Performance> deadline = performanceService.getDeadlinePerformances(category, 0, 10);

        List<Performance> latest = performanceService.getLatestPerformances(category, 0, 10);

        result.put("popular", popular);
        result.put("deadline", deadline);
        result.put("latest", latest);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/calendar")
    public ResponseEntity<Map<String, Object>> getCalendar(@RequestParam String date) {
        return performanceService.getPerformancesByDate(date);
    }

    // 공연 상세 조회
    @GetMapping("/{id}")
    public EventDetailResponse getPerformanceDetail(@PathVariable Long id) {
        Performance performance = performanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공연을 찾을 수 없습니다"));

        PerformanceDetail detail = performanceDetailRepository.findByPerformance_Idx(id);

        return convertToEventDetailResponse(performance, detail);
    }

    private EventDetailResponse convertToEventDetailResponse(Performance performance, PerformanceDetail detail) {
        EventDetailResponse response = EventDetailResponse.builder()
                // Performance 기본 정보
                .idx(performance.getIdx())
                .performanceId(performance.getPerformanceId())
                .title(performance.getTitle())
                .startDate(performance.getStartDate())
                .endDate(performance.getEndDate())
                .venue(performance.getVenue())
                .poster(performance.getPosterUrl())
                .state(performance.getState())
                .area(performance.getArea())
                .genre(performance.getGenre())
                .price(performance.getTicketPrice())
                .build();

        // PerformanceDetail 정보 추가
        if (detail != null) {
            response.setCastInfo(detail.getCastInfo());
            response.setCrewInfo(detail.getCrewInfo());
            response.setRuntime(detail.getRuntime());
            response.setAgeLimit(detail.getAgeLimit());
            response.setShowtimes(detail.getShowTimes());
            response.setProducer(detail.getProducer());
            response.setAgency(detail.getAgency());
            response.setStory(detail.getStory());
            response.setVenueId(detail.getVenueId());
            response.setIsDaehakro(detail.getIsDaehakro());

            // JSON 파싱
            response.setTicketSites(parseTicketSites(detail.getTicketUrls()));
            response.setIntroImages(parseIntroImages(detail.getIntroImageUrls()));
        }



        return response;
    }

    // 티켓 사이트 파싱 메서드
    private List<EventDetailResponse.TicketSiteInfo> parseTicketSites(String ticketUrlsJson) {
        if (ticketUrlsJson == null || ticketUrlsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, String>> rawList = objectMapper.readValue(
                    ticketUrlsJson,
                    new TypeReference<List<Map<String, String>>>() {}
            );

            return rawList.stream()
                    .map(map -> new EventDetailResponse.TicketSiteInfo(
                            map.get("siteName"),
                            map.get("url")
                    ))
                    .collect(Collectors.toList());

        } catch (JsonProcessingException e) {
            System.out.println("티켓 URL 파싱 실패: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // 소개 이미지 파싱 메서드
    private List<String> parseIntroImages(String introImagesJson) {
        if (introImagesJson == null || introImagesJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    introImagesJson,
                    new TypeReference<List<String>>() {}
            );
        } catch (JsonProcessingException e) {
            System.out.println("소개 이미지 파싱 실패: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @GetMapping("/popular")
    public List<Performance> getPopularPerformances() {
        return boxOfficeRepository.findPerformancesOrderByRank();
    }

    /**
     * 최신순 공연 목록 조회
     * @param category 장르 ("ALL", "연극", "뮤지컬" 등)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 개수
     * @return 공연 목록
     */
    @GetMapping("/new")
    public ResponseEntity<List<Performance>> getLatestPerformances(
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        List<Performance> performances = performanceService.getLatestPerformances(
                category, page, size
        );

        return ResponseEntity.ok(performances);
    }

    // PerformanceController.java에 추가
    @GetMapping("/search")
    public ResponseEntity<List<Performance>> searchPerformances(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        List<Performance> results = simpleOpenSearchService.searchToPerformances(keyword, page, size);
        // 페이징 적용 (필요시)
        int start = page * size;
        int end = Math.min(start + size, results.size());
        List<Performance> pagedResults = results.subList(start, end);

        return ResponseEntity.ok(pagedResults);
    }

    // 수동 데이터 수집(테스트용)
    @PostMapping("/admin/collect")
    public String collectPerformances() {
        try {
            kopisApiService.collectAllGenres();
            return "데이터 수집 완료!";
        } catch(Exception e) {
            return "데이터 수집 실패: " + e.getMessage();
        }
    }
}
