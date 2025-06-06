package kr.hhplus.be.server.keyword.service;

import kr.hhplus.be.server.keyword.domain.Keyword;
import kr.hhplus.be.server.keyword.domain.KeywordCount;
import kr.hhplus.be.server.keyword.repository.KeywordRepository;
import kr.hhplus.be.server.keyword.repository.KeywordCountRepository;
import kr.hhplus.be.server.fake.FakeMemoryMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class KeywordCountService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordCountService.class);

    private final KeywordRepository keywordRepository;
    private final KeywordCountRepository keywordCountRepository;
    private final RedisService redisService;
    private final FakeMemoryMap memoryMap;

    @Autowired
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
     */
    public void incrementKeywordCount(String keyword, String locationCategory) {
        try {
            // 1. 키워드 저장 (존재하지 않으면 새로 생성)
            ensureKeywordExists(keyword);

            // 2. Redis 사용 가능하면 Redis에 저장
            if (redisService.isRedisAvailable()) {
                redisService.incrementKeywordCount(keyword, locationCategory);
                logger.debug("Redis에 키워드 카운트 증가: {}", keyword);
            } else {
                // 3. Redis 사용 불가능하면 메모리 Map에 저장
                memoryMap.increment(keyword);
                logger.debug("메모리 Map에 키워드 카운트 증가: {}", keyword);
            }

        } catch (Exception e) {
            logger.error("키워드 카운트 증가 실패: keyword={}, error={}", keyword, e.getMessage());
            // Redis 실패시 메모리 Map으로 fallback
            try {
                memoryMap.increment(keyword);
                logger.info("Redis 실패로 메모리 Map으로 fallback: {}", keyword);
            } catch (Exception fallbackError) {
                logger.error("메모리 Map fallback도 실패: {}", fallbackError.getMessage());
            }
        }
    }

    /**
     * 5분마다 메모리 Map 데이터를 DB로 백업
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    public void backupMemoryMapToDatabase() {
        if (memoryMap.isEmpty()) {
            return;
        }

        logger.info("메모리 Map 데이터를 DB로 백업 시작. 항목 수: {}", memoryMap.size());

        try {
            Map<String, Integer> snapshot = memoryMap.getAllData();

            for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
                String keyword = entry.getKey();
                Integer count = entry.getValue();

                backupKeywordCount(keyword, count);
            }

            // 백업 완료 후 메모리 Map 초기화
            memoryMap.clear();
            logger.info("메모리 Map 데이터 DB 백업 완료");

        } catch (Exception e) {
            logger.error("메모리 Map 데이터 DB 백업 실패: {}", e.getMessage());
        }
    }

    private void ensureKeywordExists(String keyword) {
        Optional<Keyword> existingKeyword = keywordRepository.findByKeyword(keyword);

        if (existingKeyword.isEmpty()) {
            // 키워드 정규화 (공백 제거 등)
            String normalizedKeyword = normalizeKeyword(keyword);
            Keyword newKeyword = new Keyword(keyword, normalizedKeyword);
            keywordRepository.save(newKeyword);
            logger.debug("새 키워드 저장: {}", keyword);
        }
    }

    private void backupKeywordCount(String keyword, Integer memoryCount) {
        try {
            // 키워드 조회
            Optional<Keyword> keywordOpt = keywordRepository.findByKeyword(keyword);
            if (keywordOpt.isEmpty()) {
                logger.warn("백업할 키워드가 DB에 없음: {}", keyword);
                return;
            }

            Keyword keywordEntity = keywordOpt.get();
            LocalDate today = LocalDate.now();

            // 오늘 날짜의 키워드 카운트 조회
            Optional<KeywordCount> existingCount = keywordCountRepository
                    .findByKeywordIdAndLocationCategoryIdAndCountDate(
                            keywordEntity.getId(), null, today);

            if (existingCount.isPresent()) {
                // 기존 카운트 업데이트
                KeywordCount keywordCount = existingCount.get();
                keywordCount.updateCount(keywordCount.getCount() + memoryCount);
                keywordCountRepository.save(keywordCount);
                logger.debug("기존 키워드 카운트 업데이트: {} ({})", keyword, keywordCount.getCount());
            } else {
                // 새 카운트 생성
                KeywordCount newCount = new KeywordCount(keywordEntity.getId(), null, memoryCount);
                keywordCountRepository.save(newCount);
                logger.debug("새 키워드 카운트 생성: {} ({})", keyword, memoryCount);
            }

        } catch (Exception e) {
            logger.error("키워드 카운트 백업 실패: keyword={}, error={}", keyword, e.getMessage());
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }
}