package kr.hhplus.be.server.infrastructure.keyword;

// Redis 기반 키워드 랭킹 서비스
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RedisKeywordRankingImpl implements KeywordRanking {

    // Redis Key 패턴
    private static final String KEYWORD_RANKING_KEY = "keyword:ranking";
    private static final String LOCATION_KEYWORD_PREFIX = "keyword:location:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ZSetOperations<String, String> zSetOps;

    public RedisKeywordRankingImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.zSetOps = redisTemplate.opsForZSet(); // Redis Sorted Set 사용
    }

    @Override
    public void incrementKeywordCount(String keyword) {
        // Redis Sorted Set의 ZINCRBY 명령어 실행
        // "keyword:ranking" 키에서 keyword의 점수를 1 증가
        zSetOps.incrementScore(KEYWORD_RANKING_KEY, keyword, 1);
    }

    @Override
    public void incrementKeywordCount(String keyword, String location) {
        // 전체 키워드 카운트 증가
        incrementKeywordCount(keyword);

        // 지역별 키워드 카운트 증가
        if (location != null && !location.trim().isEmpty()) {
            String locationKey = LOCATION_KEYWORD_PREFIX + location;
            // "keyword:location:서울" 같은 키로 지역별 랭킹 관리
            zSetOps.incrementScore(locationKey, keyword, 1);
        }
    }

    @Override
    public List<KeywordDto> getTopKeywords(int limit) {
        // Redis ZREVRANGE 명령어로 점수 높은 순으로 조회
        Set<ZSetOperations.TypedTuple<String>> results =
                zSetOps.reverseRangeWithScores(KEYWORD_RANKING_KEY, 0, limit - 1);

        return results.stream()
                .map(tuple -> new KeywordDto(tuple.getValue(),
                        tuple.getScore() != null ? tuple.getScore().longValue() : 0L))
                .collect(Collectors.toList());
    }

    @Override
    public List<KeywordDto> getTopKeywordsByLocation(String location, int limit) {
        String locationKey = LOCATION_KEYWORD_PREFIX + location;
        // 지역별 Redis Key에서 상위 키워드 조회
        Set<ZSetOperations.TypedTuple<String>> results =
                zSetOps.reverseRangeWithScores(locationKey, 0, limit - 1);

        return results.stream()
                .map(tuple -> new KeywordDto(tuple.getValue(),
                        tuple.getScore() != null ? tuple.getScore().longValue() : 0L))
                .collect(Collectors.toList());
    }

    @Override
    public Long getKeywordCount(String keyword) {
        // Redis ZSCORE 명령어로 특정 키워드의 점수 조회
        Double score = zSetOps.score(KEYWORD_RANKING_KEY, keyword);
        return score != null ? score.longValue() : 0L;
    }

    @Override
    public boolean isAvailable() {
        try {
            // Redis 연결 상태 확인
            redisTemplate.hasKey(KEYWORD_RANKING_KEY);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 추가: Redis 데이터 백업/복원 메서드
    public void backupToMemory(MemoryKeywordRankingImpl memoryService) {
        try {
            // 전체 키워드 백업
            Set<ZSetOperations.TypedTuple<String>> globalKeywords =
                    zSetOps.rangeWithScores(KEYWORD_RANKING_KEY, 0, -1);

            for (ZSetOperations.TypedTuple<String> tuple : globalKeywords) {
                String keyword = tuple.getValue();
                Long count = tuple.getScore().longValue();

                // Memory 서비스에 데이터 복원
                for (int i = 0; i < count; i++) {
                    memoryService.incrementKeywordCount(keyword);
                }
            }

        } catch (Exception e) {
            System.err.println("Redis 백업 실패: " + e.getMessage());
        }
    }

    public void restoreFromMemory(MemoryKeywordRankingImpl memoryService) {
        try {
            // Memory에서 Redis로 데이터 복원
            Map<String, Long> globalKeywords = memoryService.exportGlobalKeywords();

            for (Map.Entry<String, Long> entry : globalKeywords.entrySet()) {
                zSetOps.add(KEYWORD_RANKING_KEY, entry.getKey(), entry.getValue());
            }

            // 지역별 데이터 복원
            Map<String, Map<String, Long>> locationKeywords = memoryService.exportLocationKeywords();
            for (Map.Entry<String, Map<String, Long>> locationEntry : locationKeywords.entrySet()) {
                String locationKey = LOCATION_KEYWORD_PREFIX + locationEntry.getKey();

                for (Map.Entry<String, Long> keywordEntry : locationEntry.getValue().entrySet()) {
                    zSetOps.add(locationKey, keywordEntry.getKey(), keywordEntry.getValue());
                }
            }

        } catch (Exception e) {
            System.err.println("Redis 복원 실패: " + e.getMessage());
        }
    }
}
