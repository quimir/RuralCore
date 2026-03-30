package com.example.rural.service.impl;

import com.example.rural.exception.BusinessException;
import com.example.rural.service.LocalImageCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 本地图片缓存服务实现
 *
 * 功能说明:
 *   1. 从本地文件系统路径导入图片（不经过HTTP上传，适用于离线场景）
 *   2. 将已上传的图片（uploads/）复制到本地缓存（local-cache/）
 *   3. 管理用户级别的本地缓存目录
 *
 * 存储结构:
 *   local-cache/
 *     └── {userId}/
 *           ├── a1b2c3d4.jpg     ← 从本地导入的图片
 *           └── e5f6g7h8.png     ← 从uploads缓存的图片
 *
 * 访问方式:
 *   GET /local-cache/{userId}/a1b2c3d4.jpg
 *   通过 WebConfig 静态资源映射，无需认证
 */
@Slf4j
@Service
public class LocalImageCacheServiceImpl implements LocalImageCacheService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.local-cache-dir:local-cache}")
    private String localCacheDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public String importFromLocalPath(String sourceFilePath, Long userId) {
        Path sourcePath = Paths.get(sourceFilePath);

        // 安全校验：路径规范化后检查
        Path normalizedSource = sourcePath.toAbsolutePath().normalize();

        if (!Files.exists(normalizedSource)) {
            throw new BusinessException("源文件不存在: " + sourceFilePath);
        }
        if (!Files.isRegularFile(normalizedSource)) {
            throw new BusinessException("源路径不是文件");
        }

        // 文件大小校验
        try {
            if (Files.size(normalizedSource) > MAX_FILE_SIZE) {
                throw new BusinessException("图片大小不能超过 5MB");
            }
        } catch (IOException e) {
            throw new BusinessException("无法读取文件大小");
        }

        // 扩展名校验
        String filename = normalizedSource.getFileName().toString().toLowerCase();
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("不支持的图片格式，仅允许: " + ALLOWED_EXTENSIONS);
        }

        // 生成唯一文件名
        String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;

        // 复制到缓存目录
        Path userCacheDir = getUserCacheDir(userId);
        Path targetPath = userCacheDir.resolve(newFilename);

        try {
            Files.copy(normalizedSource, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("本地图片导入失败: {} → {}", sourceFilePath, targetPath, e);
            throw new BusinessException("图片导入失败，请检查文件权限");
        }

        String url = "/local-cache/" + userId + "/" + newFilename;
        log.info("本地图片导入成功: {} → {}", sourceFilePath, url);
        return url;
    }

    @Override
    public String cacheUploadedImage(String uploadUrl, Long userId) {
        // uploadUrl 格式: /uploads/2026/03/01/xxx.jpg
        if (!uploadUrl.startsWith("/uploads/")) {
            throw new BusinessException("只能缓存已上传的图片（/uploads/开头的路径）");
        }

        // 构建源文件路径
        String relativePath = uploadUrl.substring("/uploads/".length());
        Path sourcePath = Paths.get(uploadDir, relativePath);

        if (!Files.exists(sourcePath)) {
            throw new BusinessException("源图片文件不存在");
        }

        // 保留原文件名
        String originalName = sourcePath.getFileName().toString();
        Path userCacheDir = getUserCacheDir(userId);
        Path targetPath = userCacheDir.resolve(originalName);

        try {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("图片缓存失败: {} → {}", uploadUrl, targetPath, e);
            throw new BusinessException("图片缓存失败");
        }

        String url = "/local-cache/" + userId + "/" + originalName;
        log.info("图片缓存成功: {} → {}", uploadUrl, url);
        return url;
    }

    @Override
    public List<String> listCachedImages(Long userId) {
        Path userCacheDir = Paths.get(localCacheDir, String.valueOf(userId));
        if (!Files.exists(userCacheDir)) {
            return List.of();
        }

        List<String> urls = new ArrayList<>();
        try (Stream<Path> stream = Files.list(userCacheDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return ALLOWED_EXTENSIONS.stream().anyMatch(name::endsWith);
                    })
                    .sorted()
                    .forEach(p -> urls.add("/local-cache/" + userId + "/" + p.getFileName()));
        } catch (IOException e) {
            log.error("读取缓存目录失败: userId={}", userId, e);
        }
        return urls;
    }

    @Override
    public void deleteCachedImage(String filename, Long userId) {
        // 安全校验：防止路径穿越
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new BusinessException("无效的文件名");
        }

        Path filePath = Paths.get(localCacheDir, String.valueOf(userId), filename);
        if (!Files.exists(filePath)) {
            throw new BusinessException(404, "缓存图片不存在");
        }

        try {
            Files.delete(filePath);
            log.info("缓存图片删除: userId={}, filename={}", userId, filename);
        } catch (IOException e) {
            log.error("删除缓存图片失败: {}", filePath, e);
            throw new BusinessException("删除失败");
        }
    }

    @Override
    public void clearCache(Long userId) {
        Path userCacheDir = Paths.get(localCacheDir, String.valueOf(userId));
        if (!Files.exists(userCacheDir)) return;

        try (Stream<Path> stream = Files.list(userCacheDir)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    log.warn("删除缓存文件失败: {}", p, e);
                }
            });
            log.info("清空用户缓存: userId={}", userId);
        } catch (IOException e) {
            log.error("清空缓存失败: userId={}", userId, e);
            throw new BusinessException("清空缓存失败");
        }
    }

    /**
     * 获取（并创建）用户缓存目录
     */
    private Path getUserCacheDir(Long userId) {
        Path dir = Paths.get(localCacheDir, String.valueOf(userId));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("创建缓存目录失败");
        }
        return dir;
    }
}
