package kr.hhplus.be.server.infrastructure.external.dto;

import java.util.List;

public class SearchResponseDto {
    private final List<RestaurantDto> restaurants;
    private final long totalCount;
    private final String source;
    private final boolean hasNext;

    public SearchResponseDto(List<RestaurantDto> restaurants, long totalCount,
                             String source, boolean hasNext) {
        this.restaurants = restaurants;
        this.totalCount = totalCount;
        this.source = source;
        this.hasNext = hasNext;
    }

    // Getters
    public List<RestaurantDto> getRestaurants() { return restaurants; }
    public long getTotalCount() { return totalCount; }
    public String getSource() { return source; }
    public boolean isHasNext() { return hasNext; }
}