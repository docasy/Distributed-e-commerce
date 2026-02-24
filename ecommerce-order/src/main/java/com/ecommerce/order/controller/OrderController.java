package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.utils.JwtUtil;
import com.ecommerce.order.dto.CreateOrderDTO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/order")
@Api(tags = "订单管理")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 生成幂等性Token
     */
    @GetMapping("/idempotent-token")
    @ApiOperation("生成幂等性Token")
    public Result<String> generateIdempotentToken(@RequestHeader("Authorization") String token) {
        Long userId = JwtUtil.getUserId(token);
        String idempotentToken = orderService.generateIdempotentToken(userId);
        return Result.success(idempotentToken);
    }

    /**
     * 创建订单
     */
    @PostMapping("/create")
    @ApiOperation("创建订单")
    public Result<Order> createOrder(
            @RequestHeader("Authorization") String token,
            @Validated @RequestBody CreateOrderDTO createOrderDTO) {
        Long userId = JwtUtil.getUserId(token);
        Order order = orderService.createOrder(userId, createOrderDTO);
        return Result.success(order);
    }

    /**
     * 支付订单
     */
    @PostMapping("/pay/{orderNo}")
    @ApiOperation("支付订单")
    public Result<Void> payOrder(@PathVariable String orderNo) {
        boolean result = orderService.payOrder(orderNo);
        return result ? Result.success("支付成功", null) : Result.error("支付失败");
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{orderNo}")
    @ApiOperation("取消订单")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        boolean result = orderService.cancelOrder(orderNo);
        return result ? Result.success("取消成功", null) : Result.error("取消失败");
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderNo}")
    @ApiOperation("查询订单详情")
    public Result<Order> getOrder(@PathVariable String orderNo) {
        Order order = orderService.getOrderByOrderNo(orderNo);
        return Result.success(order);
    }

    /**
     * 分页查询我的订单
     */
    @GetMapping("/my-orders")
    @ApiOperation("分页查询我的订单")
    public Result<Page<Order>> getMyOrders(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = JwtUtil.getUserId(token);
        Page<Order> page = orderService.getUserOrders(userId, pageNum, pageSize);
        return Result.success(page);
    }
}
