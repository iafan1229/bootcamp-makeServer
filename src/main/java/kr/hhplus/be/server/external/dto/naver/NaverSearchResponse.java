package kr.hhplus.be.server.external.dto.naver;
import java.util.List;

public class NaverSearchResponse {
    private String lastBuildDate;
    private int total;
    private int start;
    private int display;
    private List<NaverSearchItem> items;

    public NaverSearchResponse() {}

    public NaverSearchResponse(int total, int start, int display, List<NaverSearchItem> items) {
        this.total = total;
        this.start = start;
        this.display = display;
        this.items = items;
    }

    // Getters and Setters
    public String getLastBuildDate() { return lastBuildDate; }
    public void setLastBuildDate(String lastBuildDate) { this.lastBuildDate = lastBuildDate; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }

    public int getDisplay() { return display; }
    public void setDisplay(int display) { this.display = display; }

    public List<NaverSearchItem> getItems() { return items; }
    public void setItems(List<NaverSearchItem> items) { this.items = items; }
}