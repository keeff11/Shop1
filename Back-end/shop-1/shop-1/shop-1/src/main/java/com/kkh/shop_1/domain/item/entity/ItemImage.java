package com.kkh.shop_1.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "item_image")
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_image_id")
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean isMainImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /**
     *
     * 생성자 (Builder 패턴)
     *
     */
    @Builder
    private ItemImage(String imageUrl, int sortOrder, boolean isMainImage, Item item) {
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.isMainImage = isMainImage;
        this.item = item;
    }

    /**
     *
     * 상품(Item) 할당 및 양방향 연관관계 설정
     *
     */
    public void setItem(Item item) {
        this.item = item;
    }

    /**
     *
     * 대표 이미지 설정 변경
     *
     */
    public void changeToMain() {
        this.isMainImage = true;
    }

    public void changeToSub() {
        this.isMainImage = false;
    }

    /**
     *
     * 이미지 순서 변경
     *
     */
    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}