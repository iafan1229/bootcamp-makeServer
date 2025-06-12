package kr.hhplus.be.server.infrastructure.external.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class NaverResponseDto {
    private String lastBuildDate;
    private int total;
    private int start;
    private int display;
    private List<Item> items;

    // Getters and Setters
    public String getLastBuildDate() { return lastBuildDate; }
    public void setLastBuildDate(String lastBuildDate) { this.lastBuildDate = lastBuildDate; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }

    public int getDisplay() { return display; }
    public void setDisplay(int display) { this.display = display; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {
        private String title;
        private String link;
        private String category;
        private String description;
        private String telephone;
        private String address;
        private String roadAddress;
        private int mapx;
        private int mapy;

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getRoadAddress() { return roadAddress; }
        public void setRoadAddress(String roadAddress) { this.roadAddress = roadAddress; }

        public int getMapx() { return mapx; }
        public void setMapx(int mapx) { this.mapx = mapx; }

        public int getMapy() { return mapy; }
        public void setMapy(int mapy) { this.mapy = mapy; }
    }
}