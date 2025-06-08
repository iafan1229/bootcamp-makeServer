package kr.hhplus.be.server.keyword.service;

import kr.hhplus.be.server.keyword.domain.Keyword;
import kr.hhplus.be.server.keyword.domain.KeywordCount;
import kr.hhplus.be.server.keyword.repository.KeywordRepository;
import kr.hhplus.be.server.keyword.repository.KeywordCountRepository;
import kr.hhplus.be.server.keyword.dto.response.KeywordDto;
import kr.hhplus.be.server.keyword.dto.response.PopularKeywordResponse;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KeywordService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordService.class);
    private static final int DEFAULT_POPULAR_KEYWORD_LIMIT = 10;
    private static final String REDIS_DATA_SOURCE = "redis";
    private static final String DATABASE_DATA_SOURCE = "database";

    private final KeywordRepository keywordRepository;
    private final KeywordCountRepository keywordCountRepository;
    private final RedisService redisService;
    private final KeywordCountService keywordCountService;

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
     * 검색 키워드 기록
     * 키워드 기록 실패가 검색 기능을 방해하지 않도록 예외를 처리합니다.
     *
     * 트랜잭션 제거 이유:
     * - incrementKeywordCount는 주로 Redis/메모리 작업
     * - DB 작업(ensureKeywordExists)은 keywordCountService 내부에서 처리
     * - 불필요한 트랜잭션 오버헤드 제거
     */
    public void recordSearchKeyword(String keyword, String locationCategory) {
        try {
            keywordCountService.incrementKeywordCount(keyword, locationCategory);
            logger.debug("검색 키워드 기록 완료: keyword={}, location={}", keyword, locationCategory);
        } catch (Exception e) {
            logger.error("검색 키워드 기록 실패: keyword={}, location={}, error={}",
                    keyword, locationCategory, e.getMessage(), e);
        }
    }

    /**
     * 인기 키워드 조회
     * Redis 우선 조회, 실패시 DB에서 조회합니다.
     */
    public PopularKeywordResponse getPopularKeywords(String locationCategory, Integer limit) {
        int actualLimit = getValidatedLimit(limit);

        // Redis 조회 시도
        PopularKeywordResponse redisResult = tryGetFromRedis(locationCategory, actualLimit);
        if (redisResult != null) {
            return redisResult;
        }

        // Redis 실패시 DB에서 조회
        return getPopularKeywordsFromDatabase(locationCategory, actualLimit);
    }

    private int getValidatedLimit(Integer limit) {
        return limit != null && limit > 0 ? limit : DEFAULT_POPULAR_KEYWORD_LIMIT;
    }

    private PopularKeywordResponse tryGetFromRedis(String locationCategory, int limit) {
        try {
            if (redisService.isRedisAvailable()) {
                return getPopularKeywordsFromRedis(locationCategory, limit);
            }
        } catch (Exception e) {
            logger.warn("Redis 인기 키워드 조회 실패, DB로 fallback: location={}, error={}",
                    locationCategory, e.getMessage());
        }
        return null;
    }

    private PopularKeywordResponse getPopularKeywordsFromRedis(String locationCategory, int limit) {
        Set<ZSetOperations.TypedTuple<Object>> redisResults = hasValidLocationCategory(locationCategory)
                ? redisService.getTopKeywordsByLocation(locationCategory, limit)
                : redisService.getTopKeywords(limit);

        List<KeywordDto> keywords = redisResults.stream()
                .map(this::convertTupleToKeywordDto)
                .collect(Collectors.toList());

        return new PopularKeywordResponse(keywords, REDIS_DATA_SOURCE);
    }

    private KeywordDto convertTupleToKeywordDto(ZSetOperations.TypedTuple<Object> tuple) {
        return new KeywordDto(
                tuple.getValue().toString(),
                tuple.getScore().intValue()
        );
    }

    private PopularKeywordResponse getPopularKeywordsFromDatabase(String locationCategory, int limit) {
        Long locationCategoryId = convertToLocationCategoryId(locationCategory);

        List<KeywordCount> keywordCounts = keywordCountRepository
                .findTopKeywordsByLocationCategoryAndDate(locationCategoryId, LocalDate.now(), limit);

        // N+1 문제 해결: 배치로 키워드 조회
        List<KeywordDto> keywords = getKeywordDtosBatch(keywordCounts);

        return new PopularKeywordResponse(keywords, DATABASE_DATA_SOURCE);
    }

    /**
     * N+1 문제 해결을 위한 배치 조회
     */
    private List<KeywordDto> getKeywordDtosBatch(List<KeywordCount> keywordCounts) {
        if (keywordCounts.isEmpty()) {
            return List.of();
        }

        // 1. 키워드 ID 목록 추출
        List<Long> keywordIds = keywordCounts.stream()
                .map(KeywordCount::getKeywordId)
                .collect(Collectors.toList());

        // 2. 배치로 키워드 조회 (1번의 쿼리)
        List<Keyword> keywords = keywordRepository.findAllById(keywordIds);

        // 3. ID를 키로 하는 Map 생성 (빠른 조회용)
        Map<Long, String> keywordMap = keywords.stream()
                .collect(Collectors.toMap(Keyword::getId, Keyword::getKeyword));

        // 4. KeywordDto 변환
        return keywordCounts.stream()
                .map(kc -> {
                    String keywordText = keywordMap.get(kc.getKeywordId());
                    return keywordText != null
                            ? new KeywordDto(keywordText, kc.getCount())
                            : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean hasValidLocationCategory(String locationCategory) {
        return locationCategory != null && !locationCategory.trim().isEmpty();
    }

    private Long convertToLocationCategoryId(String locationCategory) {
        if (!hasValidLocationCategory(locationCategory)) {
            return null;
        }
        // TODO: locationCategory를 실제 ID로 변환하는 로직 구현 필요
        return 1L; // 임시값
    }
}