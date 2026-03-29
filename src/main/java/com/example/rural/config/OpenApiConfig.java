package com.example.rural.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 配置
 *
 * 访问地址:
 *   Swagger UI:     http://localhost:8080/swagger-ui.html
 *                    http://localhost:8080/swagger-ui/index.html
 *   OpenAPI JSON:   http://localhost:8080/v3/api-docs
 *   OpenAPI YAML:   http://localhost:8080/v3/api-docs.yaml
 *
 * 使用方法:
 *   1. 打开 Swagger UI 页面
 *   2. 先调用 POST /api/v1/auth/login 获取 token
 *   3. 点击页面右上角「Authorize 🔒」按钮
 *   4. 输入: Bearer xxxxx（注意 Bearer 后面有空格）
 *   5. 之后所有需要登录的接口就会自动带上 Token
 *
 * 接口分组（按 tag 展示）:
 *   - 认证管理      注册/登录
 *   - 个人中心      个人信息/修改密码
 *   - 农产品管理    CRUD + 分类
 *   - 购物车        加购/改量/删除
 *   - 订单管理      下单/支付/发货/收货
 *   - 旅游推广      景点/评论/收藏
 *   - 管理员-用户   用户管理
 *   - 管理员-旅游   审核旅游项目
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // JWT Bearer 认证方案
        final String securitySchemeName = "Bearer Token";

        return new OpenAPI()
                .info(new Info()
                        .title("乡村振兴管理系统 API")
                        .description("""
                                乡村振兴综合管理平台后端接口文档
                                
                                **模块说明:**
                                - 🔑 认证: 注册、登录（公开）
                                - 🛒 农产品: 浏览（公开）、发布/编辑（需登录）
                                - 🛍️ 购物车: 加购、修改、删除（需登录）
                                - 📦 订单: 下单、支付、发货、收货（需登录）
                                - 🏞️ 旅游: 浏览（公开）、发布（商家/农户）、评论/收藏（需登录）
                                - ⚙️ 管理: 用户管理、旅游审核（仅管理员）
                                
                                **测试流程:**
                                1. 调用 `POST /api/v1/auth/login` 登录获取 token
                                2. 点击右上角 Authorize 按钮，输入 `Bearer {token}`
                                3. 即可测试需要登录的接口
                                
                                **默认管理员:** admin / admin123
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("乡村振兴开发团队")
                                .email("dev@example.com")))

                // 全局添加 JWT 认证（每个接口右侧会出现锁头图标）
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))

                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("在下方输入 JWT Token（不需要加 Bearer 前缀）")));
    }
}
