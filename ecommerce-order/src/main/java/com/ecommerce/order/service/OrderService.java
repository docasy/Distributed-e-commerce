package com.ecommerce.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.order.dto.CreateOrderDTO;
import com.ecommerce.order.entity.Order;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单（带幂等性校验）
     */
    Order createOrder(Long userId, CreateOrderDTO createOrderDTO);

    /**
     * 支付订单
     */
    boolean payOrder(String orderNo);

    /**
     * 取消订单
     */
    boolean cancelOrder(String orderNo);

    /**
     * 查询订单
     */
    Order getOrderByOrderNo(String orderNo);

    /**
     * 分页查询用户订单
     */
    Page<Order> getUserOrders(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 生成幂等性Token
     */
    String generateIdempotentToken(Long userId);
}
