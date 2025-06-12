package kr.hhplus.be.server.infrastructure.external.dto;

public class RestaurantDto {
    private final String name;
    private final String category;
    private final String address;
    private final String phone;
    private final Double rating;
    private final Integer reviewCount;
    private final String url;

    public RestaurantDto(String name, String category, String address, String phone,
                         Double rating, Integer reviewCount, String url) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.phone = phone;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.url = url;
    }

    // Getters
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public Double getRating() { return rating; }
    public Integer getReviewCount() { return reviewCount; }
    public String getUrl() { return url; }
}