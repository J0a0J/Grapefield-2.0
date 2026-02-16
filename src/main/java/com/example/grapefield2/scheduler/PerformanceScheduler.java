package com.example.grapefield2.scheduler;

import com.example.grapefield2.dto.BoxOfficeDto;
import com.example.grapefield2.entity.BoxOffice;
import com.example.grapefield2.entity.Performance;
import com.example.grapefield2.repository.BoxOfficeRepository;
import com.example.grapefield2.repository.PerformanceRepository;
import com.example.grapefield2.service.KopisApiService;
import com.example.grapefield2.service.SimpleOpenSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PerformanceScheduler {

    private final KopisApiService kopisService;
    private final PerformanceRepository performanceRepository;
    private final BoxOfficeRepository boxOfficeRepository;
    private final SimpleOpenSearchService simpleOpenSearchService;

    // 매일 새벽 1시 - 전체 장르 수집
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailyPerformanceUpdate() {
        kopisService.collectAllGenres();
    }

    // 매일 새벽 2시 - 불완전 공연 업데이트
    @Scheduled(cron = "0 0 2 * * *")
    public void updateIncompletePerformances() {
        try {
            kopisService.updateIncompletePerformance();
            log.info("불완전 공연 업데이트 완료");
        } catch (Exception e) {
            log.error("불완전 공연 업데이트 실패", e);
        }
    }

    // 매일 자정 - 마감 공연 상태 변경
    @Scheduled(cron = "0 0 0 * * *")
    public void updateExpiredPerformances() {
        log.info("=== 공연 상태 업데이트 시작 ===");

        String today = LocalDate.now().toString().replace("-", ".");
        log.info("오늘 날짜: {}", today);

        List<Performance> expiredList = performanceRepository
                .findByStateNotAndEndDateLessThan("공연완료", today);

        log.info("찾은 공연 수: {}", expiredList.size());

        for (Performance p : expiredList) {
            log.debug("업데이트: {} ({})", p.getTitle(), p.getEndDate());
            p.setState("공연완료");
            performanceRepository.save(p);
        }

        log.info("공연 상태 업데이트 완료: {}건", expiredList.size());
    }

    // 매일 새벽 4시 - 박스오피스 업데이트
    @Scheduled(cron = "0 0 4 * * *")
    public void updateBoxOffice() throws Exception {
        boxOfficeRepository.deleteAll();
        List<String> genres = Arrays.asList("ALL", "연극", "뮤지컬", "대중음악", "서양음악(클래식)", "복합", "한국음악(국악)");

        List<Performance> allPerformances = performanceRepository.findAll();
        Map<String, Performance> performanceMap = allPerformances.stream()
                .collect(Collectors.toMap(Performance::getPerformanceId, p -> p));

        Set<String> savedPerformanceIds = new HashSet<>();

        for (String genre : genres) {
            List<BoxOfficeDto> apiData = kopisService.fetchBoxOfficeFromKopis(genre);
            LocalDateTime now = LocalDateTime.now();

            for(BoxOfficeDto dto : apiData) {
                Performance performance = performanceMap.get(dto.getPerformanceId());

                if (performance == null) {
                    log.warn("Performance not found: {}", dto.getPerformanceId());
                    kopisService.fetchDetailInfo(dto.getPerformanceId());
                    Thread.sleep(800);

                    performance = performanceRepository.findByPerformanceId(dto.getPerformanceId());
                    if (performance == null) {
                        continue;
                    }
                }

                BoxOffice bo = new BoxOffice();
                bo.setRnum(dto.getRnum());
                bo.setPerformanceId(dto.getPerformanceId());
                bo.setGenre(genre);
                bo.setUpdatedAt(now);
                boxOfficeRepository.save(bo);

                kopisService.fetchDetailInfo(dto.getPerformanceId());
                Thread.sleep(800);
            }
        }

        log.info("박스오피스 업데이트 완료");
    }

    @Scheduled(cron = "0 55 1 * * *")
    public void syncPerformances() {
        try {
            String result = simpleOpenSearchService.syncAllPerformances();
            log.info("OpenSearch 동기화 완료: {}", result);
        } catch (Exception e) {
            log.error("OpenSearch 동기화 실패", e);
        }
    }
}
