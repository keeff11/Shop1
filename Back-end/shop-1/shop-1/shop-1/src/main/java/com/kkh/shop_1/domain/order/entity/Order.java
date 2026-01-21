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

    // ★ [추가] 결제 수단 저장 (승인 단계에서 필요)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;

    @Column(name = "tid", unique = true)
    private String tid;

    @Builder(access = AccessLevel.PRIVATE)
    private Order(User user, Address address) {
        this.user = user;
        this.address = address;
        this.status = OrderStatus.PAYMENT_PENDING;
        this.orderDate = LocalDateTime.now();
    }

    public static Order create(User user, Address address) {
        return Order.builder()
                .user(user)
                .address(address)
                .build();
    }

    // ★ [추가] 결제 타입 설정 메서드
    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public void assignUser(User user) {
        this.user = user;
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }

    public void completePayment(String tid) {
        if (this.status != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 대기 중인 주문만 결제 완료 처리가 가능합니다.");
        }
        this.status = OrderStatus.PAID;
        // TID는 이미 ready 단계나 승인 요청 DTO 생성 시점에 갱신되지만, 확실히 하기 위해 저장
        if (tid != null) this.tid = tid;
    }

    public void cancel() {
        if (this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("이미 배송된 상품은 취소가 불가능합니다.");
        }
        this.status = OrderStatus.CANCELLED;
        this.orderItems.forEach(OrderItem::cancel);
    }

    public int getTotalAmount() {
        return orderItems.stream().mapToInt(OrderItem::getTotalPrice).sum();
    }

    public int getTotalQuantity() {
        return orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public void updateTid(String tid) {
        this.tid = tid;
    }
}