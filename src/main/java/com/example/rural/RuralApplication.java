package com.example.rural;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 启动类
 *
 * @EnableJpaRepositories 显式指定 repository 包由 JPA 管理，
 * 避免同时引入 spring-data-redis 时 Spring Data 不知道
 * Repository 该分配给 JPA 还是 Redis 的警告
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.rural.repository")
public class RuralApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuralApplication.class, args);
    }
}
