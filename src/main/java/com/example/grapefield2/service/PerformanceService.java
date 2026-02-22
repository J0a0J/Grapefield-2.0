package com.example.grapefield2.service;

import com.example.grapefield2.entity.Performance;
import com.example.grapefield2.repository.BoxOfficeRepository;
import com.example.grapefield2.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final BoxOfficeRepository boxOfficeRepository;

    /**
     * 최신순 공연 목록 조회
     * @param genre "ALL" 또는 특정 장르 ("연극", "뮤지컬" 등)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 개수
     * @return 공연 목록
     */
    public List<Performance> getLatestPerformances(String genre, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        List<String> activeStates = Arrays.asList("공연예정", "공연중");

        return performanceRepository.findByStatesAndGenre(
                activeStates, genre, pageable
        );
    }

    /**
     * 박스오피스 순 공연 목록
     * @param genre
     * @param page
     * @param size
     * @return
     */
    public List<Performance> getPopularPerformances(String genre, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        log.debug("genre name : {}", genre);

        return boxOfficeRepository.findPerformancesByGenreOrderByRank(
                genre,
                pageable
        );
    }

    /**
     * 마감순 공연 목록
     * @param genre
     * @param page
     * @param size
     * @return
     */
    public List<Performance> getDeadlinePerformances(String genre, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<String> activeStates = Arrays.asList("공연예정", "공연중");

        return performanceRepository.findByStatesAndGenreOrderByEndDate(
                activeStates, genre, pageable
        );
    }

    /**
     * 성능 비교용: MariaDB LIKE 키워드 검색
     */
    public Page<Performance> searchByKeyword(String keyword, int page, int size) {
        StopWatch sw = new StopWatch("MariaDB-search");
        Pageable pageable = PageRequest.of(page, size);

        sw.start("LIKE query");
        Page<Performance> result = performanceRepository.searchPerformances(keyword, pageable);
        sw.stop();

        log.info("MariaDB 검색 - keyword: {} | {}건 | {}ms",
                keyword, result.getTotalElements(), sw.getLastTaskTimeMillis());

        return result;
    }

    public ResponseEntity<Map<String, Object>> getPerformancesByDate(String date) {
        try {
            LocalDate inputDate = LocalDate.parse(date.substring(0, 10));

            // 해당 월의 1일
            LocalDate startOfMonth = inputDate.withDayOfMonth(1);
            // 해당 월의 마지막 날
            LocalDate endOfMonth = inputDate.withDayOfMonth(inputDate.lengthOfMonth());

            // DB 형식으로 변환
            String startDate = startOfMonth.toString().replace("-", ".");
            String endDate = endOfMonth.toString().replace("-", ".");

            log.info("조회기간 : {} ~ {}", startDate, endDate);

            // 해당 기간에 시작하는 공연 조회
            List<Performance> performances = performanceRepository.findByDateBetween(startDate, endDate);

            Map<String, Object> result = new HashMap<>();
            result.put("startEvents", performances);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("달력 조회 오류 : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

    }
}
