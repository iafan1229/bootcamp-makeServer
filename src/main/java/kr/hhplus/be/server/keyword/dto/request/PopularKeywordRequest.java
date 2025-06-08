package kr.hhplus.be.server.keyword.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class PopularKeywordRequest {

    private String category; // 지역 카테고리 (선택사항)

    @Min(value = 1, message = "조회할 키워드 개수는 1 이상이어야 합니다.")
    @Max(value = 50, message = "조회할 키워드 개수는 50 이하여야 합니다.")
    private Integer limit = 10;

    public PopularKeywordRequest() {}

    public PopularKeywordRequest(String category, Integer limit) {
        this.category = category;
        this.limit = limit;
    }

    // Getters and Setters
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
