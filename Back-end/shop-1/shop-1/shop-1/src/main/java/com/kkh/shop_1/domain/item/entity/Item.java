package com.kkh.shop_1.domain.item.entity;

import com.kkh.shop_1.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    private Integer discountPrice;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemCategory itemCategory;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemStatus status; // SELLING, SOLD_OUT, HIDDEN, DELETED

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StockStatus stockStatus; // IN_STOCK, OUT_OF_STOCK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemImage> images = new ArrayList<>();

    private String thumbnailUrl;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int viewCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int totalRatingScore = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int reviewCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int salesCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public Item(Long id, String name, int price, int quantity, ItemCategory itemCategory, String description, User seller, ItemStatus status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.itemCategory = itemCategory;
        this.description = description;
        this.seller = seller;
        this.status = status != null ? status : ItemStatus.SELLING;
        this.stockStatus = (quantity > 0) ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK;
    }

    // --- 비즈니스 로직 메서드 ---

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("재고는 0 미만이 될 수 없습니다.");
        }
        this.quantity = quantity;
        this.stockStatus = (this.quantity == 0) ? StockStatus.OUT_OF_STOCK : StockStatus.IN_STOCK;
    }

    public void update(String name, int price, int quantity, ItemCategory category, String description) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.itemCategory = category;
        this.description = description;
        this.stockStatus = (quantity > 0) ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK;
    }

    public void addImage(ItemImage image) {
        this.images.add(image);
        if (image.getItem() != this) {
            image.setItem(this);
        }
    }

    public void clearImages() {
        this.images.clear();
    }

    public void setThumbnailUrl(String url) {
        this.thumbnailUrl = url;
    }

    public void addReviewRating(int newRating) {
        this.totalRatingScore += newRating;
        this.reviewCount++;
    }

    public void removeReviewRating(int oldRating) {
        if (this.reviewCount > 0) {
            this.totalRatingScore -= oldRating;
            this.reviewCount--;

            if (this.reviewCount <= 0 || this.totalRatingScore < 0) {
                this.totalRatingScore = 0;
                this.reviewCount = 0;
            }
        }
    }

    public double getAverageRating() {
        if (this.reviewCount == 0) return 0.0;
        return (double) this.totalRatingScore / this.reviewCount;
    }
}