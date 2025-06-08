package kr.hhplus.be.server.restaurant.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_result")
public class SearchResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "search_request_id", nullable = false)
    private Long searchRequestId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "rank_order")
    private Integer rankOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected SearchResult() {}

    public SearchResult(Long searchRequestId, Long restaurantId, Integer rankOrder) {
        this.searchRequestId = searchRequestId;
        this.restaurantId = restaurantId;
        this.rankOrder = rankOrder;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public Long getSearchRequestId() { return searchRequestId; }
    public Long getRestaurantId() { return restaurantId; }
    public Integer getRankOrder() { return rankOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}