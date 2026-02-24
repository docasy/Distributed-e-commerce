package com.ecommerce.common.constant;

/**
 * Redis Key常量
 */
public class RedisKeyConstant {

    // 用户相关
    public static final String USER_TOKEN_PREFIX = "user:token:";
    public static final String USER_INFO_PREFIX = "user:info:";
    
    // 商品相关
    public static final String PRODUCT_INFO_PREFIX = "product:info:";
    public static final String PRODUCT_STOCK_PREFIX = "product:stock:";
    public static final String PRODUCT_SEARCH_PREFIX = "product:search:";
    
    // 购物车
    public static final String CART_PREFIX = "cart:";
    
    // 订单相关
    public static final String ORDER_PREFIX = "order:";
    public static final String ORDER_TIMEOUT_PREFIX = "order:timeout:";
    
    // 分布式锁
    public static final String LOCK_STOCK_PREFIX = "lock:stock:";
    public static final String LOCK_ORDER_PREFIX = "lock:order:";
    
    // 幂等性Token
    public static final String IDEMPOTENT_TOKEN_PREFIX = "idempotent:";

    /**
     * 构建用户Token Key
     */
    public static String buildUserTokenKey(Long userId) {
        return USER_TOKEN_PREFIX + userId;
    }

    /**
     * 构建商品库存锁Key
     */
    public static String buildStockLockKey(Long productId) {
        return LOCK_STOCK_PREFIX + productId;
    }

    /**
     * 构建购物车Key
     */
    public static String buildCartKey(Long userId) {
        return CART_PREFIX + userId;
    }

    /**
     * 构建幂等性Token Key
     */
    public static String buildIdempotentTokenKey(String token) {
        return IDEMPOTENT_TOKEN_PREFIX + token;
    }
}
