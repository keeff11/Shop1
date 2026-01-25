package com.kkh.shop_1.domain.user.service;

import com.kkh.shop_1.domain.user.dto.request.AddressCreateRequestDTO;
import com.kkh.shop_1.domain.user.dto.response.AddressResponseDTO;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserService userService; // [추가] 유저 조회를 위해 필요

    /**
     * 배송지 ID로 엔티티 조회
     */
    public Optional<Address> findById(Long addressId) {
        return addressRepository.findById(addressId);
    }

    /**
     * 배송지 저장 (엔티티 직접 저장용)
     */
    @Transactional
    public void save(Address address) {
        addressRepository.save(address);
    }

    /**
     * [추가] 배송지 신규 등록
     */
    @Transactional
    public AddressResponseDTO createAddress(Long userId, AddressCreateRequestDTO dto) {
        // 1. 유저 조회
        User user = userService.findById(userId);

        // 2. 주소 엔티티 생성 (정적 팩토리 메서드 활용)
        Address address = Address.create(
                user,
                dto.getZipCode(),
                dto.getRoadAddress(),
                dto.getDetailAddress(),
                dto.getRecipientName(),
                dto.getRecipientPhone()
        );

        // 3. 저장
        addressRepository.save(address);
        user.addAddress(address); // (선택) 양방향 연관관계 편의 메서드 호출

        // 4. DTO 변환 및 반환
        return AddressResponseDTO.from(address);
    }

    /**
     * 특정 유저의 배송지 목록을 DTO로 변환하여 조회
     */
    public List<AddressResponseDTO> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponseDTO::from)
                .toList();
    }

    /**
     * 특정 유저의 배송지 엔티티 목록 조회 (내부 로직용)
     */
    public List<Address> findByUserId(Long userId) {
        return addressRepository.findByUserId(userId);
    }

    /**
     * 기본 배송지 설정
     */
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        List<Address> addresses = addressRepository.findByUserId(userId);

        addresses.forEach(address -> {
            if (address.getId().equals(addressId)) {
                address.markAsDefault();
            } else {
                address.unmarkDefault();
            }
        });
    }
}