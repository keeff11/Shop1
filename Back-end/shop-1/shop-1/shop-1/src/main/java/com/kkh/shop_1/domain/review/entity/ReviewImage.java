package com.kkh.shop_1.domain.review.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int orderNum;

    @Builder
    public ReviewImage(Review review, String imageUrl, int orderNum) {
        this.review = review;
        this.imageUrl = imageUrl;
        this.orderNum = orderNum;
    }

    public static ReviewImage createReviewImage(Review review, String url) {
        return ReviewImage.builder()
                .review(review)
                .imageUrl(url)
                .orderNum(0)
                .build();
    }

    public void setReview(Review review) {
        this.review = review;
    }
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public void updateOrderNum(int orderNum) {
        this.orderNum = orderNum;
    }
}
