package com.example.rural.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 农产品查询参数
 *
 * 前端请求示例:
 * GET /api/v1/products?keyword=苹果&categoryId=1&minPrice=5&maxPrice=50&page=1&size=10&sort=price_asc
 *
 * 所有参数都是可选的，不传则不筛选
 */
@Data
public class ProductQueryRequest {

    /** 搜索关键词（匹配产品名称和描述） */
    private String keyword;

    /** 分类ID */
    private Long categoryId;

    /** 最低价格 */
    private BigDecimal minPrice;

    /** 最高价格 */
    private BigDecimal maxPrice;

    /** 产地 */
    private String origin;

    /** 标签筛选（匹配包含该标签的商品，如 "有机"） */
    private String tag;

    /** 页码（从1开始） */
    private int page = 1;

    /** 每页条数 */
    private int size = 10;

    /**
     * 排序方式:
     *   price_asc   — 价格从低到高
     *   price_desc  — 价格从高到低
     *   sales_desc  — 销量从高到低
     *   newest      — 最新发布（默认）
     */
    private String sort = "newest";
}
