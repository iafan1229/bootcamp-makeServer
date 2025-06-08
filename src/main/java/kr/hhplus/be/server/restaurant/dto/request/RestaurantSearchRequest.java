package kr.hhplus.be.server.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class RestaurantSearchRequest {

    @NotBlank(message = "검색 키워드는 필수입니다.")
    private String keyword;

    private String location;

    private String sort = "accuracy"; // accuracy, review_count

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private Integer size = 10;

    private String sessionId;

    public RestaurantSearchRequest() {}

    public RestaurantSearchRequest(String keyword, String location, String sort,
                                   Integer page, Integer size, String sessionId) {
        this.keyword = keyword;
        this.location = location;
        this.sort = sort;
        this.page = page;
        this.size = size;
        this.sessionId = sessionId;
    }

    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}