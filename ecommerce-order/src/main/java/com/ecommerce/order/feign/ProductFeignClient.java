package com.ecommerce.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商品服务Feign客户端
 */
@FeignClient(name = "ecommerce-product", path = "/product")
public interface ProductFeignClient {

    /**
     * 根据ID查询商品
     */
    @GetMapping("/{id}")
    Map<String, Object> getProductById(@PathVariable("id") Long id);

    /**
     * 检查库存
     */
    @GetMapping("/internal/check-stock/{productId}/{quantity}")
    Boolean checkStock(@PathVariable("productId") Long productId, 
                      @PathVariable("quantity") Integer quantity);

    /**
     * 扣减库存
     */
    @PostMapping("/internal/deduct-stock/{productId}/{quantity}")
    Boolean deductStock(@PathVariable("productId") Long productId, 
                       @PathVariable("quantity") Integer quantity);

    /**
     * 增加库存
     */
    @PostMapping("/internal/add-stock/{productId}/{quantity}")
    Boolean addStock(@PathVariable("productId") Long productId, 
                    @PathVariable("quantity") Integer quantity);
}
