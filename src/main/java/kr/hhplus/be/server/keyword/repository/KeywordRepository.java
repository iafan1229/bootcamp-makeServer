package kr.hhplus.be.server.keyword.repository;

import kr.hhplus.be.server.keyword.domain.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    /**
     * 원본 키워드로 검색
     */
    Optional<Keyword> findByKeyword(String keyword);

    /**
     * 정규화된 키워드로 검색 (Infrastructure Layer와 연동)
     */
    Optional<Keyword> findByNormalizedKeyword(String normalizedKeyword);

    /**
     * 키워드 존재 여부 확인 (정규화된 키워드 기준)
     */
    boolean existsByNormalizedKeyword(String normalizedKeyword);

    /**
     * 원본 키워드 존재 여부 확인
     */
    boolean existsByKeyword(String keyword);

    /**
     * 키워드 패턴 검색 (자동완성용)
     */
    @Query("SELECT k FROM Keyword k WHERE k.keyword LIKE %:pattern% OR k.normalizedKeyword LIKE %:pattern% ORDER BY k.keyword")
    List<Keyword> findByKeywordPattern(@Param("pattern") String pattern);

    /**
     * 인기 키워드 조회를 위한 키워드 배치 조회
     * Infrastructure Layer의 SearchHistoryEntity와 연동하여 N+1 문제 해결
     */
    @Query("SELECT k FROM Keyword k WHERE k.id IN :keywordIds ORDER BY k.keyword")
    List<Keyword> findAllByIdInOrderByKeyword(@Param("keywordIds") List<Long> keywordIds);

    /**
     * Infrastructure Layer SearchHistoryEntity와 연동한 실시간 인기 키워드 조회
     * Redis/Memory 장애시 SearchHistoryEntity에서 실시간 집계
     */
    @Query("SELECT k.keyword, k.normalizedKeyword, COUNT(sh) as searchCount " +
            "FROM Keyword k " +
            "JOIN kr.hhplus.be.server.infrastructure.persistence.entity.SearchHistoryEntity sh " +
            "ON k.normalizedKeyword = sh.keyword " +
            "WHERE sh.createdAt >= :fromDate " +
            "GROUP BY k.keyword, k.normalizedKeyword " +
            "ORDER BY searchCount DESC " +
            "LIMIT :limit")
    List<Object[]> findPopularKeywordsFromSearchHistory(@Param("fromDate") LocalDateTime fromDate,
                                                        @Param("limit") int limit);

    /**
     * Infrastructure Layer와 연동한 지역별 인기 키워드 조회
     */
    @Query("SELECT k.keyword, k.normalizedKeyword, COUNT(sh) as searchCount " +
            "FROM Keyword k " +
            "JOIN kr.hhplus.be.server.infrastructure.persistence.entity.SearchHistoryEntity sh " +
            "ON k.normalizedKeyword = sh.keyword " +
            "WHERE sh.location = :location " +
            "AND sh.createdAt >= :fromDate " +
            "GROUP BY k.keyword, k.normalizedKeyword " +
            "ORDER BY searchCount DESC " +
            "LIMIT :limit")
    List<Object[]> findPopularKeywordsByLocationFromSearchHistory(@Param("location") String location,
                                                                  @Param("fromDate") LocalDateTime fromDate,
                                                                  @Param("limit") int limit);

    /**
     * 최근 생성된 키워드 조회 (모니터링용)
     */
    @Query("SELECT k FROM Keyword k WHERE k.id > :lastId ORDER BY k.id LIMIT :limit")
    List<Keyword> findRecentKeywords(@Param("lastId") Long lastId, @Param("limit") int limit);

    /**
     * 정규화된 키워드 목록으로 배치 검색
     * Infrastructure Layer의 Memory/Redis 데이터와 DB 동기화시 사용
     */
    @Query("SELECT k FROM Keyword k WHERE k.normalizedKeyword IN :normalizedKeywords")
    List<Keyword> findByNormalizedKeywordIn(@Param("normalizedKeywords") List<String> normalizedKeywords);

    /**
     * 키워드 통계 조회 (Infrastructure Layer 모니터링용)
     */
    @Query("SELECT COUNT(k) FROM Keyword k")
    long countTotalKeywords();

    /**
     * 특정 패턴의 키워드 개수 조회
     */
    @Query("SELECT COUNT(k) FROM Keyword k WHERE k.keyword LIKE %:pattern% OR k.normalizedKeyword LIKE %:pattern%")
    long countKeywordsByPattern(@Param("pattern") String pattern);

    /**
     * 빈 정규화 키워드를 가진 키워드 조회 (데이터 정리용)
     */
    @Query("SELECT k FROM Keyword k WHERE k.normalizedKeyword IS NULL OR k.normalizedKeyword = ''")
    List<Keyword> findKeywordsWithEmptyNormalizedKeyword();

    /**
     * 중복 키워드 조회 (데이터 정리용)
     * 같은 정규화 키워드를 가진 서로 다른 원본 키워드들
     */
    @Query("SELECT k.normalizedKeyword, COUNT(k) as cnt FROM Keyword k " +
            "GROUP BY k.normalizedKeyword HAVING COUNT(k) > 1")
    List<Object[]> findDuplicateNormalizedKeywords();

    /**
     * Infrastructure Layer의 백업 작업을 위한 키워드 ID 매핑 조회
     */
    @Query("SELECT k.normalizedKeyword, k.id FROM Keyword k WHERE k.normalizedKeyword IN :normalizedKeywords")
    List<Object[]> findKeywordIdMappings(@Param("normalizedKeywords") List<String> normalizedKeywords);
}