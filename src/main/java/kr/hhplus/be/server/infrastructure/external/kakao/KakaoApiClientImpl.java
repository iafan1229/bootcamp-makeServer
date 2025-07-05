package kr.hhplus.be.server.infrastructure.external.kakao;


import kr.hhplus.be.server.infrastructure.external.api.SearchApiClient;
import kr.hhplus.be.server.infrastructure.external.dto.SearchRequestDto;
import kr.hhplus.be.server.infrastructure.external.dto.SearchResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KakaoApiClientImpl implements SearchApiClient {

    @Value("${kakao.rest.api.key:}")
    private String apiKey;

    @Override
    public String getProviderName() {
        return "kakao";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public SearchResponseDto search(SearchRequestDto request) {
        // Kakao Local API 구현
        // 실제 구현 시 Kakao API 명세에 따라 구현
        throw new UnsupportedOperationException("Kakao API 구현 중");
    }

    @Override
    public int getPriority() {
        return 2; // 낮은 우선순위
    }
}
