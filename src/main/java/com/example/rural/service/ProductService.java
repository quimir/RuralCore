package com.example.rural.service;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.ProductQueryRequest;
import com.example.rural.dto.request.ProductRequest;
import com.example.rural.dto.response.ProductCategoryResponse;
import com.example.rural.dto.request.ProductImageRequest;
import com.example.rural.dto.response.ProductImageResponse;
import com.example.rural.dto.response.ProductResponse;

import java.util.List;

/**
 * 农产品服务接口
 *
 * 分层设计:
 *
 *  ┌──────────────────────────────────────────────────────────────────┐
 *  │  ProductController                                               │
 *  │    ↓ 接收前端请求参数 (ProductRequest / ProductQueryRequest)       │
 *  │    ↓ 调用 Service 方法                                           │
 *  │    ↓ 包装成 Result<ProductResponse> 返回                         │
 *  ├──────────────────────────────────────────────────────────────────┤
 *  │  ProductService（本接口）                                         │
 *  │    ↓ 业务逻辑: 权限判断、数据校验、Entity↔DTO转换                  │
 *  │    ↓ 调用 Repository                                             │
 *  ├──────────────────────────────────────────────────────────────────┤
 *  │  ProductRepository                                               │
 *  │    ↓ JPA 自动生成 SQL，与 MySQL 交互                              │
 *  ├──────────────────────────────────────────────────────────────────┤
 *  │  Product (Entity) ←→ product 表                                  │
 *  └──────────────────────────────────────────────────────────────────┘
 */
public interface ProductService {

    // ==================== 产品 CRUD ====================

    /** 发布农产品（商家/农户） */
    ProductResponse createProduct(ProductRequest request, Long sellerId);

    /** 更新农产品信息（仅发布者可操作） */
    ProductResponse updateProduct(Long productId, ProductRequest request, Long currentUserId);

    /**
     * 部分更新农产品（PATCH 语义）
     *
     * 只更新请求中非 null 的字段。典型使用场景:
     *   - 补货:   { "stock": 500 }
     *   - 调价:   { "price": 3.99 }
     *   - 加标签: { "tags": "有机,绿色食品" }
     */
    ProductResponse patchProduct(Long productId, ProductRequest request, Long currentUserId);

    /** 获取系统中所有使用过的标签（去重，用于前端标签选择器） */
    List<String> getAllTags();

    /** 下架/上架农产品 */
    void updateProductStatus(Long productId, String status, Long currentUserId);

    /** 删除农产品（仅发布者或管理员） */
    void deleteProduct(Long productId, Long currentUserId);

    /** 获取农产品详情（公开） */
    ProductResponse getProductById(Long productId);

    /** 分页查询农产品列表（公开，支持筛选和排序） */
    PageResult<ProductResponse> listProducts(ProductQueryRequest query);

    /** 查询我发布的农产品 */
    List<ProductResponse> getMyProducts(Long sellerId);

    // ==================== 分类 ====================

    /** 获取分类树（顶级 + 子分类） */
    List<ProductCategoryResponse> getCategoryTree();

    /** 添加分类（管理员） */
    ProductCategoryResponse createCategory(String name, Long parentId, String icon);

    // ==================== 产品详情图片 ====================

    /** 为产品添加详情图片（仅发布者） */
    ProductImageResponse addProductImage(Long productId, ProductImageRequest request, Long currentUserId);

    /** 获取产品的所有详情图片 */
    List<ProductImageResponse> getProductImages(Long productId);

    /** 删除产品详情图片（仅发布者） */
    void deleteProductImage(Long productId, Long imageId, Long currentUserId);

    /** 批量设置产品详情图片（替换所有现有图片） */
    List<ProductImageResponse> setProductImages(Long productId, List<ProductImageRequest> images, Long currentUserId);
}
