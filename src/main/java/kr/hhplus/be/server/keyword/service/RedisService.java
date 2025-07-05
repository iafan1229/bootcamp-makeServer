package kr.hhplus.be.server.keyword.service;

import kr.hhplus.be.server.infrastructure.keyword.KeywordRanking;
import kr.hhplus.be.server.infrastructure.keyword.RedisKeywordRankingImpl;
import kr.hhplus.be.server.common.util.RedisKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ZSetOperations<String, Object> zSetOps;
    private final KeywordRanking redisKeywordRanking;
    private final RedisKeyGenerator keyGenerator;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate,
                        RedisKeywordRankingImpl redisKeywordRanking,
                        RedisKeyGenerator keyGenerator) {
        this.redisTemplate = redisTemplate;
        this.zSetOps = redisTemplate.opsForZSet();
        this.redisKeywordRanking = redisKeywordRanking;
        this.keyGenerator = keyGenerator;
    }

    /**
     * Redis가 사용 가능한지 확인 - Infrastructure Layer 활용
     */
    public boolean isRedisAvailable() {
        return redisKeywordRanking.isAvailable();
    }

    /**
     * 키워드 카운트를 1 증가 - Infrastructure Layer 위임
     */
    public void incrementKeywordCount(String keyword) {
        incrementKeywordCount(keyword, null);
    }

    /**
     * 지역별 키워드 카운트를 1 증가 - Infrastructure Layer 위임
     */
    public void incrementKeywordCount(String keyword, String locationCategory) {
        try {
            redisKeywordRanking.incrementKeywordCount(keyword, locationCategory);
            logger.debug("Infrastructure Layer를 통한 Redis 키워드 카운트 증가: keyword={}, location={}",
                    keyword, locationCategory);
        } catch (Exception e) {
            logger.error("Infrastructure Layer Redis 키워드 카운트 증가 실패: keyword={}, error={}",
                    keyword, e.getMessage());
            throw new RuntimeException("Redis 키워드 카운트 증가 실패", e);
        }
    }

    /**
     * 인기 키워드 Top N 조회 - Infrastructure Layer 활용
     */
    public List<kr.hhplus.be.server.infrastructure.keyword.KeywordDto> getTopKeywords(int limit) {
        try {
            return redisKeywordRanking.getTopKeywords(limit);
        } catch (Exception e) {
            logger.error("Infrastructure Layer를 통한 Redis 인기 키워드 조회 실패: {}", e.getMessage());
            throw new RuntimeException("Redis 인기 키워드 조회 실패", e);
        }
    }

    /**
     * 지역별 인기 키워드 Top N 조회 - Infrastructure Layer 활용
     */
    public List<kr.hhplus.be.server.infrastructure.keyword.KeywordDto> getTopKeywordsByLocation(
            String locationCategory, int limit) {
        try {
            return redisKeywordRanking.getTopKeywordsByLocation(locationCategory, limit);
        } catch (Exception e) {
            logger.error("Infrastructure Layer를 통한 Redis 지역별 인기 키워드 조회 실패: location={}, error={}",
                    locationCategory, e.getMessage());
            throw new RuntimeException("Redis 지역별 인기 키워드 조회 실패", e);
        }
    }

    /**
     * 키워드 점수 조회 - Infrastructure Layer 활용
     */
    public Long getKeywordScore(String keyword) {
        try {
            List<kr.hhplus.be.server.infrastructure.keyword.KeywordDto> results =
                    redisKeywordRanking.getTopKeywords(Integer.MAX_VALUE);

            return results.stream()
                    .filter(dto -> keyword.equals(dto.getKeyword()))
                    .findFirst()
                    .map(kr.hhplus.be.server.infrastructure.keyword.KeywordDto::getCount)
                    .orElse(0L);

        } catch (Exception e) {
            logger.error("Infrastructure Layer를 통한 Redis 키워드 점수 조회 실패: keyword={}, error={}",
                    keyword, e.getMessage());
            return null;
        }
    }

    /**
     * 모든 키워드 데이터 조회 - Infrastructure Layer 활용 (DB 백업용)
     */
    public List<kr.hhplus.be.server.infrastructure.keyword.KeywordDto> getAllKeywordData() {
        try {
            return redisKeywordRanking.getTopKeywords(Integer.MAX_VALUE);
        } catch (Exception e) {
            logger.error("Infrastructure Layer를 통한 Redis 전체 키워드 데이터 조회 실패: {}", e.getMessage());
            throw new RuntimeException("Redis 전체 키워드 데이터 조회 실패", e);
        }
    }

    /**
     * 키 만료 시간 설정 - 직접 Redis 템플릿 사용
     */
    public void setExpire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
            logger.debug("Redis 키 만료 시간 설정: key={}, timeout={}, unit={}", key, timeout, unit);
        } catch (Exception e) {
            logger.error("Redis 키 만료 시간 설정 실패: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 검색 결과 캐시 설정
     */
    public void setCacheWithExpiry(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            logger.debug("Redis 캐시 설정: key={}, timeout={}, unit={}", key, timeout, unit);
        } catch (Exception e) {
            logger.error("Redis 캐시 설정 실패: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 검색 결과 캐시 조회
     */
    public Object getCache(String key) {
        try {
            Object result = redisTemplate.opsForValue().get(key);
            if (result != null) {
                logger.debug("Redis 캐시 조회 성공: key={}", key);
            }
            return result;
        } catch (Exception e) {
            logger.error("Redis 캐시 조회 실패: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Redis 데이터 초기화 - Infrastructure Layer 활용 (테스트용)
     */
    public void clearAllKeywordData() {
        try {
            // Infrastructure Layer의 clear 기능 활용 (구현 필요)
            // redisKeywordRanking.clearAll();

            // 임시로 직접 키 삭제
            String globalKey = keyGenerator.generateGlobalKeywordCountKey();
            redisTemplate.delete(globalKey);

            // 지역별 키도 모두 삭제
            Set<String> locationKeys = redisTemplate.keys(
                    keyGenerator.generateLocationKeywordCountKey("*"));
            if (locationKeys != null && !locationKeys.isEmpty()) {
                redisTemplate.delete(locationKeys);
            }

            logger.info("Redis 키워드 데이터 초기화 완료");
        } catch (Exception e) {
            logger.error("Redis 키워드 데이터 초기화 실패: {}", e.getMessage());
        }
    }

    /**
     * Redis 연결 상태 확인
     */
    public boolean testConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            logger.debug("Redis 연결 상태 정상");
            return true;
        } catch (Exception e) {
            logger.warn("Redis 연결 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Redis 키워드 랭킹 시스템 상태 확인
     */
    public String getRedisSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Connection: ").append(testConnection() ? "OK" : "FAILED");
        status.append(", KeywordRanking: ").append(isRedisAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        try {
            List<kr.hhplus.be.server.infrastructure.keyword.KeywordDto> topKeywords = getTopKeywords(5);
            status.append(", TopKeywords: ").append(topKeywords.size()).append(" items");
        } catch (Exception e) {
            status.append(", TopKeywords: ERROR");
        }

        return status.toString();
    }

    /**
     * Infrastructure Layer와 직접 Redis 작업의 브리지 역할
     * 기존 코드와의 호환성을 위해 유지
     */
    @Deprecated
    public Set<ZSetOperations.TypedTuple<Object>> getTopKeywords_Legacy(int limit) {
        try {
            String globalKey = keyGenerator.generateGlobalKeywordCountKey();
            return zSetOps.reverseRangeWithScores(globalKey, 0, limit - 1);
        } catch (Exception e) {
            logger.error("Legacy Redis 인기 키워드 조회 실패: {}", e.getMessage());
            throw new RuntimeException("Legacy Redis 인기 키워드 조회 실패", e);
        }
    }

    /**
     * Infrastructure Layer와 직접 Redis 작업의 브리지 역할
     * 기존 코드와의 호환성을 위해 유지
     */
    @Deprecated
    public Set<ZSetOperations.TypedTuple<Object>> getTopKeywordsByLocation_Legacy(String locationCategory, int limit) {
        try {
            String locationKey = keyGenerator.generateLocationKeywordCountKey(locationCategory);
            return zSetOps.reverseRangeWithScores(locationKey, 0, limit - 1);
        } catch (Exception e) {
            logger.error("Legacy Redis 지역별 인기 키워드 조회 실패: location={}, error={}", locationCategory, e.getMessage());
            throw new RuntimeException("Legacy Redis 지역별 인기 키워드 조회 실패", e);
        }
    }
}