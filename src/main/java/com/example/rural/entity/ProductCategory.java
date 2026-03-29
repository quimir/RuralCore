package com.example.rural.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 农产品分类实体
 *
 * 支持二级分类：parent_id = 0 表示顶级分类
 * 例如:
 *   id=1  name=水果     parent_id=0
 *   id=2  name=苹果     parent_id=1
 *   id=3  name=柑橘     parent_id=1
 *   id=4  name=蔬菜     parent_id=0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_category")
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 分类名称 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 父分类ID，0表示顶级分类 */
    @Column(name = "parent_id")
    @Builder.Default
    private Long parentId = 0L;

    /** 排序权重（越小越靠前） */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /** 分类图标URL */
    @Column(length = 255)
    private String icon;
}
