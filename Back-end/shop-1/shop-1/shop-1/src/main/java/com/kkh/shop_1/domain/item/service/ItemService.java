package com.kkh.shop_1.domain.item.service;

import com.kkh.shop_1.common.s3.S3Service;
import com.kkh.shop_1.domain.item.dto.CreateItemRequestDTO;
import com.kkh.shop_1.domain.item.dto.ItemDetailDTO;
import com.kkh.shop_1.domain.item.dto.ItemSummaryDTO;
import com.kkh.shop_1.domain.item.dto.UpdateItemRequestDTO;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.item.entity.ItemImage;
import com.kkh.shop_1.domain.item.repository.ItemRepository;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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
     * 상품 등록
     */
    public Long createItem(CreateItemRequestDTO createItemRequestDTO,
                           List<MultipartFile> images,
                           Long sellerId) {
        validateRequest(createItemRequestDTO);

        User seller = userService.findById(sellerId);
        Item item = convertToEntity(createItemRequestDTO, seller);

        itemRepository.save(item);
        processItemImages(item, images);

        return item.getId();
    }

    /**
     * 상품 수정
     */
    public Long updateItem(Long itemId, UpdateItemRequestDTO request, List<MultipartFile> newImages, Long sellerId) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));

        if (!item.getSeller().getId().equals(sellerId)) {
            log.warn("[SECURITY] Unauthorized update attempt: ItemID={}, UserID={}", itemId, sellerId);
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }

        item.update(
                request.getName(),
                request.getPrice(),
                request.getQuantity(),
                parseCategory(request.getCategory()),
                request.getDescription()
        );

        // 이미지 업데이트 로직
        if (newImages != null && !newImages.isEmpty()) {
            // 기존 이미지 삭제 (썸네일 + 상세 이미지)
            if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().equals(DEFAULT_IMAGE)) {
                s3Service.deleteImageByUrl(item.getThumbnailUrl());
            }
            item.getImages().forEach(img -> s3Service.deleteImageByUrl(img.getImageUrl()));

            // DB 관계 끊기
            item.clearImages();

            // 새 이미지 업로드
            processItemImages(item, newImages);
        }

        return item.getId();
    }

    /**
     * 상품 삭제
     */
    public void deleteItem(Long itemId, Long currentUserId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("해당 상품을 찾을 수 없습니다."));

        if (!item.getSeller().getId().equals(currentUserId)) {
            throw new AccessDeniedException("본인이 등록한 상품만 삭제할 수 있습니다.");
        }

        // S3 이미지 삭제
        if (item.getThumbnailUrl() != null) {
            s3Service.deleteImageByUrl(item.getThumbnailUrl());
        }
        item.getImages().forEach(img -> s3Service.deleteImageByUrl(img.getImageUrl()));

        itemRepository.delete(item);
    }

    /**
     * 상품 상세 조회
     * (조회수 증가 포함)
     */
    public ItemDetailDTO getItemDetail(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. ID: " + itemId));

        // ★ [추가] 조회수 증가 (Dirty Checking에 의해 트랜잭션 종료 시 DB 업데이트)
        item.increaseViewCount();

        return ItemDetailDTO.from(item);
    }

    // --- 조회용 (ReadOnly) ---

    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getMyItems(Long sellerId) {
        return itemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(ItemSummaryDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getAllItems() {
        return itemRepository.findAll().stream()
                .map(ItemSummaryDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getItemsByCategory(String categoryName) {
        ItemCategory category = parseCategory(categoryName);
        return itemRepository.findByItemCategory(category).stream()
                .map(ItemSummaryDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Item> findById(Long itemId) {
        return itemRepository.findById(itemId);
    }

    // --- Private Helper Methods ---

    private void processItemImages(Item item, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            item.setThumbnailUrl(DEFAULT_IMAGE);
            return;
        }

        // 폴더 경로: items/{itemId}
        String folderPath = "items/" + item.getId();

        for (int i = 0; i < images.size(); i++) {
            try {
                String imageUrl = s3Service.uploadImage(folderPath, images.get(i));

                ItemImage itemImage = ItemImage.builder()
                        .imageUrl(imageUrl)
                        .sortOrder(i)
                        .build();
                item.addImage(itemImage);

                // 첫 번째 이미지를 썸네일로 지정
                if (i == 0) {
                    item.setThumbnailUrl(imageUrl);
                }
            } catch (IOException e) {
                log.error("S3 Image upload failed. ItemID: {}", item.getId(), e);
                throw new RuntimeException("S3 Image upload failed.", e);
            }
        }
    }

    private void validateRequest(CreateItemRequestDTO request) {
        if (request.getPrice() <= 0) {
            throw new IllegalArgumentException("상품 가격은 0원보다 커야 합니다.");
        }
        if (request.getQuantity() < 0) {
            throw new IllegalArgumentException("상품 수량은 0개 이상이어야 합니다.");
        }
    }

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

    private ItemCategory parseCategory(String categoryName) {
        try {
            return ItemCategory.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다: " + categoryName);
        }
    }
}