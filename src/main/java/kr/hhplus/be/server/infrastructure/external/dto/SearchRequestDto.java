package kr.hhplus.be.server.infrastructure.external.dto;

public class SearchRequestDto {
    private final String keyword;
    private final String location;
    private final String sort;
    private final int page;
    private final int size;

    public SearchRequestDto(String keyword, String location, String sort, int page, int size) {
        this.keyword = keyword;
        this.location = location;
        this.sort = sort;
        this.page = page;
        this.size = size;
    }

    // Getters
    public String getKeyword() { return keyword; }
    public String getLocation() { return location; }
    public String getSort() { return sort; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}