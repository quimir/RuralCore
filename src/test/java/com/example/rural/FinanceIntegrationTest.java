package com.example.rural;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 农村金融服务模块集成测试
 *
 * 覆盖:
 *   - 贷款产品发布与浏览
 *   - 贷款申请流程
 *   - 保险产品发布与浏览
 *   - 保险购买
 *   - 金融仪表盘
 *
 * 对应: api-test-finance.http
 *
 * 测试账号（DataInitializer自动创建）:
 *   bank01 / 123456    — 金融服务商
 *   insure01 / 123456  — 保险提供商
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinanceIntegrationTest extends BaseIntegrationTest {

    private static String adminToken;
    private static String farmerToken;
    private static String bankToken;
    private static String insureToken;
    private static Long loanProductId;
    private static Long loanApplicationId;
    private static Long insuranceProductId;

    // ======================== 0. 初始化 ========================

    @Test
    @Order(1)
    @DisplayName("初始化: 登录各角色")
    void setup() throws Exception {
        adminToken = login("admin", "admin123");

        register("farmer_fin", "123456", "张老板FIN", "FARMER");
        farmerToken = login("farmer_fin", "123456");

        bankToken = login("bank01", "123456");
        insureToken = login("insure01", "123456");
    }

    // ======================== 1. 贷款产品 ========================

    @Test
    @Order(10)
    @DisplayName("1.1 金融服务商发布贷款产品")
    void createLoanProduct() throws Exception {
        MvcResult result = doPost("/api/v1/finance/loan-products", bankToken, Map.of(
                "name", "助农春耕贷FIN",
                "description", "专为春耕备产的农户设计",
                "minAmount", 5000,
                "maxAmount", 100000,
                "interestRate", 4.35,
                "minTermMonths", 6,
                "maxTermMonths", 36
        ));
        assertEquals(200, getCode(result));

        loanProductId = getData(result).path("id").asLong();
    }

    @Test
    @Order(11)
    @DisplayName("1.2 浏览贷款产品列表")
    void listLoanProducts() throws Exception {
        MvcResult result = doGet("/api/v1/finance/loan-products", farmerToken);
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertTrue(data.isArray() ? data.size() >= 1 : data.path("records").size() >= 1,
                "至少1个贷款产品");
    }

    // ======================== 2. 贷款申请 ========================

    @Test
    @Order(20)
    @DisplayName("2.1 农户申请贷款")
    void applyLoan() throws Exception {
        MvcResult result = doPost("/api/v1/finance/loan-applications", farmerToken, Map.of(
                "loanProductId", loanProductId,
                "amount", 30000,
                "termMonths", 12,
                "purpose", "购买春耕种子和化肥"
        ));
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals("PENDING", data.path("status").asText());
        loanApplicationId = data.path("id").asLong();
    }

    @Test
    @Order(21)
    @DisplayName("2.2 查看贷款申请详情")
    void viewLoanApplication() throws Exception {
        MvcResult result = doGet("/api/v1/finance/loan-applications/" + loanApplicationId, farmerToken);
        assertEquals(200, getCode(result));

        JsonNode data = getData(result);
        assertEquals(30000, data.path("amount").asInt());
    }

    @Test
    @Order(22)
    @DisplayName("2.3 金融服务商审批贷款")
    void approveLoan() throws Exception {
        MvcResult result = doPut("/api/v1/finance/loan-applications/" + loanApplicationId + "/approve",
                bankToken, Map.of("approved", true, "remark", "审核通过，信用良好"));
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(23)
    @DisplayName("2.4 验证贷款状态为已批准")
    void verifyLoanApproved() throws Exception {
        MvcResult result = doGet("/api/v1/finance/loan-applications/" + loanApplicationId, farmerToken);
        assertEquals(200, getCode(result));

        String status = getData(result).path("status").asText();
        assertTrue(status.equals("APPROVED") || status.equals("DISBURSED"),
                "状态应为已批准或已放款");
    }

    // ======================== 3. 保险产品 ========================

    @Test
    @Order(30)
    @DisplayName("3.1 保险提供商发布保险产品")
    void createInsuranceProduct() throws Exception {
        MvcResult result = doPost("/api/v1/finance/insurance-products", insureToken, Map.of(
                "name", "农作物综合保险FIN",
                "description", "覆盖自然灾害、病虫害等风险",
                "premium", 200.00,
                "coverage", 50000,
                "termMonths", 12
        ));
        assertEquals(200, getCode(result));

        insuranceProductId = getData(result).path("id").asLong();
    }

    @Test
    @Order(31)
    @DisplayName("3.2 浏览保险产品列表")
    void listInsuranceProducts() throws Exception {
        MvcResult result = doGet("/api/v1/finance/insurance-products", farmerToken);
        assertEquals(200, getCode(result));
    }

    @Test
    @Order(32)
    @DisplayName("3.3 农户购买保险")
    void purchaseInsurance() throws Exception {
        MvcResult result = doPost("/api/v1/finance/insurance-policies", farmerToken, Map.of(
                "insuranceProductId", insuranceProductId
        ));
        assertEquals(200, getCode(result));
    }

    // ======================== 4. 金融仪表盘 ========================

    @Test
    @Order(40)
    @DisplayName("4.1 查看金融仪表盘")
    void viewDashboard() throws Exception {
        MvcResult result = doGet("/api/v1/finance/dashboard", farmerToken);
        assertEquals(200, getCode(result));
    }

    // ======================== 5. 权限测试 ========================

    @Test
    @Order(50)
    @DisplayName("5.1 游客不能发布贷款产品")
    void touristCannotCreateLoan() throws Exception {
        register("tourist_fin", "123456", "游客FIN", "TOURIST");
        String touristToken = login("tourist_fin", "123456");

        MvcResult result = doPost("/api/v1/finance/loan-products", touristToken, Map.of(
                "name", "非法贷款产品",
                "minAmount", 1000,
                "maxAmount", 10000,
                "interestRate", 5.0,
                "minTermMonths", 6,
                "maxTermMonths", 12
        ));
        assertTrue(getCode(result) == 403 || getCode(result) == 400,
                "游客应被拒绝发布贷款产品");
    }
}
