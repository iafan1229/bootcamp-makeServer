package kr.hhplus.be.server.infrastructure.external.api;

import kr.hhplus.be.server.infrastructure.external.dto.SearchRequestDto;
import kr.hhplus.be.server.infrastructure.external.dto.SearchResponseDto;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Comparator;

@Component
public class SearchApiManager {

    private final List<SearchApiClient> apiClients;

    public SearchApiManager(List<SearchApiClient> apiClients) {
        // 우선순위 순으로 정렬
        this.apiClients = apiClients.stream()
                .sorted(Comparator.comparing(SearchApiClient::getPriority))
                .toList();
    }

    public SearchResponseDto search(SearchRequestDto request) {
        Exception lastException = null;

        for (SearchApiClient client : apiClients) {
            if (!client.isAvailable()) {
                continue;
            }

            try {
                return client.search(request);
            } catch (Exception e) {
                lastException = e;
                // 로그 기록
                System.err.println("API 호출 실패: " + client.getProviderName() + ", 에러: " + e.getMessage());
            }
        }

        throw new RuntimeException("모든 외부 API 호출 실패", lastException);
    }

    public List<String> getAvailableProviders() {
        return apiClients.stream()
                .filter(SearchApiClient::isAvailable)
                .map(SearchApiClient::getProviderName)
                .toList();
    }
}