package com.kkh.shop_1.domain.review.service;

import com.kkh.shop_1.common.s3.S3Service;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.order.service.OrderService;
import com.kkh.shop_1.domain.review.dto.ReviewRequestDto;
import com.kkh.shop_1.domain.review.dto.ReviewResponseDto;
import com.kkh.shop_1.domain.review.entity.Review;
import com.kkh.shop_1.domain.review.entity.ReviewImage;
import com.kkh.shop_1.domain.review.repository.ReviewRepository;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final S3Service s3Service; // S3 서비스 주입

    /**
     * 리뷰 등록 및 상품 평점 반영
     */
    @Transactional
    public Long createReview(Long orderItemId, Long userId, ReviewRequestDto dto, List<MultipartFile> imageFiles) {

        // 1. 주문 상품 및 유저 검증
        OrderItem orderItem = orderService.findByOrderItemId(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("주문 내역이 존재하지 않습니다."));

        if (orderItem.isReviewWritten()) {
            throw new IllegalStateException("이미 리뷰를 작성한 상품입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        Item item = orderItem.getItem();

        // 2. 리뷰 엔티티 생성
        Review review = Review.createReview(item, user, dto.getRating(), dto.getContent());

        // 3. 이미지 S3 업로드 (경로: reviews/itemId/userId)
        if (imageFiles != null && !imageFiles.isEmpty()) {
            String folderPath = String.format("reviews/%d/%d", item.getId(), userId);

            for (MultipartFile file : imageFiles) {
                try {
                    // S3 업로드
                    String uploadedUrl = s3Service.uploadImage(folderPath, file);
                    // DB 저장
                    review.addImage(ReviewImage.createReviewImage(review, uploadedUrl));
                } catch (IOException e) {
                    log.error("S3 이미지 업로드 실패: {}", e.getMessage());
                    throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.");
                }
            }
        }

        // 4. 평점 반영 및 상태 변경
        item.addReviewRating(dto.getRating());
        orderItem.changeReviewStatus();

        return reviewRepository.save(review).getId();
    }

    /**
     * 리뷰 삭제 (S3 이미지 포함)
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

        // 작성자 본인 확인 (관리자는 패스하려면 로직 추가 필요)
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        // 1. S3에서 이미지 삭제
        // (리뷰에 포함된 모든 이미지를 순회하며 삭제)
        for (ReviewImage image : review.getImages()) {
            s3Service.deleteImageByUrl(image.getImageUrl());
        }

        // 2. 상품 평점 복구
        review.getItem().removeReviewRating(review.getRating());

        // 3. DB 삭제 (orphanRemoval=true 설정으로 인해 ReviewImage 데이터도 자동 삭제됨)
        reviewRepository.delete(review);
    }

    /**
     * 상품별 리뷰 목록 조회 (페이징)
     */
    public Page<ReviewResponseDto> getReviewsByItem(Long itemId, Pageable pageable, Long currentUserId) {
        return reviewRepository.findAllByItemId(itemId, pageable)
                .map(review -> ReviewResponseDto.of(review, currentUserId));
    }
}