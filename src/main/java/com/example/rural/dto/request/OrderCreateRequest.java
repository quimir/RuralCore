package com.example.rural.dto.request;

import lombok.Data;

/**
 * 下单请求（从购物车结算）
 *
 * POST /api/v1/orders
 *
 * 两种方式选其一:
 *
 *   方式1（推荐）: 传 addressId → 后端自动查地址簿填入订单
 *     { "addressId": 1, "remark": "请周末配送" }
 *
 *   方式2（兼容旧版）: 手动传地址信息
 *     { "shippingAddress": "浙江省...", "receiverName": "张三",
 *       "receiverPhone": "13800138000", "remark": "..." }
 *
 * 优先级: addressId > 手动地址字段
 * 下单时自动将购物车中「已勾选」的商品转为订单明细。
 */
@Data
public class OrderCreateRequest {

    /**
     * 地址簿ID（推荐方式）
     *
     * 传此字段时 shippingAddress/receiverName/receiverPhone 可不传，
     * 后端自动从地址簿读取并快照到订单。
     */
    private Long addressId;

    /** 收货地址（手动输入方式，addressId 为空时必填） */
    private String shippingAddress;

    /** 收货人姓名（手动输入方式，addressId 为空时必填） */
    private String receiverName;

    /** 收货人电话（手动输入方式，addressId 为空时必填） */
    private String receiverPhone;

    /** 买家备注（可选） */
    private String remark;
}
