package kr.hhplus.be.server.keyword.service;

import kr.hhplus.be.server.keyword.domain.Keyword;
import kr.hhplus.be.server.keyword.domain.KeywordCount;
import kr.hhplus.be.server.keyword.repository.KeywordRepository;
import kr.hhplus.be.server.keyword.repository.KeywordCountRepository;
import kr.hhplus.be.server.fake.FakeMemoryMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Service
public class KeywordCountService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordCountService.class);

    private final KeywordRepository keywordRepository;
    private final KeywordCountRepository keywordCountRepository;
    private final RedisService redisService;
    private final FakeMemoryMap memoryMap;

    public KeywordCountService(KeywordRepository keywordRepository,
                               KeywordCountRepository keywordCountRepository,
                               RedisService redisService,
                               FakeMemoryMap memoryMap) {
        this.keywordRepository = keywordRepository;
        this.keywordCountRepository = keywordCountRepository;
        this.redisService = redisService;
        this.memoryMap = memoryMap;
    }

    /**
     * 키워드 카운트 증가 (Redis 우선, 실패시 메모리 Map 사용)
     */
    public void incrementKeywordCount(String keyword) {
        incrementKeywordCount(keyword, null);
    }

    /**
     * 지역별 키워드 카운트 증가
     * 트랜잭션 제거 이유: 주요 작업이 Redis/메모리 기반이므로 불필요
     * DB 작업(ensureKeywordExists)만 별도 트랜잭션 적용
     */
    public void incrementKeywordCount(String keyword, String locationCategory) {
        try {
            // 1. DB 작업 - 키워드 존재 확인/생성 (별도 트랜잭션)
            ensureKeywordExists(keyword);

            // 2. 캐시/메모리 작업 - 트랜잭션 불필요
            if (redisService.isRedisAvailable()) {
                incrementRedisCount(keyword, locationCategory);
            } else {
                incrementMemoryCount(keyword);
            }

        } catch (Exception e) {
            logger.error("키워드 카운트 증가 실패: keyword={}, location={}, error={}",
                    keyword, locationCategory, e.getMessage(), e);
            // fallback 처리
            handleIncrementFallback(keyword);
        }
    }

    /**
     * 5분마다 메모리 Map 데이터를 DB로 백업
     * 트랜잭션 적용: 배치 백업 작업의 원자성 보장
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    @Transactional
    public void backupMemoryMapToDatabase() {
        if (memoryMap.isEmpty()) {
            return;
        }

        logger.info("메모리 Map 데이터를 DB로 백업 시작. 항목 수: {}", memoryMap.size());

        try {
            Map<String, Integer> snapshot = memoryMap.getAllData();

            for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
                backupSingleKeywordCount(entry.getKey(), entry.getValue());
            }

            // 백업 완료 후 메모리 Map 초기화
            memoryMap.clear();
            logger.info("메모리 Map 데이터 DB 백업 완료");

        } catch (Exception e) {
            logger.error("메모리 Map 데이터 DB 백업 실패: {}", e.getMessage(), e);
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    /**
     * 키워드 존재 확인 및 생성
     * 트랜잭션 적용: DB 일관성 보장
     */
    @Transactional
    public void ensureKeywordExists(String keyword) {
        if (isInvalidKeyword(keyword)) {
            logger.warn("유효하지 않은 키워드: {}", keyword);
            return;
        }

        Optional<Keyword> existingKeyword = keywordRepository.findByKeyword(keyword);

        if (existingKeyword.isEmpty()) {
            String normalizedKeyword = normalizeKeyword(keyword);
            Keyword newKeyword = new Keyword(keyword, normalizedKeyword);
            keywordRepository.save(newKeyword);
            logger.debug("새 키워드 저장: {}", keyword);
        }
    }

    private void incrementRedisCount(String keyword, String locationCategory) {
        try {
            redisService.incrementKeywordCount(keyword, locationCategory);
            logger.debug("Redis에 키워드 카운트 증가: keyword={}, location={}", keyword, locationCategory);
        } catch (Exception e) {
            logger.warn("Redis 카운트 증가 실패: keyword={}, error={}", keyword, e.getMessage());
            throw e; // 상위에서 fallback 처리
        }
    }

    private void incrementMemoryCount(String keyword) {
        try {
            memoryMap.increment(keyword);
            logger.debug("메모리 Map에 키워드 카운트 증가: {}", keyword);
        } catch (Exception e) {
            logger.error("메모리 Map 카운트 증가 실패: keyword={}, error={}", keyword, e.getMessage());
            throw e;
        }
    }

    private void handleIncrementFallback(String keyword) {
        try {
            memoryMap.increment(keyword);
            logger.info("Redis 실패로 메모리 Map으로 fallback: {}", keyword);
        } catch (Exception fallbackError) {
            logger.error("메모리 Map fallback도 실패: keyword={}, error={}",
                    keyword, fallbackError.getMessage(), fallbackError);
        }
    }

    private void backupSingleKeywordCount(String keyword, Integer memoryCount) {
        Optional<Keyword> keywordOpt = keywordRepository.findByKeyword(keyword);
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

    private boolean isInvalidKeyword(String keyword) {
        return keyword == null || keyword.trim().isEmpty();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }
}