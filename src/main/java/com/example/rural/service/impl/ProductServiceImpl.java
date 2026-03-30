package com.example.rural.service.impl;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.ProductImageRequest;
import com.example.rural.dto.request.ProductQueryRequest;
import com.example.rural.dto.request.ProductRequest;
import com.example.rural.dto.response.ProductCategoryResponse;
import com.example.rural.dto.response.ProductImageResponse;
import com.example.rural.dto.response.ProductResponse;
import com.example.rural.entity.Product;
import com.example.rural.entity.ProductCategory;
import com.example.rural.entity.ProductImage;
import com.example.rural.entity.User;
import com.example.rural.exception.BusinessException;
import com.example.rural.repository.ProductCategoryRepository;
import com.example.rural.repository.ProductImageRepository;
import com.example.rural.repository.ProductRepository;
import com.example.rural.repository.UserRepository;
import com.example.rural.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * 农产品服务实现
 *
 * 数据流转示意:
 *
 *   前端 JSON → ProductRequest(DTO) → ProductService → Product(Entity) → MySQL
 *   MySQL → Product(Entity) → ProductService → ProductResponse(DTO) → 前端 JSON
 *
 * 为什么要用 DTO 而不直接用 Entity？
 *   1. Entity 包含数据库细节（如 @Table 注解），不应暴露给前端
 *   2. Response 可以附加额外信息（如 categoryName、sellerName）
 *   3. Request 可以加校验注解（@NotBlank 等），Entity 不适合加
 *   4. 前端和数据库字段不一定一一对应，DTO 做中间适配
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;

    // ======================== 产品 CRUD ========================

    /**
     * 发布农产品
     *
     * 数据流: ProductRequest(DTO) → Product(Entity) → save → ProductResponse(DTO)
     */
    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long sellerId) {
        // 校验分类是否存在
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException("分类不存在"));
        }

        // DTO → Entity
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .unit(request.getUnit())
                .origin(request.getOrigin())
                .categoryId(request.getCategoryId())
                .sellerId(sellerId)
                .imageUrl(request.getImageUrl())
                .tags(normalizeTags(request.getTags()))
                .status(Product.Status.ON_SALE)
                .salesCount(0)
                .buyerCount(0)
                .build();

        Product saved = productRepository.save(product);
        log.info("农产品发布成功: id={}, name={}, sellerId={}", saved.getId(), saved.getName(), sellerId);

        // Entity → Response DTO
        return toProductResponse(saved);
    }

    /**
     * 全量更新农产品信息（PUT 语义，所有字段替换）
     */
    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, Long currentUserId) {
        Product product = findProductOrThrow(productId);
        if (!product.getSellerId().equals(currentUserId)) {
            throw new BusinessException(403, "只能修改自己发布的产品");
        }
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException("分类不存在"));
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setUnit(request.getUnit());
        product.setOrigin(request.getOrigin());
        product.setCategoryId(request.getCategoryId());
        product.setImageUrl(request.getImageUrl());
        product.setTags(normalizeTags(request.getTags()));

        autoUpdateStockStatus(product);
        Product saved = productRepository.save(product);
        log.info("农产品全量更新: id={}", productId);
        return toProductResponse(saved);
    }

    /**
     * 部分更新农产品（PATCH 语义，只更新非 null 字段）
     *
     * 典型场景:
     *   补货:   PATCH { "stock": 500 }           → 库存变500, 售罄自动恢复上架
     *   调价:   PATCH { "price": 3.99 }           → 只改价格
     *   加标签: PATCH { "tags": "有机,绿色食品" }  → 只改标签
     *   多字段: PATCH { "price": 3.99, "stock": 1000 }
     */
    @Override
    @Transactional
    public ProductResponse patchProduct(Long productId, ProductRequest request, Long currentUserId) {
        Product product = findProductOrThrow(productId);
        if (!product.getSellerId().equals(currentUserId)) {
            throw new BusinessException(403, "只能修改自己发布的产品");
        }

        // 只更新非 null 字段
        if (request.getName() != null)        product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) {
            if (request.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new BusinessException("价格必须大于0");
            }
            product.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            if (request.getStock() < 0) {
                throw new BusinessException("库存不能为负数");
            }
            product.setStock(request.getStock());
        }
        if (request.getUnit() != null)       product.setUnit(request.getUnit());
        if (request.getOrigin() != null)     product.setOrigin(request.getOrigin());
        if (request.getImageUrl() != null)   product.setImageUrl(request.getImageUrl());
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException("分类不存在"));
            product.setCategoryId(request.getCategoryId());
        }
        // tags 允许传空字符串来清除标签
        if (request.getTags() != null)       product.setTags(normalizeTags(request.getTags()));

        autoUpdateStockStatus(product);
        Product saved = productRepository.save(product);
        log.info("农产品部分更新: id={}, 变更字段由前端决定", productId);
        return toProductResponse(saved);
    }

    /**
     * 获取系统中所有使用过的标签（去重）
     *
     * 从所有商品的 tags 字段中提取，逗号分割后去重排序。
     * 用于前端标签选择器 / 筛选下拉框。
     */
    @Override
    public List<String> getAllTags() {
        return productRepository.findAll().stream()
                .map(Product::getTags)
                .filter(t -> t != null && !t.isBlank())
                .flatMap(t -> java.util.Arrays.stream(t.split(",")))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * 上架/下架
     */
    @Override
    @Transactional
    public void updateProductStatus(Long productId, String status, Long currentUserId) {
        Product product = findProductOrThrow(productId);

        if (!product.getSellerId().equals(currentUserId)) {
            throw new BusinessException(403, "只能操作自己发布的产品");
        }

        try {
            product.setStatus(Product.Status.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的状态: " + status + "（可选: ON_SALE, OFF_SHELF）");
        }

        productRepository.save(product);
        log.info("产品状态变更: id={}, status={}", productId, status);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId, Long currentUserId) {
        Product product = findProductOrThrow(productId);

        // 发布者本人或管理员可删除
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!product.getSellerId().equals(currentUserId)
                && currentUser.getRole() != User.Role.ADMIN) {
            throw new BusinessException(403, "无权删除此产品");
        }

        productRepository.deleteById(productId);
        log.info("产品删除: id={}, operator={}", productId, currentUserId);
    }

    /**
     * 获取产品详情（公开接口）
     * 包含详情图片列表
     */
    @Override
    public ProductResponse getProductById(Long productId) {
        Product product = findProductOrThrow(productId);
        ProductResponse response = toProductResponse(product);
        // 附带详情图片
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        response.setDetailImages(images.stream().map(this::toImageResponse).toList());
        return response;
    }

    /**
     * 分页查询产品列表（公开接口，支持多条件筛选）
     *
     * 这是最复杂的查询，使用 JPA Specification 动态拼接 WHERE 条件：
     *
     *   SELECT * FROM product
     *   WHERE status = 'ON_SALE'                           -- 只展示在售商品
     *     AND name LIKE '%苹果%'                            -- keyword 筛选
     *     AND category_id = 1                              -- 分类筛选
     *     AND price BETWEEN 5 AND 50                       -- 价格区间
     *     AND origin LIKE '%山东%'                           -- 产地筛选
     *   ORDER BY price ASC                                  -- 排序
     *   LIMIT 0, 10                                         -- 分页
     */
    @Override
    public PageResult<ProductResponse> listProducts(ProductQueryRequest query) {
        // 1. 构造排序
        Sort sort = parseSort(query.getSort());
        PageRequest pageable = PageRequest.of(query.getPage() - 1, query.getSize(), sort);

        // 2. 动态条件拼接
        Specification<Product> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 默认只展示在售商品
            predicates.add(cb.equal(root.get("status"), Product.Status.ON_SALE));

            // 关键词: 匹配名称或描述
            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                String kw = "%" + query.getKeyword() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), kw),
                        cb.like(root.get("description"), kw)
                ));
            }

            // 分类筛选
            if (query.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), query.getCategoryId()));
            }

            // 价格区间
            if (query.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), query.getMinPrice()));
            }
            if (query.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), query.getMaxPrice()));
            }

            // 产地
            if (query.getOrigin() != null && !query.getOrigin().isBlank()) {
                predicates.add(cb.like(root.get("origin"), "%" + query.getOrigin() + "%"));
            }

            // 标签筛选: tags 字段是逗号分隔字符串, 用 LIKE 匹配
            if (query.getTag() != null && !query.getTag().isBlank()) {
                String tag = query.getTag().trim();
                // 匹配三种位置: "有机,..." / "...,有机,..." / "...,有机" / 只有一个标签 "有机"
                predicates.add(cb.or(
                        cb.equal(root.get("tags"), tag),
                        cb.like(root.get("tags"), tag + ",%"),
                        cb.like(root.get("tags"), "%," + tag + ",%"),
                        cb.like(root.get("tags"), "%," + tag)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 3. 执行查询
        Page<Product> page = productRepository.findAll(spec, pageable);
        return PageResult.from(page, this::toProductResponse);
    }

    /**
     * 查询我发布的产品（包含所有状态）
     */
    @Override
    public List<ProductResponse> getMyProducts(Long sellerId) {
        return productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    // ======================== 分类 ========================

    /**
     * 获取分类树
     *
     * 返回结构:
     * [
     *   { id:1, name:"水果", children: [ {id:2, name:"苹果"}, {id:3, name:"柑橘"} ] },
     *   { id:4, name:"蔬菜", children: [ {id:5, name:"叶菜"}, {id:6, name:"根茎"} ] }
     * ]
     */
    @Override
    public List<ProductCategoryResponse> getCategoryTree() {
        List<ProductCategory> topCategories = categoryRepository.findTopCategories();
        return topCategories.stream().map(top -> {
            List<ProductCategory> children = categoryRepository
                    .findByParentIdOrderBySortOrderAsc(top.getId());
            return ProductCategoryResponse.builder()
                    .id(top.getId())
                    .name(top.getName())
                    .parentId(top.getParentId())
                    .sortOrder(top.getSortOrder())
                    .icon(top.getIcon())
                    .children(children.stream().map(this::toCategoryResponse).toList())
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public ProductCategoryResponse createCategory(String name, Long parentId, String icon) {
        if (categoryRepository.existsByName(name)) {
            throw new BusinessException("分类名称已存在");
        }
        if (parentId != null && parentId != 0) {
            categoryRepository.findById(parentId)
                    .orElseThrow(() -> new BusinessException("父分类不存在"));
        }

        ProductCategory category = ProductCategory.builder()
                .name(name)
                .parentId(parentId != null ? parentId : 0L)
                .icon(icon)
                .build();

        ProductCategory saved = categoryRepository.save(category);
        log.info("创建分类: id={}, name={}", saved.getId(), saved.getName());
        return toCategoryResponse(saved);
    }

    // ======================== 私有工具方法 ========================

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "产品不存在"));
    }

    /**
     * Entity → Response DTO
     *
     * 附带分类名称和卖家名称，减少前端额外请求
     */
    private ProductResponse toProductResponse(Product product) {
        // 查分类名
        String categoryName = null;
        if (product.getCategoryId() != null) {
            categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(ProductCategory::getName)
                    .orElse(null);
        }

        // 查卖家名
        String sellerName = userRepository.findById(product.getSellerId())
                .map(User::getNickname)
                .orElse("未知");

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .unit(product.getUnit())
                .origin(product.getOrigin())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus().name())
                .salesCount(product.getSalesCount())
                .buyerCount(product.getBuyerCount())
                .tags(product.getTags())
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .sellerId(product.getSellerId())
                .sellerName(sellerName)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductCategoryResponse toCategoryResponse(ProductCategory category) {
        return ProductCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .sortOrder(category.getSortOrder())
                .icon(category.getIcon())
                .build();
    }

    /**
     * 解析排序参数
     */
    private Sort parseSort(String sortStr) {
        if (sortStr == null) return Sort.by("createdAt").descending();
        return switch (sortStr) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "sales_desc" -> Sort.by("salesCount").descending();
            case "buyers_desc" -> Sort.by("buyerCount").descending();
            default -> Sort.by("createdAt").descending();  // newest
        };
    }

    /**
     * 标签规范化: 去空白、去重、去空项
     *
     * "有机, 绿色食品 , 有机, " → "有机,绿色食品"
     */
    private String normalizeTags(String tags) {
        if (tags == null || tags.isBlank()) return null;
        String normalized = java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * 库存变化后自动更新状态:
     *   stock == 0 → SOLD_OUT
     *   stock > 0 且原来是 SOLD_OUT → ON_SALE（补货自动恢复上架）
     */
    private void autoUpdateStockStatus(Product product) {
        if (product.getStock() == 0) {
            product.setStatus(Product.Status.SOLD_OUT);
        } else if (product.getStatus() == Product.Status.SOLD_OUT) {
            product.setStatus(Product.Status.ON_SALE);
        }
    }

    // ======================== 产品详情图片 ========================

    @Override
    @Transactional
    public ProductImageResponse addProductImage(Long productId, ProductImageRequest request, Long currentUserId) {
        Product product = findProductOrThrow(productId);
        if (!product.getSellerId().equals(currentUserId)) {
            throw new BusinessException(403, "只能为自己发布的产品添加图片");
        }

        // 限制每个产品最多20张详情图
        long count = productImageRepository.countByProductId(productId);
        if (count >= 20) {
            throw new BusinessException("每个产品最多上传20张详情图片");
        }

        ProductImage image = ProductImage.builder()
                .productId(productId)
                .imageUrl(request.getImageUrl())
                .caption(request.getCaption())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .imageType(parseImageType(request.getImageType()))
                .build();

        ProductImage saved = productImageRepository.save(image);
        log.info("产品详情图片添加: productId={}, imageId={}", productId, saved.getId());
        return toImageResponse(saved);
    }

    @Override
    public List<ProductImageResponse> getProductImages(Long productId) {
        findProductOrThrow(productId);
        return productImageRepository.findByProductIdOrderBySortOrderAsc(productId)
                .stream()
                .map(this::toImageResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteProductImage(Long productId, Long imageId, Long currentUserId) {
        Product product = findProductOrThrow(productId);
        if (!product.getSellerId().equals(currentUserId)) {
            throw new BusinessException(403, "只能删除自己产品的图片");
        }

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(404, "图片不存在"));
        if (!image.getProductId().equals(productId)) {
            throw new BusinessException("该图片不属于此产品");
        }

        productImageRepository.deleteById(imageId);
        log.info("产品详情图片删除: productId={}, imageId={}", productId, imageId);
    }

    @Override
    @Transactional
    public List<ProductImageResponse> setProductImages(Long productId, List<ProductImageRequest> images, Long currentUserId) {
        Product product = findProductOrThrow(productId);
        if (!product.getSellerId().equals(currentUserId)) {
            throw new BusinessException(403, "只能设置自己产品的图片");
        }

        if (images.size() > 20) {
            throw new BusinessException("每个产品最多上传20张详情图片");
        }

        // 删除旧图片，批量创建新图片
        productImageRepository.deleteByProductId(productId);

        List<ProductImage> newImages = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            ProductImageRequest req = images.get(i);
            newImages.add(ProductImage.builder()
                    .productId(productId)
                    .imageUrl(req.getImageUrl())
                    .caption(req.getCaption())
                    .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : i)
                    .imageType(parseImageType(req.getImageType()))
                    .build());
        }

        List<ProductImage> saved = productImageRepository.saveAll(newImages);
        log.info("产品详情图片批量设置: productId={}, count={}", productId, saved.size());
        return saved.stream().map(this::toImageResponse).toList();
    }

    private ProductImage.ImageType parseImageType(String type) {
        if (type == null || type.isBlank()) return ProductImage.ImageType.DETAIL;
        try {
            return ProductImage.ImageType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ProductImage.ImageType.DETAIL;
        }
    }

    private ProductImageResponse toImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .productId(image.getProductId())
                .imageUrl(image.getImageUrl())
                .caption(image.getCaption())
                .sortOrder(image.getSortOrder())
                .imageType(image.getImageType().name())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
