package kr.hhplus.be.server.keyword.dto.response;

public class KeywordDto {
    private String keyword;
    private Integer count;

    public KeywordDto() {}

    public KeywordDto(String keyword, Integer count) {
        this.keyword = keyword;
        this.count = count;
    }

    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}