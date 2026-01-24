package com.example.grapefield2.controller;

import com.example.grapefield2.service.SimpleOpenSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@CrossOrigin(origins = "http://localhost:5173")
public class SimpleSearchController {

    @Autowired
    private SimpleOpenSearchService openSearchService;

    // 인덱스 생성
    @PostMapping("/create-index")
    public String createIndex() {
        return openSearchService.createIndex();
    }

    // 테스트 데이터 추가
//    @PostMapping("/add-test")
//    public String addTestData() {
//        openSearchService.indexDocument("1", "라이온 킹", "샤롯데씨어터", "뮤지컬", "PF123456");
//        openSearchService.indexDocument("2", "오페라의 유령", "블루스퀘어", "뮤지컬", "PF234567");
//        openSearchService.indexDocument("3", "햄릿", "국립극장", "연극", "PF345678");
//        return "테스트 데이터 추가 완료";
//    }

    // 검색 API (기존 API 호환)
    @GetMapping("/all")
    public String searchAll(@RequestParam String keyword,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "12") int size) {
        try {
            System.out.println("OpenSearch 검색: " + keyword);
            // URL 디코딩 처리
            String decodedKeyword = java.net.URLDecoder.decode(keyword, "UTF-8");
            System.out.println("디코딩된 키워드: " + decodedKeyword);
            return openSearchService.search(decodedKeyword, page, size);
        } catch (Exception e) {
            return "검색 오류: " + e.getMessage();
        }
    }
    @GetMapping("/events")
    public String searchEvents(@RequestParam String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size) {
        System.out.println("OpenSearch 공연 검색: " + keyword);
        return openSearchService.search(keyword, page, size);
    }

    @PostMapping("/sync-performances")
    public String syncPerformances() {
        return openSearchService.syncAllPerformances();
    }
}