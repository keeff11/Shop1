package com.kkh.shop_1.domain.item.service;

import com.kkh.shop_1.common.s3.S3Service;
import com.kkh.shop_1.domain.item.dto.*;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.item.entity.ItemImage;
import com.kkh.shop_1.domain.item.entity.ItemStatus;
import com.kkh.shop_1.domain.item.repository.ItemRepository;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserService userService;
    private final S3Service s3Service;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String DEFAULT_IMAGE = "/no_image.jpg";

    /**
     * 상품 등록
     */
    @CacheEvict(value = "items", key = "'all'")
    public Long createItem(CreateItemRequestDTO createItemRequestDTO,
                           List<MultipartFile> images,
                           Long sellerId) {
        validateRequest(createItemRequestDTO);

        User seller = userService.findById(sellerId);
        Item item = convertToEntity(createItemRequestDTO, seller);

        item.setStatus(ItemStatus.SELLING);
        itemRepository.save(item);
        processItemImages(item, images);

        return item.getId();
    }

    /**
     * 상품 수정
     */
    @Caching(evict = {
            @CacheEvict(value = "items", key = "'all'"),       // 목록 캐시 삭제
            @CacheEvict(value = "item:detail", key = "#itemId") // 상세 캐시 삭제
    })
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

        if (newImages != null && !newImages.isEmpty()) {
            if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().equals(DEFAULT_IMAGE)) {
                s3Service.deleteImageByUrl(item.getThumbnailUrl());
            }
            item.getImages().forEach(img -> s3Service.deleteImageByUrl(img.getImageUrl()));

            item.clearImages();
            processItemImages(item, newImages);
        }

        return item.getId();
    }

    /**
     * 상품 삭제
     */
    @Caching(evict = {
            @CacheEvict(value = "items", key = "'all'"),
            @CacheEvict(value = "item:detail", key = "#itemId")
    })
    public void deleteItem(Long itemId, Long currentUserId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("해당 상품을 찾을 수 없습니다."));

        if (!item.getSeller().getId().equals(currentUserId)) {
            throw new AccessDeniedException("본인이 등록한 상품만 삭제할 수 있습니다.");
        }

        item.setStatus(ItemStatus.DELETED);
        log.info("상품 논리 삭제 완료 (ID: {})", itemId);
    }

    /**
     * 상품 상세 조회
     */
    @Transactional
    @Cacheable(value = "item:detail", key = "#itemId")
    public ItemDetailDTO getItemDetail(Long itemId) {

        itemRepository.increaseViewCount(itemId);

        redisTemplate.opsForZSet().incrementScore("ranking:items:views", String.valueOf(itemId), 1.0);
        
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. ID: " + itemId));

        if (item.getStatus() == ItemStatus.DELETED) {
            throw new EntityNotFoundException("삭제된 상품입니다.");
        }

        return ItemDetailDTO.from(item);
    }

    /**
     * 재고 차감
     */
    @CacheEvict(value = "item:detail", key = "#itemId")
    public void decreaseStock(Long itemId, int quantity) {
        int updatedRows = itemRepository.decreaseStock(itemId, quantity);
        if (updatedRows == 0) {
            throw new IllegalStateException("재고가 부족합니다. ItemID: " + itemId);
        }
    }

    // --- 조회용 (ReadOnly) ---

    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getMyItems(Long sellerId) {
        return itemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .filter(item -> item.getStatus() != ItemStatus.DELETED)
                .map(ItemSummaryDTO::from)
                .toList();
    }

    // 전체 목록 조회 (캐시 적용 유지)
    @Transactional(readOnly = true)
    @Cacheable(value = "items", key = "'all'")
    public List<ItemSummaryDTO> getAllItems() {
        return itemRepository.findAllWithImages().stream()
                .filter(item -> item.getStatus() != ItemStatus.DELETED)
                .map(ItemSummaryDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getItemsByCategory(String categoryName) {
        ItemCategory category = parseCategory(categoryName);
        return itemRepository.findByItemCategory(category).stream()
                .filter(item -> item.getStatus() != ItemStatus.DELETED)
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
                .status(ItemStatus.SELLING)
                .build();
    }

    private ItemCategory parseCategory(String categoryName) {
        try {
            return ItemCategory.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다: " + categoryName);
        }
    }

    // 검색 결과 페이징 처리
    @Transactional(readOnly = true)
    public Page<ItemSummaryDTO> searchItems(ItemSearchCondition condition, Pageable pageable) {
        return itemRepository.search(condition, pageable)
                .map(ItemSummaryDTO::from);
    }


    // 🌟 [추가] 실시간 인기 상품 Top 10 조회
    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getPopularItems() {
        // 1. Redis에서 점수(조회수)가 높은 순으로 1위(0)부터 10위(9)까지의 상품 ID 조회
        Set<Object> topItemIds = redisTemplate.opsForZSet().reverseRange("ranking:items:views", 0, 9);

        if (topItemIds == null || topItemIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Object 타입을 Long 타입으로 변환 (Jackson 직렬화 과정에서 쌍따옴표가 붙을 수 있어 제거)
        List<Long> itemIds = topItemIds.stream()
                .map(id -> Long.parseLong(id.toString().replace("\"", "")))
                .toList();

        // 3. MySQL에서 상품 정보 한 번에 조회 (IN 쿼리)
        List<Item> items = itemRepository.findAllById(itemIds);

        // 4. IN 쿼리는 결과의 순서를 보장하지 않으므로, Redis에서 가져온 랭킹 순서대로 재정렬
        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, item -> item));

        return itemIds.stream()
                .map(itemMap::get)
                .filter(item -> item != null && item.getStatus() != ItemStatus.DELETED)
                .map(ItemSummaryDTO::from)
                .toList();
    }

    // 🌟 [추가] 매일 자정에 랭킹 데이터 초기화 (스케줄러)
    @Scheduled(cron = "0 0 0 * * *") // 매일 00:00:00 실행
    public void resetItemRanking() {
        redisTemplate.delete("ranking:items:views");
        log.info("실시간 인기 상품 Redis 랭킹 데이터가 초기화되었습니다.");
    }

    /**
     * 🌟 [추가] 최근 본 상품 이력 저장 (Redis ZSet)
     */
    public void addRecentItem(String viewerId, Long itemId) {
        String key = "recent:items:" + viewerId;
        long timestamp = System.currentTimeMillis(); // 현재 시간을 점수(Score)로 사용

        // Redis ZSet에 추가 (이미 본 상품이면 Timestamp 점수만 최신으로 업데이트 됨)
        redisTemplate.opsForZSet().add(key, String.valueOf(itemId), timestamp);

        // 최신 10개만 남기고 이전 데이터는 삭제 (O(log(N)) 속도로 매우 빠름)
        Long size = redisTemplate.opsForZSet().zCard(key);
        if (size != null && size > 10) {
            // 상위 10개(0~9)를 제외하고, 그 밑의 데이터는 모두 제거
            redisTemplate.opsForZSet().removeRange(key, 0, size - 11);
        }

        // 해당 사용자가 7일간 접속하지 않으면 키 자동 삭제 (메모리 관리)
        redisTemplate.expire(key, Duration.ofDays(7));
    }

    /**
     * 🌟 [추가] 최근 본 상품 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getRecentItems(String viewerId) {
        String key = "recent:items:" + viewerId;

        // 점수(Timestamp)가 높은 순(가장 최근)으로 10개 조회
        Set<Object> itemIds = redisTemplate.opsForZSet().reverseRange(key, 0, 9);

        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = itemIds.stream()
                .map(id -> Long.parseLong(id.toString().replace("\"", "")))
                .toList();

        // MySQL에서 상품 정보 조회 후 Redis 순서에 맞게 재정렬 (IN 쿼리 한계 극복)
        List<Item> items = itemRepository.findAllById(ids);
        Map<Long, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getId, i -> i));

        return ids.stream()
                .map(itemMap::get)
                .filter(item -> item != null && item.getStatus() != ItemStatus.DELETED)
                .map(ItemSummaryDTO::from)
                .toList();
    }

}