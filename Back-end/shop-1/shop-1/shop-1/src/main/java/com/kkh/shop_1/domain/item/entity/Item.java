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

    // 통계 필드
    @Column(nullable = false)
    @ColumnDefault("0")
    private int viewCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int reviewCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0.0")
    private double averageRating = 0.0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int salesCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // [수정] Builder에 status 포함 및 기본값 설정 로직 추가
    @Builder
    public Item(Long id, String name, int price, int quantity, ItemCategory itemCategory, String description, User seller, ItemStatus status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.itemCategory = itemCategory;
        this.description = description;
        this.seller = seller;
        // 빌더로 들어온 status가 없으면 SELLING으로 초기화
        this.status = status != null ? status : ItemStatus.SELLING;
        this.stockStatus = (quantity > 0) ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK;
    }

    // --- 비즈니스 로직 메서드 ---

    // [추가] 상태 변경 메서드 (Setter 대신 비즈니스 메서드로 사용 권장)
    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    // [추가] 수량 변경 메서드 (재고 0이면 품절 처리 포함)
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        if (this.quantity <= 0) {
            this.stockStatus = StockStatus.OUT_OF_STOCK;
        } else {
            this.stockStatus = StockStatus.IN_STOCK;
        }
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

    public void updateStock(int newQuantity) {
        setQuantity(newQuantity);
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void addReviewRating(int newRating) {
        double totalScore = this.averageRating * this.reviewCount;
        this.reviewCount++;
        this.averageRating = (totalScore + newRating) / this.reviewCount;
    }

    public void removeReviewRating(int oldRating) {
        if (this.reviewCount <= 1) {
            this.reviewCount = 0;
            this.averageRating = 0.0;
        } else {
            double totalScore = this.averageRating * this.reviewCount;
            this.reviewCount--;
            this.averageRating = (totalScore - oldRating) / this.reviewCount;
        }
    }
}