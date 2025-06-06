package kr.hhplus.be.server.keyword.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
    private static final String KEYWORD_COUNT_KEY = "keyword:count";
    private static final String LOCATION_KEYWORD_COUNT_PREFIX = "keyword:count:location:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ZSetOperations<String, Object> zSetOps;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.zSetOps = redisTemplate.opsForZSet();
    }

    /**
     * Redis가 사용 가능한지 확인
     */
    public boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            logger.warn("Redis 연결 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 키워드 카운트를 1 증가
     */
    public void incrementKeywordCount(String keyword) {
        incrementKeywordCount(keyword, null);
    }

    /**
     * 지역별 키워드 카운트를 1 증가
     */
    public void incrementKeywordCount(String keyword, String locationCategory) {
        try {
            // 전체 키워드 카운트 증가
            zSetOps.incrementScore(KEYWORD_COUNT_KEY, keyword, 1);

            // 지역별 키워드 카운트 증가 (locationCategory가 있는 경우)
            if (locationCategory != null && !locationCategory.trim().isEmpty()) {
                String locationKey = LOCATION_KEYWORD_COUNT_PREFIX + locationCategory;
                zSetOps.incrementScore(locationKey, keyword, 1);
            }

            logger.debug("Redis 키워드 카운트 증가: keyword={}, location={}", keyword, locationCategory);
        } catch (Exception e) {
            logger.error("Redis 키워드 카운트 증가 실패: keyword={}, error={}", keyword, e.getMessage());
            throw new RuntimeException("Redis 키워드 카운트 증가 실패", e);
        }
    }

    /**
     * 인기 키워드 Top N 조회
     */
    public Set<ZSetOperations.TypedTuple<Object>> getTopKeywords(int limit) {
        try {
            return zSetOps.reverseRangeWithScores(KEYWORD_COUNT_KEY, 0, limit - 1);
        } catch (Exception e) {
            logger.error("Redis 인기 키워드 조회 실패: {}", e.getMessage());
            throw new RuntimeException("Redis 인기 키워드 조회 실패", e);
        }
    }

    /**
     * 지역별 인기 키워드 Top N 조회
     */
    public Set<ZSetOperations.TypedTuple<Object>> getTopKeywordsByLocation(String locationCategory, int limit) {
        try {
            String locationKey = LOCATION_KEYWORD_COUNT_PREFIX + locationCategory;
            return zSetOps.reverseRangeWithScores(locationKey, 0, limit - 1);
        } catch (Exception e) {
            logger.error("Redis 지역별 인기 키워드 조회 실패: location={}, error={}", locationCategory, e.getMessage());
            throw new RuntimeException("Redis 지역별 인기 키워드 조회 실패", e);
        }
    }

    /**
     * 키워드 점수 조회
     */
    public Double getKeywordScore(String keyword) {
        try {
            return zSetOps.score(KEYWORD_COUNT_KEY, keyword);
        } catch (Exception e) {
            logger.error("Redis 키워드 점수 조회 실패: keyword={}, error={}", keyword, e.getMessage());
            return null;
        }
    }

    /**
     * 모든 키워드 데이터 조회 (DB 백업용)
     */
    public Set<ZSetOperations.TypedTuple<Object>> getAllKeywordData() {
        try {
            return zSetOps.rangeWithScores(KEYWORD_COUNT_KEY, 0, -1);
        } catch (Exception e) {
            logger.error("Redis 전체 키워드 데이터 조회 실패: {}", e.getMessage());
            throw new RuntimeException("Redis 전체 키워드 데이터 조회 실패", e);
        }
    }

    /**
     * 키 만료 시간 설정
     */
    public void setExpire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            logger.error("Redis 키 만료 시간 설정 실패: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * Redis 데이터 초기화 (테스트용)
     */
    public void clearAllKeywordData() {
        try {
            redisTemplate.delete(KEYWORD_COUNT_KEY);
            // 지역별 키도 모두 삭제
            Set<String> keys = redisTemplate.keys(LOCATION_KEYWORD_COUNT_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            logger.info("Redis 키워드 데이터 초기화 완료");
        } catch (Exception e) {
            logger.error("Redis 키워드 데이터 초기화 실패: {}", e.getMessage());
        }
    }
}