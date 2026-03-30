package com.example.rural.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 配置
 *
 * 1. CORS 跨域配置 — 允许前端 (localhost:5173) 访问后端
 * 2. 静态资源映射 — /uploads/** → 本地 uploads/ 目录
 *
 * 上传的图片通过此映射直接访问:
 *   http://localhost:8080/uploads/2026/03/01/a1b2c3d4.jpg
 *   → 读取文件系统 uploads/2026/03/01/a1b2c3d4.jpg
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.local-cache-dir:local-cache}")
    private String localCacheDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // API 接口跨域
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // 上传文件静态资源跨域
        registry.addMapping("/uploads/**")
                .allowedOriginPatterns("http://localhost:*")
                .allowedMethods("GET")
                .maxAge(86400);  // 静态资源缓存24小时

        // 本地缓存静态资源跨域
        registry.addMapping("/local-cache/**")
                .allowedOriginPatterns("http://localhost:*")
                .allowedMethods("GET")
                .maxAge(86400);
    }

    /**
     * 静态资源映射
     *
     * 将 URL 路径 /uploads/** 映射到文件系统的 uploads/ 目录
     * 这样上传的图片可以直接通过 URL 访问，无需额外接口
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/** 映射到本地文件系统
        // 使用绝对路径确保路径解析正确
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath)
                .setCachePeriod(3600);  // 浏览器缓存1小时

        // 本地缓存目录映射: /local-cache/** → local-cache/
        String cacheAbsolutePath = Paths.get(localCacheDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/local-cache/**")
                .addResourceLocations(cacheAbsolutePath)
                .setCachePeriod(3600);
    }
}
