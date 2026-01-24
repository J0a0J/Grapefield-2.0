package com.example.grapefield2.repository;

import com.example.grapefield2.entity.Performance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    Performance findByPerformanceId(String performanceId);

    Boolean existsByPerformanceId(String performanceId);

    Page<Performance> findByGenre(String genre, Pageable pageable);

    @Query("SELECT p FROM Performance p LEFT JOIN PerformanceDetail d ON d.performance = p WHERE d.idx IS NULL OR d.introImageUrls IS NULL OR p.ticketPrice IS NULL OR p.state = '공연예정'")
    List<Performance> findIncompletePerformances();

    @Query("SELECT DISTINCT p FROM Performance p " +
            "LEFT JOIN PerformanceDetail d ON d.performance = p " +
            "WHERE p.title LIKE %:keyword% " +
            "OR p.venue LIKE %:keyword% " +
            "OR d.castInfo LIKE %:keyword% " +
            "OR d.producer LIKE %:keyword%")
    Page<Performance> searchPerformances(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Performance p WHERE p.state IN :states " +
            "AND (:genre = 'ALL' OR p.genre = :genre) " +
            "ORDER BY p.startDate DESC")
    List<Performance> findByStatesAndGenre(
            @Param("states") List<String> states,
            @Param("genre") String genre,
            Pageable pageable
    );

    // 마감순용 추가
    @Query("SELECT p FROM Performance p WHERE p.state IN :states " +
            "AND (:genre = 'ALL' OR p.genre = :genre) " +
            "ORDER BY p.endDate ASC")
    List<Performance> findByStatesAndGenreOrderByEndDate(
            @Param("states") List<String> states,
            @Param("genre") String genre,
            Pageable pageable
    );

    // 스케줄러용: 만료된 공연 찾기
    List<Performance> findByStateNotAndEndDateLessThan(
            String excludeState,
            String date
    );

    @Query("SELECT p FROM Performance p " +
            "WHERE p.startDate " +
            "BETWEEN :startDate " +
            "AND :endDate " +
            "ORDER BY p.startDate ASC")
    List<Performance> findByDateBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
