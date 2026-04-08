package com.kkh.shop_1.domain.item.service;

import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.item.entity.ItemStatus;
import com.kkh.shop_1.domain.item.repository.ItemRepository;
import com.kkh.shop_1.domain.item.repository.ItemSearchRepository;
import com.kkh.shop_1.domain.user.entity.LoginType;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.entity.UserRole;
import com.kkh.shop_1.domain.user.repository.UserRepository;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ItemServiceConcurrencyTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    @MockBean
    private ItemSearchRepository itemSearchRepository;

    @MockBean
    private ItemSearchService itemSearchService;

    @Test
    @DisplayName("DB 부하(커넥션) 측정: 100명 동시 재고 감소 요청")
    void decreaseStock_Concurrency_Test() throws InterruptedException {

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        User seller = User.builder()
                .loginType(LoginType.LOCAL)
                .email("seller_" + uniqueId + "@test.com")
                .password("1234")
                .nickname("판매자_" + uniqueId)
                .userRole(UserRole.SELLER)
                .build();
        userRepository.save(seller);

        Item testItem = Item.builder()
                .name("특가상품_" + uniqueId)
                .price(1000)
                .quantity(100)
                .itemCategory(ItemCategory.OTHERS)
                .status(ItemStatus.SELLING)
                .seller(seller)
                .build();

        Item savedItem = itemRepository.save(testItem);
        Long itemId = savedItem.getId();

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // =========================================================================
        AtomicInteger maxActiveConnections = new AtomicInteger(0);
        Thread monitorThread = new Thread(() -> {
            try {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                while (latch.getCount() > 0) {
                    int active = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
                    if (active > maxActiveConnections.get()) {
                        maxActiveConnections.set(active); // 최고 부하량 갱신
                    }
                    Thread.sleep(10);
                }
            } catch (Exception e) {}
        });
        // =========================================================================

        System.out.println("[측정 시작] 100명 동시 요청 시 DB 부하 모니터링 시작");
        monitorThread.start();
        long startTime = System.currentTimeMillis();

        // when: 100명 동시 결제 시작
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    itemService.decreaseStock(itemId, 1);
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();

        System.out.println("==================================================");
        System.out.println("[측정 종료] 총 소요 시간: " + (endTime - startTime) + " ms");
        System.out.println("[DB 부하 지표] 최대 동시 사용 DB 커넥션 수: " + maxActiveConnections.get() + " 개");
        System.out.println("==================================================");

        // then
        Item item = itemRepository.findById(itemId).orElseThrow();
        assertThat(item.getQuantity()).isEqualTo(0);
    }
}