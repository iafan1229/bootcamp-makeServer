package kr.hhplus.be.server.keyword.service;

import kr.hhplus.be.server.keyword.domain.Keyword;
import kr.hhplus.be.server.keyword.domain.KeywordCount;
import kr.hhplus.be.server.keyword.repository.KeywordRepository;
import kr.hhplus.be.server.keyword.repository.KeywordCountRepository;
import kr.hhplus.be.server.keyword.dto.response.KeywordDto;
import kr.hhplus.be.server.keyword.dto.response.PopularKeywordResponse;
import kr.hhplus.be.server.infrastructure.keyword.KeywordRanking;
import kr.hhplus.be.server.infrastructure.keyword.RedisKeywordRankingImpl;
import kr.hhplus.be.server.infrastructure.keyword.MemoryKeywordRankingImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class KeywordService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordService.class);
    private static final int DEFAULT_POPULAR_KEYWORD_LIMIT = 10;
    private static final String REDIS_DATA_SOURCE = "redis";
    private static final String MEMORY_DATA_SOURCE = "memory";
    private static final String DATABASE_DATA_SOURCE = "database";

    private final KeywordRepository keywordRepository;
    private final KeywordCountRepository keywordCountRepository;
    private final KeywordRanking redisKeywordRanking;
    private final KeywordRanking memoryKeywordRanking;
    private final KeywordCountService keywordCountService;

    @Autowired
    public KeywordService(KeywordRepository keywordRepository,
                          KeywordCountRepository keywordCountRepository,
                          @Qualifier("redisKeywordRanking") KeywordRanking redisKeywordRanking,
                          @Qualifier("memoryKeywordRanking") KeywordRanking memoryKeywordRanking,
                          KeywordCountService keywordCountService) {
        this.keywordRepository = keywordRepository;
        this.keywordCountRepository = keywordCountRepository;
        this.redisKeywordRanking = redisKeywordRanking;
        this.memoryKeywordRanking = memoryKeywordRanking;
        this.keywordCountService = keywordCountService;
    }

    /**
     * 검색 키워드 기록 - Infrastructure Layer 활용
     * 키워드 기록 실패가 검색 기능을 방해하지 않도록 예외를 처리합니다.
     */
    public void recordSearchKeyword(String keyword, String locationCategory) {
        try {
            // Infrastructure Layer의 KeywordRanking을 통한 우선순위 기반 처리
            recordKeywordWithFailover(keyword, locationCategory);
            logger.debug("검색 키워드 기록 완료: keyword={}, location={}", keyword, locationCategory);
        } catch (Exception e) {
            logger.error("검색 키워드 기록 실패: keyword={}, location={}, error={}",
                    keyword, locationCategory, e.getMessage(), e);
        }
    }

    /**
     * 인기 키워드 조회 - Infrastructure Layer 우선순위 기반
     * Redis → Memory → Database 순서로 failover
     */
    public PopularKeywordResponse getPopularKeywords(String locationCategory, Integer limit) {
        int actualLimit = getValidatedLimit(limit);

        // 1. Redis 조회 시도 (최우선)
        PopularKeywordResponse redisResult = tryGetFromRedis(locationCategory, actualLimit);
        if (redisResult != null && !redisResult.getKeywords().isEmpty()) {
            return redisResult;
        }

        // 2. Memory 조회 시도 (Redis 실패시)
        PopularKeywordResponse memoryResult = tryGetFromMemory(locationCategory, actualLimit);
        if (memoryResult != null && !memoryResult.getKeywords().isEmpty()) {
            return memoryResult;
        }

        // 3. Database 조회 (최종 fallback)
        return getPopularKeywordsFromDatabase(locationCategory, actualLimit);
    }

    /**
     * Infrastructure Layer를 통한 키워드 기록 (우선순위 기반 failover)
     */
    private void recordKeywordWithFailover(String keyword, String locationCategory) {
        // 1. Redis 시도 (최우선)
        try {
            redisKeywordRanking.incrementKeywordCount(keyword, locationCategory);
            logger.debug("Redis를 통한 키워드 기록 성공: keyword={}", keyword);

            // DB 백업을 위한 KeywordCountService 호출
            keywordCountService.ensureKeywordExists(keyword);
            return;

        } catch (Exception e) {
            logger.warn("Redis 키워드 기록 실패, Memory로 failover: keyword={}, error={}",
                    keyword, e.getMessage());
        }

        // 2. Memory로 failover
        try {
            memoryKeywordRanking.incrementKeywordCount(keyword, locationCategory);
            logger.debug("Memory를 통한 키워드 기록 성공: keyword={}", keyword);

            // DB 백업을 위한 KeywordCountService 호출
            keywordCountService.ensureKeywordExists(keyword);

        } catch (Exception e) {
            logger.error("Memory 키워드 기록도 실패: keyword={}, error={}", keyword, e.getMessage());
            throw e; // 모든 Infrastructure 실패시 예외 전파
        }
    }

    /**
     * Redis에서 인기 키워드 조회
     */
    private PopularKeywordResponse tryGetFromRedis(String locationCategory, int limit) {
        try {
            if (redisKeywordRanking.isAvailable()) {
                List<kr.hhplus.be.server.infrastructure.keyword.KeywordDto> redisResults =
                        hasValidLocationCategory(locationCategory)
                                ? redisKeywordRanking.getTopKeywordsByLocation(locationCategory, limit)
                                : redisKeywordRanking.getTopKeywords(limit);

                List<KeywordDto> keywords = redisResults.stream()
                        .map(dto -> new KeywordDto(dto.getKeyword(), dto.getCount().intValue()))
                        .collect(Collectors.toList());

                return new PopularKeywordResponse(keywords, REDIS_DATA_SOURCE);
            }
        } catch (Exception e) {
            logger.warn("Redis 인기 키워드 조회 실패, Memory로 fallback: location={}, error={}",
                    locationCategory, e.getMessage());
        }
        return null;
    }

    /**
     * Memory에서 인기 키워드 조회
     */
    private PopularKeywordResponse tryGetFromMemory(String locationCategory, int limit) {
        try {
            if (memoryKeywordRanking.isAvailable()) {
                List<kr.hhplus.be.server.infrastructure.keyword.KeywordDto> memoryResults =
                        hasValidLocationCategory(locationCategory)
                                ? memoryKeywordRanking.getTopKeywordsByLocation(locationCategory, limit)
                                : memoryKeywordRanking.getTopKeywords(limit);

                List<KeywordDto> keywords = memoryResults.stream()
                        .map(dto -> new KeywordDto(dto.getKeyword(), dto.getCount().intValue()))
                        .collect(Collectors.toList());

                return new PopularKeywordResponse(keywords, MEMORY_DATA_SOURCE);
            }
        } catch (Exception e) {
            logger.warn("Memory 인기 키워드 조회 실패, Database로 fallback: location={}, error={}",
                    locationCategory, e.getMessage());
        }
        return null;
    }

    /**
     * Database에서 인기 키워드 조회 (최종 fallback)
     */
    private PopularKeywordResponse getPopularKeywordsFromDatabase(String locationCategory, int limit) {
        try {
            Long locationCategoryId = convertToLocationCategoryId(locationCategory);

            List<KeywordCount> keywordCounts = keywordCountRepository
                    .findTopKeywordsByLocationCategoryAndDate(locationCategoryId, LocalDate.now(), limit);

            // N+1 문제 해결을 위한 배치 조회
            List<KeywordDto> keywords = getKeywordDtosBatch(keywordCounts);

            return new PopularKeywordResponse(keywords, DATABASE_DATA_SOURCE);

        } catch (Exception e) {
            logger.error("Database 인기 키워드 조회도 실패: location={}, error={}",
                    locationCategory, e.getMessage(), e);
            // 최종 실패시 빈 응답 반환
            return new PopularKeywordResponse(List.of(), DATABASE_DATA_SOURCE);
        }
    }

    /**
     * Infrastructure Layer 상태 확인
     */
    public boolean isRedisAvailable() {
        return redisKeywordRanking.isAvailable();
    }

    public boolean isMemoryAvailable() {
        return memoryKeywordRanking.isAvailable();
    }

    /**
     * 사용 가능한 키워드 저장소 목록
     */
    public List<String> getAvailableStorages() {
        return List.of(
                        isRedisAvailable() ? "redis" : null,
                        isMemoryAvailable() ? "memory" : null,
                        "database" // Database는 항상 사용 가능
                ).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // === Private Helper Methods ===

    private int getValidatedLimit(Integer limit) {
        return limit != null && limit > 0 ? limit : DEFAULT_POPULAR_KEYWORD_LIMIT;
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
}