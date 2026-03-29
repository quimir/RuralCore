package com.example.rural.repository;

import com.example.rural.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    /** 查询某个父分类下的所有子分类 */
    List<ProductCategory> findByParentIdOrderBySortOrderAsc(Long parentId);

    /** 查询所有顶级分类 */
    default List<ProductCategory> findTopCategories() {
        return findByParentIdOrderBySortOrderAsc(0L);
    }

    boolean existsByName(String name);
}
