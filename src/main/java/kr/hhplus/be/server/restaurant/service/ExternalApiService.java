package kr.hhplus.be.server.restaurant.service;

import kr.hhplus.be.server.external.client.NaverApiClient;
import kr.hhplus.be.server.external.client.KakaoApiClient;
import kr.hhplus.be.server.external.dto.naver.NaverSearchResponse;
import kr.hhplus.be.server.external.dto.naver.NaverSearchItem;
import kr.hhplus.be.server.restaurant.search.restaurant.domain.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExternalApiService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiService.class);

    private final NaverApiClient naverApiClient;
    private final KakaoApiClient kakaoApiClient;

    @Autowired
    public ExternalApiService(NaverApiClient naverApiClient, KakaoApiClient kakaoApiClient) {
        this.naverApiClient = naverApiClient;
        this.kakaoApiClient = kakaoApiClient;
    }

    public List<Restaurant> searchRestaurants(String keyword, String location) {
        // 1. 네이버 API 우선 시도
        try {
            logger.info("네이버 API로 검색 시도: keyword={}, location={}", keyword, location);
            return searchFromNaver(keyword, location);
        } catch (Exception e) {
            logger.warn("네이버 API 호출 실패, 카카오 API로 failover: {}", e.getMessage());
        }

        // 2. 네이버 API 실패시 카카오 API로 failover
        try {
            logger.info("카카오 API로 검색 시도: keyword={}, location={}", keyword, location);
            return searchFromKakao(keyword, location);
        } catch (Exception e) {
            logger.error("카카오 API 호출도 실패: {}", e.getMessage());
        }

        // 3. 모든 API 실패시 예외 발생
        throw new RuntimeException("모든 외부 API 호출이 실패했습니다.");
    }

    private List<Restaurant> searchFromNaver(String keyword, String location) {
        NaverSearchResponse response = naverApiClient.searchLocal(keyword, location);

        return response.getItems().stream()
                .map(this::convertNaverItemToRestaurant)
                .collect(Collectors.toList());
    }

    private List<Restaurant> searchFromKakao(String keyword, String location) {
        // 카카오 API 호출 구현 (임시로 빈 리스트 반환)
        // 실제 구현시에는 KakaoApiClient를 사용하여 검색
        logger.info("카카오 API 검색 구현 필요");
        return List.of();
    }

    private Restaurant convertNaverItemToRestaurant(NaverSearchItem item) {
        // HTML 태그 제거 및 데이터 정리
        String name = removeHtmlTags(item.getTitle());
        String category = item.getCategory();
        String address = item.getAddress();
        String phone = item.getTelephone();

        // 네이버 API는 평점 정보를 제공하지 않으므로 기본값 설정
        BigDecimal rating = BigDecimal.ZERO;
        Integer reviewCount = 0;

        return new Restaurant(name, category, address, phone, rating, reviewCount, "naver");
    }

    private String removeHtmlTags(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("<[^>]*>", "").trim();
    }
}