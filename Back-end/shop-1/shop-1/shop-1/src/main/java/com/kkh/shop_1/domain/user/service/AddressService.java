package com.kkh.shop_1.domain.user.service;

import com.kkh.shop_1.domain.user.dto.response.AddressResponseDTO;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 *
 * 유저 배송지 관리 서비스
 *
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;

    /**
     *
     * 배송지 ID로 엔티티 조회
     *
     */
    public Optional<Address> findById(Long addressId) {
        return addressRepository.findById(addressId);
    }

    /**
     *
     * 배송지 저장
     *
     */
    @Transactional
    public void save(Address address) {
        addressRepository.save(address);
    }

    /**
     *
     * 특정 유저의 배송지 목록을 DTO로 변환하여 조회
     *
     */
    public List<AddressResponseDTO> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponseDTO::from)
                .toList();
    }

    /**
     *
     * 특정 유저의 배송지 엔티티 목록 조회 (내부 로직용)
     *
     */
    public List<Address> findByUserId(Long userId) {
        return addressRepository.findByUserId(userId);
    }

    /**
     *
     * 기본 배송지 설정 (추가 제안)
     *
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