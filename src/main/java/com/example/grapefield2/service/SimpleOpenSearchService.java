package com.example.grapefield2.service;

import com.example.grapefield2.entity.Performance;
import com.example.grapefield2.repository.PerformanceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimpleOpenSearchService {

    private final String OPENSEARCH_URL = "http://localhost:9200";
    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 인덱스 생성
    public String createIndex() {
        try {
            String url = OPENSEARCH_URL + "/performances";

            // 기존 인덱스 삭제 시도
            try {
                restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
                System.out.println("기존 인덱스 삭제 완료");
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("삭제할 인덱스 없음 (정상)");
            }

            // 새 인덱스 생성
            String indexConfig = """
                    {
                        "settings": {
                            "index.max_ngram_diff": 8,
                            "analysis": {
                                "analyzer": {
                                    "korean_autocomplete": {
                                        "type": "custom",
                                        "tokenizer": "ngram_tokenizer",
                                        "filter": ["lowercase"]
                                    },
                                    "korean_search": {
                                        "type": "custom", 
                                        "tokenizer": "standard",
                                        "filter": ["lowercase"]
                                    }
                                },
                                "tokenizer": {
                                    "ngram_tokenizer": {
                                        "type": "ngram",
                                        "min_gram": 2,
                                        "max_gram": 10,
                                        "token_chars": ["letter", "digit"]
                                    }
                                }
                            }
                        },
                        "mappings": {
                            "properties": {
                                "idx": {"type": "integer"},
                                "title": {
                                    "type": "text",
                                    "fields": {
                                        "keyword": {"type": "keyword"},
                                        "autocomplete": {
                                            "type": "text",
                                            "analyzer": "korean_autocomplete",
                                            "search_analyzer": "korean_search"
                                        }
                                    }
                                },
                                "venue": {
                                    "type": "text",
                                    "fields": {
                                        "keyword": {"type": "keyword"},
                                        "autocomplete": {
                                            "type": "text", 
                                            "analyzer": "korean_autocomplete",
                                            "search_analyzer": "korean_search"
                                        }
                                    }
                                },
                                "genre": {"type": "keyword"},
                                "performanceId": {"type": "keyword"},
                                "posterUrl": {"type": "keyword", "index": false},    // 이미지 URL
                                "startDate": {"type": "date", "index": false},       // 시작일
                                "endDate": {"type": "date", "index": false}         // 종료일
                            }
                        }
                    }
                    """;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(indexConfig, headers);

            ResponseEntity<String> createResponse = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            return "생성 완료: " + createResponse.getBody();

        } catch (Exception e) {
            System.out.println("상세 오류: " + e.getMessage());
            e.printStackTrace();
            return "생성 실패: " + e.getMessage();
        }
    }

    public String indexDocument(Object idx, String title, String venue, String genre, String performanceId,
                                String posterUrl, String startDate, String endDate) {
        try {
            String url = OPENSEARCH_URL + "/performances/_doc/" + idx;

            String formattedStartDate = startDate.replace(".", "-");
            String formattedEndDate = endDate.replace(".", "-");

            Map<String, Object> doc = new HashMap<>();
            doc.put("idx", idx);
            doc.put("title", title);
            doc.put("venue", venue);
            doc.put("genre", genre);
            doc.put("performanceId", performanceId);
            doc.put("posterUrl", posterUrl);
            doc.put("startDate", formattedStartDate);
            doc.put("endDate", formattedEndDate);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(doc, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return "성공: " + response.getStatusCode();
        } catch (Exception e) {
            System.out.println("문서 인덱싱 실패: " + e.getMessage());
            return "실패: " + e.getMessage();
        }
    }

    public String search(String keyword, int page, int size) {
        try {
            String url = OPENSEARCH_URL + "/performances/_search";
            int from = page * size;

            String searchQuery = String.format("""
                    {
                      "from": %d,
                      "size": %d,
                      "query": {
                        "bool": {
                          "should": [
                            {
                              "multi_match": {
                                "query": "%s",
                                "fields": ["title.autocomplete^2", "venue.autocomplete"],
                                "type": "phrase_prefix"
                              }
                            },
                            {
                              "multi_match": {
                                "query": "%s", 
                                "fields": ["title^1.5", "venue", "genre"],
                                "fuzziness": "AUTO"
                              }
                            }
                          ]
                        }
                      }
                    }
                    """, from, size, keyword, keyword);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(searchQuery, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            System.out.println("검색 상세 오류: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return "검색 실패: " + e.getMessage();
        }
    }

    public String syncAllPerformances() {
        try {
            List<Performance> performances = performanceRepository.findAll();
            System.out.println("동기화할 공연 수: " + performances.size());

            int count = 0;
            for (Performance perf : performances) {
                // MariaDB 데이터를 OpenSearch에 인덱싱
                String result = indexDocument(
                        perf.getIdx(),           // idx
                        perf.getTitle(),         // 제목
                        perf.getVenue(),         // 공연장
                        perf.getGenre(),         // 장르
                        perf.getPerformanceId(),  // 공연ID
                        perf.getPosterUrl(),
                        perf.getStartDate(),
                        perf.getEndDate()
                );
                count++;

                if (count % 100 == 0) { // 100개마다 로그 출력
                    System.out.println("동기화 진행: " + count + "/" + performances.size());
                }
            }

            return "동기화 완료: " + count + "개 공연 데이터 인덱싱";
        } catch (Exception e) {
            return "동기화 실패: " + e.getMessage();
        }
    }

    public List<Performance> searchToPerformances(String keyword, int page, int size) {
        try {
            String searchResult = search(keyword, page, size);
            return parseToPerformanceList(searchResult);
        } catch (Exception e) {
            System.out.println("검색 결과 변환 실패: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Performance> parseToPerformanceList(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode hits = root.path("hits").path("hits");

            List<Performance> performances = new ArrayList<>();

            for (JsonNode hit : hits) {
                JsonNode source = hit.path("_source");

                // OpenSearch 결과를 Performance 객체로 변환
                Performance perf = performanceRepository.findById(
                        Long.valueOf(source.path("idx").asText())
                ).orElse(null);

                if (perf != null) {
                    performances.add(perf);
                }
            }

            return performances;
        } catch (Exception e) {
            System.out.println("JSON 파싱 오류: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}