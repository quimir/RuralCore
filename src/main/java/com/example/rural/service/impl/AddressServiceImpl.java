package com.example.rural.service.impl;

import com.example.rural.dto.request.AddressRequest;
import com.example.rural.dto.response.AddressResponse;
import com.example.rural.entity.Address;
import com.example.rural.exception.BusinessException;
import com.example.rural.repository.AddressRepository;
import com.example.rural.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务实现
 *
 * 核心逻辑:
 *   1. 地址归属校验: 所有操作都验证 address.userId == currentUserId
 *   2. 默认地址互斥: 设新默认前先清除旧默认
 *   3. 数量上限: 每用户最多 20 个地址
 *   4. fullAddress 由 Entity @PrePersist/@PreUpdate 自动拼接
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private static final int MAX_ADDRESS_COUNT = 20;

    private final AddressRepository addressRepository;

    @Override
    public List<AddressResponse> listAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        // 数量限制
        long count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException("最多保存" + MAX_ADDRESS_COUNT + "个收货地址");
        }

        // 如果设为默认，先清除旧的
        boolean setDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (setDefault) {
            addressRepository.clearDefaultByUserId(userId);
        }

        // 如果是用户的第一个地址，自动设为默认
        if (count == 0) {
            setDefault = true;
        }

        Address address = Address.builder()
                .userId(userId)
                .receiverName(request.getReceiverName().trim())
                .receiverPhone(request.getReceiverPhone().trim())
                .province(request.getProvince().trim())
                .city(request.getCity().trim())
                .district(request.getDistrict().trim())
                .detail(request.getDetail().trim())
                .isDefault(setDefault)
                .build();

        Address saved = addressRepository.save(address);
        log.info("新增收货地址: userId={}, addressId={}, default={}", userId, saved.getId(), setDefault);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = findAndCheckOwner(userId, addressId);

        // 如果设为默认，先清除旧的
        boolean setDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (setDefault && !address.getIsDefault()) {
            addressRepository.clearDefaultByUserId(userId);
        }

        address.setReceiverName(request.getReceiverName().trim());
        address.setReceiverPhone(request.getReceiverPhone().trim());
        address.setProvince(request.getProvince().trim());
        address.setCity(request.getCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setDetail(request.getDetail().trim());
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }
        // fullAddress 由 @PreUpdate 自动拼接

        Address saved = addressRepository.save(address);
        log.info("修改收货地址: userId={}, addressId={}", userId, addressId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findAndCheckOwner(userId, addressId);
        addressRepository.delete(address);
        log.info("删除收货地址: userId={}, addressId={}", userId, addressId);
    }

    @Override
    @Transactional
    public AddressResponse setDefault(Long userId, Long addressId) {
        Address address = findAndCheckOwner(userId, addressId);

        if (address.getIsDefault()) {
            // 已经是默认，直接返回
            return toResponse(address);
        }

        // 清除旧默认 → 设新默认
        addressRepository.clearDefaultByUserId(userId);
        address.setIsDefault(true);
        Address saved = addressRepository.save(address);
        log.info("设为默认地址: userId={}, addressId={}", userId, addressId);
        return toResponse(saved);
    }

    @Override
    public AddressResponse getAddress(Long userId, Long addressId) {
        Address address = findAndCheckOwner(userId, addressId);
        return toResponse(address);
    }

    // ======================== 内部工具 ========================

    /**
     * 查找地址并校验归属权
     */
    private Address findAndCheckOwner(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(404, "地址不存在"));
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此地址");
        }
        return address;
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .city(address.getCity())
                .district(address.getDistrict())
                .detail(address.getDetail())
                .fullAddress(address.getFullAddress())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
