package kr.hhplus.be.server.external.client;


import kr.hhplus.be.server.external.dto.naver.NaverSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NaverApiClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String baseUrl;

    public NaverApiClient(RestTemplate restTemplate,
                          @Value("${naver.api.client-id}") String clientId,
                          @Value("${naver.api.client-secret}") String clientSecret,
                          @Value("${naver.api.base-url:https://openapi.naver.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.baseUrl = baseUrl;
    }

    public NaverSearchResponse searchLocal(String keyword, String location) {
        String query = buildSearchQuery(keyword, location);
        String url = buildUrl(query);

        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<NaverSearchResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, NaverSearchResponse.class
        );

        return response.getBody();
    }

    private String buildSearchQuery(String keyword, String location) {
        if (location != null && !location.trim().isEmpty()) {
            return location + " " + keyword;
        }
        return keyword;
    }

    private String buildUrl(String query) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v1/search/local.json")
                .queryParam("query", query)
                .queryParam("display", 10)
                .queryParam("start", 1)
                .queryParam("sort", "random")
                .build()
                .toUriString();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        return headers;
    }
}