package com.example.rural.service;

import java.util.List;

/**
 * 本地图片缓存服务接口
 *
 * 支持在无网络环境下的图片本地存储与管理。
 * 图片可以从本地文件系统导入（而不仅是通过HTTP上传），
 * 也可以将已上传的图片缓存到本地以供离线访问。
 *
 * 存储结构:
 *   local-cache/
 *     └── {userId}/
 *           ├── img_001.jpg
 *           └── img_002.png
 */
public interface LocalImageCacheService {

    /**
     * 从本地文件路径导入图片到缓存目录
     *
     * @param sourceFilePath 源文件在本地的绝对路径
     * @param userId         当前用户ID（隔离存储）
     * @return 缓存后的访问URL（如 /local-cache/{userId}/xxx.jpg）
     */
    String importFromLocalPath(String sourceFilePath, Long userId);

    /**
     * 将一个远程/uploads下的图片缓存到本地
     *
     * @param uploadUrl  已上传图片的URL（如 /uploads/2026/03/01/xxx.jpg）
     * @param userId     当前用户ID
     * @return 本地缓存的访问URL
     */
    String cacheUploadedImage(String uploadUrl, Long userId);

    /**
     * 获取用户本地缓存目录下的所有图片
     *
     * @param userId 用户ID
     * @return 缓存图片URL列表
     */
    List<String> listCachedImages(Long userId);

    /**
     * 删除本地缓存中的某张图片
     *
     * @param filename 文件名
     * @param userId   用户ID
     */
    void deleteCachedImage(String filename, Long userId);

    /**
     * 清空用户的本地图片缓存
     *
     * @param userId 用户ID
     */
    void clearCache(Long userId);
}
