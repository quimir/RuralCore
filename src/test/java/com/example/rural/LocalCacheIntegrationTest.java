package com.example.rural;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 本地图片缓存集成测试
 *
 * 覆盖:
 *   - 从本地路径导入图片
 *   - 缓存已上传图片到本地
 *   - 列出缓存图片
 *   - 删除缓存图片
 *   - 清空缓存
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LocalCacheIntegrationTest extends BaseIntegrationTest {

    @Value("${file.upload-dir:target/test-uploads}")
    private String uploadDir;

    @Value("${file.local-cache-dir:target/test-local-cache}")
    private String localCacheDir;

    private static String farmerToken;
    private static String importedUrl;

    @Test
    @Order(1)
    @DisplayName("初始化: 登录 + 创建测试图片文件")
    void setup() throws Exception {
        register("farmer_cache", "123456", "缓存测试用户", "FARMER");
        farmerToken = login("farmer_cache", "123456");

        // 创建测试用的本地图片文件
        Path testDir = Paths.get("target", "test-images");
        Files.createDirectories(testDir);
        Path testImage = testDir.resolve("test_product.jpg");
        if (!Files.exists(testImage)) {
            // 创建一个小的假JPEG文件（只有文件头）
            byte[] fakeJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                    0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
            Files.write(testImage, fakeJpeg);
        }
    }

    @Test
    @Order(10)
    @DisplayName("1.1 从本地路径导入图片")
    void importFromLocal() throws Exception {
        String absPath = Paths.get("target", "test-images", "test_product.jpg")
                .toAbsolutePath().toString();

        MvcResult result = doPost("/api/v1/cache/import-local", farmerToken,
                Map.of("filePath", absPath));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        String url = data.path("url").asText();
        assertTrue(url.startsWith("/local-cache/"), "URL应以/local-cache/开头");
        assertTrue(url.endsWith(".jpg"), "应保留原扩展名");

        importedUrl = url;
    }

    @Test
    @Order(11)
    @DisplayName("1.2 导入不存在的文件（应失败）")
    void importNonExistent() throws Exception {
        MvcResult result = doPost("/api/v1/cache/import-local", farmerToken,
                Map.of("filePath", "/nonexistent/path/image.jpg"));
        assertEquals(400, getCode(result));
    }

    @Test
    @Order(20)
    @DisplayName("2.1 列出缓存图片")
    void listCachedImages() throws Exception {
        MvcResult result = doGet("/api/v1/cache/images", farmerToken);
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertTrue(data.isArray());
        assertTrue(data.size() >= 1, "至少1张缓存图片");
    }

    @Test
    @Order(30)
    @DisplayName("3.1 删除缓存图片")
    void deleteCachedImage() throws Exception {
        // 从URL中提取文件名
        String filename = importedUrl.substring(importedUrl.lastIndexOf("/") + 1);

        MvcResult result = doDelete("/api/v1/cache/images/" + filename, farmerToken);
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(31)
    @DisplayName("3.2 删除后列表为空")
    void listAfterDelete() throws Exception {
        MvcResult result = doGet("/api/v1/cache/images", farmerToken);
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals(0, data.size(), "缓存应已清空");
    }

    @Test
    @Order(40)
    @DisplayName("4.1 缓存已上传的图片到本地")
    void cacheUploaded() throws Exception {
        // 先创建一个 uploads 目录下的测试文件
        Path uploadsTestDir = Paths.get(uploadDir, "test");
        Files.createDirectories(uploadsTestDir);
        Path testFile = uploadsTestDir.resolve("cached_test.jpg");
        byte[] fakeJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        Files.write(testFile, fakeJpeg);

        MvcResult result = doPost("/api/v1/cache/cache-uploaded", farmerToken,
                Map.of("uploadUrl", "/uploads/test/cached_test.jpg"));
        assertEquals(200, getCode(result));

        String url = getData(result).path("url").asText();
        assertTrue(url.contains("cached_test.jpg"));
    }

    @Test
    @Order(50)
    @DisplayName("5.1 清空所有缓存")
    void clearCache() throws Exception {
        MvcResult result = doDelete("/api/v1/cache/images", farmerToken);
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(51)
    @DisplayName("5.2 清空后列表为空")
    void listAfterClear() throws Exception {
        MvcResult result = doGet("/api/v1/cache/images", farmerToken);
        assertEquals(200, getCode(result));
        assertEquals(0, getData(result).size());
    }
}
