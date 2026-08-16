package com.kkh.shop_1.domain.item.service;

import com.kkh.shop_1.common.s3.S3Service;
import com.kkh.shop_1.domain.item.document.ItemDocument;
import com.kkh.shop_1.domain.item.dto.CreateItemRequestDTO;
import com.kkh.shop_1.domain.item.dto.ItemDetailDTO;
import com.kkh.shop_1.domain.item.dto.UpdateItemRequestDTO;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.item.entity.ItemImage;
import com.kkh.shop_1.domain.item.entity.ItemStatus;
import com.kkh.shop_1.domain.item.repository.ItemRepository;
import com.kkh.shop_1.domain.item.repository.ItemSearchRepository;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * * 상품 서비스 단위 테스트
 * * 서비스 로직의 예외 메시지와 테스트의 기대 메시지를 일치시킴
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @InjectMocks
    private ItemService itemService;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Mock
    private S3Service s3Service;

    @Mock
    private ItemSearchRepository itemSearchRepository;

    @Nested
    @DisplayName("상품 등록 테스트")
    class CreateItem {

        /**
         * * 성공 케이스: 이미지 업로드 및 상품 저장 프로세스 검증
         */
        @Test
        @DisplayName("이미지와 함께 상품 등록 시 성공적으로 ID를 반환한다")
        void createItem_success() throws IOException {
            // given
            Long sellerId = 1L;
            CreateItemRequestDTO request = createRequest("MacBook", 2500000, 10);

            User seller = mock(User.class);
            ReflectionTestUtils.setField(seller, "id", sellerId);

            MockMultipartFile image = new MockMultipartFile("images", "test.png", "image/png", "content".getBytes());

            given(userService.findById(sellerId)).willReturn(seller);
            given(s3Service.uploadImage(any(), any())).willReturn("https://s3.url/test.png");

            given(itemRepository.save(any(Item.class))).willAnswer(invocation -> {
                Item item = invocation.getArgument(0);
                ReflectionTestUtils.setField(item, "id", 100L);
                return item;
            });

            // when
            Long savedId = itemService.createItem(request, List.of(image), sellerId);

            // then
            assertThat(savedId).isEqualTo(100L);
            verify(s3Service, atLeastOnce()).uploadImage(any(), any());
            verify(itemRepository, times(1)).save(any(Item.class));
            verify(itemSearchRepository, times(1)).save(any(ItemDocument.class));
        }

        /**
         * * 검증 실패 케이스: 서비스 코드의 예외 메시지 문구와 정확히 일치시킴
         */
        @ParameterizedTest
        @CsvSource({
                "0, 10, 상품 가격은 0원보다 커야 합니다.",
                "1000, -1, 상품 수량은 0개 이상이어야 합니다."
        })
        @DisplayName("잘못된 가격이나 수량으로 등록 시 예외가 발생한다")
        void createItem_invalidInput_fail(int price, int quantity, String expectedMessage) {
            // given
            CreateItemRequestDTO request = createRequest("FailItem", price, quantity);

            // when & then
            assertThatThrownBy(() -> itemService.createItem(request, List.of(), 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedMessage); // "0원" 등 핵심 키워드 포함 확인
        }

        /**
         * * 외부 연동 실패 케이스: 서비스 코드의 "S3 Image upload failed." 메시지와 일치시킴
         */
        @Test
        @DisplayName("S3 업로드 중 에러 발생 시 런타임 예외로 변환하여 던진다")
        void createItem_s3Fail_throwsException() throws IOException {
            // given
            CreateItemRequestDTO request = createRequest("S3Fail", 10000, 5);

            given(userService.findById(any())).willReturn(mock(User.class));
            // S3 서비스에서 IOException 발생 시뮬레이션
            given(s3Service.uploadImage(any(), any())).willThrow(new IOException("Network Error"));

            MockMultipartFile image = new MockMultipartFile("img", "t.jpg", "image/jpg", "c".getBytes());

            // when & then
            assertThatThrownBy(() -> itemService.createItem(request, List.of(image), 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("S3 Image upload failed.");
        }
    }

    @Nested
    @DisplayName("상품 조회 테스트")
    class GetItem {

        @Test
        @DisplayName("존재하는 상품 상세 조회 시 DTO를 반환한다")
        void getItemDetail_success() {
            // given
            Long itemId = 100L;
            User seller = mock(User.class);
            given(seller.getNickname()).willReturn("판매왕");

            Item item = Item.builder()
                    .name("상세상품")
                    .price(10000)
                    .quantity(10)
                    .itemCategory(ItemCategory.ELECTRONICS)
                    .seller(seller)
                    .build();

            ReflectionTestUtils.setField(item, "id", itemId);

            given(itemRepository.findById(itemId)).willReturn(Optional.of(item));

            // when
            ItemDetailDTO result = itemService.getItemDetail(itemId);

            // then
            assertThat(result.getName()).isEqualTo("상세상품");
            assertThat(result.getSellerNickname()).isEqualTo("판매왕");
        }

        @Test
        @DisplayName("존재하지 않는 상품 조회 시 예외가 발생한다")
        void getItemDetail_notFound_fail() {
            // given
            given(itemRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> itemService.getItemDetail(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품이 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("상품 수정 테스트")
    class UpdateItem {

        @Test
        @DisplayName("새 이미지와 함께 수정 시 기존 이미지를 S3에서 삭제하고 새로 등록한다")
        void updateItem_success_withNewImages() throws IOException {
            // given
            Long itemId = 100L;
            Long sellerId = 1L;

            User seller = mock(User.class);
            ReflectionTestUtils.setField(seller, "id", sellerId);

            Item item = Item.builder()
                    .name("기존상품")
                    .price(10000)
                    .quantity(5)
                    .itemCategory(ItemCategory.ELECTRONICS)
                    .seller(seller)
                    .build();
            ReflectionTestUtils.setField(item, "id", itemId);
            item.setThumbnailUrl("https://s3.url/old_thumb.png");
            item.addImage(ItemImage.builder().imageUrl("https://s3.url/old1.png").sortOrder(0).build());

            UpdateItemRequestDTO request = updateRequest("수정상품", 20000, 3);
            MockMultipartFile newImage = new MockMultipartFile("images", "new.png", "image/png", "content".getBytes());

            given(itemRepository.findById(itemId)).willReturn(Optional.of(item));
            given(s3Service.uploadImage(any(), any())).willReturn("https://s3.url/new1.png");

            // when
            Long updatedId = itemService.updateItem(itemId, request, List.of(newImage), sellerId);

            // then
            assertThat(updatedId).isEqualTo(itemId);
            assertThat(item.getName()).isEqualTo("수정상품");
            verify(s3Service).deleteImageByUrl("https://s3.url/old_thumb.png");
            verify(s3Service).deleteImageByUrl("https://s3.url/old1.png");
            verify(s3Service, atLeastOnce()).uploadImage(any(), any());
            verify(itemSearchRepository, times(1)).save(any(ItemDocument.class));
        }

        @Test
        @DisplayName("새 이미지 없이 수정 시 기존 이미지는 유지되고 필드만 변경된다")
        void updateItem_success_withoutNewImages() {
            // given
            Long itemId = 100L;
            Long sellerId = 1L;

            User seller = mock(User.class);
            ReflectionTestUtils.setField(seller, "id", sellerId);

            Item item = Item.builder()
                    .name("기존상품")
                    .price(10000)
                    .quantity(5)
                    .itemCategory(ItemCategory.ELECTRONICS)
                    .seller(seller)
                    .build();
            ReflectionTestUtils.setField(item, "id", itemId);
            item.setThumbnailUrl("https://s3.url/old_thumb.png");

            UpdateItemRequestDTO request = updateRequest("수정상품", 20000, 3);

            given(itemRepository.findById(itemId)).willReturn(Optional.of(item));

            // when
            Long updatedId = itemService.updateItem(itemId, request, null, sellerId);

            // then
            assertThat(updatedId).isEqualTo(itemId);
            assertThat(item.getName()).isEqualTo("수정상품");
            assertThat(item.getPrice()).isEqualTo(20000);
            assertThat(item.getQuantity()).isEqualTo(3);
            assertThat(item.getThumbnailUrl()).isEqualTo("https://s3.url/old_thumb.png");
            verify(s3Service, never()).deleteImageByUrl(any());
            verify(itemSearchRepository, times(1)).save(any(ItemDocument.class));
        }

        @ParameterizedTest
        @CsvSource({
                "0, 10, 상품 가격은 0원보다 커야 합니다.",
                "1000, -1, 상품 수량은 0개 이상이어야 합니다."
        })
        @DisplayName("잘못된 가격이나 수량으로 수정 시 예외가 발생한다")
        void updateItem_invalidInput_fail(int price, int quantity, String expectedMessage) {
            // given
            UpdateItemRequestDTO request = updateRequest("수정상품", price, quantity);

            // when & then
            assertThatThrownBy(() -> itemService.updateItem(100L, request, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedMessage);

            verifyNoInteractions(itemRepository);
        }

        @Test
        @DisplayName("존재하지 않는 상품 수정 시 예외가 발생한다")
        void updateItem_notFound_fail() {
            // given
            given(itemRepository.findById(anyLong())).willReturn(Optional.empty());
            UpdateItemRequestDTO request = updateRequest("수정상품", 10000, 5);

            // when & then
            assertThatThrownBy(() -> itemService.updateItem(999L, request, null, 1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("판매자 본인이 아닌 사용자가 수정 시 예외가 발생한다")
        void updateItem_wrongSeller_fail() {
            // given
            Long itemId = 100L;
            Long ownerId = 1L;
            Long otherUserId = 2L;

            User seller = mock(User.class);
            ReflectionTestUtils.setField(seller, "id", ownerId);

            Item item = Item.builder()
                    .name("기존상품")
                    .price(10000)
                    .quantity(5)
                    .itemCategory(ItemCategory.ELECTRONICS)
                    .seller(seller)
                    .build();
            ReflectionTestUtils.setField(item, "id", itemId);

            given(itemRepository.findById(itemId)).willReturn(Optional.of(item));
            UpdateItemRequestDTO request = updateRequest("수정상품", 10000, 5);

            // when & then
            assertThatThrownBy(() -> itemService.updateItem(itemId, request, null, otherUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("수정 권한이 없습니다.");
        }
    }

    @Nested
    @DisplayName("상품 삭제 테스트")
    class DeleteItem {

        @Test
        @DisplayName("판매자 본인이 상품 삭제 시 논리 삭제되고 색인에서 제거된다")
        void deleteItem_success() {
            // given
            Long itemId = 100L;
            Long sellerId = 1L;

            User seller = mock(User.class);
            ReflectionTestUtils.setField(seller, "id", sellerId);

            Item item = Item.builder()
                    .name("삭제될상품")
                    .price(10000)
                    .quantity(5)
                    .itemCategory(ItemCategory.ELECTRONICS)
                    .seller(seller)
                    .build();
            ReflectionTestUtils.setField(item, "id", itemId);

            given(itemRepository.findById(itemId)).willReturn(Optional.of(item));

            // when
            itemService.deleteItem(itemId, sellerId);

            // then
            assertThat(item.getStatus()).isEqualTo(ItemStatus.DELETED);
            verify(itemSearchRepository, times(1)).deleteById(itemId);
        }

        @Test
        @DisplayName("존재하지 않는 상품 삭제 시 예외가 발생한다")
        void deleteItem_notFound_fail() {
            // given
            given(itemRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> itemService.deleteItem(999L, 1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("해당 상품을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("판매자 본인이 아닌 사용자가 삭제 시 예외가 발생한다")
        void deleteItem_wrongSeller_fail() {
            // given
            Long itemId = 100L;
            Long ownerId = 1L;
            Long otherUserId = 2L;

            User seller = mock(User.class);
            ReflectionTestUtils.setField(seller, "id", ownerId);

            Item item = Item.builder()
                    .name("삭제될상품")
                    .price(10000)
                    .quantity(5)
                    .itemCategory(ItemCategory.ELECTRONICS)
                    .seller(seller)
                    .build();
            ReflectionTestUtils.setField(item, "id", itemId);

            given(itemRepository.findById(itemId)).willReturn(Optional.of(item));

            // when & then
            assertThatThrownBy(() -> itemService.deleteItem(itemId, otherUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("본인이 등록한 상품만 삭제할 수 있습니다.");
        }
    }

    // --- Helper Methods ---

    private CreateItemRequestDTO createRequest(String name, int price, int quantity) {
        return CreateItemRequestDTO.builder()
                .name(name)
                .price(price)
                .quantity(quantity)
                .category("ELECTRONICS")
                .description("설명")
                .build();
    }

    private UpdateItemRequestDTO updateRequest(String name, int price, int quantity) {
        return new UpdateItemRequestDTO(name, price, quantity, "ELECTRONICS", "설명");
    }
}