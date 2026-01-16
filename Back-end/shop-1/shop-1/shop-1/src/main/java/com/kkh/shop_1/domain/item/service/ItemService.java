package com.kkh.shop_1.domain.item.service;

import com.kkh.shop_1.common.s3.S3Service;
import com.kkh.shop_1.domain.item.dto.CreateItemRequestDTO;
import com.kkh.shop_1.domain.item.dto.ItemDetailDTO;
import com.kkh.shop_1.domain.item.dto.ItemSummaryDTO;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.item.entity.ItemImage;
import com.kkh.shop_1.domain.item.repository.ItemRepository;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserService userService;
    private final S3Service s3Service;

    private static final String DEFAULT_IMAGE = "/no_image.jpg";

    /**
     *
     * 상품 등록
     *
     */
    public Long createItem(CreateItemRequestDTO request, List<MultipartFile> images, Long sellerId) {
        validateRequest(request);

        User seller = userService.findById(sellerId);
        Item item = convertToEntity(request, seller);

        itemRepository.save(item);
        processItemImages(item, images);

        return item.getId();
    }

    /**
     *
     * 상품 상세 조회
     *
     */
    @Transactional(readOnly = true)
    public ItemDetailDTO getItemDetail(Long itemId) {
        return itemRepository.findById(itemId)
                .map(ItemDetailDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. ID: " + itemId));
    }

    /**
     *
     * 전체 상품 목록 조회
     *
     */
    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getAllItems() {
        return itemRepository.findAll().stream()
                .map(ItemSummaryDTO::from)
                .toList();
    }

    /**
     *
     * 카테고리별 상품 목록 조회
     *
     */
    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getItemsByCategory(String categoryName) {
        ItemCategory category = parseCategory(categoryName);
        return itemRepository.findByItemCategory(category).stream()
                .map(ItemSummaryDTO::from)
                .toList();
    }

    /**
     *
     * 상품 직접 조회 (내부 로직용)
     *
     */
    @Transactional(readOnly = true)
    public Optional<Item> findById(Long itemId) {
        return itemRepository.findById(itemId);
    }

    /**
     *
     * (Private) 이미지 처리 및 업로드 로직
     *
     */
    private void processItemImages(Item item, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            item.setThumbnailUrl(DEFAULT_IMAGE);
            return;
        }

        String folderPath = "items/" + item.getId();

        for (int i = 0; i < images.size(); i++) {
            try {
                String imageUrl = s3Service.uploadImage(folderPath, images.get(i));

                ItemImage itemImage = ItemImage.builder()
                        .imageUrl(imageUrl)
                        .sortOrder(i)
                        .build();
                item.addImage(itemImage);

                if (i == 0) {
                    item.setThumbnailUrl(imageUrl);
                }
            } catch (IOException e) {
                log.error("S3 Image upload failed. ItemID: {}, Error: {}", item.getId(), e.getMessage());
                throw new RuntimeException("S3 Image upload failed.", e);
            }
        }
    }

    /**
     *
     * (Private) 입력값 검증
     *
     */
    private void validateRequest(CreateItemRequestDTO request) {
        if (request.getPrice() <= 0) {
            throw new IllegalArgumentException("상품 가격은 0원보다 커야 합니다.");
        }
        if (request.getQuantity() < 0) {
            throw new IllegalArgumentException("상품 수량은 0개 이상이어야 합니다.");
        }
    }

    /**
     *
     * (Private) DTO -> Entity 변환
     *
     */
    private Item convertToEntity(CreateItemRequestDTO request, User seller) {
        return Item.builder()
                .name(request.getName())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .itemCategory(parseCategory(request.getCategory()))
                .description(request.getDescription())
                .seller(seller)
                .build();
    }

    /**
     *
     * (Private) 카테고리 문자열 파싱
     *
     */
    private ItemCategory parseCategory(String categoryName) {
        try {
            return ItemCategory.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다: " + categoryName);
        }
    }
}