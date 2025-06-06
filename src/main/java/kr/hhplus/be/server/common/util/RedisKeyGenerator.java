package kr.hhplus.be.server.common.util;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyGenerator {

    private static final String KEYWORD_COUNT_PREFIX = "keyword:count";
    private static final String LOCATION_PREFIX = "location";
    private static final String SEPARATOR = ":";

    /**
     * 전체 키워드 카운트 키 생성
     */
    public String generateGlobalKeywordCountKey() {
        return KEYWORD_COUNT_PREFIX;
    }

    /**
     * 지역별 키워드 카운트 키 생성
     */
    public String generateLocationKeywordCountKey(String locationCategory) {
        return KEYWORD_COUNT_PREFIX + SEPARATOR + LOCATION_PREFIX + SEPARATOR + locationCategory;
    }

    /**
     * 검색 결과 캐시 키 생성
     */
    public String generateSearchCacheKey(String keyword, String location, String sort, int page, int size) {
        StringBuilder keyBuilder = new StringBuilder("search:cache");
        keyBuilder.append(SEPARATOR).append(keyword);

        if (location != null && !location.trim().isEmpty()) {
            keyBuilder.append(SEPARATOR).append("location").append(SEPARATOR).append(location);
        }

        keyBuilder.append(SEPARATOR).append("sort").append(SEPARATOR).append(sort);
        keyBuilder.append(SEPARATOR).append("page").append(SEPARATOR).append(page);
        keyBuilder.append(SEPARATOR).append("size").append(SEPARATOR).append(size);

        return keyBuilder.toString();
    }
}