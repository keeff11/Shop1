package com.kkh.shop_1.domain.item.dto;

import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemImage;
import lombok.*;

import java.util.List;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemDetailDTO {

    private Long id;
    private String name;
    private int price;
    private Integer discountPrice;
    private String category;
    private String description;
    private String stockStatus;
    private String status;
    private String sellerNickname;
    private List<String> images;

    /**
     *
     * Item entity -> DTO 변환
     *
     */
    public static ItemDetailDTO from(Item item) {
        return ItemDetailDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .discountPrice(item.getDiscountPrice())
                .category(item.getItemCategory().name())
                .description(item.getDescription())
                .stockStatus(item.getStockStatus().name())
                .status(item.getStatus().name())
                .sellerNickname(item.getSeller().getNickname())
                .images(item.getImages().stream()
                        .map(ItemImage::getImageUrl)
                        .toList())
                .build();
    }
}