package com.example.rural.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发布/编辑旅游项目请求
 *
 * 前端调用:
 *   POST /api/v1/tourism/spots       → 发布
 *   PUT  /api/v1/tourism/spots/{id}  → 编辑
 */
@Data
public class TourismSpotRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    /** 摘要（列表页展示，建议100字以内） */
    private String summary;

    /** 详细内容（富文本/Markdown） */
    private String content;

    /** 旅游类型: SCENIC_SPOT / FARMSTAY / PICKING_GARDEN / AGRI_EXPERIENCE / FOLK_CULTURE */
    @NotNull(message = "类型不能为空")
    private String type;

    /** 封面图 URL */
    private String coverImage;

    /** 多图（JSON 数组字符串） */
    private String images;

    /** 详细地址 */
    private String address;

    /** 经度 */
    private Double longitude;

    /** 纬度 */
    private Double latitude;

    /** 联系电话 */
    private String contactPhone;

    /** 开放时间 */
    private String openingHours;

    /** 门票价格（0=免费） */
    private BigDecimal ticketPrice;

    /** 标签（JSON 数组字符串） */
    private String tags;
}
