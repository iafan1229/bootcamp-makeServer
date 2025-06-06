package kr.hhplus.be.server.keyword.dto.response;


import java.util.List;

public class PopularKeywordResponse {
    private List<KeywordDto> keywords;
    private String source; // "redis" or "database"
    private int totalCount;

    public PopularKeywordResponse() {}

    public PopularKeywordResponse(List<KeywordDto> keywords, String source) {
        this.keywords = keywords;
        this.source = source;
        this.totalCount = keywords != null ? keywords.size() : 0;
    }

    // Getters and Setters
    public List<KeywordDto> getKeywords() { return keywords; }
    public void setKeywords(List<KeywordDto> keywords) {
        this.keywords = keywords;
        this.totalCount = keywords != null ? keywords.size() : 0;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
}