package kr.hhplus.be.server.keyword.controller;

import kr.hhplus.be.server.keyword.dto.request.PopularKeywordRequest;
import kr.hhplus.be.server.keyword.dto.response.PopularKeywordResponse;
import kr.hhplus.be.server.keyword.service.KeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/keywords")
@Validated
public class KeywordController {

    private final KeywordService keywordService;

    @Autowired
    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }

    @GetMapping("/popular")
    public ResponseEntity<PopularKeywordResponse> getPopularKeywords(
            @Valid @ModelAttribute PopularKeywordRequest request) {

        PopularKeywordResponse response = keywordService.getPopularKeywords(
                request.getCategory(), request.getLimit());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/popular")
    public ResponseEntity<PopularKeywordResponse> getPopularKeywordsPost(
            @Valid @RequestBody PopularKeywordRequest request) {

        PopularKeywordResponse response = keywordService.getPopularKeywords(
                request.getCategory(), request.getLimit());

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "인기 키워드 조회 중 오류가 발생했습니다.");
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