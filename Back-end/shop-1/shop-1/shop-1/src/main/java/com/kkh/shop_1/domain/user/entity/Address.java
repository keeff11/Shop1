package com.kkh.shop_1.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 *
 * 사용자의 배송지 정보를 저장하는 엔티티
 *
 */
@Entity
@Table(name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String zipCode;

    @Column(nullable = false)
    private String roadAddress;

    @Column(nullable = false)
    private String detailAddress;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private boolean isDefault;

    @Builder
    private Address(Long id, User user, String zipCode, String roadAddress, String detailAddress,
                    String recipientName, String recipientPhone, boolean isDefault) {
        this.id = id;
        this.user = user;
        this.zipCode = zipCode;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.isDefault = isDefault;
    }

    public static Address create(User user, String zipCode, String roadAddress, String detailAddress,
                                 String recipientName, String recipientPhone) {
        return Address.builder()
                .user(user)
                .zipCode(zipCode)
                .roadAddress(roadAddress)
                .detailAddress(detailAddress)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .isDefault(false)
                .build();
    }

    public static Address createDefault(User user, String zipCode, String roadAddress, String detailAddress,
                                        String recipientName, String recipientPhone) {
        Address address = create(user, zipCode, roadAddress, detailAddress, recipientName, recipientPhone);
        address.isDefault = true;
        return address;
    }

    public void assignUser(User user) {
        this.user = user;
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void unmarkDefault() {
        this.isDefault = false;
    }

    public void update(String zipCode, String roadAddress, String detailAddress,
                       String recipientName, String recipientPhone) {
        this.zipCode = zipCode;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
    }
}