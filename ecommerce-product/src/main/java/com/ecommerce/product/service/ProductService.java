package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.product.entity.Product;

import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 分页查询商品
     */
    Page<Product> getProductPage(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 根据ID查询商品
     */
    Product getProductById(Long productId);

    /**
     * 批量查询商品
     */
    List<Product> getProductByIds(List<Long> productIds);

    /**
     * 扣减库存（带分布式锁）
     */
    boolean deductStock(Long productId, Integer quantity);

    /**
     * 增加库存
     */
    boolean addStock(Long productId, Integer quantity);

    /**
     * 检查库存是否充足
     */
    boolean checkStock(Long productId, Integer quantity);
}
