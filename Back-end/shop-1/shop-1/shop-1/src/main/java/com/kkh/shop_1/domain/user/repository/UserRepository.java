package com.kkh.shop_1.domain.user.repository;

import com.kkh.shop_1.domain.user.entity.LoginType;
import com.kkh.shop_1.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByLoginTypeAndSocialId(LoginType kakao, String socialId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

}
