package kr.hhplus.be.server.keyword.service;

import kr.hhplus.be.server.keyword.domain.Keyword;
import kr.hhplus.be.server.keyword.domain.KeywordCount;
import kr.hhplus.be.server.keyword.repository.KeywordRepository;
import kr.hhplus.be.server.keyword.repository.KeywordCountRepository;
import kr.hhplus.be.server.infrastructure.keyword.KeywordRanking;
import kr.hhplus.be.server.infrastructure.keyword.RedisKeywordRankingImpl;
import kr.hhplus.be.server.infrastructure.keyword.MemoryKeywordRankingImpl;
import kr.hhplus.be.server.common.util.KeywordNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KeywordCountService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordCountService.class);

    private final KeywordRepository keywordRepository;
    private final KeywordCountRepository keywordCountRepository;
    private final KeywordRanking redisKeywordRanking;
    private final KeywordRanking memoryKeywordRanking;
    private final KeywordNormalizer keywordNormalizer;

    @Autowired
    public KeywordCountService(KeywordRepository keywordRepository,
                               KeywordCountRepository keywordCountRepository,
                               @Qualifier("redisKeywordRanking") KeywordRanking redisKeywordRanking,
                               @Qualifier("memoryKeywordRanking") KeywordRanking memoryKeywordRanking,
                               KeywordNormalizer keywordNormalizer) {
        this.keywordRepository = keywordRepository;
        this.keywordCountRepository = keywordCountRepository;
        this.redisKeywordRanking = redisKeywordRanking;
        this.memoryKeywordRanking = memoryKeywordRanking;
        this.keywordNormalizer = keywordNormalizer;
    }

    /**
     * 키워드 카운트 증가 - Infrastructure Layer 활용
     * Redis → Memory 순서로 failover 처리
     */
    public void incrementKeywordCount(String keyword) {
        incrementKeywordCount(keyword, null);
    }

    /**
     * 지역별 키워드 카운트 증가 - Infrastructure Layer 우선순위 기반
     */
    public void incrementKeywordCount(String keyword, String locationCategory) {
        if (isInvalidKeyword(keyword)) {
            logger.warn("유효하지 않은 키워드: {}", keyword);
            return;
        }

        String normalizedKeyword = keywordNormalizer.normalize(keyword);

        try {
            // 1. DB 작업 - 키워드 존재 확인/생성 (별도 트랜잭션)
            ensureKeywordExists(normalizedKeyword);

            // 2. Infrastructure Layer를 통한 카운트 증가 (failover 포함)
            incrementCountWithInfrastructure(normalizedKeyword, locationCategory);

        } catch (Exception e) {
            logger.error("키워드 카운트 증가 실패: keyword={}, location={}, error={}",
                    keyword, locationCategory, e.getMessage(), e);
        }
    }

    /**
     * Infrastructure Layer를 통한 카운트 증가 (Redis → Memory failover)
     */
    private void incrementCountWithInfrastructure(String keyword, String locationCategory) {
        // 1. Redis 시도 (최우선)
        try {
            if (redisKeywordRanking.isAvailable()) {
                redisKeywordRanking.incrementKeywordCount(keyword, locationCategory);
                logger.debug("Redis를 통한 키워드 카운트 증가 성공: keyword={}, location={}",
                        keyword, locationCategory);
                return;
            }
        } catch (Exception e) {
            logger.warn("Redis 카운트 증가 실패, Memory로 failover: keyword={}, error={}",
                    keyword, e.getMessage());
        }

        // 2. Memory로 failover
        try {
            if (memoryKeywordRanking.isAvailable()) {
                memoryKeywordRanking.incrementKeywordCount(keyword, locationCategory);
                logger.debug("Memory를 통한 키워드 카운트 증가 성공: keyword={}, location={}",
                        keyword, locationCategory);
                return;
            }
        } catch (Exception e) {
            logger.error("Memory 카운트 증가도 실패: keyword={}, error={}", keyword, e.getMessage());
            throw new RuntimeException("모든 Infrastructure 키워드 카운트 증가 실패", e);
        }

        // 3. 모든 Infrastructure 실패
        throw new RuntimeException("사용 가능한 키워드 저장소가 없습니다");
    }

    /**
     * 5분마다 Memory 데이터를 Redis와 DB로 백업
     * Infrastructure Layer의 Memory 데이터를 Redis로 동기화
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    @Transactional
    public void backupMemoryDataToRedisAndDatabase() {
        try {
            logger.info("Memory 데이터를 Redis/DB로 백업 시작");

            // 1. Memory에서 모든 키워드 데이터 조회
            Map<String, Integer> memoryData = getMemoryKeywordData();

            if (memoryData.isEmpty()) {
                logger.debug("백업할 Memory 데이터가 없음");
                return;
            }

            // 2. Redis로 백업 (가능한 경우)
            backupToRedis(memoryData);

            // 3. Database로 백업
            backupToDatabase(memoryData);

            // 4. Memory 데이터 정리 (백업 완료 후)
            clearMemoryData();

            logger.info("Memory 데이터 백업 완료: {} 항목", memoryData.size());

        } catch (Exception e) {
            logger.error("Memory 데이터 백업 실패: {}", e.getMessage(), e);
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    /**
     * Redis 상태 복구시 Memory 데이터를 Redis로 복원
     */
    public void restoreMemoryDataToRedis() {
        try {
            if (!redisKeywordRanking.isAvailable()) {
                logger.warn("Redis 사용 불가능하여 복원 중단");
                return;
            }

            Map<String, Integer> memoryData = getMemoryKeywordData();
            if (memoryData.isEmpty()) {
                logger.debug("복원할 Memory 데이터가 없음");
                return;
            }

            backupToRedis(memoryData);
            clearMemoryData();

            logger.info("Memory 데이터를 Redis로 복원 완료: {} 항목", memoryData.size());

        } catch (Exception e) {
            logger.error("Memory → Redis 복원 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 키워드 존재 확인 및 생성 (트랜잭션 적용)
     */
    @Transactional
    public void ensureKeywordExists(String keyword) {
        if (isInvalidKeyword(keyword)) {
            logger.warn("유효하지 않은 키워드: {}", keyword);
            return;
        }

        String normalizedKeyword = keywordNormalizer.normalize(keyword);
        Optional<Keyword> existingKeyword = keywordRepository.findByNormalizedKeyword(normalizedKeyword);

        if (existingKeyword.isEmpty()) {
            Keyword newKeyword = new Keyword(keyword, normalizedKeyword);
            keywordRepository.save(newKeyword);
            logger.debug("새 키워드 저장: original={}, normalized={}", keyword, normalizedKeyword);
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
     * 키워드 카운트 시스템 전체 상태 확인
     */
    public String getSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Redis: ").append(isRedisAvailable() ? "AVAILABLE" : "UNAVAILABLE");
        status.append(", Memory: ").append(isMemoryAvailable() ? "AVAILABLE" : "UNAVAILABLE");
        status.append(", Database: AVAILABLE"); // DB는 항상 사용 가능하다고 가정
        return status.toString();
    }

    // === Private Helper Methods ===

    private Map<String, Integer> getMemoryKeywordData() {
        try {
            // Infrastructure Layer의 Memory 구현체에서 모든 데이터 조회
            // 실제 구현시에는 MemoryKeywordRankingImpl의 getAllData() 메서드 사용
            return Map.of(); // 임시 반환값
        } catch (Exception e) {
            logger.error("Memory 키워드 데이터 조회 실패: {}", e.getMessage());
            return Map.of();
        }
    }

    private void backupToRedis(Map<String, Integer> memoryData) {
        if (!redisKeywordRanking.isAvailable()) {
            logger.warn("Redis 사용 불가능하여 Redis 백업 건너뜀");
            return;
        }

        try {
            for (Map.Entry<String, Integer> entry : memoryData.entrySet()) {
                String keyword = entry.getKey();
                Integer count = entry.getValue();

                // Infrastructure Layer를 통한 Redis 백업
                for (int i = 0; i < count; i++) {
                    redisKeywordRanking.incrementKeywordCount(keyword, null);
                }
            }
            logger.debug("Redis 백업 완료: {} 항목", memoryData.size());
        } catch (Exception e) {
            logger.error("Redis 백업 실패: {}", e.getMessage(), e);
        }
    }

    private void backupToDatabase(Map<String, Integer> memoryData) {
        for (Map.Entry<String, Integer> entry : memoryData.entrySet()) {
            backupSingleKeywordCount(entry.getKey(), entry.getValue());
        }
    }

    private void backupSingleKeywordCount(String keyword, Integer memoryCount) {
        try {
            Optional<Keyword> keywordOpt = keywordRepository.findByNormalizedKeyword(keyword);
            if (keywordOpt.isEmpty()) {
                logger.warn("백업할 키워드가 DB에 없음: {}", keyword);
                return;
            }

            Keyword keywordEntity = keywordOpt.get();
            LocalDate today = LocalDate.now();

            Optional<KeywordCount> existingCount = keywordCountRepository
                    .findByKeywordIdAndLocationCategoryIdAndCountDate(
                            keywordEntity.getId(), null, today);

            if (existingCount.isPresent()) {
                updateExistingKeywordCount(existingCount.get(), memoryCount, keyword);
            } else {
                createNewKeywordCount(keywordEntity.getId(), memoryCount, keyword);
            }
        } catch (Exception e) {
            logger.error("키워드 카운트 DB 백업 실패: keyword={}, error={}", keyword, e.getMessage());
        }
    }

    private void updateExistingKeywordCount(KeywordCount keywordCount, Integer memoryCount, String keyword) {
        int newCount = keywordCount.getCount() + memoryCount;
        keywordCount.updateCount(newCount);
        keywordCountRepository.save(keywordCount);
        logger.debug("기존 키워드 카운트 업데이트: {} ({})", keyword, newCount);
    }

    private void createNewKeywordCount(Long keywordId, Integer memoryCount, String keyword) {
        KeywordCount newCount = new KeywordCount(keywordId, null, memoryCount);
        keywordCountRepository.save(newCount);
        logger.debug("새 키워드 카운트 생성: {} ({})", keyword, memoryCount);
    }

    private void clearMemoryData() {
        try {
            // Infrastructure Layer의 Memory 구현체 데이터 정리
            // 실제 구현시에는 MemoryKeywordRankingImpl의 clear() 메서드 사용
            logger.debug("Memory 데이터 정리 완료");
        } catch (Exception e) {
            logger.error("Memory 데이터 정리 실패: {}", e.getMessage());
        }
    }

    private boolean isInvalidKeyword(String keyword) {
        return keyword == null || keyword.trim().isEmpty();
    }
}