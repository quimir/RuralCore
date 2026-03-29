package com.example.rural.config;

import com.example.rural.entity.User;
import com.example.rural.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化
 *
 * 应用启动时自动执行：
 * - 检查是否存在 admin 账号
 * - 不存在则自动创建默认管理员（用户名: admin, 密码: admin123）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .nickname("系统管理员")
                    .role(User.Role.ADMIN)
                    .status(1)
                    .build();
            userRepository.save(admin);
            log.info("======= 已创建默认管理员账号 =======");
            log.info("  用户名: admin");
            log.info("  密码:   admin123");
            log.info("====================================");
        }

        // 金融服务商测试账号
        if (!userRepository.existsByUsername("bank01")) {
            User bank = User.builder()
                    .username("bank01")
                    .password(passwordEncoder.encode("123456"))
                    .nickname("农村信用社牟平支行")
                    .role(User.Role.FINANCIAL_PROVIDER)
                    .orgName("农村信用社牟平支行")
                    .licenseNo("FIN-2024-001")
                    .status(1)
                    .build();
            userRepository.save(bank);
            log.info("已创建金融服务商测试账号: bank01 / 123456");
        }

        // 保险提供商测试账号
        if (!userRepository.existsByUsername("insure01")) {
            User insurer = User.builder()
                    .username("insure01")
                    .password(passwordEncoder.encode("123456"))
                    .nickname("中国人保财险烟台分公司")
                    .role(User.Role.INSURANCE_PROVIDER)
                    .orgName("中国人保财险烟台分公司")
                    .licenseNo("INS-2024-001")
                    .status(1)
                    .build();
            userRepository.save(insurer);
            log.info("已创建保险提供商测试账号: insure01 / 123456");
        }
    }
}
