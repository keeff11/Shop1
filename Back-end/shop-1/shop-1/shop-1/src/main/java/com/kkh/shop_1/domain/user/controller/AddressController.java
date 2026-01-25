package com.kkh.shop_1.domain.user.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.user.dto.request.AddressCreateRequestDTO;
import com.kkh.shop_1.domain.user.dto.response.AddressResponseDTO;
import com.kkh.shop_1.domain.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 유저 배송지 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponseDTO>>> getUserAddresses(
            @AuthenticationPrincipal Long userId
    ) {
        List<AddressResponseDTO> response = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * [추가] 배송지 등록
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponseDTO>> createAddress(
            @AuthenticationPrincipal Long userId,
            @RequestBody AddressCreateRequestDTO requestDTO
    ) {
        AddressResponseDTO response = addressService.createAddress(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}