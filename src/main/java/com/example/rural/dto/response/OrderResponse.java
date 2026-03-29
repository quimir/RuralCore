package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应
 *
 * 包含订单头信息 + 订单明细列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNo;
    private String status;
    private BigDecimal totalAmount;

    // ---- 收货信息 ----
    private Long addressId;
    private String shippingAddress;
    private String receiverName;
    private String receiverPhone;
    private String remark;

    // ---- 买家信息 ----
    private Long buyerId;
    private String buyerName;

    // ---- 时间线 ----
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;

    /** 订单明细 */
    private List<OrderItemResponse> items;

    /**
     * 订单明细行
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal unitPrice;
        private Integer quantity;
        private String unit;
        private BigDecimal subtotal;
        private Long sellerId;
        private String sellerName;
    }
}
