package com.example.rural.config;

import com.example.rural.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

/**
 * Spring Security 配置
 *
 * 核心配置：
 * 1. 关闭 Session（使用 JWT 无状态认证）
 * 2. 配置哪些接口公开访问、哪些需要登录
 * 3. 注册 JWT 过滤器
 * 4. 配置密码加密器（BCrypt）
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（前后端分离项目不需要）
                .csrf(csrf -> csrf.disable())

                // 使用无状态 Session（JWT 不需要 Session）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ==================== 接口权限配置 ====================
                .authorizeHttpRequests(auth -> auth

                        // ---- 公开: 认证 ----
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // ---- 公开: 农产品浏览 ----
                        // ⚠️ Spring Security 7.x 必须用 HttpMethod 枚举
                        .requestMatchers(HttpMethod.GET, "/api/v1/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/tags").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/{id}").permitAll()

                        // ---- 公开: 旅游浏览 ----
                        .requestMatchers(HttpMethod.GET, "/api/v1/tourism/spots").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tourism/spots/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tourism/spots/{spotId}/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tourism/spots/{spotId}/tickets").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tourism/routes/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tourism/routes/{id}").permitAll()

                        // ---- 半公开: 金融产品浏览（需登录但不限角色） ----
                        .requestMatchers(HttpMethod.GET, "/api/v1/finance/loan-products").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/finance/insurance-products").authenticated()

                        // ---- 金融模块（需登录，角色由 Service 层检查） ----
                        .requestMatchers("/api/v1/finance/**").authenticated()

                        // ---- 公开: Swagger / OpenAPI 文档 ----
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // ---- 公开: 上传文件的静态资源访问 ----
                        .requestMatchers("/uploads/**").permitAll()

                        // ---- 管理员专属 ----
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ---- 其他接口需要登录 ----
                        .anyRequest().authenticated()
                )

                // 未认证时返回 JSON 错误（而不是重定向到登录页）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            new ObjectMapper().writeValue(response.getWriter(),
                                    Map.of("code", 401, "message", "未登录或Token已过期"));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            new ObjectMapper().writeValue(response.getWriter(),
                                    Map.of("code", 403, "message", "无权限访问"));
                        })
                )

                // 在 UsernamePasswordAuthenticationFilter 之前插入 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码加密器
     * BCrypt 是业界推荐的密码哈希算法，自动加盐，安全性高
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
