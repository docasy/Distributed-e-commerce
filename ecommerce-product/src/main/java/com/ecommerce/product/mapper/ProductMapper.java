package com.ecommerce.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 扣减库存（使用乐观锁）
     */
    @Update("UPDATE tb_product SET stock = stock - #{quantity}, sales = sales + #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity} AND deleted = 0")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 增加库存
     */
    @Update("UPDATE tb_product SET stock = stock + #{quantity}, sales = sales - #{quantity} " +
            "WHERE id = #{productId} AND deleted = 0")
    int addStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
