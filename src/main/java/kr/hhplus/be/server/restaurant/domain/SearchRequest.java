package kr.hhplus.be.server.restaurant.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_request")
public class SearchRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String keyword;

    private String location;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected SearchRequest() {}

    public SearchRequest(String keyword, String location, String sessionId) {
        this.keyword = keyword;
        this.location = location;
        this.sessionId = sessionId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getKeyword() { return keyword; }
    public String getLocation() { return location; }
    public String getSessionId() { return sessionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
