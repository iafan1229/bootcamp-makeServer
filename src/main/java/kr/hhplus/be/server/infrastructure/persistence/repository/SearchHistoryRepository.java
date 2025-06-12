package kr.hhplus.be.server.infrastructure.persistence.repository;

import kr.hhplus.be.server.infrastructure.persistence.entity.SearchHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;


public interface SearchHistoryRepository extends JpaRepository<SearchHistoryEntity, Long> {

    @Query("SELECT s.keyword, COUNT(s) as count FROM SearchHistoryEntity s " +
            "WHERE s.createdAt >= :fromDate " +
            "GROUP BY s.keyword " +
            "ORDER BY count DESC")
    List<Object[]> findPopularKeywords(@Param("fromDate") LocalDateTime fromDate, Pageable pageable);

    @Query("SELECT s.keyword, COUNT(s) as count FROM SearchHistoryEntity s " +
            "WHERE s.createdAt >= :fromDate AND s.location = :location " +
            "GROUP BY s.keyword " +
            "ORDER BY count DESC")
    List<Object[]> findPopularKeywordsByLocation(@Param("fromDate") LocalDateTime fromDate,
                                                 @Param("location") String location,
                                                 Pageable pageable);

    Page<SearchHistoryEntity> findByKeywordContainingIgnoreCase(String keyword, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
