package com.kkh.shop_1.domain.item.dto;

import com.kkh.shop_1.domain.item.entity.Item;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemResponseDTO {

    private Long id;
    private String name;
    private int price;
    private Integer discountPrice;
    private int quantity;
    private String stockStatus;
    private String itemCategory;
    private String description;
    private String thumbnailUrl;
    private String status;
    private String sellerNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     *
     * Item Entity -> DTO 변환
     *
     */
    public static ItemResponseDTO from(Item item) {
        return ItemResponseDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .discountPrice(item.getDiscountPrice())
                .quantity(item.getQuantity())
                .stockStatus(item.getStockStatus().name())
                .itemCategory(item.getItemCategory().name())
                .description(item.getDescription())
                .thumbnailUrl(item.getThumbnailUrl())
                .status(item.getStatus().name())
                .sellerNickname(item.getSeller().getNickname())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}