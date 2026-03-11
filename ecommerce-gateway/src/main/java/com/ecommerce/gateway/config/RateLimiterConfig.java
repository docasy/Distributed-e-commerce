package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * 限流配置
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 基于IP的限流Key解析器
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() { // ← 解析请求的IP地址作为限流Key
        return exchange -> Mono.just(
                exchange.getRequest()   // 获取请求的IP地址
                        .getRemoteAddress() // 获取IP地址字符串
                        .getAddress()   // 获取InetAddress对象
                        .getHostAddress()   // 获取IP地址字符串
        );
    }

    /**
     * 基于用户的限流Key解析器
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest()
                        .getHeaders()
                        .getFirst("Authorization") != null  
                        ? exchange.getRequest().getHeaders().getFirst("Authorization") 
                        : "anonymous" 
        );
    }
}
