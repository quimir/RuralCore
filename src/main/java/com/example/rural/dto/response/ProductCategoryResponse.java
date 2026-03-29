package com.example.rural.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 产品分类响应
 *
 * 支持树形结构，children 包含子分类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryResponse {

    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String icon;

    /** 子分类列表（仅顶级分类会填充） */
    private List<ProductCategoryResponse> children;
}
