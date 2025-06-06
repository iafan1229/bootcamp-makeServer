package kr.hhplus.be.server.restaurant.dto.response;
import kr.hhplus.be.server.restaurant.domain.Restaurant;

import java.math.BigDecimal;

public class RestaurantDto {
    private Long id;
    private String name;
    private String category;
    private String address;
    private String phone;
    private BigDecimal rating;
    private Integer reviewCount;
    private String source;

    public RestaurantDto() {}

    public RestaurantDto(Restaurant restaurant) {
        this.id = restaurant.getId();
        this.name = restaurant.getName();
        this.category = restaurant.getCategory();
        this.address = restaurant.getAddress();
        this.phone = restaurant.getPhone();
        this.rating = restaurant.getRating();
        this.reviewCount = restaurant.getReviewCount();
        this.source = restaurant.getSource();
    }

    public RestaurantDto(Long id, String name, String category, String address,
                         String phone, BigDecimal rating, Integer reviewCount, String source) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.address = address;
        this.phone = phone;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.source = source;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}