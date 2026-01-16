package com.kkh.shop_1.domain.cart.entity;

import com.kkh.shop_1.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cart_item")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long id;

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     *
     * 내부 빌더용 생성자 (외부 접근 제한)
     *
     */
    @Builder(access = AccessLevel.PRIVATE)
    private CartItem(User user, Long itemId, int quantity) {
        this.user = user;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    /**
     *
     * 새로운 장바구니 아이템 생성
     *
     */
    public static CartItem createCartItem(User user, Long itemId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("장바구니 담기 시 초기 수량은 1개 이상이어야 합니다.");
        }
        return CartItem.builder()
                .user(user)
                .itemId(itemId)
                .quantity(quantity)
                .build();
    }

    // ======= 비즈니스 메서드 =======

    /**
     *
     * 기존 수량에 특정 양만큼 추가
     *
     */
    public void increaseQuantity(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("증가시킬 수량은 1개 이상이어야 합니다.");
        }
        this.quantity += amount;
    }

    /**
     *
     * 수량 1개 감소 (최소 수량 1개 유지)
     *
     */
    public void decreaseQuantity() {
        if (this.quantity <= 1) {
            throw new IllegalStateException("수량이 1개인 경우 더 이상 줄일 수 없습니다.");
        }
        this.quantity -= 1;
    }

    /**
     *
     * 특정 수량으로 직접 변경
     *
     */
    public void updateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 최소 1개 이상이어야 합니다.");
        }
        this.quantity = quantity;
    }
}