package com.kkh.shop_1.domain.cart.service;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.cart.dto.CartItemAddRequestDTO;
import com.kkh.shop_1.domain.cart.dto.CartItemResponseDTO;
import com.kkh.shop_1.domain.cart.entity.CartItem;
import com.kkh.shop_1.domain.cart.repository.CartItemRepository;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.repository.ItemRepository;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;

    /**
     *
     * 장바구니에 상품 추가
     *
     */
    @Transactional
    public ApiResponse<Void> addItemToCart(Long userId, CartItemAddRequestDTO request) {

        User user = userService.findById(userId);
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        cartItemRepository.findByUserAndItemId(user, item.getId())
                .ifPresentOrElse(
                        existingItem -> existingItem.increaseQuantity(request.getQuantity()),
                        () -> {
                            CartItem newItem = CartItem.createCartItem(user, item.getId(), request.getQuantity());
                            cartItemRepository.save(newItem);
                        }
                );

        return ApiResponse.successNoData();
    }

    /**
     *
     * 장바구니 수량 1개 감소
     *
     */
    @Transactional
    public ApiResponse<Void> decreaseItemQuantity(Long userId, Long itemId) {
        User user = userService.findById(userId);

        CartItem cartItem = cartItemRepository.findByUserAndItemId(user, itemId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니에 해당 상품이 없습니다."));

        if (cartItem.getQuantity() > 1) {
            cartItem.decreaseQuantity();
        } else {
            cartItemRepository.delete(cartItem);
        }

        return ApiResponse.successNoData();
    }

    /**
     *
     * 장바구니에서 특정 상품 완전 삭제
     *
     */
    @Transactional
    public ApiResponse<Void> removeItemFromCart(Long userId, Long itemId) {
        User user = userService.findById(userId);

        cartItemRepository.findByUserAndItemId(user, itemId)
                .ifPresent(cartItemRepository::delete);

        return ApiResponse.successNoData();
    }

    /**
     *
     * 사용자의 장바구니 전체 목록 조회
     *
     */
    public ApiResponse<List<CartItemResponseDTO>> getCartItems(Long userId) {
        if (userId == null) {
            return ApiResponse.success(new ArrayList<>());
        }

        User user = userService.findById(userId);
        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return ApiResponse.success(new ArrayList<>());
        }

        List<Long> itemIds = cartItems.stream().map(CartItem::getItemId).toList();
        Map<Long, Item> itemMap = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));

        List<CartItemResponseDTO> response = cartItems.stream()
                .map(cartItem -> {
                    Item item = itemMap.get(cartItem.getItemId());
                    return (item != null) ? new CartItemResponseDTO(
                            cartItem.getId(),
                            item.getId(),
                            item.getName(),
                            item.getPrice(),
                            cartItem.getQuantity(),
                            item.getThumbnailUrl() // getImageUrl() -> getThumbnailUrl() 수정
                    ) : null;
                })
                .filter(Objects::nonNull)
                .toList();

        return ApiResponse.success(response);
    }

    /**
     *
     * 특정 사용자의 장바구니 아이템들을 조회함
     *
     */
    public List<CartItem> findByUser(User user) {
        return cartItemRepository.findByUser(user);
    }

    /**
     *
     * 주문 완료 후 사용자의 장바구니를 모두 비움
     *
     */
    @Transactional
    public void deleteAllByUser(User user) {
        cartItemRepository.deleteAllByUser(user);
    }

    /**
     *
     * 사용자가 선택한 특정 장바구니 아이템들을 조회함
     *
     */
    public List<CartItem> findByUserAndItemIds(User user, List<Long> itemIds) {
        return cartItemRepository.findByUserAndItemIdIn(user, itemIds);
    }

    /**
     *
     * 주문이 진행된 특정 장바구니 아이템들만 선택하여 삭제함
     *
     */
    @Transactional
    public void deleteSelectedItems(User user, List<Long> itemIds) {
        cartItemRepository.deleteSelectedItemsByUser(user, itemIds);
    }

    public void deleteCartItemsByUserIdAndItemIds(Long userId, List<Long> orderedItemIds) {
        cartItemRepository.deleteCartItemsByUserIdAndItemIds(userId, orderedItemIds);
    }
}
