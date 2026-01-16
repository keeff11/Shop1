package com.kkh.shop_1.domain.order.entity;

import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "tid", unique = true)
    private String tid;

    /**
     *
     * 내부용
     *
     */
    @Builder(access = AccessLevel.PRIVATE)
    private Order(User user, Address address) {
        this.user = user;
        this.address = address;
        this.status = OrderStatus.PAYMENT_PENDING;
        this.orderDate = LocalDateTime.now();
    }

    /**
     *
     * 주문 생성
     *
     */
    public static Order create(User user, Address address) {
        Order order = Order.builder()
                .user(user)
                .address(address)
                .build();

        user.addOrder(order);
        return order;
    }

    /**
     *
     * 유저 정보 할당
     *
     */
    public void assignUser(User user) {
        this.user = user;
    }

    /**
     *
     * 주문 상품 추가 및 주문 상품의 주문 정보 설정
     *
     */
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }

    /**
     *
     * 결제 완료 처리 및 TID 저장
     *
     */
    public void completePayment(String tid) {
        if (this.status != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 대기 중인 주문만 결제 완료 처리가 가능합니다.");
        }
        this.status = OrderStatus.PAID;
        this.tid = tid;
    }

    /**
     *
     * 주문 취소 처리 및 주문 상품 재고 복구
     *
     */
    public void cancel() {
        if (this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("이미 배송된 상품은 취소가 불가능합니다.");
        }

        this.status = OrderStatus.CANCELLED;
        this.orderItems.forEach(OrderItem::cancel);
    }

    /**
     *
     * 주문 상품 전체 금액 합계 계산
     *
     */
    public int getTotalAmount() {
        return orderItems.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
    }

    /**
     *
     * 주문 상품 전체 수량 합계 계산
     *
     */
    public int getTotalQuantity() {
        return orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    /**
     *
     * TID 업데이트 (결제 준비 요청 시점)
     *
     */
    public void updateTid(String tid) {
        this.tid = tid;
    }
}