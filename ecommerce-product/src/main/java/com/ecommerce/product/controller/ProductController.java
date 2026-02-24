package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 */
@Slf4j
@RestController
@RequestMapping("/product")
@Api(tags = "商品管理")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 分页查询商品
     */
    @GetMapping("/page")
    @ApiOperation("分页查询商品")
    public Result<Page<Product>> getProductPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        Page<Product> page = productService.getProductPage(pageNum, pageSize, keyword);
        return Result.success(page);
    }

    /**
     * 根据ID查询商品
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询商品")
    public Result<Product> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return Result.success(product);
    }

    /**
     * 批量查询商品（内部调用）
     */
    @PostMapping("/internal/batch")
    @ApiOperation(value = "批量查询商品", hidden = true)
    public Result<List<Product>> getProductByIds(@RequestBody List<Long> productIds) {
        List<Product> products = productService.getProductByIds(productIds);
        return Result.success(products);
    }

    /**
     * 检查库存（内部调用）
     */
    @GetMapping("/internal/check-stock/{productId}/{quantity}")
    @ApiOperation(value = "检查库存", hidden = true)
    public Result<Boolean> checkStock(@PathVariable Long productId, @PathVariable Integer quantity) {
        boolean result = productService.checkStock(productId, quantity);
        return Result.success(result);
    }

    /**
     * 扣减库存（内部调用）
     */
    @PostMapping("/internal/deduct-stock/{productId}/{quantity}")
    @ApiOperation(value = "扣减库存", hidden = true)
    public Result<Boolean> deductStock(@PathVariable Long productId, @PathVariable Integer quantity) {
        boolean result = productService.deductStock(productId, quantity);
        return Result.success(result);
    }

    /**
     * 增加库存（内部调用）
     */
    @PostMapping("/internal/add-stock/{productId}/{quantity}")
    @ApiOperation(value = "增加库存", hidden = true)
    public Result<Boolean> addStock(@PathVariable Long productId, @PathVariable Integer quantity) {
        boolean result = productService.addStock(productId, quantity);
        return Result.success(result);
    }
}
