package com.example.rural.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发表旅游评论请求
 *
 * 前端调用: POST /api/v1/tourism/spots/{spotId}/reviews
 */
@Data
public class TourismReviewRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer rating;

    /** 评论内容 */
    private String content;

    /** 评论配图（JSON 数组字符串） */
    private String images;
}
