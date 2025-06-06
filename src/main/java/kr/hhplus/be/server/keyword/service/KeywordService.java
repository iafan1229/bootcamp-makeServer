package kr.hhplus.be.server.keyword.service;

import kr.hhplus.be.server.keyword.domain.Keyword;
import kr.hhplus.be.server.keyword.domain.KeywordCount;
import kr.hhplus.be.server.keyword.repository.KeywordRepository;
import kr.hhplus.be.server.keyword.repository.KeywordCountRepository;
import kr.hhplus.be.server.keyword.dto.response.KeywordDto;
import kr.hhplus.be.server.keyword.dto.response.PopularKeywordResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class KeywordService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordService.class);
    private static final int DEFAULT_POPULAR_KEYWORD_LIMIT = 10;

    private final KeywordRepository keywordRepository;
    private final KeywordCountRepository keywordCountRepository;
    private final RedisService redisService;
    private final KeywordCountService keywordCountService;

    @Autowired
    public KeywordService(KeywordRepository keywordRepository,
                          KeywordCountRepository keywordCountRepository,
                          RedisService redisService,
                          KeywordCountService keywordCountService) {
        this.keywordRepository = keywordRepository;
        this.keywordCountRepository = keywordCountRepository;
        this.redisService = redisService;
        this.keywordCountService = keywordCountService;
    }

    /**
     * 검색 키워드 기록 (비동기 처리)
     */
    @Transactional
    public void recordSearchKeyword(String keyword, String locationCategory) {
        try {
            keywordCountService.incrementKeywordCount(keyword, locationCategory);
            logger.debug("검색 키워드 기록 완료: keyword={}, location={}", keyword, locationCategory);
        } catch (Exception e) {
            logger.error("검색 키워드 기록 실패: keyword={}, error={}", keyword, e.getMessage());
            // 키워드 기록 실패가 검색 기능을 방해하지 않도록 예외를 삼킴
        }
    }

    /**
     * 인기 키워드 조회 (Redis 우선, 실패시 DB 조회)
     */
    public PopularKeywordResponse getPopularKeywords(String locationCategory, Integer limit) {
        int actualLimit = limit != null ? limit : DEFAULT_POPULAR_KEYWORD_LIMIT;

        try {
            // Redis에서 인기 키워드 조회 시도
            if (redisService.isRedisAvailable()) {
                return getPopularKeywordsFromRedis(locationCategory, actualLimit);
            }
        } catch (Exception e) {
            logger.warn("Redis 인기 키워드 조회 실패, DB로 fallback: {}", e.getMessage());
        }

        // Redis 실패시 DB에서 조회
        return getPopularKeywordsFromDatabase(locationCategory, actualLimit);
    }

    private PopularKeywordResponse getPopularKeywordsFromRedis(String locationCategory, int limit) {
        Set<ZSetOperations.TypedTuple<Object>> redisResults;

        if (locationCategory != null && !locationCategory.trim().isEmpty()) {
            redisResults = redisService.getTopKeywordsByLocation(locationCategory, limit);
        } else {
            redisResults = redisService.getTopKeywords(limit);
        }

        List<KeywordDto> keywords = redisResults.stream()
                .map(tuple -> new KeywordDto(
                        tuple.getValue().toString(),
                        tuple.getScore().intValue()
                ))
                .collect(Collectors.toList());

        return new PopularKeywordResponse(keywords, "redis");
    }

    private PopularKeywordResponse getPopularKeywordsFromDatabase(String locationCategory, int limit) {
        Long locationCategoryId = null;
        if (locationCategory != null && !locationCategory.trim().isEmpty()) {
            // locationCategory를 ID로 변환하는 로직 필요 (여기서는 단순화)
            locationCategoryId = 1L; // 임시
        }

        List<KeywordCount> keywordCounts = keywordCountRepository
                .findTopKeywordsByLocationCategoryAndDate(locationCategoryId, LocalDate.now(), limit);

        List<KeywordDto> keywords = keywordCounts.stream()
                .map(kc -> {
                    Optional<Keyword> keyword = keywordRepository.findById(kc.getKeywordId());
                    return keyword.map(k -> new KeywordDto(k.getKeyword(), kc.getCount()))
                            .orElse(null);
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return new PopularKeywordResponse(keywords, "database");
    }
}