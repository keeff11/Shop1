package com.kkh.shop_1.domain.user.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.user.dto.request.UserInfoUpdateRequestDTO;
import com.kkh.shop_1.domain.user.dto.request.UserPasswordUpdateRequestDTO;
import com.kkh.shop_1.domain.user.dto.response.UserInfoResponseDTO;
import com.kkh.shop_1.domain.user.dto.response.UserInfoUpdateResponseDTO;
import com.kkh.shop_1.domain.user.service.UserService;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     *
     * 사용자 정보 조회
     *
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<UserInfoResponseDTO>> getUserInfo(
            @AuthenticationPrincipal Long userId
    ) {
        UserInfoResponseDTO userInfoResponseDTO = userService.findUserInfo(userId);
        return ResponseEntity.ok(ApiResponse.success(userInfoResponseDTO));
    }

    /**
     *
     * 프로필 수정
     *
     */
    @PatchMapping("/my")
    public ResponseEntity<ApiResponse<UserInfoUpdateResponseDTO>> updateUserInfo(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserInfoUpdateRequestDTO userInfoUpdateRequestDTO
            ){
        UserInfoUpdateResponseDTO userInfoUpdateResponseDTO = userService.updateUserProfile(userId, userInfoUpdateRequestDTO);
        return ResponseEntity.ok(ApiResponse.success(userInfoUpdateResponseDTO));
    }

    /**
     *
     * 비밀번호 변경
     *
     */
    @PatchMapping("/my/password")
    public ResponseEntity<ApiResponse<?>> updateUserPassword(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserPasswordUpdateRequestDTO userPasswordUpdateRequestDTO
    ){
        userService.updateUserPassword(userId, userPasswordUpdateRequestDTO);
        return ResponseEntity.ok(ApiResponse.successNoData());
    }

}

