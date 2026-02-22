package com.example.grapefield2.service;

import com.example.grapefield2.entity.Performance;
import com.example.grapefield2.repository.PerformanceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleOpenSearchService {

    private static final String OPENSEARCH_URL = "http://localhost:9200";
    private final PerformanceRepository performanceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public boolean indexExists() {
        try {
            restTemplate.exchange(OPENSEARCH_URL + "/performances", HttpMethod.HEAD, null, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String createIndex() {
        try {
            String url = OPENSEARCH_URL + "/performances";

            try {
                restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
                log.info("기존 인덱스 삭제 완료");
                Thread.sleep(1000);
            } catch (Exception e) {
                log.info("삭제할 인덱스 없음 (정상)");
            }

            String indexConfig = """
{
  "settings": {
    "index.max_ngram_diff": 8,
    "analysis": {
      "char_filter": {
        "remove_brackets": {
          "type": "pattern_replace",
          "pattern": "[\\\\[\\\\]]",
          "replacement": ""
        }
      },
      "analyzer": {
        "korean_autocomplete": {
          "type": "custom",
          "char_filter": ["remove_brackets"],
          "tokenizer": "autocomplete_tokenizer",
          "filter": ["lowercase"]
        },
        "korean_search": {
          "type": "custom",
          "char_filter": ["remove_brackets"],
          "tokenizer": "nori_tokenizer",
          "filter": ["lowercase"]
        }
      },
      "tokenizer": {
        "nori_tokenizer": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed"
        },
        "autocomplete_tokenizer": {
          "type": "ngram",
          "min_gram": 2,
          "max_gram": 10,
          "token_chars": ["letter"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "idx": { "type": "integer" },
      "title": {
        "type": "text",
        "analyzer": "korean_search",
        "fields": {
          "keyword": { "type": "keyword" },
          "autocomplete": {
            "type": "text",
            "analyzer": "korean_autocomplete",
            "search_analyzer": "korean_search"
          }
        }
      },
      "venue": {
        "type": "text",
        "analyzer": "korean_search",
        "fields": {
          "keyword": { "type": "keyword" },
          "autocomplete": {
            "type": "text",
            "analyzer": "korean_autocomplete",
            "search_analyzer": "korean_search"
          }
        }
      },
      "genre": { "type": "keyword" },
      "performanceId": { "type": "keyword" },
      "posterUrl": { "type": "keyword", "index": false },
      "startDate": { "type": "date", "index": false },
      "endDate": { "type": "date", "index": false }
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
            log.error("인덱스 생성 실패: {}", e.getMessage(), e);
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
            log.error("문서 인덱싱 실패: {}", e.getMessage());
            return "실패: " + e.getMessage();
        }
    }

    public String search(String keyword, int page, int size) {
        StopWatch sw = new StopWatch("OpenSearch-search");
        try {
            String url = OPENSEARCH_URL + "/performances/_search";
            int from = page * size;

            String escapedKeyword = keyword
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            String searchQuery = String.format("""
{
  "from": %d,
  "size": %d,
  "query": {
    "bool": {
      "should": [
        {
          "match": {
            "title": {
              "query": "%s",
              "analyzer": "korean_search",
              "boost": 100
            }
          }
        },
        {
          "match": {
            "title.autocomplete": {
              "query": "%s",
              "boost": 50
            }
          }
        },
        {
          "multi_match": {
            "query": "%s",
            "fields": ["title^2", "venue"],
            "type": "phrase_prefix"
          }
        },
        {
          "multi_match": {
            "query": "%s",
            "fields": ["title^1.5", "venue", "genre"],
            "operator": "and"
          }
        }
      ],
      "minimum_should_match": 1
    }
  }
}
""", from, size, escapedKeyword, escapedKeyword, escapedKeyword, escapedKeyword);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(searchQuery, headers);

            sw.start("HTTP request");
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            sw.stop();

            // 결과 건수 파싱
            long total = 0;
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                total = root.path("hits").path("total").path("value").asLong();
            } catch (Exception ignored) {}

            log.info("OpenSearch 검색 - keyword: {} | {}건 | {}ms",
                    keyword, total, sw.getLastTaskTimeMillis());

            return response.getBody();
        } catch (Exception e) {
            log.error("검색 상세 오류: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return "검색 실패: " + e.getMessage();
        }
    }

    public String searchAutocomplete(String prefix, int size) {
        try {
            String url = OPENSEARCH_URL + "/performances/_search";

            String escapedPrefix = prefix
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");

            String searchQuery = String.format("""
{
  "size": %d,
  "query": {
    "match": {
      "title.autocomplete": {
        "query": "%s"
      }
    }
  }
}
""", size, escapedPrefix);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(searchQuery, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("자동완성 검색 오류: {}", e.getMessage());
            return "검색 실패: " + e.getMessage();
        }
    }

    public String syncAllPerformances() {
        try {
            List<Performance> performances = performanceRepository.findAll();
            log.info("동기화할 공연 수: {}", performances.size());

            int count = 0;
            for (Performance perf : performances) {
                String result = indexDocument(
                        perf.getIdx(),
                        perf.getTitle(),
                        perf.getVenue(),
                        perf.getGenre(),
                        perf.getPerformanceId(),
                        perf.getPosterUrl(),
                        perf.getStartDate(),
                        perf.getEndDate()
                );
                count++;

                if (count % 100 == 0) {
                    log.info("동기화 진행: {}/{}", count, performances.size());
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
            log.error("검색 결과 변환 실패: {}", e.getMessage());
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

                Performance perf = performanceRepository.findById(
                        Long.valueOf(source.path("idx").asText())
                ).orElse(null);

                if (perf != null) {
                    performances.add(perf);
                }
            }

            return performances;
        } catch (Exception e) {
            log.error("JSON 파싱 오류: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}