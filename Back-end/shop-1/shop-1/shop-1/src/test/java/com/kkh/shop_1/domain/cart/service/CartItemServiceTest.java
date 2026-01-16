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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * * 장바구니 서비스(CartItemService) 비즈니스 로직 테스트
 * * 빌더 접근 제한 문제를 회피하기 위해 Mock과 Reflection을 활용함
 */
@ExtendWith(MockitoExtension.class)
class CartItemServiceTest {

    @InjectMocks
    private CartItemService cartItemService;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Nested
    @DisplayName("장바구니 상품 추가 테스트")
    class AddItem {

        /**
         * * User.builder() 대신 mock(User.class)를 사용하여 private access 문제 해결
         */
        @Test
        @DisplayName("새로운 상품 추가 성공")
        void addItemSuccess() {
            // given
            Long userId = 1L;
            CartItemAddRequestDTO request = new CartItemAddRequestDTO(100L, 2);

            // User와 Item을 빌더 없이 생성
            User user = mock(User.class);
            Item item = Item.builder().name("테스트상품").build();
            ReflectionTestUtils.setField(item, "id", 100L);

            given(userService.findById(userId)).willReturn(user);
            given(itemRepository.findById(request.getItemId())).willReturn(Optional.of(item));
            given(cartItemRepository.findByUserAndItemId(user, 100L)).willReturn(Optional.empty());

            // when
            ApiResponse<Void> response = cartItemService.addItemToCart(userId, request);

            // then
            assertThat(response.isSuccess()).isTrue();
            verify(cartItemRepository, times(1)).save(any(CartItem.class));
        }

        @Test
        @DisplayName("이미 존재하는 상품 추가 시 수량 증가")
        void increaseQuantitySuccess() {
            // given
            Long userId = 1L;
            CartItemAddRequestDTO request = new CartItemAddRequestDTO(100L, 3);

            User user = mock(User.class);
            Item item = Item.builder().name("테스트상품").build();
            ReflectionTestUtils.setField(item, "id", 100L);

            // CartItem 생성 시에도 Mock User 전달
            CartItem existingCartItem = CartItem.createCartItem(user, 100L, 2);

            given(userService.findById(userId)).willReturn(user);
            given(itemRepository.findById(request.getItemId())).willReturn(Optional.of(item));
            given(cartItemRepository.findByUserAndItemId(user, 100L)).willReturn(Optional.of(existingCartItem));

            // when
            cartItemService.addItemToCart(userId, request);

            // then
            assertThat(existingCartItem.getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("존재하지 않는 상품 추가 시 예외 발생")
        void addItemFailInvalidItem() {
            given(itemRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> cartItemService.addItemToCart(1L, new CartItemAddRequestDTO(999L, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 상품입니다.");
        }
    }

    @Nested
    @DisplayName("장바구니 수량 감소 및 삭제 테스트")
    class DecreaseOrRemove {

        @Test
        @DisplayName("수량이 2개 이상일 때 감소 성공")
        void decreaseQuantitySuccess() {
            User user = mock(User.class);
            CartItem cartItem = CartItem.createCartItem(user, 100L, 2);
            given(userService.findById(1L)).willReturn(user);
            given(cartItemRepository.findByUserAndItemId(user, 100L)).willReturn(Optional.of(cartItem));

            cartItemService.decreaseItemQuantity(1L, 100L);

            assertThat(cartItem.getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("수량이 1개일 때 감소 시 자동 삭제")
        void deleteWhenQuantityIsOne() {
            User user = mock(User.class);
            CartItem cartItem = CartItem.createCartItem(user, 100L, 1);
            given(userService.findById(1L)).willReturn(user);
            given(cartItemRepository.findByUserAndItemId(user, 100L)).willReturn(Optional.of(cartItem));

            cartItemService.decreaseItemQuantity(1L, 100L);

            verify(cartItemRepository, times(1)).delete(cartItem);
        }
    }

    @Nested
    @DisplayName("장바구니 조회 테스트")
    class GetItems {

        @Test
        @DisplayName("장바구니 목록 조회 성공")
        void getCartItemsSuccess() {
            // given
            User user = mock(User.class);
            Item item = Item.builder().name("테스트상품").price(10000).build();
            ReflectionTestUtils.setField(item, "id", 100L);

            CartItem cartItem = CartItem.createCartItem(user, 100L, 2);

            given(userService.findById(1L)).willReturn(user);
            given(cartItemRepository.findByUser(user)).willReturn(List.of(cartItem));
            given(itemRepository.findAllById(anyList())).willReturn(List.of(item));

            // when
            ApiResponse<List<CartItemResponseDTO>> response = cartItemService.getCartItems(1L);

            // then
            assertThat(response.getData()).hasSize(1);
            assertThat(response.getData().get(0).getQuantity()).isEqualTo(2);
        }
    }
}