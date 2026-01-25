package com.kkh.shop_1.domain.user.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.user.dto.request.UserInfoUpdateRequestDTO;
import com.kkh.shop_1.domain.user.dto.request.UserPasswordUpdateRequestDTO;
import com.kkh.shop_1.domain.user.dto.response.UserInfoResponseDTO;
import com.kkh.shop_1.domain.user.dto.response.UserInfoUpdateResponseDTO;
import com.kkh.shop_1.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 사용자 정보 조회
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<UserInfoResponseDTO>> getUserInfo(
            @AuthenticationPrincipal Long userId
    ) {
        UserInfoResponseDTO userInfoResponseDTO = userService.findUserInfo(userId);
        return ResponseEntity.ok(ApiResponse.success(userInfoResponseDTO));
    }

    /**
     * [추가] 프로필 수정 (이미지 업로드 포함)
     * 프론트엔드 FormData에 맞춰 @RequestPart 사용
     */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserInfoUpdateResponseDTO>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestPart("nickname") String nickname,
            @RequestPart(value = "profileImg", required = false) MultipartFile profileImg
    ) {
        // 닉네임만 있는 DTO 생성
        UserInfoUpdateRequestDTO requestDTO = new UserInfoUpdateRequestDTO(nickname);

        // 서비스 호출 (이미지 파일 함께 전달)
        UserInfoUpdateResponseDTO responseDTO = userService.updateUserProfile(userId, requestDTO, profileImg);

        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    /**
     * 비밀번호 변경
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