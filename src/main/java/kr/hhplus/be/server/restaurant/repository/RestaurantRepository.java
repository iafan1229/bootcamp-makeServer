package kr.hhplus.be.server.restaurant.repository;

import kr.hhplus.be.server.restaurant.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByNameContaining(String name);

    List<Restaurant> findByCategory(String category);

    List<Restaurant> findByAddressContaining(String address);

    List<Restaurant> findAllByOrderByRatingDesc();

    List<Restaurant> findAllByOrderByReviewCountDesc();

    @Query("SELECT r FROM Restaurant r WHERE " +
            "(r.name LIKE %:keyword% OR r.category LIKE %:keyword%) " +
            "AND (:location IS NULL OR r.address LIKE %:location%) " +
            "ORDER BY r.rating DESC")
    List<Restaurant> searchByKeywordAndLocation(
            @Param("keyword") String keyword,
            @Param("location") String location);

    @Query("SELECT r FROM Restaurant r WHERE " +
            "(r.name LIKE %:keyword% OR r.category LIKE %:keyword%) " +
            "AND (:location IS NULL OR r.address LIKE %:location%) " +
            "ORDER BY r.reviewCount DESC")
    List<Restaurant> searchByKeywordAndLocationOrderByReviewCount(
            @Param("keyword") String keyword,
            @Param("location") String location);
}