package com.kkh.shop_1.domain.item.repository;

import com.kkh.shop_1.domain.item.dto.ItemSearchCondition;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.kkh.shop_1.domain.item.entity.QItem.item;
import static com.kkh.shop_1.domain.item.entity.QItemImage.itemImage;

@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Item> search(ItemSearchCondition condition) {
        return queryFactory
                .selectFrom(item)
                .leftJoin(item.images, itemImage).fetchJoin()
                .where(
                        keywordContains(condition.getKeyword()),
                        categoryEq(condition.getCategory()),
                        priceGoe(condition.getMinPrice()),
                        priceLoe(condition.getMaxPrice())
                )
                .orderBy(getOrderSpecifier(condition.getSort()))
                .fetch();
    }

    /**
     * [최종 해결책] lower() 함수 완전 제거
     * Hibernate 6의 타입 추론 에러를 피하기 위해 DB 함수 호출을 제거하고,
     * 순수하게 like 연산자만 사용합니다.
     * * MySQL/MariaDB는 기본적으로 Case-Insensitive(대소문자 무시)이므로
     * lower() 없이도 'iphone'으로 'iPhone' 검색이 가능합니다.
     */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;

        // 검색어 앞뒤에 % 붙이기
        String searchPattern = "%" + keyword + "%";

        // lower() 없이 단순 like 검색 (에러 원천 차단)
        return item.name.like(searchPattern)
                .or(item.description.like(searchPattern));
    }

    private BooleanExpression categoryEq(String category) {
        if (!StringUtils.hasText(category)) return null;
        try {
            return item.itemCategory.eq(ItemCategory.valueOf(category.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BooleanExpression priceGoe(Integer minPrice) {
        return minPrice != null ? item.price.goe(minPrice) : null;
    }

    private BooleanExpression priceLoe(Integer maxPrice) {
        return maxPrice != null ? item.price.loe(maxPrice) : null;
    }

    private OrderSpecifier<?> getOrderSpecifier(String sort) {
        if (!StringUtils.hasText(sort)) {
            return item.createdAt.desc();
        }

        switch (sort) {
            case "priceHigh": return item.price.desc();
            case "priceLow": return item.price.asc();
            case "views": return item.viewCount.desc();
            case "latest":
            default: return item.createdAt.desc();
        }
    }
}