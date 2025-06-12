package kr.hhplus.be.server.infrastructure.keyword;

public class KeywordDto {
    private final String keyword;
    private final Long count;

    public KeywordDto(String keyword, Long count) {
        this.keyword = keyword;
        this.count = count;
    }

    public String getKeyword() { return keyword; }
    public Long getCount() { return count; }

    @Override
    public String toString() {
        return String.format("KeywordDto{keyword='%s', count=%d}", keyword, count);
    }
}
