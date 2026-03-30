package com.example.rural.repository;

import com.example.rural.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /** 查询某产品的所有详情图（按排序号升序） */
    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    /** 查询某产品指定类型的图片 */
    List<ProductImage> findByProductIdAndImageTypeOrderBySortOrderAsc(Long productId, ProductImage.ImageType imageType);

    /** 删除某产品的所有图片 */
    void deleteByProductId(Long productId);

    /** 统计某产品的图片数量 */
    long countByProductId(Long productId);
}
