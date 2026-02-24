package com.ecommerce.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResultCode;
import com.ecommerce.order.dto.CreateOrderDTO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.feign.ProductFeignClient;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.mq.OrderMessageProducer;
import com.ecommerce.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private OrderMessageProducer orderMessageProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, CreateOrderDTO createOrderDTO) {
        // 1. 幂等性校验
        String tokenKey = RedisKeyConstant.buildIdempotentTokenKey(createOrderDTO.getIdempotentToken());
        Boolean deleted = redisTemplate.delete(tokenKey);
        if (!Boolean.TRUE.equals(deleted)) {
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST);
        }

        // 2. 查询商品信息（通过Feign调用商品服务）
        Map<String, Object> productResult = productFeignClient.getProductById(createOrderDTO.getProductId());
        if (productResult == null || productResult.get("data") == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) productResult.get("data");
        BigDecimal price = new BigDecimal(product.get("price").toString());
        String productName = product.get("name").toString();

        // 3. 检查库存
        Boolean stockEnough = productFeignClient.checkStock(
                createOrderDTO.getProductId(), 
                createOrderDTO.getQuantity()
        );
        if (!Boolean.TRUE.equals(stockEnough)) {
            throw new BusinessException(ResultCode.PRODUCT_STOCK_NOT_ENOUGH);
        }

        // 4. 扣减库存
        Boolean deductResult = productFeignClient.deductStock(
                createOrderDTO.getProductId(), 
                createOrderDTO.getQuantity()
        );
        if (!Boolean.TRUE.equals(deductResult)) {
            throw new BusinessException(ResultCode.PRODUCT_STOCK_NOT_ENOUGH);
        }

        // 5. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(createOrderDTO.getProductId());
        order.setProductName(productName);
        order.setProductPrice(price);
        order.setQuantity(createOrderDTO.getQuantity());
        order.setTotalAmount(price.multiply(new BigDecimal(createOrderDTO.getQuantity())));
        order.setStatus(0); // 待支付
        order.setAddress(createOrderDTO.getAddress());
        order.setReceiver(createOrderDTO.getReceiver());
        order.setReceiverPhone(createOrderDTO.getReceiverPhone());
        order.setRemark(createOrderDTO.getRemark());

        int result = orderMapper.insert(order);
        if (result <= 0) {
            throw new BusinessException("创建订单失败");
        }

        // 6. 发送延迟消息（30分钟后检查订单状态，未支付则自动取消）
        orderMessageProducer.sendOrderTimeoutMessage(order.getOrderNo(), 30 * 60 * 1000);

        log.info("订单创建成功：{}", order.getOrderNo());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }

        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 更新订单状态为已支付
        order.setStatus(1);
        order.setPaymentTime(LocalDateTime.now());
        int result = orderMapper.updateById(order);

        if (result > 0) {
            log.info("订单支付成功：{}", orderNo);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }

        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_CANCEL_FAIL.getCode(), "只能取消待支付订单");
        }

        // 更新订单状态为已取消
        order.setStatus(2);
        int result = orderMapper.updateById(order);

        if (result > 0) {
            // 恢复库存
            productFeignClient.addStock(order.getProductId(), order.getQuantity());
            log.info("订单取消成功：{}", orderNo);
            return true;
        }

        return false;
    }

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        return orderMapper.selectOne(wrapper);
    }

    @Override
    public Page<Order> getUserOrders(Long userId, Integer pageNum, Integer pageSize) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(page, wrapper);
    }

    @Override
    public String generateIdempotentToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenKey = RedisKeyConstant.buildIdempotentTokenKey(token);
        // Token有效期5分钟
        redisTemplate.opsForValue().set(tokenKey, userId, 5, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return IdUtil.getSnowflakeNextIdStr();
    }
}
