package com.example.rural.controller;

import com.example.rural.common.Result;
import com.example.rural.dto.request.AddressRequest;
import com.example.rural.dto.response.AddressResponse;
import com.example.rural.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址控制器
 *
 * 所有接口需登录，地址与当前用户绑定。
 *
 * ┌────────────────────────────────────────────────────────────┐
 * │  GET    /api/v1/addresses              地址列表(默认排前)   │
 * │  POST   /api/v1/addresses              新增地址             │
 * │  PUT    /api/v1/addresses/{id}         修改地址             │
 * │  DELETE /api/v1/addresses/{id}         删除地址             │
 * │  PUT    /api/v1/addresses/{id}/default 设为默认地址         │
 * └────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 获取当前用户的地址列表
     *
     * 默认地址排第一，其余按更新时间倒序
     */
    @GetMapping
    public Result<List<AddressResponse>> listAddresses(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(addressService.listAddresses(userId));
    }

    /**
     * 新增收货地址
     *
     * - 第一个地址自动设为默认
     * - 每用户最多 20 个地址
     * - 传 isDefault: true 会取消旧默认
     */
    @PostMapping
    public Result<AddressResponse> createAddress(@Valid @RequestBody AddressRequest request,
                                                  Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok("地址添加成功", addressService.createAddress(userId, request));
    }

    /**
     * 修改收货地址
     */
    @PutMapping("/{id}")
    public Result<AddressResponse> updateAddress(@PathVariable Long id,
                                                  @Valid @RequestBody AddressRequest request,
                                                  Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok("地址修改成功", addressService.updateAddress(userId, id, request));
    }

    /**
     * 删除收货地址
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        addressService.deleteAddress(userId, id);
        return Result.ok("地址已删除", null);
    }

    /**
     * 设为默认地址
     *
     * 自动取消旧的默认地址
     */
    @PutMapping("/{id}/default")
    public Result<AddressResponse> setDefault(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok("已设为默认地址", addressService.setDefault(userId, id));
    }
}
