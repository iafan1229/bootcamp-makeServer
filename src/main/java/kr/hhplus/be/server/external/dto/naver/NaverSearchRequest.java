package kr.hhplus.be.server.external.dto.naver;

public class NaverSearchRequest {
    private String query;
    private String location;
    private int display = 10;
    private int start = 1;
    private String sort = "random"; // random, comment

    public NaverSearchRequest() {}

    public NaverSearchRequest(String query, String location) {
        this.query = query;
        this.location = location;
    }

    public NaverSearchRequest(String query, String location, int display, int start, String sort) {
        this.query = query;
        this.location = location;
        this.display = display;
        this.start = start;
        this.sort = sort;
    }

    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getDisplay() { return display; }
    public void setDisplay(int display) { this.display = display; }

    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}