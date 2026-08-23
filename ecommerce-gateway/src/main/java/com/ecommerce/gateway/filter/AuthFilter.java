package com.ecommerce.gateway.filter;

import com.ecommerce.common.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<Object> {

    // 白名单：不需要鉴权的路径
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/user/register",
            "/product/page",
            "/product/\\d+"  // 商品详情（正则）
    );

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            log.debug("请求路径：{}", path);

            // 检查是否在白名单中
            if (isInWhiteList(path)) {
                return chain.filter(exchange);
            }

            // 获取Token
            String token = request.getHeaders().getFirst("Authorization");
            if (token == null || token.isEmpty()) {
                return unauthorized(exchange.getResponse(), "未授权，请先登录");
            }

            // 验证Token
            try {
                boolean valid = JwtUtil.validateToken(token);
                if (!valid) {
                    return unauthorized(exchange.getResponse(), "Token无效或已过期");
                }

                // Token有效，继续执行
                return chain.filter(exchange);

            } catch (Exception e) {
                log.error("Token验证失败：{}", e.getMessage());
                return unauthorized(exchange.getResponse(), "Token验证失败");
            }
        };
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isInWhiteList(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> {
            if (pattern.contains("\\d+")) {
                return path.matches(pattern);
            }
            return path.contains(pattern);
        });
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        
        String result = String.format(
                "{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                message, System.currentTimeMillis()
        );
        
        DataBuffer buffer = response.bufferFactory()
                .wrap(result.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
}
