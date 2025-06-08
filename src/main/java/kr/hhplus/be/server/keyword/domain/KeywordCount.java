package kr.hhplus.be.server.keyword.domain;


import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "keyword_count")
public class KeywordCount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    @Column(name = "location_category_id")
    private Long locationCategoryId;

    @Column(nullable = false)
    private Integer count;

    @Column(name = "count_date")
    private LocalDate countDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected KeywordCount() {}

    public KeywordCount(Long keywordId, Long locationCategoryId, Integer count) {
        this.keywordId = keywordId;
        this.locationCategoryId = locationCategoryId;
        this.count = count;
        this.countDate = LocalDate.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementCount() {
        this.count++;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateCount(Integer count) {
        this.count = count;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public Long getKeywordId() { return keywordId; }
    public Long getLocationCategoryId() { return locationCategoryId; }
    public Integer getCount() { return count; }
    public LocalDate getCountDate() { return countDate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}