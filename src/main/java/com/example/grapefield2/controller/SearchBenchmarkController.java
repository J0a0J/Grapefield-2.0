package com.example.grapefield2.controller;

import com.example.grapefield2.entity.Performance;
import com.example.grapefield2.service.PerformanceService;
import com.example.grapefield2.service.SimpleOpenSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/benchmark")
@RequiredArgsConstructor
@Tag(name = "성능 벤치마크", description = "MariaDB vs OpenSearch 검색 응답시간 비교")
public class SearchBenchmarkController {

    private final PerformanceService performanceService;
    private final SimpleOpenSearchService openSearchService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "검색 성능 비교", description = "같은 키워드로 MariaDB(LIKE)와 OpenSearch를 각각 호출해 응답시간을 비교합니다.")
    @GetMapping("/search")
    public Map<String, Object> compareSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", keyword);
        result.put("page", page);
        result.put("size", size);

        // MariaDB LIKE 검색
        StopWatch mariaWatch = new StopWatch();
        mariaWatch.start();
        Page<Performance> mariaResult = performanceService.searchByKeyword(keyword, page, size);
        mariaWatch.stop();

        long mariaMs = mariaWatch.getLastTaskTimeMillis();
        Map<String, Object> mariaStats = new LinkedHashMap<>();
        mariaStats.put("engine", "MariaDB (LIKE)");
        mariaStats.put("totalHits", mariaResult.getTotalElements());
        mariaStats.put("returnedCount", mariaResult.getContent().size());
        mariaStats.put("elapsedMs", mariaMs);
        result.put("mariaDB", mariaStats);

        // OpenSearch 검색
        StopWatch osWatch = new StopWatch();
        osWatch.start();
        String osRaw = openSearchService.search(keyword, page, size);
        osWatch.stop();

        long osMs = osWatch.getLastTaskTimeMillis();
        long osTotalHits = 0;
        int osReturnedCount = 0;
        try {
            JsonNode root = objectMapper.readTree(osRaw);
            osTotalHits = root.path("hits").path("total").path("value").asLong();
            osReturnedCount = root.path("hits").path("hits").size();
        } catch (Exception e) {
            log.warn("OpenSearch 결과 파싱 실패: {}", e.getMessage());
        }

        Map<String, Object> osStats = new LinkedHashMap<>();
        osStats.put("engine", "OpenSearch (nori + ngram)");
        osStats.put("totalHits", osTotalHits);
        osStats.put("returnedCount", osReturnedCount);
        osStats.put("elapsedMs", osMs);
        result.put("openSearch", osStats);

        // 요약
        long diff = mariaMs - osMs;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("fasterEngine", diff > 0 ? "OpenSearch" : (diff < 0 ? "MariaDB" : "동일"));
        summary.put("differenceMs", Math.abs(diff));
        summary.put("speedupRatio", osMs > 0 ? String.format("%.2fx", (double) mariaMs / osMs) : "N/A");
        result.put("summary", summary);

        log.info("검색 벤치마크 - keyword: {} | MariaDB: {}ms ({} hits) | OpenSearch: {}ms ({} hits)",
                keyword, mariaMs, mariaResult.getTotalElements(), osMs, osTotalHits);

        return result;
    }
}
