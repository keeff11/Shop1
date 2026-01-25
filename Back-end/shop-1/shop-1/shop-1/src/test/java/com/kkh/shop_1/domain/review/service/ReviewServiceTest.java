package com.kkh.shop_1.domain.review.service;

import com.kkh.shop_1.common.s3.S3Service;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.order.service.OrderService;
import com.kkh.shop_1.domain.review.dto.ReviewRequestDto;
import com.kkh.shop_1.domain.review.entity.Review;
import com.kkh.shop_1.domain.review.repository.ReviewRepository;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private S3Service s3Service;

    @Test
    @DisplayName("리뷰 등록 성공 - Happy Path")
    void createReview_Success() {
        // given
        Long orderItemId = 1L;
        Long userId = 1L;
        ReviewRequestDto dto = new ReviewRequestDto(5, "좋아요");
        List<MultipartFile> images = Collections.emptyList();

        User user = mock(User.class);
        Item item = mock(Item.class);
        OrderItem orderItem = mock(OrderItem.class);
        Review review = mock(Review.class);

        given(orderService.findByOrderItemId(orderItemId)).willReturn(Optional.of(orderItem));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderItem.getItem()).willReturn(item);
        given(reviewRepository.save(any(Review.class))).willReturn(review);
        given(review.getId()).willReturn(100L);

        // when
        Long reviewId = reviewService.createReview(orderItemId, userId, dto, images);

        // then
        assertThat(reviewId).isEqualTo(100L);
        verify(item).addReviewRating(5);
    }

    @Test
    @DisplayName("리뷰 등록 실패 - 주문 내역 없음")
    void createReview_Fail_NoOrderItem() {
        // given
        Long orderItemId = 999L;
        Long userId = 1L;
        ReviewRequestDto dto = new ReviewRequestDto(5, "좋아요");
        List<MultipartFile> images = Collections.emptyList();

        given(orderService.findByOrderItemId(orderItemId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderItemId, userId, dto, images))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문 내역이 존재하지 않습니다.");
    }

    @Test
    @DisplayName("리뷰 등록 실패 - 사용자 없음")
    void createReview_Fail_NoUser() {
        // given
        Long orderItemId = 1L;
        Long userId = 999L;
        ReviewRequestDto dto = new ReviewRequestDto(5, "좋아요");
        List<MultipartFile> images = Collections.emptyList();
        OrderItem orderItem = mock(OrderItem.class);

        given(orderService.findByOrderItemId(orderItemId)).willReturn(Optional.of(orderItem));
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderItemId, userId, dto, images))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("리뷰 삭제 성공 - Happy Path")
    void deleteReview_Success() {
        // given
        Long reviewId = 100L;
        Long userId = 1L;

        Review review = mock(Review.class);
        User user = mock(User.class);
        Item item = mock(Item.class);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(review.getUser()).willReturn(user);
        given(user.getId()).willReturn(userId);
        given(review.getItem()).willReturn(item);
        given(review.getRating()).willReturn(5);

        // when
        reviewService.deleteReview(reviewId, userId);

        // then
        verify(item).removeReviewRating(5);
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 리뷰 없음")
    void deleteReview_Fail_NoReview() {
        // given
        Long reviewId = 999L;
        Long userId = 1L;

        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리뷰가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 권한 없음")
    void deleteReview_Fail_NoPermission() {
        // given
        Long reviewId = 100L;
        Long userId = 1L;
        Long otherUserId = 2L;

        Review review = mock(Review.class);
        User user = mock(User.class);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(review.getUser()).willReturn(user);
        given(user.getId()).willReturn(otherUserId); // 다른 사용자 ID

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("삭제 권한이 없습니다.");
    }
}
