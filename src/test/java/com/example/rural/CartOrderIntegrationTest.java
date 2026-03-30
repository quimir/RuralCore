package com.example.rural;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 购物车 + 下单流程集成测试
 *
 * 覆盖:
 *   - 购物车增删改查、数量累加、勾选/取消
 *   - 下单流程（购物车→订单）
 *   - 订单状态流转（待付款→已付款→已发货→已完成）
 *   - 取消订单 + 库存恢复
 *   - 异常场景（空购物车下单、重复支付、越权查看）
 *
 * 对应: api-test-cart-order.http
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CartOrderIntegrationTest extends BaseIntegrationTest {

    private static String adminToken;
    private static String farmerToken;
    private static String buyerToken;
    private static Long productAppleId;
    private static Long productTomatoId;
    private static Long productOrangeId;
    private static Long cartItemAppleId;
    private static Long cartItemOrangeId;
    private static Long orderId;
    private static String orderNo;
    private static Long cancelOrderId;

    // ======================== 0. 初始化 ========================

    @Test
    @Order(1)
    @DisplayName("初始化: 登录 + 发布商品")
    void setup() throws Exception {
        adminToken = login("admin", "admin123");

        register("farmer_co", "123456", "张三果园CO", "MERCHANT");
        farmerToken = login("farmer_co", "123456");

        register("buyer_co", "123456", "李四CO", "TOURIST");
        buyerToken = login("buyer_co", "123456");

        // 创建分类
        doPost("/api/v1/admin/categories", adminToken, Map.of("name", "水果CO", "parentId", 0));

        // 发布3个商品
        MvcResult r1 = doPost("/api/v1/products", farmerToken, Map.of(
                "name", "红富士苹果CO", "price", 5.50, "stock", 100, "unit", "斤", "origin", "山东烟台"));
        productAppleId = getData(r1).path("id").asLong();

        MvcResult r2 = doPost("/api/v1/products", farmerToken, Map.of(
                "name", "有机番茄CO", "price", 8.00, "stock", 50, "unit", "斤", "origin", "山东寿光"));
        productTomatoId = getData(r2).path("id").asLong();

        MvcResult r3 = doPost("/api/v1/products", farmerToken, Map.of(
                "name", "沙糖桔CO", "price", 3.80, "stock", 200, "unit", "斤", "origin", "广西桂林"));
        productOrangeId = getData(r3).path("id").asLong();
    }

    // ======================== 1. 购物车操作 ========================

    @Test
    @Order(10)
    @DisplayName("1.1 加入购物车 — 苹果3斤")
    void addAppleToCart() throws Exception {
        MvcResult result = doPost("/api/v1/cart", buyerToken,
                Map.of("productId", productAppleId, "quantity", 3));
        assertEquals(200, getCode(result));
        assertEquals(3, getData(result).path("quantity").asInt());

        cartItemAppleId = getData(result).path("id").asLong();
    }

    @Test
    @Order(11)
    @DisplayName("1.2 加入购物车 — 番茄2斤")
    void addTomatoToCart() throws Exception {
        MvcResult result = doPost("/api/v1/cart", buyerToken,
                Map.of("productId", productTomatoId, "quantity", 2));
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(12)
    @DisplayName("1.3 加入购物车 — 沙糖桔5斤")
    void addOrangeToCart() throws Exception {
        MvcResult result = doPost("/api/v1/cart", buyerToken,
                Map.of("productId", productOrangeId, "quantity", 5));
        assertEquals(200, getCode(result));

        cartItemOrangeId = getData(result).path("id").asLong();
    }

    @Test
    @Order(13)
    @DisplayName("1.4 再次加入苹果（数量累加）")
    void addAppleAgain() throws Exception {
        MvcResult result = doPost("/api/v1/cart", buyerToken,
                Map.of("productId", productAppleId, "quantity", 2));
        assertEquals(200, getCode(result));
        assertEquals(5, getData(result).path("quantity").asInt(), "数量应累加到5");
    }

    @Test
    @Order(14)
    @DisplayName("1.5 查看购物车")
    void viewCart() throws Exception {
        MvcResult result = doGet("/api/v1/cart", buyerToken);
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals(3, data.path("totalCount").asInt(), "购物车3种商品");
        assertEquals(3, data.path("selectedCount").asInt(), "默认全部选中");
    }

    @Test
    @Order(15)
    @DisplayName("1.6 修改苹果数量为10斤")
    void updateAppleQuantity() throws Exception {
        MvcResult result = doPut("/api/v1/cart/" + cartItemAppleId, buyerToken,
                Map.of("quantity", 10));
        assertEquals(200, getCode(result));
        assertEquals(10, getData(result).path("quantity").asInt());
    }

    @Test
    @Order(16)
    @DisplayName("1.7 取消勾选沙糖桔")
    void deselectOrange() throws Exception {
        MvcResult result = doPut("/api/v1/cart/" + cartItemOrangeId, buyerToken,
                Map.of("selected", false));
        assertEquals(200, getCode(result));
        assertFalse(getData(result).path("selected").asBoolean());
    }

    @Test
    @Order(17)
    @DisplayName("1.8 购物车角标数量")
    void cartCount() throws Exception {
        MvcResult result = doGet("/api/v1/cart/count", buyerToken);
        assertEquals(200, getCode(result));
        assertEquals(3, getData(result).asInt());
    }

    // ======================== 2. 下单流程 ========================

    @Test
    @Order(20)
    @DisplayName("2.1 下单（选中的商品→订单）")
    void createOrder() throws Exception {
        MvcResult result = doPost("/api/v1/orders", buyerToken, Map.of(
                "shippingAddress", "浙江省杭州市西湖区文三路123号",
                "receiverName", "李四",
                "receiverPhone", "13900139000",
                "remark", "请周末配送"
        ));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals("PENDING_PAYMENT", data.path("status").asText());
        assertEquals(2, data.path("items").size(), "应有苹果和番茄2个明细");

        orderId = data.path("id").asLong();
        orderNo = data.path("orderNo").asText();
    }

    @Test
    @Order(21)
    @DisplayName("2.2 下单后购物车清理")
    void cartCleanedAfterOrder() throws Exception {
        MvcResult result = doGet("/api/v1/cart", buyerToken);
        assertEquals(1, getData(result).path("totalCount").asInt(), "只剩沙糖桔");
    }

    @Test
    @Order(22)
    @DisplayName("2.3 下单后库存扣减")
    void stockDeducted() throws Exception {
        MvcResult result = doGet("/api/v1/products/" + productAppleId);
        assertEquals(90, getData(result).path("stock").asInt(), "100 - 10 = 90");
    }

    // ======================== 3. 订单状态流转 ========================

    @Test
    @Order(30)
    @DisplayName("3.1 查看订单详情")
    void viewOrder() throws Exception {
        MvcResult result = doGet("/api/v1/orders/" + orderId, buyerToken);
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals(orderNo, data.path("orderNo").asText());
        assertEquals("李四", data.path("receiverName").asText());
    }

    @Test
    @Order(31)
    @DisplayName("3.2 买家支付")
    void payOrder() throws Exception {
        MvcResult result = doPut("/api/v1/orders/" + orderId + "/pay", buyerToken);
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(32)
    @DisplayName("3.3 验证支付后状态")
    void verifyPaid() throws Exception {
        MvcResult result = doGet("/api/v1/orders/" + orderId, buyerToken);
        assertEquals("PAID", getData(result).path("status").asText());
    }

    @Test
    @Order(33)
    @DisplayName("3.4 卖家发货")
    void shipOrder() throws Exception {
        MvcResult result = doPut("/api/v1/orders/" + orderId + "/ship", farmerToken);
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(34)
    @DisplayName("3.5 验证发货后状态")
    void verifyShipped() throws Exception {
        MvcResult result = doGet("/api/v1/orders/" + orderId, buyerToken);
        assertEquals("SHIPPED", getData(result).path("status").asText());
    }

    @Test
    @Order(35)
    @DisplayName("3.6 买家确认收货")
    void confirmOrder() throws Exception {
        MvcResult result = doPut("/api/v1/orders/" + orderId + "/confirm", buyerToken);
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(36)
    @DisplayName("3.7 验证订单已完成")
    void verifyCompleted() throws Exception {
        MvcResult result = doGet("/api/v1/orders/" + orderId, buyerToken);
        assertEquals("COMPLETED", getData(result).path("status").asText());
    }

    // ======================== 4. 取消订单测试 ========================

    @Test
    @Order(40)
    @DisplayName("4.1 重新选中沙糖桔并下单")
    void createOrderForCancel() throws Exception {
        // 选中沙糖桔
        doPut("/api/v1/cart/" + cartItemOrangeId, buyerToken, Map.of("selected", true));

        // 下单
        MvcResult result = doPost("/api/v1/orders", buyerToken, Map.of(
                "shippingAddress", "上海市浦东新区xxx路",
                "receiverName", "李四",
                "receiverPhone", "13900139000"
        ));
        assertEquals(200, getCode(result));
        cancelOrderId = getData(result).path("id").asLong();
    }

    @Test
    @Order(41)
    @DisplayName("4.2 库存已扣减")
    void orangeStockDeducted() throws Exception {
        MvcResult result = doGet("/api/v1/products/" + productOrangeId);
        assertEquals(195, getData(result).path("stock").asInt());
    }

    @Test
    @Order(42)
    @DisplayName("4.3 取消订单")
    void cancelOrder() throws Exception {
        MvcResult result = doPut("/api/v1/orders/" + cancelOrderId + "/cancel", buyerToken);
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(43)
    @DisplayName("4.4 库存恢复")
    void orangeStockRestored() throws Exception {
        MvcResult result = doGet("/api/v1/products/" + productOrangeId);
        assertEquals(200, getData(result).path("stock").asInt());
    }

    @Test
    @Order(44)
    @DisplayName("4.5 订单状态为CANCELLED")
    void verifyCancelled() throws Exception {
        MvcResult result = doGet("/api/v1/orders/" + cancelOrderId, buyerToken);
        assertEquals("CANCELLED", getData(result).path("status").asText());
    }

    // ======================== 5. 异常场景 ========================

    @Test
    @Order(50)
    @DisplayName("5.1 空购物车不能下单")
    void emptyCartCannotOrder() throws Exception {
        MvcResult result = doPost("/api/v1/orders", buyerToken, Map.of(
                "shippingAddress", "测试地址",
                "receiverName", "测试",
                "receiverPhone", "13800138000"
        ));
        assertEquals(400, getCode(result));
    }

    @Test
    @Order(51)
    @DisplayName("5.2 已完成订单不能再次支付")
    void completedOrderCannotPay() throws Exception {
        MvcResult result = doPut("/api/v1/orders/" + orderId + "/pay", buyerToken);
        assertEquals(400, getCode(result));
    }

    @Test
    @Order(52)
    @DisplayName("5.3 已取消订单不能支付")
    void cancelledOrderCannotPay() throws Exception {
        MvcResult result = doPut("/api/v1/orders/" + cancelOrderId + "/pay", buyerToken);
        assertEquals(400, getCode(result));
    }

    @Test
    @Order(53)
    @DisplayName("5.4 其他用户不能查看别人的订单")
    void otherUserCannotViewOrder() throws Exception {
        MvcResult result = doGet("/api/v1/orders/" + orderId, adminToken);
        assertEquals(403, getCode(result));
    }
}
