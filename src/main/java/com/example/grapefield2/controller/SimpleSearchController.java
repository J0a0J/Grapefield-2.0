package com.example.grapefield2.controller;

import com.example.grapefield2.service.SimpleOpenSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "검색", description = "OpenSearch 기반 검색 API")
public class SimpleSearchController {

    private final SimpleOpenSearchService openSearchService;

    @Operation(summary = "인덱스 생성", description = "OpenSearch 인덱스 생성 (관리자)")
    @PostMapping("/create-index")
    public String createIndex() {
        return openSearchService.createIndex();
    }

    @Operation(summary = "통합 검색", description = "키워드 기반 전체 검색")
    @GetMapping("/all")
    public String searchAll(@RequestParam String keyword,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "12") int size,
                            @RequestParam(required = false) String date) {
        try {
            String decodedKeyword = java.net.URLDecoder.decode(keyword, "UTF-8");
            log.debug("검색 요청: keyword={}", decodedKeyword);
            return openSearchService.search(decodedKeyword, page, size, date);
        } catch (Exception e) {
            return "검색 오류: " + e.getMessage();
        }
    }

    @Operation(summary = "공연 검색", description = "공연 정보 키워드 검색")
    @GetMapping("/events")
    public String searchEvents(@RequestParam String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size,
                               @RequestParam(required = false) String date) {
        log.debug("공연 검색: keyword={}", keyword);
        return openSearchService.search(keyword, page, size, date);
    }

    @Operation(summary = "데이터 동기화", description = "DB → OpenSearch 동기화 (관리자)")
    @PostMapping("/sync-performances")
    public String syncPerformances() {
        return openSearchService.syncAllPerformances();
    }

    @Operation(summary = "자동완성", description = "검색어 자동완성 추천")
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@RequestParam String prefix) {
        try {
            // URL 디코딩
            String decodedPrefix = java.net.URLDecoder.decode(prefix, "UTF-8");

            // OpenSearch에서 자동완성 검색
            String searchResult = openSearchService.searchAutocomplete(decodedPrefix, 10);

            // 제목만 추출
            List<String> titles = extractTitles(searchResult);

            return ResponseEntity.ok(titles);
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    private List<String> extractTitles(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode hits = root.path("hits").path("hits");

            List<String> titles = new ArrayList<>();
            for (JsonNode hit : hits) {
                String title = hit.path("_source").path("title").asText();
                titles.add(title);
            }

            return titles;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
