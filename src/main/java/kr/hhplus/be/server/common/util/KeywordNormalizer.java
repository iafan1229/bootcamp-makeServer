package kr.hhplus.be.server.common.util;

import org.springframework.stereotype.Component;

@Component
public class KeywordNormalizer {

    /**
     * 키워드 정규화
     * - 공백 제거
     * - 소문자 변환
     * - 특수문자 처리
     */
    public String normalize(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return keyword;
        }

        return keyword.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ") // 연속된 공백을 하나로
                .replaceAll("[^a-zA-Z0-9가-힣\\s]", ""); // 특수문자 제거
    }

    /**
     * 검색용 키워드 정규화 (더 관대한 정규화)
     */
    public String normalizeForSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return keyword;
        }

        return keyword.trim()
                .replaceAll("\\s+", " "); // 연속된 공백만 처리
    }
}