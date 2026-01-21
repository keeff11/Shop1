package com.kkh.shop_1.domain.review.entity;

import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    Long Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    int rating;

    @Column(nullable = false, length = 1000)
    String content;


    @OneToMany(
            mappedBy = "review",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ReviewImage> images = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    private Review(Item item, User user, int rating, String content) {
        this.item = item;
        this.user = user;
        this.rating = rating;
        this.content = content;
    }

    public static Review createReview(Item item, User user, int rating, String content) {
        return Review.builder()
                .item(item)
                .user(user)
                .rating(rating)
                .content(content)
                .build();
    }

    public void updateReview(int rating, String content) {
        this.rating = rating;
        this.content = content;
    }

    public void addImage(ReviewImage image) {
        this.images.add(image);
        if (image.getReview() != this) {
            image.setReview(this);
        }
    }

    public void clearImages() {
        this.images.clear();
    }

}
