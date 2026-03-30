package com.example.rural.controller;

import com.example.rural.common.PageResult;
import com.example.rural.common.Result;
import com.example.rural.dto.request.ProductImageRequest;
import com.example.rural.dto.request.ProductQueryRequest;
import com.example.rural.dto.request.ProductRequest;
import com.example.rural.dto.response.ProductCategoryResponse;
import com.example.rural.dto.response.ProductImageResponse;
import com.example.rural.dto.response.ProductResponse;
import com.example.rural.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 农产品控制器
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  接口权限一览                                                     │
 * │                                                                  │
 * │  公开（游客可访问）:                                               │
 * │    GET  /api/v1/products              产品列表（分页+筛选+排序）    │
 * │    GET  /api/v1/products/{id}         产品详情                     │
 * │    GET  /api/v1/products/categories   分类树                      │
 * │                                                                  │
 * │  需要登录:                                                        │
 * │    POST /api/v1/products              发布产品                     │
 * │    PUT  /api/v1/products/{id}         更新产品（仅发布者）          │
 * │    PUT  /api/v1/products/{id}/status  上架/下架（仅发布者）         │
 * │    DELETE /api/v1/products/{id}       删除产品（发布者或管理员）     │
 * │    GET  /api/v1/products/mine         我发布的产品                  │
 * │                                                                  │
 * │  管理员:                                                          │
 * │    POST /api/v1/admin/categories      添加分类                     │
 * └──────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ==================== 公开接口 ====================

    /**
     * 分页查询产品列表
     *
     * 请求示例:
     *   GET /api/v1/products?keyword=苹果&categoryId=1&minPrice=5&maxPrice=50&page=1&size=10&sort=price_asc
     *   GET /api/v1/products?tag=有机                        ← 按标签筛选
     *   GET /api/v1/products?sort=buyers_desc                ← 按购买人次排序
     *
     * sort 可选: newest(默认), price_asc, price_desc, sales_desc, buyers_desc
     */
    @GetMapping("/api/v1/products")
    public Result<PageResult<ProductResponse>> listProducts(ProductQueryRequest query) {
        return Result.ok(productService.listProducts(query));
    }

    /**
     * 获取产品详情
     */
    @GetMapping("/api/v1/products/{id}")
    public Result<ProductResponse> getProduct(@PathVariable Long id) {
        return Result.ok(productService.getProductById(id));
    }

    /**
     * 获取分类树
     */
    @GetMapping("/api/v1/products/categories")
    public Result<List<ProductCategoryResponse>> getCategories() {
        return Result.ok(productService.getCategoryTree());
    }

    /**
     * 获取系统中所有标签（去重、排序）
     *
     * 请求: GET /api/v1/products/tags
     * 返回: ["助农", "有机", "时令", "绿色食品", ...]
     *
     * 用途: 前端标签选择器 / 筛选下拉框
     */
    @GetMapping("/api/v1/products/tags")
    public Result<List<String>> getAllTags() {
        return Result.ok(productService.getAllTags());
    }

    // ==================== 需要登录的接口 ====================

    /**
     * 发布农产品
     *
     * 请求: POST /api/v1/products
     * Body: { "name":"红富士苹果", "price":5.5, "stock":100, "unit":"斤", ... }
     */
    @PostMapping("/api/v1/products")
    public Result<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request,
                                                 Authentication auth) {
        Long sellerId = (Long) auth.getPrincipal();
        return Result.ok("产品发布成功", productService.createProduct(request, sellerId));
    }

    /**
     * 全量更新农产品（PUT）
     *
     * 请求: PUT /api/v1/products/1
     */
    @PutMapping("/api/v1/products/{id}")
    public Result<ProductResponse> updateProduct(@PathVariable Long id,
                                                 @Valid @RequestBody ProductRequest request,
                                                 Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        return Result.ok("产品更新成功", productService.updateProduct(id, request, currentUserId));
    }

    /**
     * 部分更新农产品（PATCH — 只传要改的字段）
     *
     * 使用场景:
     *   补货:     PATCH { "stock": 500 }
     *   调价:     PATCH { "price": 3.99 }
     *   加标签:   PATCH { "tags": "有机,绿色食品" }
     *   清除标签: PATCH { "tags": "" }
     *   多字段:   PATCH { "price": 3.99, "stock": 1000, "tags": "特价,限时" }
     */
    @PatchMapping("/api/v1/products/{id}")
    public Result<ProductResponse> patchProduct(@PathVariable Long id,
                                                @RequestBody ProductRequest request,
                                                Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        return Result.ok("产品更新成功", productService.patchProduct(id, request, currentUserId));
    }

    /**
     * 上架/下架产品
     *
     * 请求: PUT /api/v1/products/1/status
     * Body: { "status": "OFF_SHELF" }
     */
    @PutMapping("/api/v1/products/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestBody Map<String, String> body,
                                     Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        productService.updateProductStatus(id, body.get("status"), currentUserId);
        return Result.ok("状态更新成功", null);
    }

    /**
     * 删除产品（发布者或管理员）
     *
     * 请求: DELETE /api/v1/products/1
     */
    @DeleteMapping("/api/v1/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id, Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        productService.deleteProduct(id, currentUserId);
        return Result.ok("产品已删除", null);
    }

    /**
     * 查询我发布的所有产品（包含所有状态）
     *
     * 请求: GET /api/v1/products/mine
     */
    @GetMapping("/api/v1/products/mine")
    public Result<List<ProductResponse>> getMyProducts(Authentication auth) {
        Long sellerId = (Long) auth.getPrincipal();
        return Result.ok(productService.getMyProducts(sellerId));
    }

    // ==================== 产品详情图片接口 ====================

    /**
     * 获取产品的所有详情图片（公开）
     *
     * 请求: GET /api/v1/products/1/images
     */
    @GetMapping("/api/v1/products/{id}/images")
    public Result<List<ProductImageResponse>> getProductImages(@PathVariable Long id) {
        return Result.ok(productService.getProductImages(id));
    }

    /**
     * 为产品添加一张详情图片（需登录，仅发布者）
     *
     * 请求: POST /api/v1/products/1/images
     * Body: { "imageUrl": "/uploads/2026/03/01/xxx.jpg", "caption": "产品正面", "imageType": "DETAIL" }
     */
    @PostMapping("/api/v1/products/{id}/images")
    public Result<ProductImageResponse> addProductImage(@PathVariable Long id,
                                                        @Valid @RequestBody ProductImageRequest request,
                                                        Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        return Result.ok("图片添加成功", productService.addProductImage(id, request, currentUserId));
    }

    /**
     * 删除产品的一张详情图片（需登录，仅发布者）
     *
     * 请求: DELETE /api/v1/products/1/images/5
     */
    @DeleteMapping("/api/v1/products/{id}/images/{imageId}")
    public Result<Void> deleteProductImage(@PathVariable Long id,
                                           @PathVariable Long imageId,
                                           Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        productService.deleteProductImage(id, imageId, currentUserId);
        return Result.ok("图片已删除", null);
    }

    /**
     * 批量设置产品详情图片（替换所有现有图片，需登录，仅发布者）
     *
     * 请求: PUT /api/v1/products/1/images
     * Body: [ { "imageUrl": "...", "caption": "...", "sortOrder": 0 }, ... ]
     */
    @PutMapping("/api/v1/products/{id}/images")
    public Result<List<ProductImageResponse>> setProductImages(@PathVariable Long id,
                                                               @RequestBody List<ProductImageRequest> images,
                                                               Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        return Result.ok("图片设置成功", productService.setProductImages(id, images, currentUserId));
    }

    // ==================== 管理员接口 ====================

    /**
     * 添加产品分类（管理员）
     *
     * 请求: POST /api/v1/admin/categories
     * Body: { "name": "水果", "parentId": 0, "icon": "/icons/fruit.png" }
     */
    @PostMapping("/api/v1/admin/categories")
    public Result<ProductCategoryResponse> createCategory(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : 0L;
        String icon = (String) body.get("icon");

        if (name == null || name.isBlank()) {
            return Result.error(400, "分类名称不能为空");
        }

        return Result.ok("分类创建成功", productService.createCategory(name, parentId, icon));
    }
}
