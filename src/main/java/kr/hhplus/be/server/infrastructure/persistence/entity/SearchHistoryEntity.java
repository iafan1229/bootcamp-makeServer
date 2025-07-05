package kr.hhplus.be.server.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_history", indexes = {
        @Index(name = "idx_keyword", columnList = "keyword"),
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_keyword_location", columnList = "keyword, location")
})
public class SearchHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(length = 100)
    private String location;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 45)
    private String userIp;

    @Column(length = 20)
    private String sortType;

    @Column
    private Integer resultCount;

    // 기본 생성자
    protected SearchHistoryEntity() {}

    public SearchHistoryEntity(String keyword, String location, String userIp,
                               String sortType, Integer resultCount) {
        this.keyword = keyword;
        this.location = location;
        this.userIp = userIp;
        this.sortType = sortType;
        this.resultCount = resultCount;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getKeyword() { return keyword; }
    public String getLocation() { return location; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getUserIp() { return userIp; }
    public String getSortType() { return sortType; }
    public Integer getResultCount() { return resultCount; }
}
