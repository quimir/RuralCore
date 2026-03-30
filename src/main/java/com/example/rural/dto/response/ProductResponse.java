package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 农产品响应
 *
 * 包含产品所有前端需要展示的信息，
 * 额外附带分类名称和卖家名称（避免前端再次请求）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String unit;
    private String origin;
    private String imageUrl;
    private String status;
    private Integer salesCount;

    /** 购买人次（订单数） */
    private Integer buyerCount;

    /** 商品标签（逗号分隔），如 "有机,绿色食品,助农" */
    private String tags;

    /** 分类信息 */
    private Long categoryId;
    private String categoryName;

    /** 卖家信息 */
    private Long sellerId;
    private String sellerName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 产品详情图片列表（仅在详情接口中返回） */
    private List<ProductImageResponse> detailImages;
}
