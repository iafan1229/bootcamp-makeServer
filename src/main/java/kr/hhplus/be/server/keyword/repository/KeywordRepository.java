package kr.hhplus.be.server.keyword.repository;


import kr.hhplus.be.server.keyword.domain.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    Optional<Keyword> findByKeyword(String keyword);

    Optional<Keyword> findByNormalizedKeyword(String normalizedKeyword);
}