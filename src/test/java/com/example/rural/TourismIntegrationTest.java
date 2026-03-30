package com.example.rural;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 旅游推广模块集成测试
 *
 * 覆盖:
 *   - 景点发布与审核
 *   - 景点列表与详情
 *   - 游客评论
 *   - 收藏功能
 *   - 旅游路线规划
 *
 * 对应: api-test-tourism.http + api-test-booking-route.http
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TourismIntegrationTest extends BaseIntegrationTest {

    private static String adminToken;
    private static String farmerToken;
    private static String touristToken;
    private static Long spotId;
    private static Long routeId;

    // ======================== 0. 初始化 ========================

    @Test
    @Order(1)
    @DisplayName("初始化: 登录各角色")
    void setup() throws Exception {
        adminToken = login("admin", "admin123");

        register("farmer_tm", "123456", "张三果园TM", "MERCHANT");
        farmerToken = login("farmer_tm", "123456");

        register("tourist_tm", "123456", "旅行者小王TM", "TOURIST");
        touristToken = login("tourist_tm", "123456");
    }

    // ======================== 1. 景点管理 ========================

    @Test
    @Order(10)
    @DisplayName("1.1 商家发布旅游景点")
    void createSpot() throws Exception {
        MvcResult result = doPost("/api/v1/tourism/spots", farmerToken, Map.of(
                "name", "樱桃采摘园TM",
                "description", "体验采摘乐趣，品尝新鲜樱桃",
                "address", "山东省烟台市牟平区",
                "contactPhone", "13800138000",
                "openingHours", "08:00-18:00",
                "ticketPrice", 50.00
        ));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertNotNull(data.path("id"));
        spotId = data.path("id").asLong();
    }

    @Test
    @Order(11)
    @DisplayName("1.2 管理员审核通过景点")
    void auditSpot() throws Exception {
        MvcResult result = doPut("/api/v1/tourism/spots/" + spotId + "/audit", adminToken,
                Map.of("status", "APPROVED", "auditRemark", "审核通过"));
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(12)
    @DisplayName("1.3 公开查看景点列表")
    void listSpots() throws Exception {
        MvcResult result = doGet("/api/v1/tourism/spots");
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertTrue(data.path("records").size() >= 1, "至少1个景点");
    }

    @Test
    @Order(13)
    @DisplayName("1.4 查看景点详情")
    void getSpotDetail() throws Exception {
        MvcResult result = doGet("/api/v1/tourism/spots/" + spotId);
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertTrue(data.path("name").asText().contains("樱桃采摘园"));
    }

    // ======================== 2. 评论功能 ========================

    @Test
    @Order(20)
    @DisplayName("2.1 游客发表评论")
    void postReview() throws Exception {
        MvcResult result = doPost("/api/v1/tourism/spots/" + spotId + "/reviews", touristToken,
                Map.of("content", "景色非常好，推荐大家来！", "rating", 5));
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(21)
    @DisplayName("2.2 查看景点评论")
    void getReviews() throws Exception {
        MvcResult result = doGet("/api/v1/tourism/spots/" + spotId + "/reviews");
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertTrue(data.isArray() || data.path("records").isArray(),
                "评论列表应为数组");
    }

    // ======================== 3. 收藏功能 ========================

    @Test
    @Order(30)
    @DisplayName("3.1 收藏景点")
    void addFavorite() throws Exception {
        MvcResult result = doPost("/api/v1/tourism/favorites", touristToken,
                Map.of("spotId", spotId));
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(31)
    @DisplayName("3.2 查看我的收藏")
    void listFavorites() throws Exception {
        MvcResult result = doGet("/api/v1/tourism/favorites", touristToken);
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(32)
    @DisplayName("3.3 取消收藏")
    void removeFavorite() throws Exception {
        MvcResult result = doDelete("/api/v1/tourism/favorites/" + spotId, touristToken);
        assertEquals(200, getCode(result));
    }

    // ======================== 4. 旅游路线 ========================

    @Test
    @Order(40)
    @DisplayName("4.1 创建旅游路线")
    void createRoute() throws Exception {
        MvcResult result = doPost("/api/v1/tourism/routes", touristToken, Map.of(
                "name", "烟台一日游TM",
                "description", "体验烟台乡村风情"
        ));
        assertEquals(200, getCode(result));

        routeId = getData(result).path("id").asLong();
    }

    @Test
    @Order(41)
    @DisplayName("4.2 查看路线列表")
    void listRoutes() throws Exception {
        MvcResult result = doGet("/api/v1/tourism/routes/public");
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(42)
    @DisplayName("4.3 查看路线详情")
    void getRouteDetail() throws Exception {
        MvcResult result = doGet("/api/v1/tourism/routes/" + routeId);
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertTrue(data.path("name").asText().contains("烟台一日游"));
    }
}
