package com.example.grapefield2.service;

import com.example.grapefield2.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.example.grapefield2.entity.Performance;
import com.example.grapefield2.entity.PerformanceDetail;
import com.example.grapefield2.enums.GenreCode;
import com.example.grapefield2.repository.PerformanceDetailRepository;
import com.example.grapefield2.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KopisApiService {

    private final PerformanceDetailRepository performanceDetailRepository;
    @Value("${kopis.api.key}")  // application.yml에서 가져옴
    private String apiKey;
    ObjectMapper objectMapper = new ObjectMapper();

    // 공연 목록 조회 API url
    private final String BASE_URL = "http://www.kopis.or.kr/openApi/restful/pblprfr";

    private final PerformanceRepository performanceRepository;

    @Transactional
    public void collectAllGenres() {
        // 공연 장르마다 따로 API 호출해야함.
        for(GenreCode genre : GenreCode.values()) {
            try{
                collectPerformancesByGenre(genre);
                Thread.sleep(800);
            }

            catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
    }

    // 특정 장르의 공연 목록 및 상세정보 저장
    private void collectPerformancesByGenre(GenreCode genre) throws Exception {
        LocalDate now = LocalDate.now();
        int cpage = 1;
        boolean hasMore = true;

        while (hasMore) {
            String url = buildUrl(genre.getCode(), now, now.plusDays(90), cpage);

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            XmlMapper xmlMapper = new XmlMapper();

            PerformanceListResponse performanceListResponse = xmlMapper.readValue(
                    response.body(),
                    PerformanceListResponse.class
            );

            List<Performance> newPerformances = new ArrayList<>();

            // 응답이 비어있으면 종료
            if (performanceListResponse.getPerformances() == null ||
                    performanceListResponse.getPerformances().isEmpty()) {
                hasMore = false;
                break;
            }

            List<String> performanceIds = new ArrayList<>();

            // 기본 정보 저장
            for (PerformanceDto dto : performanceListResponse.getPerformances()) {
                saveBasicInfoFromDto(dto);
                performanceIds.add(dto.getPerformanceId());
            }

            if (!newPerformances.isEmpty()) {
                performanceRepository.saveAll(newPerformances);
                System.out.println(genre.getName() + " 페이지 " + cpage + ": " + newPerformances.size() + "개 저장");
            }

            // 상세 정보 수집
            for (Performance performance : newPerformances) {
                fetchDetailInfo(performance.getPerformanceId());
                Thread.sleep(800);
            }

            cpage++;
            Thread.sleep(800); // 페이지 간 딜레이
        }

        System.out.println(genre.getName() + " 총 " + (cpage - 1) + "페이지 수집 완료");
    }

    // List API 응답에서 기본 정보 저장
    private void saveBasicInfoFromDto(PerformanceDto dto) {
        if (!performanceRepository.existsByPerformanceId(dto.getPerformanceId())) {
            Performance performance = convertToEntity(dto);
            performanceRepository.save(performance);
        }
    }

    // API 호출 url 생성
    private String buildUrl(String genreCode, LocalDate startDate, LocalDate endDate, int cpage) {
        return BASE_URL +
                "?service=" + apiKey +
                "&stdate=" + startDate.toString().replace("-", "") +
                "&eddate=" + endDate.toString().replace("-", "") +
                "&shcate=" + genreCode +
                "&cpage=" + cpage +
                "&rows=100";
    }

    public void updateIncompletePerformance() throws Exception {
        List<Performance> incompleteList = performanceRepository.findIncompletePerformances();

        for (Performance performance : incompleteList) {
            String performanceId = performance.getPerformanceId();

            // API 호출해서 둘 다 업데이트
            fetchDetailInfo(performanceId);
            System.out.println("퍼포먼스 아이디!!!!!! " + performanceId + "\n\n");

            Thread.sleep(800);
        }
    }

    private void updatePerformance(Performance performance) {

    }

    Performance convertToEntity(PerformanceDto dto) {
        Performance performance = new Performance();
        performance.setPerformanceId(dto.getPerformanceId());
        performance.setTitle(dto.getTitle());
        performance.setStartDate(dto.getStartDate());
        performance.setEndDate(dto.getEndDate());
        performance.setVenue(dto.getVenue());
        performance.setPosterUrl(dto.getPosterUrl());
        performance.setState(dto.getState());
        performance.setArea(dto.getArea());
        performance.setCreatedAt(LocalDateTime.now());
        performance.setGenre(dto.getGenre());
        performance.setTicketPrice(dto.getTicketPrice());

        return performance;
    }

    public Page<Performance> getPerformancesByGenre(String genre, Pageable pageable) {
        System.out.println("입력받은 category: [" + genre + "]");

        if (genre == null || "ALL".equals(genre)) {
            return performanceRepository.findAll(pageable);
        }

        String koreanGenre = convertToKoreanGenre(genre);
        System.out.println("변환된 한글 장르: [" + koreanGenre + "]");

        Page<Performance> result = performanceRepository.findByGenre(koreanGenre, pageable);
        System.out.println("조회 결과 개수: " + result.getTotalElements());

        return result;
    }

    private String convertToKoreanGenre(String genre) {
        switch (genre) {
            case "MUSICAL": return "뮤지컬";
            case "연극": return "연극";
            case "대중음악": return "대중음악";
            default: return genre;
        }
    }

    // 공연 상세 정보 불러오는 메서드
    private PerformanceDetailDto fetchDetailInfoFromApi(String performanceId) throws Exception {
        String detailUrl = BASE_URL + "/" + performanceId + "?service=" + apiKey;

        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(detailUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        XmlMapper xmlMapper = new XmlMapper();
        try {
            PerformanceDetailResponse detailResponse = xmlMapper.readValue(response.body(), PerformanceDetailResponse.class);
            return detailResponse.getDetailInfo();
        } catch (Exception e) {
            System.out.println("상세정보 파싱 실패! "+ e.getMessage());
            throw e;
        }
    }

    public void fetchDetailInfo(String performanceId) throws Exception {
        try {

            // 상세 API 호출
            PerformanceDetailDto detailDto = fetchDetailInfoFromApi(performanceId);

            Performance performance = performanceRepository.findByPerformanceId(performanceId);



            System.out.println("공연 ID: " + performanceId);
            System.out.println("API 가격 응답: [" + detailDto.getTicketPrice() + "]");

            // 가격 업데이트
            if (detailDto.getTicketPrice() != null && !detailDto.getTicketPrice().trim().isEmpty()) {
                performance.setTicketPrice(detailDto.getTicketPrice());
                performanceRepository.save(performance);
            }

            // 기존 PerformanceDetail 찾기
            PerformanceDetail performanceDetail = performanceDetailRepository
                    .findByPerformance_Idx(performance.getIdx());

            if (performanceDetail == null) {
                // 없으면 새로 생성
                performanceDetail = convertToDetailEntity(detailDto, performanceId);
            } else {
                // 있으면 ID만 유지하고 나머지 전부 덮어쓰기
                performanceDetail.setCastInfo(detailDto.getCastInfo());
                performanceDetail.setCrewInfo(detailDto.getCrewInfo());
                performanceDetail.setRuntime(detailDto.getRunTime());
                performanceDetail.setAgeLimit(detailDto.getAgeLimit());
                performanceDetail.setShowTimes(detailDto.getShowTimes());
                performanceDetail.setProducer(detailDto.getProducer());
                performanceDetail.setAgency(detailDto.getAgency());
                performanceDetail.setStory(detailDto.getStory());
                performanceDetail.setVenueId(detailDto.getVenueId());
                performanceDetail.setIsDaehakro(detailDto.getIsDaehakro());

                System.out.println("공연 ID: " + performanceId);
                System.out.println("API 가격 응답: [" + detailDto.getTicketPrice() + "]");
                // 이미지 URLs 변환
                if (detailDto.getIntroImageUrls() != null && !detailDto.getIntroImageUrls().isEmpty()) {
                    performanceDetail.setIntroImageUrls(
                            objectMapper.writeValueAsString(detailDto.getIntroImageUrls())
                    );
                }

                // 티켓 URLs 변환
                if (detailDto.getTicketSites() != null && !detailDto.getTicketSites().isEmpty()) {
                    performanceDetail.setTicketUrls(
                            objectMapper.writeValueAsString(detailDto.getTicketSites())
                    );
                }
            }
            performanceDetailRepository.save(performanceDetail);
        } catch (Exception e) {
            System.out.println("공연 ID  " + performanceId + "처리 실패 : " + e.getMessage());
        }
    }

    PerformanceDetail convertToDetailEntity(PerformanceDetailDto dto, String performanceId) {
        PerformanceDetail performanceDetail = new PerformanceDetail();

        Performance performance = performanceRepository.findByPerformanceId(performanceId);
        performanceDetail.setPerformance(performance);
        performanceDetail.setCrewInfo(dto.getCrewInfo());
        performanceDetail.setPerformance(performance);
        performanceDetail.setAgency(dto.getAgency());
        performanceDetail.setAgeLimit(dto.getAgeLimit());
        performanceDetail.setCastInfo(dto.getCastInfo());
        performanceDetail.setRuntime(dto.getRunTime());
        performanceDetail.setProducer(dto.getProducer());
        performanceDetail.setStory(dto.getStory());
        performanceDetail.setIsDaehakro(dto.getIsDaehakro());
        performanceDetail.setVenueId(dto.getVenueId());

        // 이미지 URLs 변환 (List<String> → JSON String)
        if (dto.getIntroImageUrls() != null && !dto.getIntroImageUrls().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                performanceDetail.setIntroImageUrls(
                        objectMapper.writeValueAsString(dto.getIntroImageUrls())
                );
            } catch (JsonProcessingException e) {
                System.out.println("이미지 URL 변환 실패: " + e.getMessage());
            }
        }

        // 티켓 사이트 URLs 변환 (List<RelateInfo> → JSON String)
        if (dto.getTicketSites() != null && !dto.getTicketSites().isEmpty()) {
            try {
                performanceDetail.setTicketUrls(
                        objectMapper.writeValueAsString(dto.getTicketSites())
                );
            } catch (JsonProcessingException e) {
                System.out.println("티켓 URL 변환 실패: " + e.getMessage());
            }
        }

        return performanceDetail;
    }

    public List<BoxOfficeDto> fetchBoxOfficeFromKopis(String genre) throws Exception {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String weekAgo = LocalDate.now().minusDays(7).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String url = "http://www.kopis.or.kr/openApi/restful/boxoffice"
                + "?service=" + apiKey
                + "&stdate=" + weekAgo
                + "&eddate=" + today
                + "&catecode=" + getGenreCode(genre)
                + "&srchseatscale=100";

        System.out.println("박스오피스 API URL: " + url);

        // HTTP 요청
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        XmlMapper xmlMapper = new XmlMapper();

        try {
            BoxOfficeListResponse boxOfficeListResponse = xmlMapper.readValue(response.body(), BoxOfficeListResponse.class);

            return boxOfficeListResponse.getBoxOffices();
        } catch (Exception e) {
            System.out.println("박스 오피스 파싱 실패 " + e.getMessage());
            throw e;
        }
    }

    private String getGenreCode(String genre) {
        switch(genre) {
            case "연극": return "AAAA";
            case "서양음악(클래식)": return "CCCA";
            case "한국음악(국악)": return "CCCC";
            case "대중음악": return "CCCD";
            case "복합": return "EEEA";
            case "뮤지컬": return "GGGA";
            default: return "";  // ALL
        }
    }
}
