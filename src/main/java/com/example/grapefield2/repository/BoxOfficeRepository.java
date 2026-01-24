package com.example.grapefield2.repository;

import com.example.grapefield2.entity.BoxOffice;
import com.example.grapefield2.entity.Performance;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoxOfficeRepository extends JpaRepository<BoxOffice, Long> {
    List<BoxOffice> findAllByOrderByRnumAsc(); // 순위순 조회

    @Query("SELECT p FROM Performance p " +
            "JOIN BoxOffice b ON b.performanceId = p.performanceId " +
            "ORDER BY b.rnum ASC")
    List<Performance> findPerformancesOrderByRank();

    @Query("SELECT p FROM Performance p " +
            "JOIN BoxOffice b ON b.performanceId = p.performanceId " +
            "WHERE(:genre = 'ALL' OR b.genre = :genre) " +
            "ORDER BY b.rnum ASC")
    List<Performance> findPerformancesByGenreOrderByRank(
            @Param("genre") String genre,
            Pageable pageable
    );
}
