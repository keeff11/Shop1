package com.kkh.shop_1.domain.item.entity;

import com.kkh.shop_1.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private StockStatus stockStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ItemCategory itemCategory;

    @Column(length = 2000)
    private String description;

    private String thumbnailUrl;

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ItemImage> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ItemStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public Item(String name, int price, int quantity, ItemCategory itemCategory,
                String description, User seller, ItemStatus status, StockStatus stockStatus) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.itemCategory = itemCategory;
        this.description = description;
        this.seller = seller;
        this.status = status != null ? status : ItemStatus.SELLING;
        this.stockStatus = stockStatus != null ? stockStatus :
                (quantity > 0 ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK);
    }

    /**
     *
     * 이미지 추가
     *
     */
    public void addImage(ItemImage image) {
        this.images.add(image);
        if (image.getItem() != this) {
            image.setItem(this);
        }
    }

    /**
     *
     * 대표 이미지(썸네일) 설정
     *
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     *
     * 재고 수정 및 상태 자동 변경
     *
     */
    public void updateStock(int quantity) {
        this.quantity = quantity;
        if (this.quantity <= 0) {
            this.stockStatus = StockStatus.OUT_OF_STOCK;
        } else if (this.quantity < 10) {
            this.stockStatus = StockStatus.LIMITED;
        } else {
            this.stockStatus = StockStatus.IN_STOCK;
        }
    }

}