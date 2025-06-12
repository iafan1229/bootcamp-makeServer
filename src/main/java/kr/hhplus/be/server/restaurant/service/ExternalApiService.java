package kr.hhplus.be.server.restaurant.service;

import kr.hhplus.be.server.infrastructure.external.naver.NaverApiClientImpl;
import kr.hhplus.be.server.infrastructure.external.kakao.KakaoApiClientImpl;
import kr.hhplus.be.server.restaurant.domain.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@Service
public class ExternalApiService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiService.class);

    private final NaverApiClientImpl naverApiClient;
    private final KakaoApiClientImpl kakaoApiClient;

    @Autowired
    public ExternalApiService(NaverApiClientImpl naverApiClient, KakaoApiClientImpl kakaoApiClient) {
        this.naverApiClient = naverApiClient;
        this.kakaoApiClient = kakaoApiClient;
    }

    /**
     * 맛집 검색 - Infrastructure Layer의 NaverApiClientImpl 사용
     */
    public List<Restaurant> searchRestaurants(String keyword, String location) {
        return searchRestaurants(keyword, location, "accuracy", 1, 10);
    }

    /**
     * 맛집 검색 - 페이징 및 정렬 옵션 포함
     * 우선순위: Naver API → Kakao API failover
     */
    public List<Restaurant> searchRestaurants(String keyword, String location,
                                              String sort, int page, int size) {
        validateSearchParameters(keyword, page, size);

        // 1. 네이버 API 우선 시도 (높은 우선순위)
        try {
            logger.info("네이버 API로 검색 시도: keyword={}, location={}, sort={}, page={}, size={}",
                    keyword, location, sort, page, size);

            if (naverApiClient.isAvailable()) {
                // SearchRequestDto 생성해서 호출해야 하지만, 아직 구현되지 않았으므로
                // 임시로 빈 리스트 반환
                logger.info("NaverApiClientImpl 사용 가능하지만 SearchRequestDto/SearchResponseDto가 미구현");
                // return searchWithNaver(keyword, location, sort, page, size);
            } else {
                logger.warn("네이버 API 사용 불가능");
            }

        } catch (Exception e) {
            logger.error("네이버 API 검색 실패, 카카오 API로 failover: keyword={}, location={}, error={}",
                    keyword, location, e.getMessage());
        }

        // 2. 카카오 API로 failover (낮은 우선순위)
        try {
            logger.info("카카오 API로 검색 시도: keyword={}, location={}, sort={}, page={}, size={}",
                    keyword, location, sort, page, size);

            if (kakaoApiClient.isAvailable()) {
                // SearchRequestDto 생성해서 호출해야 하지만, 아직 구현되지 않았으므로
                // 임시로 빈 리스트 반환
                logger.info("KakaoApiClientImpl 사용 가능하지만 SearchRequestDto/SearchResponseDto가 미구현");
                // return searchWithKakao(keyword, location, sort, page, size);
            } else {
                logger.warn("카카오 API 사용 불가능");
            }

        } catch (Exception e) {
            logger.error("카카오 API 검색도 실패: keyword={}, location={}, error={}",
                    keyword, location, e.getMessage());
        }

        // 3. 모든 API 실패시 빈 리스트 반환 (서비스 연속성 보장)
        logger.warn("모든 외부 API 호출 실패. 빈 결과 반환: keyword={}, location={}", keyword, location);
        return new ArrayList<>();
    }

    /**
     * 네이버 API 상태 확인
     */
    public boolean isNaverApiAvailable() {
        return naverApiClient.isAvailable();
    }

    /**
     * 카카오 API 상태 확인
     */
    public boolean isKakaoApiAvailable() {
        return kakaoApiClient.isAvailable();
    }

    /**
     * 네이버 API 제공자명 확인
     */
    public String getNaverProviderName() {
        return naverApiClient.getProviderName();
    }

    /**
     * 카카오 API 제공자명 확인
     */
    public String getKakaoProviderName() {
        return kakaoApiClient.getProviderName();
    }

    /**
     * API 우선순위 확인 (낮은 숫자가 높은 우선순위)
     */
    public int getNaverPriority() {
        return naverApiClient.getPriority();
    }

    public int getKakaoPriority() {
        return kakaoApiClient.getPriority();
    }

    /**
     * 사용 가능한 API 제공자 목록 (우선순위순 정렬)
     */
    public List<String> getAvailableProviders() {
        List<String> providers = new ArrayList<>();

        // 우선순위순으로 추가 (낮은 숫자가 높은 우선순위)
        if (isNaverApiAvailable() && isKakaoApiAvailable()) {
            if (getNaverPriority() <= getKakaoPriority()) {
                providers.add(naverApiClient.getProviderName());
                providers.add(kakaoApiClient.getProviderName());
            } else {
                providers.add(kakaoApiClient.getProviderName());
                providers.add(naverApiClient.getProviderName());
            }
        } else if (isNaverApiAvailable()) {
            providers.add(naverApiClient.getProviderName());
        } else if (isKakaoApiAvailable()) {
            providers.add(kakaoApiClient.getProviderName());
        }

        return providers;
    }

    /**
     * 검색 파라미터 유효성 검사
     */
    private void validateSearchParameters(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 키워드는 필수입니다.");
        }
        if (page < 1) {
            throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1-100 사이여야 합니다.");
        }
    }

    // TODO: Infrastructure Layer 완성시 아래 메서드들 구현
    /*
    private SearchRequestDto createSearchRequest(String keyword, String location,
                                               String sort, int page, int size) {
        SearchRequestDto request = new SearchRequestDto();
        request.setKeyword(keyword.trim());
        request.setLocation(location);
        request.setSort(sort);
        request.setPage(page);
        request.setSize(size);
        return request;
    }

    private List<Restaurant> convertToRestaurants(SearchResponseDto response) {
        if (response == null || response.getRestaurants() == null) {
            return List.of();
        }

        return response.getRestaurants().stream()
                .map(this::convertToRestaurant)
                .collect(Collectors.toList());
    }

    private Restaurant convertToRestaurant(RestaurantDto dto) {
        BigDecimal rating = dto.getRating() != null ? dto.getRating() : BigDecimal.ZERO;
        Integer reviewCount = dto.getReviewCount() != null ? dto.getReviewCount() : 0;

        return new Restaurant(
                cleanText(dto.getName()),
                cleanText(dto.getCategory()),
                cleanText(dto.getAddress()),
                cleanText(dto.getPhone()),
                rating,
                reviewCount,
                dto.getSource() != null ? dto.getSource() : "unknown"
        );
    }

    private String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.replaceAll("<[^>]*>", "").trim();
    }
    */
}