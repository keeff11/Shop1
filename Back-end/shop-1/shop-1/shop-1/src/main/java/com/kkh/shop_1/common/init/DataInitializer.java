package com.kkh.shop_1.common.init;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.item.entity.ItemImage;
import com.kkh.shop_1.domain.item.entity.ItemStatus;
import com.kkh.shop_1.domain.item.repository.ItemRepository;
import com.kkh.shop_1.domain.review.entity.Review;
import com.kkh.shop_1.domain.review.repository.ReviewRepository;
import com.kkh.shop_1.domain.user.entity.LoginType;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.entity.UserRole;
import com.kkh.shop_1.domain.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @PostConstruct
    @Transactional
    public void initData() {
        if (userRepository.count() > 0) {
            log.info("ℹ️ [DataInitializer] 데이터가 이미 존재합니다.");
            return;
        }

        log.info("🚀 [DataInitializer] V2 데이터 생성을 시작합니다...");

        Faker faker = new Faker(new Locale("ko"));
        Random random = new Random();

        // 1. 유저 생성
        List<User> allUsers = createUsers(faker);
        List<User> sellers = allUsers.stream().filter(u -> u.getUserRole() == UserRole.SELLER).toList();
        List<User> customers = allUsers.stream().filter(u -> u.getUserRole() == UserRole.CUSTOMER).toList();

        if (sellers.isEmpty()) sellers = List.of(allUsers.get(0));
        if (customers.isEmpty()) customers = List.of(allUsers.get(0));

        // 2. 상품 생성 (v2 JSON 사용)
        List<Item> items = createRealItemsFromJson(sellers, random);

        // 3. 리뷰 생성
        createReviews(items, customers, random, faker);

        log.info("🎉 초기화 완료! 상품 {}개 생성됨.", items.size());
    }

    private List<User> createUsers(Faker faker) {
        List<User> users = new ArrayList<>();
        String pw = passwordEncoder.encode("1234");
        users.add(User.builder().email("admin@test.com").password(pw).nickname("관리자").userRole(UserRole.ADMIN).loginType(LoginType.LOCAL).build());

        for (int i = 1; i <= 30; i++) {
            UserRole role = (i % 3 == 0) ? UserRole.SELLER : UserRole.CUSTOMER;
            users.add(User.builder().email("user" + i + "@test.com").password(pw).nickname("User_" + i).userRole(role).loginType(LoginType.LOCAL).build());
        }
        return userRepository.saveAll(users);
    }

    private List<Item> createRealItemsFromJson(List<User> sellers, Random random) {
        List<Item> items = new ArrayList<>();
        try {
            // v2 JSON 파일 로드 (약 70~80개 데이터)
            ClassPathResource resource = new ClassPathResource("data/items_v2.json");
            InputStream inputStream = resource.getInputStream();
            List<ItemJsonDto> jsonItems = objectMapper.readValue(inputStream, new TypeReference<List<ItemJsonDto>>() {});

            // 반복 횟수 설정 (총 300개 목표 -> 약 4바퀴)
            int targetTotal = 300;
            int loops = (targetTotal / jsonItems.size()) + 1;

            String[] prefixes = {"", "[공식]", "[특가]", "[해외직구]", "[무료배송]", "[한정판]", "[S급]", "[리퍼브]"};

            for (int i = 0; i < loops; i++) {
                // 매 바퀴마다 섞어서 패턴이 보이지 않게 함
                Collections.shuffle(jsonItems);

                for (ItemJsonDto dto : jsonItems) {
                    if (items.size() >= targetTotal) break;

                    User seller = sellers.get(random.nextInt(sellers.size()));

                    // 가격 변동
                    int priceNoise = (int) (dto.getPrice() * (0.9 + random.nextDouble() * 0.2));
                    int finalPrice = (priceNoise / 100) * 100;

                    // 접두사 랜덤 선택 (순차적 반복 X)
                    String prefix = prefixes[random.nextInt(prefixes.length)];
                    String fullName = prefix.isEmpty() ? dto.getName() : prefix + " " + dto.getName();

                    Item item = Item.builder()
                            .name(fullName)
                            .price(finalPrice)
                            .quantity(random.nextInt(100) + 10)
                            .description(dto.getDescription())
                            .itemCategory(ItemCategory.valueOf(dto.getCategory()))
                            .seller(seller)
                            .status(ItemStatus.SELLING)
                            .build();

                    // 깨지지 않는 검증된 URL 사용
                    String safeImageUrl = (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty())
                            ? dto.getImageUrl()
                            : "https://placehold.co/600x400?text=No+Image";

                    item.setThumbnailUrl(safeImageUrl);

                    // 상세 이미지
                    item.addImage(ItemImage.builder().imageUrl(safeImageUrl).isMainImage(true).sortOrder(0).item(item).build());
                    item.addImage(ItemImage.builder().imageUrl(safeImageUrl).isMainImage(false).sortOrder(1).item(item).build());

                    items.add(item);
                }
            }
        } catch (Exception e) {
            log.error("❌ 데이터 로드 실패: ", e);
        }
        return itemRepository.saveAll(items);
    }

    private void createReviews(List<Item> items, List<User> customers, Random random, Faker faker) {
        List<Review> reviews = new ArrayList<>();
        String[] comments = {"좋아요", "배송 빨라요", "가성비 굿", "추천합니다", "별로예요"};

        for (int i = 0; i < 500; i++) {
            reviews.add(Review.builder()
                    .content(comments[random.nextInt(comments.length)] + " " + faker.lorem().sentence())
                    .rating(random.nextInt(5) + 1)
                    .user(customers.get(random.nextInt(customers.size())))
                    .item(items.get(random.nextInt(items.size())))
                    .build());
        }
        reviewRepository.saveAll(reviews);
    }

    @Getter @Setter
    static class ItemJsonDto {
        private String category;
        private String name;
        private int price;
        private String imageUrl;
        private String description;
    }
}