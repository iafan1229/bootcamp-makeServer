package kr.hhplus.be.server.restaurant.repository;

import kr.hhplus.be.server.restaurant.domain.SearchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchResultRepository extends JpaRepository<SearchResult, Long> {

    List<SearchResult> findBySearchRequestIdOrderByRankOrder(Long searchRequestId);

    long countByRestaurantId(Long restaurantId);

    @Query("SELECT sr.restaurantId, COUNT(sr) as searchCount FROM SearchResult sr " +
            "GROUP BY sr.restaurantId " +
            "ORDER BY searchCount DESC " +
            "LIMIT :limit")
    List<Object[]> findMostSearchedRestaurants(@Param("limit") int limit);

    @Query("SELECT sr FROM SearchResult sr " +
            "WHERE sr.searchRequestId IN " +
            "(SELECT req.id FROM SearchRequest req WHERE req.keyword = :keyword) " +
            "ORDER BY sr.rankOrder")
    List<SearchResult> findByKeyword(@Param("keyword") String keyword);
}