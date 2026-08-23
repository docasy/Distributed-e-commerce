package com.ecommerce.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "ecommerce-product", path = "/product")
public interface ProductFeignClient {

    @GetMapping("/{id}")
    Map<String, Object> getProductById(@PathVariable("id") Long id);

    @GetMapping("/internal/check-stock/{productId}/{quantity}")
    Map<String, Object> checkStock(@PathVariable("productId") Long productId,
                                   @PathVariable("quantity") Integer quantity);

    @PostMapping("/internal/deduct-stock/{productId}/{quantity}")
    Map<String, Object> deductStock(@PathVariable("productId") Long productId,
                                    @PathVariable("quantity") Integer quantity);

    @PostMapping("/internal/add-stock/{productId}/{quantity}/{orderNo}")
    Map<String, Object> addStock(@PathVariable("productId") Long productId,
                                 @PathVariable("quantity") Integer quantity,
                                 @PathVariable("orderNo") String orderNo);
}
