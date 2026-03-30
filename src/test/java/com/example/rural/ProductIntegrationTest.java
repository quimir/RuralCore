package com.example.rural;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 农产品模块集成测试
 *
 * 覆盖:
 *   - 产品发布（带标签、无标签、标签去重）
 *   - 标签查询与筛选
 *   - PATCH 部分更新（补货、调价、标签增删改）
 *   - 库存状态自动管理（售罄/恢复上架）
 *   - 产品详情图片 CRUD
 *   - 权限验证
 *
 * 对应: api-test-product-tags.http
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductIntegrationTest extends BaseIntegrationTest {

    // 测试过程中保存的状态（static 以便跨方法使用）
    private static String adminToken;
    private static String farmerToken;
    private static String buyerToken;
    private static Long productAppleId;
    private static Long productCabbageId;
    private static Long productCherryId;

    // ======================== 0. 初始化 ========================

    @Test
    @Order(1)
    @DisplayName("0.1 管理员登录")
    void adminLogin() throws Exception {
        adminToken = login("admin", "admin123");
        assertNotNull(adminToken);
        assertFalse(adminToken.isBlank());
    }

    @Test
    @Order(2)
    @DisplayName("0.2 注册商家")
    void registerFarmer() throws Exception {
        register("farmer_pt", "123456", "张三果园", "FARMER");
    }

    @Test
    @Order(3)
    @DisplayName("0.3 商家登录")
    void farmerLogin() throws Exception {
        farmerToken = login("farmer_pt", "123456");
        assertNotNull(farmerToken);
    }

    @Test
    @Order(4)
    @DisplayName("0.4 注册买家")
    void registerBuyer() throws Exception {
        register("buyer_pt", "123456", "购物达人小王", "TOURIST");
    }

    @Test
    @Order(5)
    @DisplayName("0.5 买家登录")
    void buyerLogin() throws Exception {
        buyerToken = login("buyer_pt", "123456");
        assertNotNull(buyerToken);
    }

    @Test
    @Order(6)
    @DisplayName("0.6 创建分类")
    void createCategory() throws Exception {
        MvcResult result = doPost("/api/v1/admin/categories", adminToken,
                Map.of("name", "测试水果PT", "parentId", 0));
        assertEquals(200, getCode(result));
    }

    // ======================== 1. 发布带标签的产品 ========================

    @Test
    @Order(10)
    @DisplayName("1.1 发布产品 — 带标签")
    void createProductWithTags() throws Exception {
        MvcResult result = doPost("/api/v1/products", farmerToken, Map.of(
                "name", "红富士苹果PT",
                "description", "山东烟台正宗红富士，脆甜多汁",
                "price", 5.50,
                "stock", 100,
                "unit", "斤",
                "origin", "山东烟台",
                "tags", "有机, 绿色食品, 助农"
        ));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals("有机,绿色食品,助农", data.path("tags").asText());
        assertEquals(0, data.path("buyerCount").asInt());
        assertEquals(0, data.path("salesCount").asInt());
        assertEquals(100, data.path("stock").asInt());

        productAppleId = data.path("id").asLong();
    }

    @Test
    @Order(11)
    @DisplayName("1.2 发布产品 — 无标签")
    void createProductWithoutTags() throws Exception {
        MvcResult result = doPost("/api/v1/products", farmerToken, Map.of(
                "name", "有机大白菜PT",
                "description", "本地种植，无农药",
                "price", 2.00,
                "stock", 200,
                "unit", "斤",
                "origin", "山东烟台"
        ));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertTrue(data.path("tags").isNull() || data.path("tags").isMissingNode(),
                "无标签时应为null");

        productCabbageId = data.path("id").asLong();
    }

    @Test
    @Order(12)
    @DisplayName("1.3 发布产品 — 带重复标签（自动去重）")
    void createProductWithDuplicateTags() throws Exception {
        MvcResult result = doPost("/api/v1/products", farmerToken, Map.of(
                "name", "烟台大樱桃PT",
                "description", "时令鲜果，产地直发",
                "price", 35.00,
                "stock", 50,
                "unit", "斤",
                "origin", "山东烟台",
                "tags", "时令, 有机, 时令, 有机, 助农"
        ));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        String[] tags = data.path("tags").asText().split(",");
        assertEquals(3, tags.length, "去重后应只有3个标签");

        productCherryId = data.path("id").asLong();
    }

    // ======================== 2. 标签查询 ========================

    @Test
    @Order(20)
    @DisplayName("2.1 获取所有标签")
    void getAllTags() throws Exception {
        MvcResult result = doGet("/api/v1/products/tags");
        assertEquals(200, getCode(result));

        JsonNode tags = getData(result);
        assertTrue(tags.isArray());
        assertTrue(tags.size() >= 3, "至少3个标签");
    }

    @Test
    @Order(21)
    @DisplayName("2.2 按标签筛选 — 有机")
    void filterByTagOrganic() throws Exception {
        MvcResult result = doGet("/api/v1/products?tag=有机&page=1&size=10");
        assertEquals(200, getCode(result));

        JsonNode records = getData(result).path("records");
        assertTrue(records.size() >= 2, "至少2个有机产品");
        for (JsonNode p : records) {
            assertTrue(p.path("tags").asText().contains("有机"),
                    p.path("name").asText() + " 应包含有机标签");
        }
    }

    @Test
    @Order(22)
    @DisplayName("2.3 按标签筛选 — 不存在的标签")
    void filterByNonExistentTag() throws Exception {
        MvcResult result = doGet("/api/v1/products?tag=不存在的标签&page=1&size=10");
        JsonNode records = getData(result).path("records");
        assertEquals(0, records.size());
    }

    // ======================== 3. PATCH 补货测试 ========================

    @Test
    @Order(30)
    @DisplayName("3.1 补货: 库存100→500")
    void patchStock() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productAppleId, farmerToken,
                Map.of("stock", 500));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals(500, data.path("stock").asInt());
        assertEquals(5.50, data.path("price").asDouble(), 0.01);
        assertEquals("有机,绿色食品,助农", data.path("tags").asText());
        assertEquals("ON_SALE", data.path("status").asText());
    }

    @Test
    @Order(31)
    @DisplayName("3.2 库存清零→售罄")
    void patchStockToZero() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productAppleId, farmerToken,
                Map.of("stock", 0));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals(0, data.path("stock").asInt());
        assertEquals("SOLD_OUT", data.path("status").asText());
    }

    @Test
    @Order(32)
    @DisplayName("3.3 补货后自动恢复上架")
    void patchStockRestore() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productAppleId, farmerToken,
                Map.of("stock", 300));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals(300, data.path("stock").asInt());
        assertEquals("ON_SALE", data.path("status").asText());
    }

    // ======================== 4. PATCH 调价测试 ========================

    @Test
    @Order(40)
    @DisplayName("4.1 降价: 5.50→3.99")
    void patchPriceDown() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productAppleId, farmerToken,
                Map.of("price", 3.99));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals(3.99, data.path("price").asDouble(), 0.01);
        assertEquals(300, data.path("stock").asInt(), "库存没变");
    }

    @Test
    @Order(41)
    @DisplayName("4.2 调回原价: 3.99→5.50")
    void patchPriceRestore() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productAppleId, farmerToken,
                Map.of("price", 5.50));
        assertEquals(200, getCode(result));
        assertEquals(5.50, getData(result).path("price").asDouble(), 0.01);
    }

    @Test
    @Order(42)
    @DisplayName("4.3 调价为0（应拒绝）")
    void patchPriceZeroRejected() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productAppleId, farmerToken,
                Map.of("price", 0));
        assertEquals(400, getCode(result));
    }

    @Test
    @Order(43)
    @DisplayName("4.4 库存为负数（应拒绝）")
    void patchNegativeStockRejected() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productAppleId, farmerToken,
                Map.of("stock", -10));
        assertEquals(400, getCode(result));
    }

    // ======================== 5. PATCH 标签增删改 ========================

    @Test
    @Order(50)
    @DisplayName("5.1 给无标签商品加标签")
    void patchAddTagsToCabbage() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productCabbageId, farmerToken,
                Map.of("tags", "新鲜蔬菜,本地种植"));
        assertEquals(200, getCode(result));
        assertEquals("新鲜蔬菜,本地种植", getData(result).path("tags").asText());
    }

    @Test
    @Order(51)
    @DisplayName("5.2 清除标签（传空字符串）")
    void patchClearTags() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productCabbageId, farmerToken,
                Map.of("tags", ""));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertTrue(data.path("tags").isNull() || data.path("tags").isMissingNode(),
                "标签已清除");
    }

    @Test
    @Order(52)
    @DisplayName("5.3 同时改价格+标签+库存")
    void patchMultipleFields() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productCabbageId, farmerToken,
                Map.of("price", 1.50, "stock", 500, "tags", "新鲜蔬菜,特价,限时优惠"));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals(1.50, data.path("price").asDouble(), 0.01);
        assertEquals(500, data.path("stock").asInt());
        assertEquals("新鲜蔬菜,特价,限时优惠", data.path("tags").asText());
    }

    // ======================== 6. 权限验证 ========================

    @Test
    @Order(60)
    @DisplayName("6.1 买家不能PATCH别人的产品")
    void buyerCannotPatchOtherProduct() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productAppleId, buyerToken,
                Map.of("price", 0.01));
        assertEquals(403, getCode(result));
    }

    @Test
    @Order(61)
    @DisplayName("6.2 数据完整性检查")
    void dataIntegrityCheck() throws Exception {
        MvcResult result = doGet("/api/v1/products/" + productAppleId);
        JsonNode data = getData(result);

        assertEquals(5.50, data.path("price").asDouble(), 0.01, "价格未被篡改");
        assertEquals("有机,绿色食品,助农", data.path("tags").asText(), "标签未被篡改");
        assertEquals("张三果园", data.path("sellerName").asText(), "卖家信息正确");
    }

    // ======================== 7. 产品详情图片 ========================

    @Test
    @Order(70)
    @DisplayName("7.1 为产品添加详情图片")
    void addProductImage() throws Exception {
        MvcResult result = doPost("/api/v1/products/" + productAppleId + "/images", farmerToken,
                Map.of("imageUrl", "/uploads/test/apple_front.jpg",
                        "caption", "苹果正面图",
                        "sortOrder", 0,
                        "imageType", "DETAIL"));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals("/uploads/test/apple_front.jpg", data.path("imageUrl").asText());
        assertEquals("苹果正面图", data.path("caption").asText());
        assertEquals("DETAIL", data.path("imageType").asText());
    }

    @Test
    @Order(71)
    @DisplayName("7.2 添加第二张图片")
    void addSecondImage() throws Exception {
        MvcResult result = doPost("/api/v1/products/" + productAppleId + "/images", farmerToken,
                Map.of("imageUrl", "/uploads/test/apple_side.jpg",
                        "caption", "苹果侧面图",
                        "sortOrder", 1,
                        "imageType", "DETAIL"));
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(72)
    @DisplayName("7.3 获取产品详情图片列表")
    void getProductImages() throws Exception {
        MvcResult result = doGet("/api/v1/products/" + productAppleId + "/images");
        assertEquals(200, getCode(result));

        JsonNode images = getData(result);
        assertTrue(images.isArray());
        assertEquals(2, images.size(), "应有2张图片");
    }

    @Test
    @Order(73)
    @DisplayName("7.4 获取产品详情时包含图片列表")
    void getProductDetailWithImages() throws Exception {
        MvcResult result = doGet("/api/v1/products/" + productAppleId);
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        JsonNode detailImages = data.path("detailImages");
        assertTrue(detailImages.isArray(), "详情应包含detailImages");
        assertEquals(2, detailImages.size(), "应有2张详情图");
    }

    @Test
    @Order(74)
    @DisplayName("7.5 批量设置产品详情图片（替换全部）")
    void setProductImages() throws Exception {
        MvcResult result = doPut("/api/v1/products/" + productAppleId + "/images", farmerToken,
                List.of(
                        Map.of("imageUrl", "/uploads/test/new1.jpg", "caption", "新图1", "sortOrder", 0),
                        Map.of("imageUrl", "/uploads/test/new2.jpg", "caption", "新图2", "sortOrder", 1),
                        Map.of("imageUrl", "/uploads/test/new3.jpg", "caption", "新图3", "sortOrder", 2)
                ));
        assertEquals(200, getCode(result));

        JsonNode images = getData(result);
        assertEquals(3, images.size(), "批量设置后应有3张图片");
    }

    @Test
    @Order(75)
    @DisplayName("7.6 买家不能添加别人产品的图片")
    void buyerCannotAddImage() throws Exception {
        MvcResult result = doPost("/api/v1/products/" + productAppleId + "/images", buyerToken,
                Map.of("imageUrl", "/uploads/test/hack.jpg", "caption", "非法"));
        assertEquals(403, getCode(result));
    }

    // ======================== 8. 综合场景: 售罄→补货 ========================

    @Test
    @Order(80)
    @DisplayName("8.1 把樱桃库存设为0→售罄")
    void cherrySoldOut() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productCherryId, farmerToken,
                Map.of("stock", 0));
        assertEquals("SOLD_OUT", getData(result).path("status").asText());
    }

    @Test
    @Order(81)
    @DisplayName("8.2 商家补货樱桃→自动恢复上架")
    void cherryRestock() throws Exception {
        MvcResult result = doPatch("/api/v1/products/" + productCherryId, farmerToken,
                Map.of("stock", 100, "price", 30.00, "tags", "时令,有机,助农,新到货"));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals(100, data.path("stock").asInt());
        assertEquals("ON_SALE", data.path("status").asText());
        assertEquals(30.00, data.path("price").asDouble(), 0.01);
        assertTrue(data.path("tags").asText().contains("新到货"));
    }

    @Test
    @Order(82)
    @DisplayName("8.3 补货后可以被搜索到")
    void cherrySearchable() throws Exception {
        MvcResult result = doGet("/api/v1/products?keyword=樱桃PT&page=1&size=10");
        JsonNode records = getData(result).path("records");
        assertTrue(records.size() >= 1, "樱桃应回到搜索结果");
        assertEquals("ON_SALE", records.get(0).path("status").asText());
    }
}
