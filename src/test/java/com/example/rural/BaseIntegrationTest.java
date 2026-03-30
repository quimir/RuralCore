package com.example.rural;

import com.example.rural.common.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * 集成测试基类
 *
 * 提供:
 *   1. MockMvc 实例 — 发送HTTP请求
 *   2. 注册/登录快捷方法 — 返回JWT Token
 *   3. JSON解析工具方法
 *   4. H2内存数据库（无需外部MySQL）
 *
 * 使用方式:
 *   class XxxTest extends BaseIntegrationTest { ... }
 *
 * 每个测试类独立运行，通过 @Transactional 或手动清理保证隔离性。
 * DataInitializer 会在每次启动时自动创建 admin/bank01/insure01 账号。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ======================== 认证相关工具方法 ========================

    /**
     * 注册用户（忽略已存在错误）
     */
    protected void register(String username, String password, String nickname, String role) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "username", username,
                "password", password,
                "confirmPassword", password,
                "nickname", nickname,
                "role", role != null ? role : "TOURIST"
        ));
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
        // 忽略结果，可能已注册
    }

    /**
     * 登录并获取JWT Token
     */
    protected String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "username", username,
                "password", password
        ));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("token").asText();
    }

    /**
     * 登录并获取完整的登录响应（含用户ID）
     */
    protected JsonNode loginFull(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "username", username,
                "password", password
        ));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    // ======================== HTTP请求工具方法 ========================

    /**
     * 发送GET请求（无认证）
     */
    protected MvcResult doGet(String url) throws Exception {
        return mockMvc.perform(get(url)).andReturn();
    }

    /**
     * 发送GET请求（带Token）
     */
    protected MvcResult doGet(String url, String token) throws Exception {
        return mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
    }

    /**
     * 发送POST请求（带Token和JSON Body）
     */
    protected MvcResult doPost(String url, String token, Object body) throws Exception {
        return mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    /**
     * 发送POST请求（无认证）
     */
    protected MvcResult doPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    /**
     * 发送PUT请求（带Token和JSON Body）
     */
    protected MvcResult doPut(String url, String token, Object body) throws Exception {
        return mockMvc.perform(put(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    /**
     * 发送PUT请求（带Token，无Body）
     */
    protected MvcResult doPut(String url, String token) throws Exception {
        return mockMvc.perform(put(url)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
    }

    /**
     * 发送PATCH请求（带Token和JSON Body）
     */
    protected MvcResult doPatch(String url, String token, Object body) throws Exception {
        return mockMvc.perform(patch(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    /**
     * 发送DELETE请求（带Token）
     */
    protected MvcResult doDelete(String url, String token) throws Exception {
        return mockMvc.perform(delete(url)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
    }

    // ======================== 响应解析工具方法 ========================

    /**
     * 解析响应JSON
     */
    protected JsonNode parseResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /**
     * 获取响应中的code字段
     */
    protected int getCode(MvcResult result) throws Exception {
        return parseResponse(result).path("code").asInt();
    }

    /**
     * 获取响应中的data字段
     */
    protected JsonNode getData(MvcResult result) throws Exception {
        return parseResponse(result).path("data");
    }
}
