package com.example.rural.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 分页结果封装
 *
 * 将 Spring Data 的 Page 对象转换为前端友好的格式
 *
 * 响应示例:
 * {
 *   "records": [ ... ],
 *   "total": 100,
 *   "page": 1,
 *   "size": 10,
 *   "totalPages": 10
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码（从1开始） */
    private int page;

    /** 每页条数 */
    private int size;

    /** 总页数 */
    private int totalPages;

    /**
     * 从 Spring Data Page 对象构造，同时做 Entity → DTO 转换
     *
     * 用法:
     *   Page<Product> page = productRepository.findAll(pageable);
     *   PageResult<ProductResponse> result = PageResult.from(page, this::toResponse);
     *
     * @param page      JPA 分页查询结果
     * @param converter Entity → DTO 转换函数
     */
    public static <E, T> PageResult<T> from(Page<E> page, Function<E, T> converter) {
        return PageResult.<T>builder()
                .records(page.getContent().stream().map(converter).toList())
                .total(page.getTotalElements())
                .page(page.getNumber() + 1)   // Spring Data 页码从0开始，前端习惯从1开始
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }
}
