package kr.hhplus.be.server.restaurant.service;

import kr.hhplus.be.server.restaurant.domain.Restaurant;
import kr.hhplus.be.server.restaurant.domain.SearchRequest;
import kr.hhplus.be.server.restaurant.domain.SearchResult;
import kr.hhplus.be.server.restaurant.repository.RestaurantRepository;
import kr.hhplus.be.server.restaurant.repository.SearchRequestRepository;
import kr.hhplus.be.server.restaurant.repository.SearchResultRepository;
import kr.hhplus.be.server.restaurant.dto.request.RestaurantSearchRequest;
import kr.hhplus.be.server.restaurant.dto.response.RestaurantDto;
import kr.hhplus.be.server.restaurant.dto.response.RestaurantSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RestaurantSearchService {

    private final RestaurantRepository restaurantRepository;
    private final SearchRequestRepository searchRequestRepository;
    private final SearchResultRepository searchResultRepository;
    private final ExternalApiService externalApiService;

    @Autowired
    public RestaurantSearchService(RestaurantRepository restaurantRepository,
                                   SearchRequestRepository searchRequestRepository,
                                   SearchResultRepository searchResultRepository,
                                   ExternalApiService externalApiService) {
        this.restaurantRepository = restaurantRepository;
        this.searchRequestRepository = searchRequestRepository;
        this.searchResultRepository = searchResultRepository;
        this.externalApiService = externalApiService;
    }

    public RestaurantSearchResponse searchRestaurants(RestaurantSearchRequest request) {
        validateSearchRequest(request);

        // 1. 검색 요청 저장
        SearchRequest searchRequest = saveSearchRequest(request);

        // 2. DB에서 검색
        List<Restaurant> restaurants = searchFromDatabase(request);

        // 3. DB에 결과가 없으면 외부 API 호출
        if (restaurants.isEmpty()) {
            restaurants = searchFromExternalApi(request);
            if (!restaurants.isEmpty()) {
                restaurants = restaurantRepository.saveAll(restaurants);
            }
        }

        // 4. 검색 결과 저장
        saveSearchResults(searchRequest.getId(), restaurants);

        // 5. 페이징 처리 및 응답 생성
        return createPaginatedResponse(restaurants, request);
    }

    @Transactional(readOnly = true)
    public List<RestaurantDto> getRecentSearchResults(String sessionId, int limit) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("세션 ID는 필수입니다.");
        }

        List<SearchRequest> recentRequests = searchRequestRepository
                .findBySessionIdOrderByCreatedAtDesc(sessionId);

        if (recentRequests.isEmpty()) {
            return List.of();
        }

        SearchRequest latestRequest = recentRequests.get(0);
        List<SearchResult> searchResults = searchResultRepository
                .findBySearchRequestIdOrderByRankOrder(latestRequest.getId());

        return searchResults.stream()
                .limit(limit)
                .map(result -> {
                    Restaurant restaurant = restaurantRepository.findById(result.getRestaurantId())
                            .orElseThrow(() -> new RuntimeException("맛집을 찾을 수 없습니다."));
                    return new RestaurantDto(restaurant);
                })
                .collect(Collectors.toList());
    }

    private void validateSearchRequest(RestaurantSearchRequest request) {
        if (!StringUtils.hasText(request.getKeyword())) {
            throw new IllegalArgumentException("검색 키워드는 필수입니다.");
        }
        if (request.getPage() != null && request.getPage() < 1) {
            throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
        }
        if (request.getSize() != null && (request.getSize() < 1 || request.getSize() > 100)) {
            throw new IllegalArgumentException("페이지 크기는 1-100 사이여야 합니다.");
        }
    }

    private SearchRequest saveSearchRequest(RestaurantSearchRequest request) {
        SearchRequest searchRequest = new SearchRequest(
                request.getKeyword().trim(),
                request.getLocation(),
                request.getSessionId()
        );
        return searchRequestRepository.save(searchRequest);
    }

    private List<Restaurant> searchFromDatabase(RestaurantSearchRequest request) {
        String keyword = request.getKeyword().trim();
        String location = request.getLocation();
        String sort = request.getSort();

        if ("review_count".equals(sort)) {
            return restaurantRepository.searchByKeywordAndLocationOrderByReviewCount(keyword, location);
        } else {
            return restaurantRepository.searchByKeywordAndLocation(keyword, location);
        }
    }

    private List<Restaurant> searchFromExternalApi(RestaurantSearchRequest request) {
        try {
            return externalApiService.searchRestaurants(
                    request.getKeyword().trim(),
                    request.getLocation()
            );
        } catch (Exception e) {
            // 외부 API 실패 시 빈 리스트 반환 (서비스 연속성 보장)
            return List.of();
        }
    }

    private void saveSearchResults(Long searchRequestId, List<Restaurant> restaurants) {
        for (int i = 0; i < restaurants.size(); i++) {
            Restaurant restaurant = restaurants.get(i);
            SearchResult searchResult = new SearchResult(
                    searchRequestId,
                    restaurant.getId(),
                    i + 1 // 순위는 1부터 시작
            );
            searchResultRepository.save(searchResult);
        }
    }

    private RestaurantSearchResponse createPaginatedResponse(
            List<Restaurant> allRestaurants, RestaurantSearchRequest request) {

        int page = request.getPage() != null ? request.getPage() : 1;
        int size = request.getSize() != null ? request.getSize() : 10;
        int totalCount = allRestaurants.size();

        // 페이징 처리
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalCount);

        List<RestaurantDto> pagedRestaurants;
        if (startIndex >= totalCount) {
            pagedRestaurants = List.of();
        } else {
            pagedRestaurants = allRestaurants.subList(startIndex, endIndex)
                    .stream()
                    .map(RestaurantDto::new)
                    .collect(Collectors.toList());
        }

        return new RestaurantSearchResponse(pagedRestaurants, totalCount, page, size);
    }
}