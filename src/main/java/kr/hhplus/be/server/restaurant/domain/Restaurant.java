package kr.hhplus.be.server.restaurant.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "restaurant")
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;

    private String address;

    private String phone;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    private String source;

    protected Restaurant() {}

    public Restaurant(String name, String category, String address, String phone,
                      BigDecimal rating, Integer reviewCount, String source) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.phone = phone;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.source = source;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public BigDecimal getRating() { return rating; }
    public Integer getReviewCount() { return reviewCount; }
    public String getSource() { return source; }
}
