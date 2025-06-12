package kr.hhplus.be.server.infrastructure.keyword;

// 공통 인터페이스

import java.util.List;

public interface KeywordRanking {
    void incrementKeywordCount(String keyword);
    void incrementKeywordCount(String keyword, String location);
    List<KeywordDto> getTopKeywords(int limit);
    List<KeywordDto> getTopKeywordsByLocation(String location, int limit);
    Long getKeywordCount(String keyword);
    boolean isAvailable();
}
