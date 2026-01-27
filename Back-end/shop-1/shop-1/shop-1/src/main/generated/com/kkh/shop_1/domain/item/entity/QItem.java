package com.kkh.shop_1.domain.item.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QItem is a Querydsl query type for Item
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItem extends EntityPathBase<Item> {

    private static final long serialVersionUID = -1984166822L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QItem item = new QItem("item");

    public final NumberPath<Double> averageRating = createNumber("averageRating", Double.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final NumberPath<Integer> discountPrice = createNumber("discountPrice", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<ItemImage, QItemImage> images = this.<ItemImage, QItemImage>createList("images", ItemImage.class, QItemImage.class, PathInits.DIRECT2);

    public final EnumPath<ItemCategory> itemCategory = createEnum("itemCategory", ItemCategory.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final NumberPath<Integer> reviewCount = createNumber("reviewCount", Integer.class);

    public final NumberPath<Integer> salesCount = createNumber("salesCount", Integer.class);

    public final com.kkh.shop_1.domain.user.entity.QUser seller;

    public final EnumPath<ItemStatus> status = createEnum("status", ItemStatus.class);

    public final EnumPath<StockStatus> stockStatus = createEnum("stockStatus", StockStatus.class);

    public final StringPath thumbnailUrl = createString("thumbnailUrl");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> viewCount = createNumber("viewCount", Integer.class);

    public QItem(String variable) {
        this(Item.class, forVariable(variable), INITS);
    }

    public QItem(Path<? extends Item> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QItem(PathMetadata metadata, PathInits inits) {
        this(Item.class, metadata, inits);
    }

    public QItem(Class<? extends Item> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.seller = inits.isInitialized("seller") ? new com.kkh.shop_1.domain.user.entity.QUser(forProperty("seller")) : null;
    }

}

