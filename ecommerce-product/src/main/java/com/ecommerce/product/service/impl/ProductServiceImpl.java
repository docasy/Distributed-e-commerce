package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResultCode;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Lua 脚本：原子检查库存并扣减
     * 返回 1=成功, 0=库存不足, -1=Key不存在
     */
    private final DefaultRedisScript<Long> deductStockScript;

    public ProductServiceImpl() {
        String lua =
            "local v = redis.call('GET', KEYS[1])\n" +
            "if v == false then return -1 end\n" +
            "if tonumber(v) >= tonumber(ARGV[1]) then\n" +
            "    redis.call('DECRBY', KEYS[1], ARGV[1])\n" +
            "    return 1\n" +
            "end\n" +
            "return 0";
        deductStockScript = new DefaultRedisScript<>(lua, Long.class);
    }

    @Override
    public Page<Product> getProductPage(Integer pageNum, Integer pageSize, String keyword) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        
        // 使用 MySQL LIKE 查询 性能差
        if (keyword != null && !keyword.isEmpty()) {  // 关键词不为空，添加模糊查询条件
            wrapper.like(Product::getName, keyword) // 根据商品名称模糊查询
                   .or()
                   .like(Product::getTitle, keyword); 
        }
        
        wrapper.eq(Product::getStatus, 1)
               .orderByDesc(Product::getCreateTime);
        
        return productMapper.selectPage(page, wrapper);
    }

    @Override
    public Product getProductById(Long productId) {
        // 先从Redis查询
        String key = RedisKeyConstant.PRODUCT_INFO_PREFIX + productId;
        Product product = (Product) redisTemplate.opsForValue().get(key);
        
        if (product == null) {
            // Redis没有，从数据库查询
            product = productMapper.selectById(productId);
            if (product != null) {
                // 缓存到Redis（30分钟）
                redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
            }
        }
        
        return product;
    }

    @Override
    public List<Product> getProductByIds(List<Long> productIds) {
        return productMapper.selectBatchIds(productIds);
    }

    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        String stockKey = RedisKeyConstant.PRODUCT_STOCK_PREFIX + productId;

        // 1. Redis Lua 原子扣减：单条命令完成"检查+扣减"，Redis单线程天然串行，无需分布式锁
        Long luaResult = redisTemplate.execute(
                deductStockScript,
                Collections.singletonList(stockKey),
                quantity);

        // 2. 库存Key不存在，从DB加载到Redis后重试
        if (luaResult == null || luaResult == -1) {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                redisTemplate.opsForValue()
                        .setIfAbsent(stockKey, product.getStock(), 30, TimeUnit.MINUTES);
            }
            luaResult = redisTemplate.execute(
                    deductStockScript,
                    Collections.singletonList(stockKey),
                    quantity);
        }

        // 3. 库存不足
        if (luaResult == null || luaResult == 0) {
            throw new BusinessException(ResultCode.PRODUCT_STOCK_NOT_ENOUGH);
        }

        // 4. 同步落库（MySQL行锁+WHERE stock>=? 双重保障）
        int dbResult = productMapper.deductStock(productId, quantity);
        if (dbResult <= 0) {
            // DB校验失败，回滚Redis
            redisTemplate.opsForValue().increment(stockKey, quantity);
            throw new BusinessException(ResultCode.PRODUCT_STOCK_NOT_ENOUGH);
        }

        // 5. 删除商品缓存
        redisTemplate.delete(RedisKeyConstant.PRODUCT_INFO_PREFIX + productId);

        log.info("扣减库存成功：商品ID={}, 数量={}", productId, quantity);
        return true;
    }

    @Override
    public boolean addStock(Long productId, Integer quantity) {
        return addStock(productId, quantity, null);
    }

    @Override
    public boolean addStock(Long productId, Integer quantity, String orderNo) {
        // 幂等校验：同一笔订单的恢复库存只执行一次
        if (orderNo != null) {
            String restoreKey = "restore:" + orderNo;
            Boolean firstTime = redisTemplate.opsForValue()
                    .setIfAbsent(restoreKey, "1", 7, TimeUnit.DAYS);
            if (!Boolean.TRUE.equals(firstTime)) {
                log.info("订单{}已恢复过库存，跳过", orderNo);
                return true;
            }
        }

        int result = productMapper.addStock(productId, quantity);

        if (result > 0) {
            String stockKey = RedisKeyConstant.PRODUCT_STOCK_PREFIX + productId;
            redisTemplate.opsForValue().increment(stockKey, quantity);

            String productKey = RedisKeyConstant.PRODUCT_INFO_PREFIX + productId;
            redisTemplate.delete(productKey);

            log.info("增加库存成功：商品ID={}, 数量={}, 订单={}", productId, quantity, orderNo);
            return true;
        }

        return false;
    }

    @Override
    public boolean checkStock(Long productId, Integer quantity) {
        Product product = getProductById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        return product.getStock() >= quantity;
    }
}
