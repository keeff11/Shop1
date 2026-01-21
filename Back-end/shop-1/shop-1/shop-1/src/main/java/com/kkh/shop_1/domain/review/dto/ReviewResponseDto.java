package com.kkh.shop_1.domain.review.dto;

import com.kkh.shop_1.domain.review.entity.Review;
import com.kkh.shop_1.domain.review.entity.ReviewImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReviewResponseDto {
    private Long reviewId;
    private String nickname;     // 작성자 닉네임
    private int rating;
    private String content;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private boolean isOwner;     // 현재 로그인한 사용자가 작성자인지 여부

    public static ReviewResponseDto of(Review review, Long currentUserId) {
        return ReviewResponseDto.builder()
                .reviewId(review.getId())
                .nickname(review.getUser().getNickname())
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrls(review.getImages().stream()
                        .map(ReviewImage::getImageUrl)
                        .toList())
                .createdAt(review.getCreatedAt())
                .isOwner(review.getUser().getId().equals(currentUserId))
                .build();
    }
}