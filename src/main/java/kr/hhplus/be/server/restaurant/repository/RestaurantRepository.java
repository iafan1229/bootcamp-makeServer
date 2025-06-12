package kr.hhplus.be.server.restaurant.repository;

import kr.hhplus.be.server.restaurant.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    // === 기본 검색 메서드들 ===

    /**
     * 이름으로 검색 (Infrastructure Layer의 외부 API 연동용)
     */
    List<Restaurant> findByNameContaining(String name);

    /**
     * 카테고리로 검색
     */
    List<Restaurant> findByCategory(String category);

    /**
     * 주소로 검색 (지역 필터링용)
     */
    List<Restaurant> findByAddressContaining(String address);

    /**
     * 평점순 정렬 조회
     */
    List<Restaurant> findAllByOrderByRatingDesc();

    /**
     * 리뷰 수순 정렬 조회
     */
    List<Restaurant> findAllByOrderByReviewCountDesc();

    // === Infrastructure Layer와 연동된 고급 검색 메서드들 ===

    /**
     * 통합 검색 (키워드 + 지역) - 정확도순
     * Infrastructure Layer의 SearchApiManager와 연동
     */
    @Query("SELECT r FROM Restaurant r WHERE " +
            "(r.name LIKE %:keyword% OR r.category LIKE %:keyword%) " +
            "AND (:location IS NULL OR r.address LIKE %:location%) " +
            "ORDER BY " +
            "CASE WHEN r.name LIKE %:keyword% THEN 1 " +
            "     WHEN r.category LIKE %:keyword% THEN 2 " +
            "     ELSE 3 END, " +
            "r.rating DESC")
    List<Restaurant> searchByKeywordAndLocation(
            @Param("keyword") String keyword,
            @Param("location") String location);

    /**
     * 통합 검색 (키워드 + 지역) - 리뷰 수순
     */
    @Query("SELECT r FROM Restaurant r WHERE " +
            "(r.name LIKE %:keyword% OR r.category LIKE %:keyword%) " +
            "AND (:location IS NULL OR r.address LIKE %:location%) " +
            "ORDER BY r.reviewCount DESC NULLS LAST, r.rating DESC")
    List<Restaurant> searchByKeywordAndLocationOrderByReviewCount(
            @Param("keyword") String keyword,
            @Param("location") String location);

    /**
     * 페이징 지원 검색 (Infrastructure Layer 페이징 처리용)
     */
    @Query("SELECT r FROM Restaurant r WHERE " +
            "(r.name LIKE %:keyword% OR r.category LIKE %:keyword%) " +
            "AND (:location IS NULL OR r.address LIKE %:location%) " +
            "ORDER BY " +
            "CASE WHEN :sort = 'review_count' THEN r.reviewCount ELSE r.rating END DESC")
    List<Restaurant> searchWithPaging(@Param("keyword") String keyword,
                                      @Param("location") String location,
                                      @Param("sort") String sort,
                                      Pageable pageable);

    /**
     * 외부 API 중복 확인 (Infrastructure Layer의 External API 연동용)
     * 같은 이름과 주소를 가진 레스토랑 중복 방지
     */
    @Query("SELECT r FROM Restaurant r WHERE r.name = :name AND r.address = :address")
    Optional<Restaurant> findByNameAndAddress(@Param("name") String name, @Param("address") String address);

    /**
     * 소스별 레스토랑 조회 (Infrastructure Layer 모니터링용)
     */
    List<Restaurant> findBySource(String source);

    /**
     * 소스별 레스토랑 개수 통계
     */
    @Query("SELECT r.source, COUNT(r) FROM Restaurant r GROUP BY r.source")
    List<Object[]> getRestaurantCountBySource();

    // === Infrastructure Layer 캐싱 지원 메서드들 ===

    /**
     * 인기 레스토랑 조회 (캐싱용)
     * 높은 평점과 많은 리뷰를 가진 레스토랑
     */
    @Query("SELECT r FROM Restaurant r WHERE r.rating >= :minRating AND r.reviewCount >= :minReviewCount " +
            "ORDER BY r.rating DESC, r.reviewCount DESC")
    List<Restaurant> findPopularRestaurants(@Param("minRating") BigDecimal minRating,
                                            @Param("minReviewCount") Integer minReviewCount,
                                            Pageable pageable);

    /**
     * 지역별 인기 레스토랑 조회
     */
    @Query("SELECT r FROM Restaurant r WHERE r.address LIKE %:location% " +
            "AND r.rating >= :minRating " +
            "ORDER BY r.rating DESC, r.reviewCount DESC")
    List<Restaurant> findPopularRestaurantsByLocation(@Param("location") String location,
                                                      @Param("minRating") BigDecimal minRating,
                                                      Pageable pageable);

    /**
     * 최근 추가된 레스토랑 조회 (Infrastructure Layer 모니터링용)
     */
    @Query("SELECT r FROM Restaurant r WHERE r.id > :lastId ORDER BY r.id LIMIT :limit")
    List<Restaurant> findRecentRestaurants(@Param("lastId") Long lastId, @Param("limit") int limit);

    // === Infrastructure Layer 데이터 품질 관리 메서드들 ===

    /**
     * 중복 레스토랑 검출 (데이터 정리용)
     */
    @Query("SELECT r.name, r.address, COUNT(r) as cnt FROM Restaurant r " +
            "GROUP BY r.name, r.address HAVING COUNT(r) > 1")
    List<Object[]> findDuplicateRestaurants();

    /**
     * 불완전한 데이터를 가진 레스토랑 조회
     */
    @Query("SELECT r FROM Restaurant r WHERE " +
            "r.name IS NULL OR r.name = '' OR " +
            "r.address IS NULL OR r.address = '' OR " +
            "r.category IS NULL OR r.category = ''")
    List<Restaurant> findIncompleteRestaurants();

    /**
     * 평점이나 리뷰 수가 비정상적인 레스토랑 조회
     */
    @Query("SELECT r FROM Restaurant r WHERE " +
            "r.rating < 0 OR r.rating > 5 OR " +
            "r.reviewCount < 0")
    List<Restaurant> findRestaurantsWithInvalidRatings();

    // === Infrastructure Layer 배치 작업 지원 메서드들 ===

    /**
     * 외부 API 업데이트를 위한 배치 조회
     */
    @Query("SELECT r FROM Restaurant r WHERE r.source = :source ORDER BY r.id")
    List<Restaurant> findAllBySourceForBatch(@Param("source") String source, Pageable pageable);

    /**
     * 평점/리뷰 수 업데이트 (외부 API 동기화용)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Restaurant r SET r.rating = :rating, r.reviewCount = :reviewCount " +
            "WHERE r.id = :id")
    int updateRatingAndReviewCount(@Param("id") Long id,
                                   @Param("rating") BigDecimal rating,
                                   @Param("reviewCount") Integer reviewCount);

    /**
     * 소스별 배치 업데이트 시간 기록을 위한 메타데이터 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE Restaurant r SET r.source = :source WHERE r.source = :oldSource")
    int updateSourceBatch(@Param("oldSource") String oldSource, @Param("source") String source);

    // === Infrastructure Layer 통계 및 분석 메서드들 ===

    /**
     * 카테고리별 레스토랑 통계
     */
    @Query("SELECT r.category, COUNT(r), AVG(r.rating), SUM(r.reviewCount) " +
            "FROM Restaurant r WHERE r.category IS NOT NULL " +
            "GROUP BY r.category ORDER BY COUNT(r) DESC")
    List<Object[]> getRestaurantStatisticsByCategory();

    /**
     * 지역별 레스토랑 분포 분석
     */
    @Query("SELECT " +
            "CASE WHEN r.address LIKE '%서울%' THEN '서울' " +
            "     WHEN r.address LIKE '%부산%' THEN '부산' " +
            "     WHEN r.address LIKE '%대구%' THEN '대구' " +
            "     WHEN r.address LIKE '%인천%' THEN '인천' " +
            "     WHEN r.address LIKE '%광주%' THEN '광주' " +
            "     WHEN r.address LIKE '%대전%' THEN '대전' " +
            "     WHEN r.address LIKE '%울산%' THEN '울산' " +
            "     ELSE '기타' END as region, " +
            "COUNT(r), AVG(r.rating) " +
            "FROM Restaurant r " +
            "GROUP BY " +
            "CASE WHEN r.address LIKE '%서울%' THEN '서울' " +
            "     WHEN r.address LIKE '%부산%' THEN '부산' " +
            "     WHEN r.address LIKE '%대구%' THEN '대구' " +
            "     WHEN r.address LIKE '%인천%' THEN '인천' " +
            "     WHEN r.address LIKE '%광주%' THEN '광주' " +
            "     WHEN r.address LIKE '%대전%' THEN '대전' " +
            "     WHEN r.address LIKE '%울산%' THEN '울산' " +
            "     ELSE '기타' END " +
            "ORDER BY COUNT(r) DESC")
    List<Object[]> getRestaurantDistributionByRegion();

    /**
     * Infrastructure Layer 성능 모니터링을 위한 검색 성능 분석용 메서드
     */
    @Query("SELECT COUNT(r) FROM Restaurant r WHERE " +
            "(r.name LIKE %:keyword% OR r.category LIKE %:keyword%) " +
            "AND (:location IS NULL OR r.address LIKE %:location%)")
    long countSearchResults(@Param("keyword") String keyword, @Param("location") String location);
}