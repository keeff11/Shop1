package com.kkh.shop_1.domain.coupon.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCoupon is a Querydsl query type for Coupon
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCoupon extends EntityPathBase<Coupon> {

    private static final long serialVersionUID = -2049613376L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCoupon coupon = new QCoupon("coupon");

    public final EnumPath<com.kkh.shop_1.domain.item.entity.ItemCategory> category = createEnum("category", com.kkh.shop_1.domain.item.entity.ItemCategory.class);

    public final EnumPath<CouponType> couponType = createEnum("couponType", CouponType.class);

    public final com.kkh.shop_1.domain.user.entity.QUser createdBy;

    public final EnumPath<DiscountType> discountType = createEnum("discountType", DiscountType.class);

    public final NumberPath<Integer> discountValue = createNumber("discountValue", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> expiredAt = createDateTime("expiredAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final com.kkh.shop_1.domain.item.entity.QItem targetItem;

    public QCoupon(String variable) {
        this(Coupon.class, forVariable(variable), INITS);
    }

    public QCoupon(Path<? extends Coupon> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCoupon(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCoupon(PathMetadata metadata, PathInits inits) {
        this(Coupon.class, metadata, inits);
    }

    public QCoupon(Class<? extends Coupon> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.createdBy = inits.isInitialized("createdBy") ? new com.kkh.shop_1.domain.user.entity.QUser(forProperty("createdBy")) : null;
        this.targetItem = inits.isInitialized("targetItem") ? new com.kkh.shop_1.domain.item.entity.QItem(forProperty("targetItem"), inits.get("targetItem")) : null;
    }

}

