package com.kkh.shop_1.domain.item.repository;

import com.kkh.shop_1.domain.item.dto.ItemSearchCondition;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.kkh.shop_1.domain.item.entity.QItem.item;
import static com.kkh.shop_1.domain.item.entity.QItemImage.itemImage;

@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Item> search(ItemSearchCondition condition, Pageable pageable) {
        // 1. 컨텐츠 조회
        List<Item> content = queryFactory
                .selectFrom(item)
                .leftJoin(item.images, itemImage).fetchJoin()
                .where(
                        keywordContains(condition.getKeyword()),
                        categoryEq(condition.getCategory()),
                        priceGoe(condition.getMinPrice()), // 최소 가격
                        priceLoe(condition.getMaxPrice())  // 최대 가격
                )
                .orderBy(getOrderSpecifier(condition.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 카운트 쿼리 (최적화)
        JPAQuery<Long> countQuery = queryFactory
                .select(item.count())
                .from(item)
                .where(
                        keywordContains(condition.getKeyword()),
                        categoryEq(condition.getCategory()),
                        priceGoe(condition.getMinPrice()),
                        priceLoe(condition.getMaxPrice())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // [유지] 지난번 해결된 검색 로직 (Hibernate 6 호환)
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        String searchPattern = "%" + keyword + "%";
        return item.name.like(searchPattern).or(item.description.like(searchPattern));
    }

    private BooleanExpression categoryEq(String category) {
        if (!StringUtils.hasText(category)) return null;
        try {
            return item.itemCategory.eq(ItemCategory.valueOf(category.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // 가격 필터 조건
    private BooleanExpression priceGoe(Integer minPrice) {
        return minPrice != null ? item.price.goe(minPrice) : null;
    }

    private BooleanExpression priceLoe(Integer maxPrice) {
        return maxPrice != null ? item.price.loe(maxPrice) : null;
    }

    private OrderSpecifier<?> getOrderSpecifier(String sort) {
        if (!StringUtils.hasText(sort)) return item.createdAt.desc();
        switch (sort) {
            case "priceHigh": return item.price.desc();
            case "priceLow": return item.price.asc();
            case "views": return item.viewCount.desc();
            case "latest": default: return item.createdAt.desc();
        }
    }
}