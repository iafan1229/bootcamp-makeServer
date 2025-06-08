package kr.hhplus.be.server.keyword.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "keyword")
public class Keyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String keyword;

    @Column(name = "normalized_keyword")
    private String normalizedKeyword;

    protected Keyword() {}

    public Keyword(String keyword, String normalizedKeyword) {
        this.keyword = keyword;
        this.normalizedKeyword = normalizedKeyword;
    }

    // Getters
    public Long getId() { return id; }
    public String getKeyword() { return keyword; }
    public String getNormalizedKeyword() { return normalizedKeyword; }

    // Setter for tests
    public void setId(Long id) { this.id = id; }
}
