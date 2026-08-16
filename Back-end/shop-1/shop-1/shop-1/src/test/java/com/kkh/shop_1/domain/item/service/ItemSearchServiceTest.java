package com.kkh.shop_1.domain.item.service;

import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.item.repository.ItemRepository;
import com.kkh.shop_1.domain.item.repository.ItemSearchRepository;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.entity.UserRole;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * * 상품 검색 서비스(ItemSearchService) 단위 테스트
 * * MySQL -> Elasticsearch 동기화 로직과 관리자 권한 검증을 검증함
 */
@ExtendWith(MockitoExtension.class)
class ItemSearchServiceTest {

    @InjectMocks
    private ItemSearchService itemSearchService;

    @Mock
    private ItemSearchRepository itemSearchRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Nested
    @DisplayName("관리자용 전체 동기화 테스트 (requesterId 있음)")
    class SyncWithRequester {

        @Test
        @DisplayName("요청자가 ADMIN이면 동기화에 성공한다")
        void syncItemsToElasticsearch_admin_success() {
            // given
            Long requesterId = 1L;

            User admin = mock(User.class);
            given(admin.getUserRole()).willReturn(UserRole.ADMIN);
            given(userService.findById(requesterId)).willReturn(admin);

            Item item = Item.builder()
                    .name("동기화상품")
                    .price(10000)
                    .quantity(5)
                    .itemCategory(ItemCategory.ELECTRONICS)
                    .seller(mock(User.class))
                    .build();
            ReflectionTestUtils.setField(item, "id", 100L);

            given(itemRepository.findAll()).willReturn(List.of(item));

            // when
            itemSearchService.syncItemsToElasticsearch(requesterId);

            // then
            verify(itemSearchRepository, times(1)).deleteAll();
            verify(itemSearchRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("요청자가 ADMIN이 아니면 예외가 발생하고 색인에는 접근하지 않는다")
        void syncItemsToElasticsearch_notAdmin_fail() {
            // given
            Long requesterId = 2L;

            User customer = mock(User.class);
            given(customer.getUserRole()).willReturn(UserRole.CUSTOMER);
            given(userService.findById(requesterId)).willReturn(customer);

            // when & then
            assertThatThrownBy(() -> itemSearchService.syncItemsToElasticsearch(requesterId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("관리자만 실행할 수 있습니다.");

            verifyNoInteractions(itemSearchRepository);
            verifyNoInteractions(itemRepository);
        }

        @Test
        @DisplayName("요청자를 찾을 수 없으면 예외가 그대로 전파된다")
        void syncItemsToElasticsearch_userNotFound_propagates() {
            // given
            Long requesterId = 999L;
            given(userService.findById(requesterId))
                    .willThrow(new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // when & then
            assertThatThrownBy(() -> itemSearchService.syncItemsToElasticsearch(requesterId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자를 찾을 수 없습니다.");

            verifyNoInteractions(itemSearchRepository);
            verifyNoInteractions(itemRepository);
        }
    }

    @Nested
    @DisplayName("내부용 전체 동기화 테스트 (requesterId 없음)")
    class SyncWithoutRequester {

        @Test
        @DisplayName("권한 검사 없이 동기화가 수행된다")
        void syncItemsToElasticsearch_noArgs_success() {
            // given
            Item item = Item.builder()
                    .name("동기화상품")
                    .price(10000)
                    .quantity(5)
                    .itemCategory(ItemCategory.ELECTRONICS)
                    .seller(mock(User.class))
                    .build();
            ReflectionTestUtils.setField(item, "id", 200L);

            given(itemRepository.findAll()).willReturn(List.of(item));

            // when
            itemSearchService.syncItemsToElasticsearch();

            // then
            verify(itemSearchRepository, times(1)).deleteAll();
            verify(itemSearchRepository, times(1)).saveAll(anyList());
            verifyNoInteractions(userService);
        }
    }
}
