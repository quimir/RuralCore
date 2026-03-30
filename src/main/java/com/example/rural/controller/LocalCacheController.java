package com.example.rural.controller;

import com.example.rural.common.Result;
import com.example.rural.service.LocalImageCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 本地图片缓存控制器
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  本地缓存接口（需登录）                                               │
 * │                                                                      │
 * │  POST   /api/v1/cache/import-local    从本地路径导入图片               │
 * │  POST   /api/v1/cache/cache-uploaded  缓存已上传的图片到本地           │
 * │  GET    /api/v1/cache/images          列出我的缓存图片                 │
 * │  DELETE /api/v1/cache/images/{name}   删除某张缓存图片                 │
 * │  DELETE /api/v1/cache/images          清空我的缓存                     │
 * │                                                                      │
 * │  缓存图片访问（公开）:                                                 │
 * │  GET /local-cache/{userId}/xxx.jpg    通过静态资源映射直接访问         │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * 使用场景:
 *   1. 离线环境：商户无网络时从本地文件系统导入产品图片
 *   2. 本地缓存：将已上传图片缓存到本地，断网时仍可展示
 *   3. 后续可扩展：网络恢复后批量同步到远程服务器
 */
@RestController
@RequestMapping("/api/v1/cache")
@RequiredArgsConstructor
public class LocalCacheController {

    private final LocalImageCacheService cacheService;

    /**
     * 从本地文件路径导入图片
     *
     * 请求: POST /api/v1/cache/import-local
     * Body: { "filePath": "/home/user/photos/product_front.jpg" }
     * 返回: { "url": "/local-cache/1/a1b2c3d4.jpg" }
     */
    @PostMapping("/import-local")
    public Result<Map<String, String>> importFromLocal(@RequestBody Map<String, String> body,
                                                       Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String filePath = body.get("filePath");
        if (filePath == null || filePath.isBlank()) {
            return Result.error(400, "filePath 不能为空");
        }

        String url = cacheService.importFromLocalPath(filePath, userId);
        return Result.ok("本地导入成功", Map.of("url", url));
    }

    /**
     * 将已上传图片缓存到本地
     *
     * 请求: POST /api/v1/cache/cache-uploaded
     * Body: { "uploadUrl": "/uploads/2026/03/01/xxx.jpg" }
     * 返回: { "url": "/local-cache/1/xxx.jpg" }
     */
    @PostMapping("/cache-uploaded")
    public Result<Map<String, String>> cacheUploaded(@RequestBody Map<String, String> body,
                                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String uploadUrl = body.get("uploadUrl");
        if (uploadUrl == null || uploadUrl.isBlank()) {
            return Result.error(400, "uploadUrl 不能为空");
        }

        String url = cacheService.cacheUploadedImage(uploadUrl, userId);
        return Result.ok("缓存成功", Map.of("url", url));
    }

    /**
     * 列出我的缓存图片
     *
     * 请求: GET /api/v1/cache/images
     * 返回: ["/local-cache/1/a1b2c3d4.jpg", "/local-cache/1/e5f6g7h8.png"]
     */
    @GetMapping("/images")
    public Result<List<String>> listCachedImages(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(cacheService.listCachedImages(userId));
    }

    /**
     * 删除某张缓存图片
     *
     * 请求: DELETE /api/v1/cache/images/a1b2c3d4.jpg
     */
    @DeleteMapping("/images/{filename}")
    public Result<Void> deleteCachedImage(@PathVariable String filename,
                                          Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        cacheService.deleteCachedImage(filename, userId);
        return Result.ok("删除成功", null);
    }

    /**
     * 清空我的所有缓存图片
     *
     * 请求: DELETE /api/v1/cache/images
     */
    @DeleteMapping("/images")
    public Result<Void> clearCache(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        cacheService.clearCache(userId);
        return Result.ok("缓存已清空", null);
    }
}
