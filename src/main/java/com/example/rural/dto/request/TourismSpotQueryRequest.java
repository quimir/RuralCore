package com.example.rural.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 旅游项目查询参数
 *
 * 前端调用: GET /api/v1/tourism/spots?keyword=采摘&type=PICKING_GARDEN&sort=rating_desc
 */
@Data
public class TourismSpotQueryRequest {

    /** 关键词搜索（匹配标题、摘要、地址） */
    private String keyword;

    /** 旅游类型筛选 */
    private String type;

    /** 最低票价 */
    private BigDecimal minPrice;

    /** 最高票价 */
    private BigDecimal maxPrice;

    /** 页码 */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /**
     * 排序方式:
     *   newest   — 最新发布（默认）
     *   rating_desc  — 评分最高
     *   views_desc   — 最多浏览
     *   price_asc    — 价格最低
     *   price_desc   — 价格最高
     */
    private String sort = "newest";
}
