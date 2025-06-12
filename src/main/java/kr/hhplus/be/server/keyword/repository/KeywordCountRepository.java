package kr.hhplus.be.server.keyword.repository;

import kr.hhplus.be.server.keyword.domain.KeywordCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordCountRepository extends JpaRepository<KeywordCount, Long> {

    /**
     * 특정 키워드의 특정 날짜 카운트 조회
     * Infrastructure Layer의 백업 작업에서 중복 방지용
     */
    Optional<KeywordCount> findByKeywordIdAndLocationCategoryIdAndCountDate(
            Long keywordId, Long locationCategoryId, LocalDate countDate);

    /**
     * 인기 키워드 조회 (Infrastructure Layer fallback용)
     * Redis/Memory 실패시 Database에서 조회
     */
    @Query("SELECT kc FROM KeywordCount kc " +
            "WHERE (:locationCategoryId IS NULL OR kc.locationCategoryId = :locationCategoryId) " +
            "AND kc.countDate = :countDate " +
            "ORDER BY kc.count DESC " +
            "LIMIT :limit")
    List<KeywordCount> findTopKeywordsByLocationCategoryAndDate(
            @Param("locationCategoryId") Long locationCategoryId,
            @Param("countDate") LocalDate countDate,
            @Param("limit") int limit);

    /**
     * 기간별 인기 키워드 조회 (Infrastructure Layer 분석용)
     */
    @Query("SELECT kc FROM KeywordCount kc " +
            "WHERE (:locationCategoryId IS NULL OR kc.locationCategoryId = :locationCategoryId) " +
            "AND kc.countDate BETWEEN :startDate AND :endDate " +
            "ORDER BY kc.count DESC " +
            "LIMIT :limit")
    List<KeywordCount> findTopKeywordsByLocationCategoryAndDateRange(
            @Param("locationCategoryId") Long locationCategoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit);

    /**
     * 특정 키워드의 일별 카운트 추이 조회
     */
    @Query("SELECT kc FROM KeywordCount kc " +
            "WHERE kc.keywordId = :keywordId " +
            "AND (:locationCategoryId IS NULL OR kc.locationCategoryId = :locationCategoryId) " +
            "AND kc.countDate BETWEEN :startDate AND :endDate " +
            "ORDER BY kc.countDate DESC")
    List<KeywordCount> findKeywordCountTrend(
            @Param("keywordId") Long keywordId,
            @Param("locationCategoryId") Long locationCategoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Infrastructure Layer 백업 작업을 위한 배치 업데이트
     * Memory/Redis 데이터를 DB로 백업시 사용
     */
    @Modifying
    @Transactional
    @Query("UPDATE KeywordCount kc SET kc.count = kc.count + :incrementCount, kc.updatedAt = :updateTime " +
            "WHERE kc.keywordId = :keywordId AND kc.locationCategoryId = :locationCategoryId AND kc.countDate = :countDate")
    int incrementKeywordCount(@Param("keywordId") Long keywordId,
                              @Param("locationCategoryId") Long locationCategoryId,
                              @Param("countDate") LocalDate countDate,
                              @Param("incrementCount") Integer incrementCount,
                              @Param("updateTime") LocalDateTime updateTime);

    /**
     * 특정 날짜의 전체 키워드 카운트 합계
     */
    @Query("SELECT COALESCE(SUM(kc.count), 0) FROM KeywordCount kc WHERE kc.countDate = :countDate")
    Long getTotalCountByDate(@Param("countDate") LocalDate countDate);

    /**
     * 지역별 키워드 카운트 합계
     */
    @Query("SELECT COALESCE(SUM(kc.count), 0) FROM KeywordCount kc " +
            "WHERE kc.locationCategoryId = :locationCategoryId AND kc.countDate = :countDate")
    Long getTotalCountByLocationAndDate(@Param("locationCategoryId") Long locationCategoryId,
                                        @Param("countDate") LocalDate countDate);

    /**
     * Infrastructure Layer 모니터링을 위한 통계 조회
     */
    @Query("SELECT kc.countDate, COUNT(kc), SUM(kc.count) FROM KeywordCount kc " +
            "WHERE kc.countDate BETWEEN :startDate AND :endDate " +
            "GROUP BY kc.countDate ORDER BY kc.countDate DESC")
    List<Object[]> getKeywordCountStatistics(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * 오래된 데이터 정리를 위한 조회 (데이터 보관 정책용)
     */
    @Query("SELECT kc FROM KeywordCount kc WHERE kc.countDate < :cutoffDate")
    List<KeywordCount> findOldKeywordCounts(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 오래된 데이터 삭제 (배치 작업용)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM KeywordCount kc WHERE kc.countDate < :cutoffDate")
    int deleteOldKeywordCounts(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Infrastructure Layer의 Redis/Memory 동기화를 위한 최근 업데이트된 데이터 조회
     */
    @Query("SELECT kc FROM KeywordCount kc WHERE kc.updatedAt >= :since ORDER BY kc.updatedAt DESC")
    List<KeywordCount> findRecentlyUpdatedKeywordCounts(@Param("since") LocalDateTime since);

    /**
     * 특정 키워드 ID 목록의 최신 카운트 조회 (배치 조회)
     */
    @Query("SELECT kc FROM KeywordCount kc WHERE kc.keywordId IN :keywordIds " +
            "AND kc.countDate = :countDate AND kc.locationCategoryId IS NULL")
    List<KeywordCount> findCurrentCountsByKeywordIds(@Param("keywordIds") List<Long> keywordIds,
                                                     @Param("countDate") LocalDate countDate);

    /**
     * Infrastructure Layer 백업 검증을 위한 카운트 합계 비교
     */
    @Query("SELECT kc.keywordId, SUM(kc.count) FROM KeywordCount kc " +
            "WHERE kc.countDate = :countDate " +
            "GROUP BY kc.keywordId")
    List<Object[]> getKeywordCountSummaryByDate(@Param("countDate") LocalDate countDate);

    /**
     * 지역별 인기 키워드 순위 조회 (Infrastructure Layer 분석용)
     */
    @Query("SELECT kc.locationCategoryId, kc.keywordId, kc.count, " +
            "ROW_NUMBER() OVER (PARTITION BY kc.locationCategoryId ORDER BY kc.count DESC) as rank " +
            "FROM KeywordCount kc WHERE kc.countDate = :countDate AND kc.locationCategoryId IS NOT NULL")
    List<Object[]> getKeywordRankingsByLocation(@Param("countDate") LocalDate countDate);

    /**
     * Infrastructure Layer 성능 모니터링을 위한 키워드별 활동량 조회
     */
    @Query("SELECT k.keyword, SUM(kc.count) as totalCount, COUNT(kc) as entryCount " +
            "FROM KeywordCount kc JOIN Keyword k ON kc.keywordId = k.id " +
            "WHERE kc.countDate BETWEEN :startDate AND :endDate " +
            "GROUP BY kc.keywordId, k.keyword " +
            "ORDER BY totalCount DESC " +
            "LIMIT :limit")
    List<Object[]> getKeywordActivityReport(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("limit") int limit);
}