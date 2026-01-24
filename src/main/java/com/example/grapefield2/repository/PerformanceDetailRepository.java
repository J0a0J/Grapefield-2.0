package com.example.grapefield2.repository;

import com.example.grapefield2.entity.Performance;
import com.example.grapefield2.entity.PerformanceDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceDetailRepository extends JpaRepository<PerformanceDetail, Long> {

    // Performance 엔티티의 idx(PK)로 상세정보 조회
    PerformanceDetail findByPerformance_Idx(Long performanceIdx);

    // Performance 엔티티의 performanceId(KOPIS ID)로 상세정보 조회 _ PF279883
    PerformanceDetail findByPerformance_PerformanceId(String performanceId);

    // KOPIS 공연 ID로 상세정보 존재 여부 확인 (중복 방지용)
    boolean existsByPerformance_PerformanceId(String performanceId);

    /**
     * 마감순 조회: 공연중/공연예정 -> endDate 오름차순
     */
    @Query("SELECT p FROM Performance p " +
            "WHERE p.state IN :states " +
            "AND (:genre = 'ALL' OR p.genre = :genre) " +
            "ORDER BY p.endDate ASC")
    List<Performance> findByStatesAndGenreOrderByEndDate(
            @Param("states") List<String> states, @Param("genre") String genre, Pageable pageable);


}