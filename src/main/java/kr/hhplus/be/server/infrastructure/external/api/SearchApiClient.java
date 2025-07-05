package kr.hhplus.be.server.infrastructure.external.api;

import kr.hhplus.be.server.infrastructure.external.dto.SearchRequestDto;
import kr.hhplus.be.server.infrastructure.external.dto.SearchResponseDto;

public interface SearchApiClient {
    String getProviderName();
    boolean isAvailable();
    SearchResponseDto search(SearchRequestDto request);
    int getPriority(); // 우선순위 (낮을수록 우선)
}