package kr.hhplus.be.server.restaurant.dto.response;

import java.util.List;

public class RestaurantSearchResponse {
    private List<RestaurantDto> restaurants;
    private int totalCount;
    private int currentPage;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public RestaurantSearchResponse() {}

    public RestaurantSearchResponse(List<RestaurantDto> restaurants, int totalCount,
                                    int currentPage, int pageSize) {
        this.restaurants = restaurants;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.totalPages = (int) Math.ceil((double) totalCount / pageSize);
        this.hasNext = currentPage < totalPages;
        this.hasPrevious = currentPage > 1;
    }

    // Getters and Setters
    public List<RestaurantDto> getRestaurants() { return restaurants; }
    public void setRestaurants(List<RestaurantDto> restaurants) { this.restaurants = restaurants; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
}