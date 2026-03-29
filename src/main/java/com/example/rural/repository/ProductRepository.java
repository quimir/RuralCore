package com.example.rural.repository;

import com.example.rural.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
                                           JpaSpecificationExecutor<Product> {

    /** 查询某商家发布的所有产品 */
    List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    /** 查询某分类下在售的产品数量 */
    long countByCategoryIdAndStatus(Long categoryId, Product.Status status);

    /** 查询某卖家指定状态的产品数量（金融模块信用评分使用） */
    long countBySellerIdAndStatus(Long sellerId, Product.Status status);
}
