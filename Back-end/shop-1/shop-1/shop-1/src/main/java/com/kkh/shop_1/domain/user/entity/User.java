package com.kkh.shop_1.domain.user.entity;

import com.kkh.shop_1.domain.coupon.entity.UserCoupon;
import com.kkh.shop_1.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * * 시스템 사용자 엔티티
 * */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false)
    private LoginType loginType;

    @Column(name = "social_id", unique = true)
    private String socialId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 100)
    private String password;

    private String profileImg;

    @Column(unique = true, nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCoupon> userCoupons = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    /**
     * * 내부 빌더용 생성자
     * */
    @Builder(access = AccessLevel.PRIVATE)
    private User(LoginType loginType, String socialId, String email, String password,
                 String profileImg, String nickname, UserRole userRole) {
        this.loginType = loginType;
        this.socialId = socialId;
        this.email = email;
        this.password = password;
        this.profileImg = profileImg;
        this.nickname = nickname;
        this.userRole = userRole;
    }

    /**
     * * [정적 팩토리] 로컬 유저 생성
     * */
    public static User createLocalUser(String email, String encodedPassword, String nickname, UserRole role) {
        return User.builder()
                .loginType(LoginType.LOCAL)
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .userRole(role)
                .build();
    }

    /**
     * * [정적 팩토리] 소셜 유저 생성
     * */
    public static User createSocialUser(LoginType type, String socialId, String email, String nickname, String profileImg, UserRole role) {
        return User.builder()
                .loginType(type)
                .socialId(socialId)
                .email(email)
                .nickname(nickname)
                .profileImg(profileImg)
                .userRole(role)
                .build();
    }

    // ======= 연관관계 편의 메서드 =======

    public void addOrder(Order order) {
        this.orders.add(order);
        if (order.getUser() != this) {
            order.assignUser(this);
        }
    }

    public void addAddress(Address address) {
        this.addresses.add(address);
        if (address.getUser() != this) {
            address.assignUser(this);
        }
    }

    // ======= 비즈니스 메서드 =======

    /**
     * * 프로필 정보 수정
     * */
    public void updateProfile(String nickname, String profileImg) {
        this.nickname = nickname;
        if (profileImg != null) {
            this.profileImg = profileImg;
        }
    }

    /**
     * * 비밀번호 변경 (로컬 유저 전용)
     * */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}