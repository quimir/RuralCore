package com.example.rural.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 产品详情图片请求
 */
@Data
public class ProductImageRequest {

    @NotBlank(message = "图片URL不能为空")
    @Size(max = 500, message = "图片URL最多500个字符")
    private String imageUrl;

    /** 图片说明（可选） */
    @Size(max = 200, message = "图片说明最多200个字符")
    private String caption;

    /** 排序序号（默认0） */
    private Integer sortOrder;

    /** 图片类型: DETAIL(默认), SPEC, ORIGIN */
    private String imageType;
}
