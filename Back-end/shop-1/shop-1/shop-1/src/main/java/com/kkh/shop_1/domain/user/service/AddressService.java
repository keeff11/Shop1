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

    @Transactional
    public AddressResponseDTO createAddress(Long userId, AddressCreateRequestDTO dto) {

        User user = userService.findById(userId);
        Address address = Address.create(
                user,
                dto.getZipCode(),
                dto.getRoadAddress(),
                dto.getDetailAddress(),
                dto.getRecipientName(),
                dto.getRecipientPhone()
        );

        addressRepository.save(address);
        user.addAddress(address);
        return AddressResponseDTO.from(address);
    }
    public List<AddressResponseDTO> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponseDTO::from)
                .toList();
    }


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