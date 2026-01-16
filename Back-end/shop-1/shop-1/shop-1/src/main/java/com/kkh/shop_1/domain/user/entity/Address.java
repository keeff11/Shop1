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

    /**
     *
     * 내부 빌더용 생성자 (AccessLevel.PRIVATE)
     *
     */
    @Builder(access = AccessLevel.PRIVATE)
    private Address(User user, String zipCode, String roadAddress, String detailAddress,
                    String recipientName, String recipientPhone, boolean isDefault) {
        this.user = user;
        this.zipCode = zipCode;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.isDefault = isDefault;
    }

    /**
     *
     * 정적 팩토리 메서드: 일반 배송지 객체 생성
     *
     */
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

    /**
     *
     * 정적 팩토리 메서드: 회원가입 시 등 기본 배송지로 설정될 객체 생성
     *
     */
    public static Address createDefault(User user, String zipCode, String roadAddress, String detailAddress,
                                        String recipientName, String recipientPhone) {
        Address address = create(user, zipCode, roadAddress, detailAddress, recipientName, recipientPhone);
        address.isDefault = true;
        return address;
    }

    /**
     *
     * 연관관계 편의 메서드: 소유 유저 할당
     *
     */
    public void assignUser(User user) {
        this.user = user;
    }

    /**
     *
     * 현재 배송지를 기본 배송지로 활성화
     *
     */
    public void markAsDefault() {
        this.isDefault = true;
    }

    /**
     *
     * 현재 배송지의 기본 배송지 설정을 해제
     *
     */
    public void unmarkDefault() {
        this.isDefault = false;
    }

    /**
     *
     * 기존 배송지 정보 필드 일괄 업데이트
     *
     */
    public void update(String zipCode, String roadAddress, String detailAddress,
                       String recipientName, String recipientPhone) {
        this.zipCode = zipCode;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
    }
}