package kr.hhplus.be.server.keyword.repository;


import kr.hhplus.be.server.keyword.domain.KeywordCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordCountRepository extends JpaRepository<KeywordCount, Long> {

    Optional<KeywordCount> findByKeywordIdAndLocationCategoryIdAndCountDate(
            Long keywordId, Long locationCategoryId, LocalDate countDate);

    @Query("SELECT kc FROM KeywordCount kc " +
            "WHERE (:locationCategoryId IS NULL OR kc.locationCategoryId = :locationCategoryId) " +
            "AND kc.countDate = :countDate " +
            "ORDER BY kc.count DESC " +
            "LIMIT :limit")
    List<KeywordCount> findTopKeywordsByLocationCategoryAndDate(
            @Param("locationCategoryId") Long locationCategoryId,
            @Param("countDate") LocalDate countDate,
            @Param("limit") int limit);
}
