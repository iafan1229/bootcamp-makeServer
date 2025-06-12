package kr.hhplus.be.server.infrastructure.external.naver;


import kr.hhplus.be.server.infrastructure.external.api.SearchApiClient;
import kr.hhplus.be.server.infrastructure.external.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class NaverApiClientImpl implements SearchApiClient {

    private static final String NAVER_LOCAL_SEARCH_URL = "https://openapi.naver.com/v1/search/local.json";

    @Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public NaverApiClientImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "naver";
    }

    @Override
    public boolean isAvailable() {
        return clientId != null && !clientId.isEmpty() &&
                clientSecret != null && !clientSecret.isEmpty();
    }

    @Override
    public SearchResponseDto search(SearchRequestDto request) {
        try {
            String url = buildSearchUrl(request);
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            NaverResponseDto naverResponse = objectMapper.readValue(
                    response.getBody(), NaverResponseDto.class);

            return convertToCommonResponse(naverResponse);

        } catch (Exception e) {
            throw new RuntimeException("Naver API 호출 실패", e);
        }
    }

    @Override
    public int getPriority() {
        return 1; // 높은 우선순위
    }

    private String buildSearchUrl(SearchRequestDto request) {
        String query = request.getKeyword();
        if (request.getLocation() != null && !request.getLocation().isEmpty()) {
            query += " " + request.getLocation();
        }

        return UriComponentsBuilder.fromHttpUrl(NAVER_LOCAL_SEARCH_URL)
                .queryParam("query", query)
                .queryParam("display", request.getSize())
                .queryParam("start", (request.getPage() - 1) * request.getSize() + 1)
                .queryParam("sort", convertSortType(request.getSort()))
                .build()
                .toUriString();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String convertSortType(String sort) {
        if ("review_count".equals(sort)) {
            return "comment"; // 네이버는 comment로 리뷰 수 정렬
        }
        return "random"; // 기본값
    }

    private SearchResponseDto convertToCommonResponse(NaverResponseDto naverResponse) {
        List<RestaurantDto> restaurants = naverResponse.getItems().stream()
                .map(this::convertToRestaurantInfo)
                .collect(Collectors.toList());

        return new SearchResponseDto(
                restaurants,
                naverResponse.getTotal(),
                "naver",
                naverResponse.getItems().size() == naverResponse.getDisplay()
        );
    }

    private RestaurantDto convertToRestaurantInfo(NaverResponseDto.Item item) {
        return new RestaurantDto(
                removeHtmlTags(item.getTitle()),
                item.getCategory(),
                removeHtmlTags(item.getAddress()),
                item.getTelephone(),
                null, // 네이버 API는 평점 정보 제공하지 않음
                null, // 네이버 API는 리뷰 수 정보 제공하지 않음
                item.getLink()
        );
    }

    private String removeHtmlTags(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]*>", "");
    }
}
