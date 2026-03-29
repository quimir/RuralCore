package com.example.rural.filter;

import com.example.rural.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * 执行时机：每个 HTTP 请求到达 Controller 之前
 *
 * 工作流程：
 * 1. 从请求头 Authorization 中提取 "Bearer xxx" Token
 * 2. 调用 JwtUtil 校验 Token 是否有效
 * 3. 如果有效，将用户信息放入 Spring Security 上下文
 * 4. 后续的权限判断由 Spring Security 自动完成
 *
 * 对于公开接口（如登录、注册），即使没有 Token 也会放行，
 * 由 SecurityConfig 中的 permitAll() 配置决定
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 提取 Token
        String token = extractToken(request);

        // 2. 校验并设置认证信息
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                // 根据 Token 中的角色设置 Spring Security 权限
                // ROLE_USER: 通用已登录权限（所有角色都有）
                // ROLE_ADMIN / ROLE_MERCHANT / ROLE_FARMER / ROLE_TOURIST: 具体角色权限
                List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                // 将真实角色（ADMIN/MERCHANT/FARMER/TOURIST）也放入权限列表
                // 这样 Controller 可以通过 extractRole() 拿到真实角色
                if (role != null && !role.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,                  // principal: 用户ID
                                null,                    // credentials: 不需要
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT 认证成功: userId={}, username={}, role={}", userId, username, role);

            } catch (Exception e) {
                log.warn("JWT 解析失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // 3. 继续过滤器链（无论认证是否成功都要放行，由 Security 配置决定是否拒绝）
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer Token
     *
     * 前端请求格式: Authorization: Bearer eyJhbGciOiJ...
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
