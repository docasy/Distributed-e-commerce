package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResultCode;
import com.ecommerce.common.utils.RedisLockUtil;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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

    @Autowired
    private RedisLockUtil redisLockUtil;

    @Override
    public Page<Product> getProductPage(Integer pageNum, Integer pageSize, String keyword) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getName, keyword)
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
        // 获取分布式锁（防止超卖）
        String lockKey = RedisKeyConstant.buildStockLockKey(productId);
        String lockValue = redisLockUtil.tryLockWithRetry(lockKey, 10, 3, 100);
        
        if (lockValue == null) {
            throw new BusinessException(ResultCode.ACQUIRE_LOCK_FAIL);
        }
        
        try {
            // 先检查Redis中的库存
            String stockKey = RedisKeyConstant.PRODUCT_STOCK_PREFIX + productId;
            Integer redisStock = (Integer) redisTemplate.opsForValue().get(stockKey);
            
            if (redisStock != null) {
                if (redisStock < quantity) {
                    throw new BusinessException(ResultCode.PRODUCT_STOCK_NOT_ENOUGH);
                }
                // 扣减Redis库存
                redisTemplate.opsForValue().decrement(stockKey, quantity);
            }
            
            // 扣减数据库库存
            int result = productMapper.deductStock(productId, quantity);
            if (result <= 0) {
                // 回滚Redis库存
                if (redisStock != null) {
                    redisTemplate.opsForValue().increment(stockKey, quantity);
                }
                throw new BusinessException(ResultCode.PRODUCT_STOCK_NOT_ENOUGH);
            }
            
            // 删除商品缓存
            String productKey = RedisKeyConstant.PRODUCT_INFO_PREFIX + productId;
            redisTemplate.delete(productKey);
            
            log.info("扣减库存成功：商品ID={}, 数量={}", productId, quantity);
            return true;
            
        } finally {
            // 释放锁
            redisLockUtil.unlock(lockKey, lockValue);
        }
    }

    @Override
    public boolean addStock(Long productId, Integer quantity) {
        int result = productMapper.addStock(productId, quantity);
        
        if (result > 0) {
            // 更新Redis库存
            String stockKey = RedisKeyConstant.PRODUCT_STOCK_PREFIX + productId;
            redisTemplate.opsForValue().increment(stockKey, quantity);
            
            // 删除商品缓存
            String productKey = RedisKeyConstant.PRODUCT_INFO_PREFIX + productId;
            redisTemplate.delete(productKey);
            
            log.info("增加库存成功：商品ID={}, 数量={}", productId, quantity);
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
