package com.example.rural.service;

import com.example.rural.dto.request.AddressRequest;
import com.example.rural.dto.response.AddressResponse;

import java.util.List;

/**
 * 收货地址服务接口
 *
 * 业务规则:
 *   - 每个用户最多 20 个收货地址
 *   - 每个用户最多 1 个默认地址
 *   - 新增时若设为默认，自动取消旧的默认
 *   - 删除默认地址后不自动设新默认（由用户自行选择）
 *   - 地址只能本人操作（userId 绑定）
 */
public interface AddressService {

    /** 获取当前用户的地址列表（默认地址排第一） */
    List<AddressResponse> listAddresses(Long userId);

    /** 新增收货地址 */
    AddressResponse createAddress(Long userId, AddressRequest request);

    /** 修改收货地址 */
    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    /** 删除收货地址 */
    void deleteAddress(Long userId, Long addressId);

    /** 设为默认地址 */
    AddressResponse setDefault(Long userId, Long addressId);

    /** 获取单个地址详情（下单时内部使用） */
    AddressResponse getAddress(Long userId, Long addressId);
}
