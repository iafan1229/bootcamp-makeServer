package kr.hhplus.be.server.restaurant.controller;


import kr.hhplus.be.server.restaurant.dto.request.RestaurantSearchRequest;
import kr.hhplus.be.server.restaurant.dto.response.RestaurantDto;
import kr.hhplus.be.server.restaurant.dto.response.RestaurantSearchResponse;
import kr.hhplus.be.server.restaurant.service.RestaurantSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@Validated
public class RestaurantController {

    private final RestaurantSearchService restaurantSearchService;

    @Autowired
    public RestaurantController(RestaurantSearchService restaurantSearchService) {
        this.restaurantSearchService = restaurantSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<RestaurantSearchResponse> searchRestaurants(
            @Valid @ModelAttribute RestaurantSearchRequest request,
            HttpServletRequest httpRequest) {

        // 세션 ID 설정
        String sessionId = httpRequest.getSession().getId();
        request.setSessionId(sessionId);

        RestaurantSearchResponse response = restaurantSearchService.searchRestaurants(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<RestaurantSearchResponse> searchRestaurantsPost(
            @Valid @RequestBody RestaurantSearchRequest request,
            HttpServletRequest httpRequest) {

        // 세션 ID 설정
        String sessionId = httpRequest.getSession().getId();
        request.setSessionId(sessionId);

        RestaurantSearchResponse response = restaurantSearchService.searchRestaurants(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RestaurantDto>> getRecentSearchResults(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest) {

        String sessionId = httpRequest.getSession().getId();
        List<RestaurantDto> recentResults = restaurantSearchService
                .getRecentSearchResults(sessionId, limit);

        return ResponseEntity.ok(recentResults);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다.");
        return ResponseEntity.internalServerError().body(error);
    }

    // 에러 응답 DTO
    public static class ErrorResponse {
        private String code;
        private String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}
