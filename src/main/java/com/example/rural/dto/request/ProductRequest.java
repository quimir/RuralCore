package com.example.rural.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 农产品创建/更新请求
 *
 * 用于:
 *   POST /api/v1/products      → 创建时所有必填字段都要传
 *   PUT  /api/v1/products/{id} → 更新时只传需要修改的字段
 */
@Data
public class ProductRequest {

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称最多100个字符")
    private String name;

    @Size(max = 2000, message = "产品描述最多2000个字符")
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    /** 计量单位: 斤、公斤、箱、个 */
    @Size(max = 20)
    private String unit;

    /** 产地 */
    @Size(max = 100)
    private String origin;

    /** 分类ID */
    private Long categoryId;

    /** 主图URL */
    @Size(max = 500)
    private String imageUrl;

    /**
     * 商品标签（逗号分隔）
     * 示例: "有机,绿色食品,助农"
     * 可选, 不传则无标签
     */
    @Size(max = 500, message = "标签总长度不超过500字符")
    private String tags;
}
