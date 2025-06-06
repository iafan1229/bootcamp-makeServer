package kr.hhplus.be.server.restaurant.repository;

import kr.hhplus.be.server.restaurant.domain.SearchRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchRequestRepository extends JpaRepository<SearchRequest, Long> {

    List<SearchRequest> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<SearchRequest> findByKeywordAndCreatedAtBetween(
            String keyword, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT sr.keyword, COUNT(sr) as count FROM SearchRequest sr " +
            "WHERE sr.createdAt BETWEEN :startTime AND :endTime " +
            "GROUP BY sr.keyword " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findPopularKeywordsBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit);

    @Query("SELECT sr.keyword, COUNT(sr) as count FROM SearchRequest sr " +
            "WHERE sr.location LIKE %:location% " +
            "AND sr.createdAt BETWEEN :startTime AND :endTime " +
            "GROUP BY sr.keyword " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findPopularKeywordsByLocationAndPeriod(
            @Param("location") String location,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit);
}