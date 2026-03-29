package com.example.rural.controller;

import com.example.rural.common.Result;
import com.example.rural.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │  POST /api/v1/upload/image   上传图片（需登录）           │
 * │                                                         │
 * │  存储结构:                                               │
 * │    uploads/                                              │
 * │      └── 2026/03/01/                                    │
 * │            ├── a1b2c3d4.jpg                              │
 * │            └── e5f6g7h8.png                              │
 * │                                                         │
 * │  访问方式:                                               │
 * │    GET /uploads/2026/03/01/a1b2c3d4.jpg                 │
 * │    (通过 WebConfig 静态资源映射, 无需认证)               │
 * └─────────────────────────────────────────────────────────┘
 *
 * 前端对接:
 *   const formData = new FormData();
 *   formData.append("file", file);
 *   const res = await api.post("/upload/image", formData, {
 *     headers: { "Content-Type": "multipart/form-data" }
 *   });
 *   // res.data.data.url → "/uploads/2026/03/01/a1b2c3d4.jpg"
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
public class FileUploadController {

    /** 上传目录, 可通过 application.yml 的 file.upload-dir 配置 */
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /** 允许的图片 MIME 类型 */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    /** 最大文件大小: 5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传图片
     *
     * @param file 图片文件 (表单字段名: "file")
     * @return { url: "/uploads/2026/03/01/xxx.jpg", filename: "xxx.jpg" }
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {

        // 1. 空文件校验
        if (file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        // 2. 文件类型校验
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("只允许上传图片文件 (JPG/PNG/GIF/WebP)");
        }

        // 3. 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }

        // 4. 生成唯一文件名 (UUID + 原始扩展名)
        String originalFilename = file.getOriginalFilename();
        String ext = ".jpg"; // 默认扩展名
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        // 5. 按日期分目录: uploads/2026/03/01/
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        try {
            // 6. 创建目录 + 写入文件
            Path dir = Paths.get(uploadDir, dateDir);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 7. 返回访问 URL
            String url = "/uploads/" + dateDir + "/" + filename;

            log.info("图片上传成功: {} → {} ({}KB)",
                    originalFilename, url, file.getSize() / 1024);

            return Result.ok("上传成功", Map.of(
                    "url", url,
                    "filename", filename
            ));

        } catch (IOException e) {
            log.error("图片保存失败: {}", e.getMessage(), e);
            throw new BusinessException("图片保存失败，请稍后重试");
        }
    }
}
