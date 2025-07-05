package kr.hhplus.be.server.infrastructure.keyword;

// Memory 기반 Fallback 구현

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Component
public class MemoryKeywordRankingImpl implements KeywordRanking {

    private final Map<String, LongAdder> globalKeywordCounts = new ConcurrentHashMap<>();
    private final Map<String, Map<String, LongAdder>> locationKeywordCounts = new ConcurrentHashMap<>();

    @Override
    public void incrementKeywordCount(String keyword) {
        globalKeywordCounts.computeIfAbsent(keyword, k -> new LongAdder()).increment();
    }

    @Override
    public void incrementKeywordCount(String keyword, String location) {
        incrementKeywordCount(keyword);

        if (location != null && !location.trim().isEmpty()) {
            locationKeywordCounts
                    .computeIfAbsent(location, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(keyword, k -> new LongAdder())
                    .increment();
        }
    }

    @Override
    public List<KeywordDto> getTopKeywords(int limit) {
        return globalKeywordCounts.entrySet().stream()
                .map(entry -> new KeywordDto(entry.getKey(), entry.getValue().longValue()))
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<KeywordDto> getTopKeywordsByLocation(String location, int limit) {
        Map<String, LongAdder> locationMap = locationKeywordCounts.get(location);
        if (locationMap == null) {
            return Collections.emptyList();
        }

        return locationMap.entrySet().stream()
                .map(entry -> new KeywordDto(entry.getKey(), entry.getValue().longValue()))
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Long getKeywordCount(String keyword) {
        LongAdder adder = globalKeywordCounts.get(keyword);
        return adder != null ? adder.longValue() : 0L;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    // 데이터 내보내기 (Redis 복구 시 사용)
    public Map<String, Long> exportGlobalKeywords() {
        return globalKeywordCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().longValue()
                ));
    }

    public Map<String, Map<String, Long>> exportLocationKeywords() {
        return locationKeywordCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> e.getValue().longValue()
                                ))
                ));
    }
}
