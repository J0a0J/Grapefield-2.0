package com.example.grapefield2.controller;

import com.example.grapefield2.entity.BoxOffice;
import com.example.grapefield2.entity.Performance;
import com.example.grapefield2.repository.BoxOfficeRepository;
import com.example.grapefield2.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/boxoffice")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class BoxOfficeController {

    private final BoxOfficeRepository boxOfficeRepository;
    private final PerformanceRepository performanceRepository;

    @GetMapping
    public List<Performance> getBoxOffice() {
        // 박스오피스 순위 가져오기 (순위순 정렬)
        List<BoxOffice> rankings = boxOfficeRepository.findAllByOrderByRnumAsc();

        // performanceId로 실제 공연 정보 조회
        return rankings.stream()
                .map(bo -> performanceRepository.findByPerformanceId(bo.getPerformanceId()))
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }
}
