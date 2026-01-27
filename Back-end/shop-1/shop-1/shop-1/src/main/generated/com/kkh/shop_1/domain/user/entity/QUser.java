package com.kkh.shop_1.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 1800114634L;

    public static final QUser user = new QUser("user");

    public final ListPath<Address, QAddress> addresses = this.<Address, QAddress>createList("addresses", Address.class, QAddress.class, PathInits.DIRECT2);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<LoginType> loginType = createEnum("loginType", LoginType.class);

    public final StringPath nickname = createString("nickname");

    public final ListPath<com.kkh.shop_1.domain.order.entity.Order, com.kkh.shop_1.domain.order.entity.QOrder> orders = this.<com.kkh.shop_1.domain.order.entity.Order, com.kkh.shop_1.domain.order.entity.QOrder>createList("orders", com.kkh.shop_1.domain.order.entity.Order.class, com.kkh.shop_1.domain.order.entity.QOrder.class, PathInits.DIRECT2);

    public final StringPath password = createString("password");

    public final StringPath profileImg = createString("profileImg");

    public final StringPath socialId = createString("socialId");

    public final ListPath<com.kkh.shop_1.domain.coupon.entity.UserCoupon, com.kkh.shop_1.domain.coupon.entity.QUserCoupon> userCoupons = this.<com.kkh.shop_1.domain.coupon.entity.UserCoupon, com.kkh.shop_1.domain.coupon.entity.QUserCoupon>createList("userCoupons", com.kkh.shop_1.domain.coupon.entity.UserCoupon.class, com.kkh.shop_1.domain.coupon.entity.QUserCoupon.class, PathInits.DIRECT2);

    public final EnumPath<UserRole> userRole = createEnum("userRole", UserRole.class);

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

