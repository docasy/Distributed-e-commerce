package com.ecommerce.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@FeignClient(name = "ecommerce-user", path = "/user")
public interface UserFeignClient {

    @GetMapping("/address/{id}")
    Map<String, Object> getAddressById(@PathVariable("id") Long id);
}
