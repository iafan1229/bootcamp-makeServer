package kr.hhplus.be.server.external.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KakaoApiClient {

    private final String apiKey;
    private final String baseUrl;

    public KakaoApiClient(@Value("${kakao.api.key}") String apiKey,
                          @Value("${kakao.api.base-url:https://dapi.kakao.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public Object searchKeyword(String keyword, String location) {
        // 카카오 API 구현 (향후 확장용)
        throw new UnsupportedOperationException("카카오 API는 향후 구현 예정입니다.");
    }
}